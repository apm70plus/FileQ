package com.apm70.fileq.client.monitor;

import java.util.concurrent.ArrayBlockingQueue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.apm70.fileq.client.conn.Connection;
import com.apm70.fileq.client.conn.ConnectionManager;
import com.apm70.fileq.config.Configuration;
import com.apm70.fileq.protocol.message.LogReport;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.util.LQWaiter;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public class TcpSocketAppender<Event extends DeferredProcessingAware> extends UnsynchronizedAppenderBase<Event> {

    private static final int WAIT_TIMEOUT = 10;
    private Connection conn;
    private final ArrayBlockingQueue<String> logs = new ArrayBlockingQueue<>(500);
    private long latestReportTime;
    private final LQWaiter logWaiter = new LQWaiter("TcpSocketAppender");

    private Layout<Event> layout;

    public void setEncoder(final Encoder<Event> encoder) {
        if (encoder instanceof LayoutWrappingEncoder) {
            this.layout = ((LayoutWrappingEncoder<Event>) encoder).getLayout();
        }
    }

    @Override
    protected void append(final Event event) {
        event.prepareForDeferredProcessing();
        String log = null;
        if (this.layout != null) {
            log = this.layout.doLayout(event);
        } else {
            log = event.toString();
        }
        try {
            this.logs.put(log);
            this.logWaiter.signal();
        } catch (final Exception e) {
        }
    }

    @Override
    public void start() {
        super.start();
        this.latestReportTime = System.currentTimeMillis();
        new Thread(() -> {
            while (this.isStarted()) {
                try {
                    this.tryToReport();
                } catch (final Exception e) {
                }
            }
        }, "TcpSocketAppender-thread").start();
    }

    private void tryToReport() throws Exception {
        final Connection conn = this.getConn();
        if (conn == null) {// 未建立连接，需要关注队列是否已满
            if (this.logs.remainingCapacity() == 0) {
                // 队列已满，抛弃最早的日志
                for (int i = 0; i < 10; i++) {
                    this.logs.poll();
                }
            }
            this.logWaiter.await(WAIT_TIMEOUT);
        }
        final int waitingSeconds = this.waitingSeconds();
        if ((waitingSeconds <= 0) || (this.logs.size() >= 100)) {
            final JSONArray array = new JSONArray();
            for (int i = 0; i < this.logs.size(); i++) {
                array.add(this.logs.poll());
            }
            this.latestReportTime = System.currentTimeMillis();
            if (conn != null) {
                final ProtocolMessage msg = new ProtocolMessage();
                final LogReport body = new LogReport();
                body.setLogs(JSON.toJSONBytes(array));
                msg.setBody(body);
                conn.writeWithoutAck(msg);
            }
        } else {
            if (this.logs.size() == 0) {
                this.logWaiter.await();
            } else {
                this.logWaiter.await(waitingSeconds);
            }
        }
    }

    private Connection getConn() {
        if (this.conn != null) {
            return this.conn;
        }
        if (this.conn == null) {
            final ConnectionManager connManager = ConnectionManager.instance();
            final Configuration config = connManager.getConfig();
            if (config == null || !config.isMonitorReport()) {
                return null;
            }
            this.conn = connManager.getConnection(config.getReportHost(), config.getReportPort());
        }
        if (!this.conn.isConnected()) {
            try {
                this.conn.connect();
            } catch (final Exception e) {
                // 连接失败，放弃上报
                e.printStackTrace();
                return null;
            }
        }
        return this.conn;
    }

    private int waitingSeconds() {
        return WAIT_TIMEOUT - ((int) (System.currentTimeMillis() - this.latestReportTime) / 1000);
    }

    @Override
    public void stop() {
        this.started = false;
        this.logWaiter.signal();
    }
}
