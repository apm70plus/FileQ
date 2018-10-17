package com.apm70.fileq.client;

import java.io.File;

import com.apm70.fileq.client.publish.FilePublishContext;
import com.apm70.fileq.client.publish.PublishStateListener;
import com.apm70.fileq.client.publish.Publisher;
import com.apm70.fileq.client.publish.TextPublishContext;
import com.apm70.fileq.client.publish.WaitCancelRepublish;
import com.apm70.fileq.config.Constants;

import lombok.Setter;

/**
 * 客户端
 *
 * @author liuyg
 */
@Setter
public class Client {
    @Setter
    private Publisher publisher;

    public void registerPublishStateListener(final PublishStateListener listener) {
        this.publisher.registerPublishStateListener(listener);
    }

    public void publishFile(
            final File file,
            final String topicNo,
            final String businessId,
            final String host,
            final int port) {
        final FilePublishContext context =
                new FilePublishContext(file.getAbsolutePath(), topicNo, businessId, host, port);
        this.publisher.publishAsync(context);
    }

    /**
     * 发布消息，文本消息体过长时，会转换成文件发送
     *
     * @param file
     * @param topicNo
     * @param businessId
     * @param host
     * @param port
     */
    public void publish(
            final String text,
            final String topicNo,
            final String businessId,
            final String host,
            final int port) {
        final byte[] bytes = text.getBytes(Constants.defaultCharset);
        if (bytes.length > Constants.ChunckSize) {// 超出分块大小，改为文件传输
            final FilePublishContext context =
                    new FilePublishContext(null, topicNo, businessId, host, port);
            context.setFileContent(bytes);
            this.publisher.publishAsync(context);
        } else {
            final TextPublishContext context = new TextPublishContext(text, topicNo, businessId, host, port);
            this.publisher.publishAsync(context);
        }
    }

    /**
     * 取消重新发布消息
     *
     * @param topicNo
     * @param businessId
     */
    public void cancelRepublish(
            final String topicNo,
            final String businessId) {
        final WaitCancelRepublish waitCancleRepublish = new WaitCancelRepublish(topicNo, businessId);
        this.publisher.registerCancelRepublish(waitCancleRepublish);
    }
}
