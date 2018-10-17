package com.apm70.fileq.client.publish;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.apm70.fileq.client.conn.Connection;
import com.apm70.fileq.client.conn.ConnectionManager;
import com.apm70.fileq.config.Constants;
import com.apm70.fileq.protocol.message.FileChunk;
import com.apm70.fileq.protocol.message.FilePublishRequest;
import com.apm70.fileq.protocol.message.FilePublishRequestAck;
import com.apm70.fileq.protocol.message.NormalAck;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.util.LQWaiter;
import com.apm70.fileq.util.MD5Utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilePublishTask {
    /** 如果回调函数超时，回调函数接收参数的默认值 */
    private final LQWaiter publishFinishedWaiter = new LQWaiter("filePublishFinishedWaiter");

    @Setter
    private ConnectionManager connectionManager;

    public PublishState publish(final FilePublishContext context) {
        final PublishState result = PublishState.retry(context.topicNo, context.businessId, "");
        // 1. 询问服务端是否发布文件
        final ProtocolMessage filePublishRequestMsg = this.createFilePublishRequestMsg(context);
        try {
            FilePublishTask.log.trace("LQCLIENT开始处理：“文件发布申请”，文件信息：{}", filePublishRequestMsg);
            this.getConnection(context).write(filePublishRequestMsg, resp -> {
                final ProtocolMessage ack = (ProtocolMessage) resp;
                if (resp == Constants.EXPIRED_ACK) {//超时、失败处理
                    result.setState(3);
                    result.setError(((NormalAck) ack.getBody()).getCause());
                    this.publishFinishedWaiter.signal();
                    return;
                }

                // 服务端回复，返回上传文件的起始块儿编号
                final FilePublishRequestAck response = (FilePublishRequestAck) ack.getBody();
                final int startChunkNo = response.getChunkNo();
                if (startChunkNo == -1) {// 服务端已经存在文件，无需上传
                    FilePublishTask.log.trace("LQCLIENT开始处理：“通知发布者文件发布完成”");
                    result.setState(2);
                    this.publishFinishedWaiter.signal();
                    return;
                }

                // 发布文件到服务端
                final PublishState state = this.publishFile(context, response.getChunkNo());
                result.setState(state.getState());
                result.setError(state.getError());
                this.publishFinishedWaiter.signal();
            }, Constants.EXPIRED_ACK);
            FilePublishTask.log.trace("LQCLIENT处理结束：“文件发布申请”，文件信息：{}", filePublishRequestMsg);

            // 等待文件发布完成
            this.publishFinishedWaiter.await();
        } catch (final SocketException e) {
            FilePublishTask.log.error("网络异常，消息尝试重新发送！", e);
            result.setError("网络异常，消息尝试重新发送！");
        } catch (final Exception e) {
            FilePublishTask.log.error("未知的服务异常，消息尝试重新发送！", e);
            result.setError("未知的服务异常，消息尝试重新发送！");
        }
        return result;
    }

    private PublishState publishFile(final FilePublishContext context, final int startChunk) {
        final LQWaiter ackWaiter = new LQWaiter("ackWaiter");
        try (FileChannel channel = new RandomAccessFile(new File(context.getFilePath()), "r").getChannel()) {
            final byte[] md5 = MD5Utils.decodeHex(context.getFileMD5());
            final Connection conn = this.getConnection(context);
            channel.position(1L * Constants.ChunckSize * (startChunk - 1));
            final AtomicInteger chunkCounter = new AtomicInteger(startChunk);
            final ByteBuffer buffer = ByteBuffer.allocate(Constants.ChunckSize);
            while (true) {
                final int size = channel.read(buffer);
                if (size <= 0) {// 全部发送成功
                    break;
                }
                byte[] payload = null;
                if (size == Constants.ChunckSize) {
                    payload = buffer.array();
                } else {
                    payload = new byte[size];
                    buffer.flip();
                    buffer.get(payload);
                }
                buffer.clear();
                final int chunkNo = chunkCounter.get();
                this.publishFileChunk(conn, payload, md5, chunkNo, ack -> {
                    if (ack != Constants.EXPIRED_ACK) {// 非超时回调
                        final NormalAck normalAck = (NormalAck) ((ProtocolMessage) ack).getBody();
                        if (normalAck.isSuccess()) {
                            chunkCounter.incrementAndGet();// 准备传下一分片
                        }
                    }
                    ackWaiter.signal();//ACK到达
                });
                ackWaiter.await();// 等待ACK
                if (chunkCounter.get() == chunkNo) {// ACK应答超时，上传失败
                    FilePublishTask.log.error("ACK应答超时，可能出现了网络问题，消息尝试重新发送！");
                    return PublishState.retry(context.topicNo, context.businessId, "ACK应答超时，可能出现了网络问题，消息尝试重新发送！");
                }
            }
            return PublishState.published(context.topicNo, context.businessId);
        } catch (final FileNotFoundException e) {
            FilePublishTask.log.error("文件不存在，消息无法发送！", e);
            return PublishState.failure(context.topicNo, context.businessId, "文件不存在，消息无法发送！");
        } catch (final IOException e) {
            FilePublishTask.log.error("网络异常，消息尝试重新发送！", e);
            return PublishState.retry(context.topicNo, context.businessId, "网络异常，消息尝试重新发送！");
        } catch (final Exception e) {
            FilePublishTask.log.error("未知的服务异常，消息尝试重新发送！", e);
            return PublishState.retry(context.topicNo, context.businessId, "未知的服务异常，消息无法发送！");
        }
    }

    private void publishFileChunk(final Connection conn, final byte[] body, final byte[] md5, final int chunkNo,
            final Consumer<Object> ackCallback) throws Exception {
        final ProtocolMessage msg = new ProtocolMessage();
        final FileChunk chunk = new FileChunk();
        chunk.setChunkNo(chunkNo);
        chunk.setFileFlag(md5);
        chunk.setPayload(body);
        msg.setBody(chunk);
        conn.write(msg, ackCallback, Constants.EXPIRED_ACK);
    }

    /**
     * 构造询问服务端是否可以发布文件的消息
     *
     * @param context
     */
    private ProtocolMessage createFilePublishRequestMsg(final FilePublishContext context) {
        final ProtocolMessage msg = new ProtocolMessage();
        final FilePublishRequest body = new FilePublishRequest();
        body.setFileFlag(MD5Utils.decodeHex(context.getFileMD5()));
        body.setFilename(context.getFilename());
        final long fileSize = context.getFileSize();
        body.setFileSize(context.getFileSize());
        body.setBusinessId(context.getBusinessId());
        // 计算分片数量
        long chunkCount = fileSize / Constants.ChunckSize;
        if ((fileSize % Constants.ChunckSize) != 0) {
            chunkCount++;
        }
        body.setChunkCount((short) chunkCount);
        body.setTopic(context.getTopicNo());
        msg.setBody(body);
        return msg;
    }

    private Connection getConnection(final PublishContext context) {
        return this.connectionManager.getConnection(context.getServerHost(), context.getServerPort());
    }
}
