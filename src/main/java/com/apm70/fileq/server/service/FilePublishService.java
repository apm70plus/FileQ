package com.apm70.fileq.server.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.apm70.fileq.protocol.message.FileChunk;
import com.apm70.fileq.protocol.message.FilePublishRequest;
import com.apm70.fileq.protocol.message.FilePublishRequestAck;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.server.FileStore;
import com.apm70.fileq.server.topic.MessageType;
import com.apm70.fileq.server.topic.TopicMessageBean;
import com.apm70.fileq.util.ClientUtils;
import com.apm70.fileq.util.MD5Utils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.netty.channel.ChannelHandlerContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilePublishService {

    @Setter
    private TopicPublishService topicPublishService;
    @Setter
    private FileStore fileStore;

    private final Cache<String, TopicMessageBean> fileTopicCache =
            CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(500).build();

    public void handleFilePublishReq(final ChannelHandlerContext ctx, final ProtocolMessage msg) {
        final FilePublishRequest req = (FilePublishRequest) msg.getBody();
        final String fileFlag = MD5Utils.encodeHex(req.getFileFlag());
        {// 缓存消息信息
            final TopicMessageBean bean = new TopicMessageBean();
            bean.setBusinessId(req.getBusinessId());
            bean.setTopic(req.getTopic());
            bean.setMsgType(MessageType.FILE);
            this.fileTopicCache.put(fileFlag, bean);
        }
        log.info("LQ开始处理：客户端“文件发布请求”，文件信息：{}", req);
        try {
            int startChunkNo = 1;
            if (this.fileStore.fileExists(fileFlag)) {// 已经存在
                startChunkNo = this.fileStore.breakPointChunkNo(fileFlag);
            } else {
                this.fileStore.addFile(fileFlag, req.getFilename(), req.getFileSize(), req.getChunkCount());
            }
            if (startChunkNo == -1) {// 已经是完整文件, 无需断点续传，直接发布
                final File file = this.fileStore.getFile(fileFlag);
                log.info("LQ开始处理：客户端“文件发布”因为文件已存在，无需再次上传，直接发布完整文件给订阅方，topic：{}, file: {}",
                        req.getTopic(), file.getName());
                this.topicPublishService.publish(this.createFileTopicMessageBean(fileFlag, file));
            } else {
                final TopicMessageBean msgBean = new TopicMessageBean();
                msgBean.setBusinessId(req.getBusinessId());
                msgBean.setTopic(req.getTopic());
                msgBean.setMsgType(MessageType.FILE);
                this.fileTopicCache.put(fileFlag, msgBean);// 缓存信息，等待文件上传完成
            }
            // ACK
            final FilePublishRequestAck ack = new FilePublishRequestAck();
            ack.setChunkNo(startChunkNo);
            msg.setBody(ack);
            ctx.writeAndFlush(msg);
            log.debug("LQ处理完成：客户端“文件发布请求”，回复信息：{}", ack);
        } catch (final IOException e) {
            log.error("LQ异常：客户端“文件发布请求”失败，文件标识：{}， 失败原因：发生IO异常, {}", fileFlag, e.getMessage());
            ClientUtils.sendFailureAck(ctx, msg.getMsgId(), "发生IO异常，需要检查磁盘空间");
        }
    }

    public void handleFileChunk(final ChannelHandlerContext ctx, final ProtocolMessage msg) {
        // 执行合并文件操作
        final FileChunk chunk = (FileChunk) msg.getBody();
        final String fileIdentifier = MD5Utils.encodeHex(chunk.getFileFlag());
        log.debug("LQ开始处理：客户端“文件分片上传”，文件分片信息：{}", chunk);
        final TopicMessageBean topic = this.fileTopicCache.getIfPresent(fileIdentifier);
        try {
            this.fileStore.mergeFileChunk(fileIdentifier, chunk.getChunkNo(), chunk.getPayload());
            ClientUtils.sendSuccessAck(ctx, msg.getMsgId());
            log.debug("LQ处理完成：客户端“文件分片上传”，文件分片信息：{}", chunk);
        } catch (final IOException e) {
            log.error("LQ异常：客户端“文件分片上传”失败，文件分片信息：" + chunk, e);
            ClientUtils.sendFailureAck(ctx, msg.getMsgId(), "发生IO异常，需要检查磁盘空间");
        }
        try {
            if (!this.fileStore.isFileComplete(fileIdentifier)) {
                return;
            }
            final File file = this.fileStore.getFile(fileIdentifier);
            if (topic != null) {
                log.info("LQ开始处理：客户端“文件分片”已全部接收，发布完整文件给订阅方，topic：{}, file: {}",
                        topic, file.getName());
                this.topicPublishService.publish(this.createFileTopicMessageBean(fileIdentifier, file));
            } else {
                log.error("LQ异常：客户端“文件分片”已全部接收，但文件的发布topic丢失，发布失败！”");
            }
        } catch (final IOException e) {
            log.error("文件发布失败", e);
        }
    }

    private TopicMessageBean createFileTopicMessageBean(final String fileFlag, final File file) {
        final TopicMessageBean msgBean = this.fileTopicCache.getIfPresent(fileFlag);
        if (msgBean == null) {
            log.error("BUG：文件的业务信息丢失，无法发布消息！文件路径：{}", file.getAbsolutePath());
            return null;
        }
        this.fileTopicCache.invalidate(fileFlag);
        msgBean.setValue(file.getAbsolutePath());
        return msgBean;
    }
}
