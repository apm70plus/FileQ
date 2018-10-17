package com.apm70.fileq.client.publish;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class PublishContext {
    protected String topicNo;
    protected String businessId;
    protected String serverHost;
    protected int serverPort;

    public PublishContext() {
    }
}
