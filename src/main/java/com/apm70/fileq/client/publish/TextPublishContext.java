package com.apm70.fileq.client.publish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextPublishContext extends PublishContext {
	private String charset;
    private String text;
    private long expiredTime;

    public TextPublishContext() {
    }

    public TextPublishContext(final String text, final String topicNo, final String businessId,
            final String serverHost, final int serverPort) {
        super(topicNo, businessId, serverHost, serverPort);
        this.text = text;
    }

}
