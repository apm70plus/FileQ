package com.apm70.fileq.server.topic;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicMessageBean {

    private String topic;
    private String businessId;
    private String value;
    private String publishTime;
    private MessageType msgType;

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("TopicMessageBean(")
                .append("topic=").append(this.topic).append(", ")
                .append("businessId=").append(this.businessId).append(", ")
                .append("msgType=").append(this.msgType).append(", ")
                .append("value=").append(this.value).append(", ")
                .append("publishTime=").append(this.publishTime)
                .append(")");
        return builder.toString();
    }
}
