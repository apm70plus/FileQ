package com.leading.lq;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.util.PersistanceList;

/**
 * 基于文件系统的持久化队列测试
 * @author liuyg
 *
 */
public class PersistenceListTest {

    private PersistanceList<TopicMessageBean> list;

    @Before
    public void init() {
        try {
            final File store = new File("/tmp/list.dat");
            final File index = new File("/tmp/index.dat");
            store.delete();
            index.delete();
            this.list = new PersistanceList<>(index, store, TopicMessageBean.class);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAdd() {
        final String msg = "文件存储队列的持久化消息测试：";
        final TopicMessageBean bean = new TopicMessageBean();
        bean.setTopic("环保通知");
        bean.setBusinessId("STR:businessId");
        bean.setPublishTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        for (int i = 1; i <= 10; i++) {
            bean.setValue(msg + i);
            this.list.add(bean);
        }
        Assert.assertEquals(10, this.list.size());
        for (int i = 1; i <= 10; i++) {
            final TopicMessageBean v = this.list.get(i - 1);
            Assert.assertEquals(msg + i, v.getValue());
        }
        Assert.assertEquals(10, this.list.size());
    }
}
