package com.apm70.fileq.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BlockingThreadPoolExecutor extends ThreadPoolExecutor {

    public BlockingThreadPoolExecutor(
            final int corePoolSize,
            final int maximumPoolSize,
            final long keepAliveTime,
            final TimeUnit unit,
            final BlockingQueue<Runnable> workQueue,
            final ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    protected void beforeExecute(final Runnable r) {
        while (this.getQueue().remainingCapacity() == 0) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
            }
        }
    }
}
