package com.apm70.fileq.server.topic;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.apm70.fileq.util.Destroyable;
import com.apm70.fileq.util.PersistanceCounter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 订阅消息的持久化队列</br>
 * 队列存储结构为 链表式的分段文件，每个文件存储65536条消息。可以基于消息索引反复读取未销毁的消息。
 *
 * @author liuyg
 */
@Slf4j
public class TopicMessageQueue implements Destroyable {
    @Setter
    private TopicClearStrategy clearStrategy;
    private Timer cleanTimer;
    private final String storeDir;
    private TopicFragment activeFragment;
    private PersistanceCounter count;
    private final Cache<TopicFragmentIndex, TopicFragment> fragmentCache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.SECONDS).initialCapacity(100).maximumSize(200).build();

    public TopicMessageQueue(final String storeDir) throws IOException {
        this.storeDir = storeDir;
        this.init();
    }

    /**
     * 添加消息
     *
     * @param bean
     * @throws IOException
     */
    public long add(final TopicMessageBean bean) throws IOException {
        synchronized (this.count) {
            this.getActiveFragment().add(bean);
            return this.count.getAndIncrement();
        }
    }

    /**
     * 根据索引获取消息，如果索引无效，则返回null
     *
     * @param index
     * @return
     * @throws IOException
     */
    public TopicMessageBean get(final long index) throws IOException {
        final TopicFragment fragment = this.findFragment(index);
        return fragment != null ? fragment.get(index) : null;
    }

    private TopicFragment getActiveFragment() throws IOException {
        if (this.activeFragment.isFull()) {
            this.activeFragment = this.activeFragment.nextFragment();
            this.activeFragment.build();
        }
        return this.activeFragment;
    }

    private TopicFragment findFragment(final long index) throws IOException {
        if (this.activeFragment.contains(index)) {
            return this.activeFragment;
        }
        TopicFragment fragment = this.fragmentCache.getIfPresent(new TopicFragmentIndex(index));
        if (fragment != null) {
            return fragment;
        }
        fragment = new TopicFragment(this.storeDir, index);
        if (!fragment.exists()) {
            return null;
        }
        this.fragmentCache.put(fragment.getFragmentIndex(), fragment);
        return fragment;
    }

    private void init() throws IOException {
        final File dir = new File(this.storeDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.count = new PersistanceCounter(this.storeDir);
        this.activeFragment = new TopicFragment(this.storeDir, this.count.get());
        if (!this.activeFragment.exists()) {
            this.activeFragment.build();
        }
        this.cleanTimer = new Timer("ClearTopicMsgThread");
        this.cleanTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    TopicMessageQueue.this.clearStrategy.clear();
                } catch (final Throwable e) {
                    TopicMessageQueue.log.error("订阅消息清理时发生异常", e);
                }
            }

        }, 60000L, 60000L * 60 * 24);
    }

    @Override
    public void destroy() {
        this.cleanTimer.cancel();
    }
}
