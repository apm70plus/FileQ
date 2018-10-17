package com.apm70.fileq.client.publish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilePublishContext extends PublishContext {

	private String filePath;
	private byte[] fileContent;
	private String filename;
	private String fileMD5;
	private long fileSize;

	public FilePublishContext() {
	}

	public FilePublishContext(final String filePath, final String topicNo, final String businessId,
			final String serverHost, final int serverPort) {
		super(topicNo, businessId, serverHost, serverPort);
		this.filePath = filePath;
	}
}
