package com.apm70.fileq.server.topic;

import java.io.IOException;

import lombok.Setter;

public class TopicSubscriber {

    @Setter
    private TopicMessageQueue topicMessageQueue;

    /**
     * 根据索引消费订阅的消息。</br>
     * 索引值需要业务自己维护，根据索引，可以反复读取消息（消息存活期内）
     *
     * @param index
     * @return
     * @throws IOException
     */
    public TopicMessageBean consume(final long index) throws IOException {
        return this.topicMessageQueue.get(index);
    }
}
