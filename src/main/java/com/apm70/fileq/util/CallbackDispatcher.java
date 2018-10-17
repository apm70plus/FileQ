package com.apm70.fileq.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallbackDispatcher implements Destroyable {

    @Setter
    private long expiredInterval = 120 * 1000L;
    private final BlockingThreadPoolExecutor workExecutor = new BlockingThreadPoolExecutor(4, 128, 1, TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(500), new TaskThreadFactory("CallbackDispatcher-Worker-"));
    private final ConcurrentHashMap<CallbackRoutingKey, Consumer<Object>> callbacks = new ConcurrentHashMap<>();

    public CallbackDispatcher() {
        this.startExpiringChecking();
    }

    private void startExpiringChecking() {
        this.workExecutor.execute(() -> {
            while (true) {
                try {
                    this.callbacks.keySet().stream().filter(CallbackRoutingKey::isExpired).forEach(key -> {
                        final Object defaultValue = key.getExpiredDefaultValue();
                        CallbackDispatcher.log.warn("执行超时回调！回调key：{}", key);
                        this.callbacks.remove(key).accept(defaultValue);
                    });
                    Thread.sleep(2000L);
                } catch (final Throwable e) {
                    CallbackDispatcher.log.error("回调超时检查处理发生异常", e);
                }
            }
        });
    }

    public void registerCallback(final Object key, final Consumer<Object> callback, final Object expiredDefaultValue) {
        this.callbacks.put(
                new CallbackRoutingKey(key, System.currentTimeMillis() + this.expiredInterval, expiredDefaultValue),
                callback);
    }

    public void fireCallback(final Object key, final Object msg) {
        final Consumer<Object> callback = this.callbacks.remove(new CallbackRoutingKey(key, 0L, null));
        if (callback == null) {
            CallbackDispatcher.log.warn("未找到回调函数，可能是注册的回调已经超时！回调key：{}", key);
            return;
        }
        this.workExecutor.execute(() -> {
            try {
                CallbackDispatcher.log.debug("执行回调！回调key：{}", key);
                callback.accept(msg);
            } catch (final Throwable e) {
                CallbackDispatcher.log.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void destroy() {
        this.workExecutor.shutdown();
    }

    class CallbackRoutingKey {
        @Getter
        private final Object id;
        @Getter
        private final Object expiredDefaultValue;
        private final long expiredTime;

        public CallbackRoutingKey(final Object id, final long expiredTime, final Object expiredDefaultValue) {
            this.id = id;
            this.expiredTime = expiredTime;
            this.expiredDefaultValue = expiredDefaultValue;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > this.expiredTime;
        }

        @Override
        public boolean equals(final Object v) {
            if (v == null) {
                return false;
            }
            if (v instanceof CallbackRoutingKey) {
                return this.id.equals(((CallbackRoutingKey) v).id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        @Override
        public String toString() {
            return "{ id=" + this.id + ",expiredTime=" + this.expiredTime + " }";
        }

    }
}
