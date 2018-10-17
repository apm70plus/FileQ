package com.leading.lq;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.apm70.fileq.util.PersistenceQueue;

/**
 * 基于文件系统的持久化队列测试
 * @author liuyg
 *
 */
public class FileStoreQueueTest {

    private static PersistenceQueue<String> queue;

    static {
        try {
            final File store = new File("/tmp/store.dat");
            final File meta = new File("/tmp/meta.dat");
            FileStoreQueueTest.queue = new PersistenceQueue<>(meta, store, String.class);
        } catch (final Exception e) {
        }
    }

    @Test
    public void testAdd() throws InterruptedException {
        final String msg = "文件存储队列的持久化消息测试：";
        for (int i = 1; i <= 10; i++) {
            FileStoreQueueTest.queue.add(msg + i);
        }
        Assert.assertEquals(10, queue.size());
        for (int i = 1; i <= 10; i++) {
        		String v = FileStoreQueueTest.queue.poll();
        		Assert.assertEquals(msg + i, v);
        }
        Assert.assertEquals(0, queue.size());
    }

    @Test
    public void testPoll() throws InterruptedException {
        final String msg = "文件存储队列的持久化消息测试：";
        for (int i = 1; i <= 10; i++) {
            FileStoreQueueTest.queue.add(msg + i);
            final String value = FileStoreQueueTest.queue.poll();
            Assert.assertEquals(msg + i, value);
        }
    }
}
