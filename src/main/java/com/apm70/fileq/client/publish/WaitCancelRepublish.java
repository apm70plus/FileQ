package com.apm70.fileq.client.publish;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WaitCancelRepublish {

    private String businessId;

    private String topicNo;

    private Date createDate;

    public WaitCancelRepublish() {
    };

    public WaitCancelRepublish(String businessId, String topicNo) {
        this.businessId = businessId;
        this.topicNo = topicNo;
        this.createDate = new Date();
    }
}
