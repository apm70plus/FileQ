package com.apm70.fileq.client.publish;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublishState {
    private String topic;
	private String businessId;
	private int state; // 0: 排队中，1: 发布中， 2: 已送达， 3: 失败重试， 4: 发布失败
	private String error;

	public static PublishState queuing(String topic, String businessId) {
		return new PublishState(topic, businessId, 0, null);
	}
	
	public static PublishState publishing(String topic, String businessId) {
		return new PublishState(topic, businessId, 1, null);
	}
	
	public static PublishState published(String topic, String businessId) {
        return new PublishState(topic, businessId, 2, null);
    }
	
	public static PublishState retry(String topic, String businessId, String error) {
		return new PublishState(topic, businessId, 3, error);
	}
	
	public static PublishState failure(String topic, String businessId, String error) {
		return new PublishState(topic, businessId, 4, error);
	}
}
