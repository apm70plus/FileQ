package com.apm70.fileq.client.publish;

public interface Publisher {

    void registerPublishStateListener(PublishStateListener listener);

    void publishAsync(PublishContext context);

    boolean matches(PublishContext context);

    void registerCancelRepublish(WaitCancelRepublish cancleRepublish);
}
