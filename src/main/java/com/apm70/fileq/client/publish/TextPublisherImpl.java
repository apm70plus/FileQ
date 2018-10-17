package com.apm70.fileq.client.publish;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.Date;

import com.apm70.fileq.client.conn.Connection;
import com.apm70.fileq.client.conn.ConnectionManager;
import com.apm70.fileq.config.Constants;
import com.apm70.fileq.protocol.message.NormalAck;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.protocol.message.Text;
import com.apm70.fileq.util.Destroyable;
import com.apm70.fileq.util.LQWaiter;
import com.apm70.fileq.util.PersistenceQueue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TextPublisherImpl extends AbstractPublisher implements Destroyable {

    private final String fileTmpPath;
    private final PersistenceQueue<TextPublishContext> publishTextsQueue;
    private final PersistenceQueue<TextPublishContext> publishFailedQueue;
    private final LQWaiter publishTaskWaiter = new LQWaiter("textPublishTaskWaiter");
    private final LQWaiter failedTaskRetryWaiter = new LQWaiter("failedTextTaskRetryWaiter");
    private final LQWaiter publishFinishedWaiter = new LQWaiter("filePublishFinishedWaiter");
    private volatile boolean stoped;

    public TextPublisherImpl(final ConnectionManager connectionManager,
            final String fileTmpPath) throws IOException {
        this.fileTmpPath = fileTmpPath;
        this.connectionManager = connectionManager;
        final File dir = new File(fileTmpPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final File store = new File(this.fileTmpPath, "text-publishing-store.dat");
        final File meta = new File(this.fileTmpPath, "text-publishing-meta.dat");
        this.publishTextsQueue = new PersistenceQueue<>(meta, store, TextPublishContext.class);
        final File failedStore = new File(fileTmpPath, "text-failed-store.dat");
        final File failedMeta = new File(fileTmpPath, "text-failed-meta.dat");
        this.publishFailedQueue = new PersistenceQueue<>(failedMeta, failedStore, TextPublishContext.class);
        this.stoped = false;
        this.startPublishing();
        this.startPublishFiledRetry();
    }

    @Override
    public void publishAsync(final PublishContext context) {
        this.persistant((TextPublishContext) context);
        this.publishTaskWaiter.signal();
    }

    private void startPublishFiledRetry() {
        new Thread((Runnable) () -> {
            while (!this.stoped) {
                this.failedTaskRetryWaiter.await(30);
                final TextPublishContext context = this.publishFailedQueue.peek();
                if (context == null) {
                    continue;
                }
                try {
                    if (System.currentTimeMillis() > context.getExpiredTime()) {
                        // 超时，放弃发布
                        TextPublisherImpl.log.warn("LQCLIENT 文本消息超时，放弃发布，发布信息：{}", context);
                        this.notifyAllListener(
                                PublishState.failure(context.topicNo, context.businessId, "文本消息重试超时，放弃发布"));
                        continue;
                    }
                    final PublishState state = this.publish(context);
                    this.notifyAllListener(state);// 通知所有监听发布状态
                    if (state.getState() == 3) {
                        // 发布失败，写发布任务到失败队列
                        TextPublisherImpl.log.error("LQCLIENT 文本消息发布失败。失败原因：{}", state.getError());
                        this.publishFailedQueue.add(context);
                    }
                } catch (final Exception e1) {
                    // 发布失败，写发布任务到失败队列
                    TextPublisherImpl.log.error("LQCLIENT 文件发布失败。", e1);
                    this.publishFailedQueue.add(context);
                } finally {
                    try {
                        this.publishFailedQueue.poll();
                    } catch (final Exception e2) {
                        TextPublisherImpl.log.error("", e2);
                    }
                }
            }
        }, "text-publish-retry-thread").start();
    }

    private void startPublishing() {
        new Thread((Runnable) () -> {
            while (!this.stoped) {
                final TextPublishContext context = this.publishTextsQueue.peek();
                if (context == null) {
                    this.publishTaskWaiter.await();
                    continue;
                }
                boolean isCancelPublish = false;

                for (final WaitCancelRepublish w : AbstractPublisher.cancelRepublish) {
                    if (w.getBusinessId().equals(context.getBusinessId())
                            && w.getTopicNo().equals(context.getTopicNo())) {
                        isCancelPublish = true;
                        AbstractPublisher.cancelRepublish.remove(w);
                        this.publishTextsQueue.poll();
                    }
                    final Date today = new Date();
                    if (today.getTime() - w.getCreateDate().getTime() > 24 * 3600000) {
                        AbstractPublisher.cancelRepublish.remove(w);
                    }
                }
                if (!isCancelPublish) {
                    try {
                        // 通知业务开始发送
                        this.notifyAllListener(PublishState.publishing(context.topicNo, context.businessId));

                        final PublishState state = this.publish(context);
                        this.notifyAllListener(state);// 通知所有监听发布状态
                        if (state.getState() == 3) {
                            // 发布失败，写发布任务到失败队列
                            TextPublisherImpl.log.error("LQCLIENT 文件发布失败。失败原因：{}", state.getError());
                            this.publishFailedQueue.add(context);
                        }
                    } catch (final Exception e1) {
                        // 发布失败，写发布任务到失败队列
                        TextPublisherImpl.log.error("LQCLIENT 文件发布失败。", e1);
                        this.publishFailedQueue.add(context);
                    } finally {
                        try {
                            this.publishTextsQueue.poll();
                        } catch (final Exception e2) {
                            TextPublisherImpl.log.error("", e2);
                        }
                    }
                }
            }
        }, "text-publishing-thread").start();
    }

    private PublishState publish(final TextPublishContext context) {
        final Connection conn =
                this.connectionManager.getConnection(context.getServerHost(), context.getServerPort());
        final ProtocolMessage msg = new ProtocolMessage();
        final Text body = new Text();
        body.setBusinessId(context.businessId);
        body.setTopic(context.topicNo);
        body.setPayload(context.getText().getBytes(Constants.defaultCharset));
        msg.setBody(body);
        final PublishState result = PublishState.retry(context.topicNo, context.businessId, "");
        try {
            conn.write(msg, resp -> {
                final NormalAck ack = (NormalAck) ((ProtocolMessage) resp).getBody();
                if (ack.isSuccess()) {
                    result.setState((byte) 2); // 已发布
                } else {
                    result.setError("ACK应答超时，尝试重新发布");
                }
                this.publishFinishedWaiter.signal();
            }, Constants.EXPIRED_ACK);
            // 等待文件发布完成
            this.publishFinishedWaiter.await();
        } catch (final SocketException e) {
            TextPublisherImpl.log.error("网络异常，消息尝试重新发送！", e);
            result.setError("网络异常，消息尝试重新发送！");
        } catch (final Exception e) {
            TextPublisherImpl.log.error("未知的服务异常，消息尝试重新发送！", e);
            result.setError("未知的服务异常，消息尝试重新发送！");
        }
        return result;
    }

    private void persistant(final TextPublishContext context) {
        // 持久化文件信息到队列
        this.publishTextsQueue.add(context);
        this.notifyAllListener(PublishState.queuing(context.topicNo, context.businessId));
    }

    @Override
    public boolean matches(final PublishContext context) {
        return context instanceof TextPublishContext;
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
