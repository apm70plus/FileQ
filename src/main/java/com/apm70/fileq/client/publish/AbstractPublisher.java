package com.apm70.fileq.client.publish;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.apm70.fileq.client.conn.ConnectionManager;

import lombok.Setter;

public abstract class AbstractPublisher implements Publisher {
    @Setter
    protected ConnectionManager connectionManager;

    protected Set<PublishStateListener> listeners = new HashSet<>();

    protected static CopyOnWriteArraySet<WaitCancelRepublish> cancelRepublish = new CopyOnWriteArraySet<>();

    @Override
    public void registerPublishStateListener(final PublishStateListener listener) {
        this.listeners.add(listener);
    }

    /**
     * 发布状态通知给所有监听者
     *
     * @param lqPublishState
     */
    protected void notifyAllListener(final PublishState lqPublishState) {
        this.listeners.forEach(l -> {
            l.stateChanged(lqPublishState);
        });
    }

    @Override
    public void registerCancelRepublish(final WaitCancelRepublish cancleRepublish) {
        AbstractPublisher.cancelRepublish.add(cancleRepublish);
    }
}
