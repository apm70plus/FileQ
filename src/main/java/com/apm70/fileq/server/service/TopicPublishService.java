package com.apm70.fileq.server.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.server.topic.TopicMessageQueue;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TopicPublishService {
    @Setter
    private TopicMessageQueue topicMessageQueue;

    public void publish(final TopicMessageBean msg) {
        if (msg == null) {
            return;
        }
        msg.setPublishTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        try {
            this.topicMessageQueue.add(msg);
        } catch (final IOException e) {
            log.error("订阅消息发布失败，消息内容：{}", msg);
            log.error("异常信息", e);
        }
    }
}
