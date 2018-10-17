package com.apm70.fileq.util;

/**
 * 限流器
 *
 * @author liuyg
 */
public class FlowLimiter {
    /** 10M的容量缓冲区 */
    private final long CAPACITY;
    /** 流量速率（KB/秒） */
    private final long flowSpeed;
    private long lastInflowTime;
    private long limit;

    public FlowLimiter(final long flowSpeed) {
        this.flowSpeed = flowSpeed;
        this.CAPACITY = flowSpeed * 1024 * 5;
        this.limit = this.CAPACITY;
    }

    public void flowIn(long bytes) {
        this.flowOut();
        // 流量超了，需要阻塞
        while (bytes > this.limit) {
            bytes -= this.limit;
            this.limit = 0;
            try {
                Thread.sleep(5);
            } catch (final InterruptedException e) {
            }
            this.flowOut();
        }
        this.limit -= bytes;
    }

    private void flowOut() {
        final long currentTime = System.currentTimeMillis();
        final long flowOut = (currentTime - this.lastInflowTime) * this.flowSpeed;
        this.limit += flowOut;
        if (this.limit > this.CAPACITY) {
            this.limit = this.CAPACITY;
        }
        this.lastInflowTime = currentTime;
    }
}
