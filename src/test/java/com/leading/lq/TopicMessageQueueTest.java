package com.leading.lq;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.server.topic.TopicMessageQueue;

public class TopicMessageQueueTest {

    private TopicMessageQueue queue;

    @Before
    public void init() throws IOException {
        final File rootDir = new File("/tmp/topics");
        if (rootDir.exists()) {
            this.deleteDir(rootDir);
        }
        this.queue = new TopicMessageQueue("/tmp/topics");
    }

    private void deleteDir(final File dir) {
        if (dir.isDirectory()) {
            for (final File child : dir.listFiles()) {
                this.deleteDir(child);
            }
        }
        dir.delete();
    }

    @Test
    public void testAdd() throws IOException {
        final String msg = "文件存储队列的持久化消息测试：";
        final String businessId = "business";
        final TopicMessageBean bean = new TopicMessageBean();
        bean.setTopic("环保通知");
        bean.setBusinessId(businessId);
        bean.setPublishTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        for (int i = 0; i < 65537; i++) {
            bean.setBusinessId(businessId + i);
            bean.setValue(msg + i);
            this.queue.add(bean);
        }
        for (int i = 0; i < 65537; i++) {
            final TopicMessageBean v = this.queue.get(i);
            Assert.assertEquals(msg + i, v.getValue());
            Assert.assertEquals(businessId + i, v.getBusinessId());
        }

        for (int i = 0; i < 65537; i += 100) {
            final TopicMessageBean v = this.queue.get(i);
            Assert.assertEquals(msg + i, v.getValue());
            Assert.assertEquals(businessId + i, v.getBusinessId());
        }
    }
}
