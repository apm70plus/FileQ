package com.apm70.fileq.protocol.handler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.server.ReceivedDataProcessor;
import com.apm70.fileq.util.BlockingThreadPoolExecutor;
import com.apm70.fileq.util.Destroyable;
import com.apm70.fileq.util.TaskThreadFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter implements Destroyable {

    private final int highWaterMarker = 400;
    private final int lowWaterMarker = 10;
    private final ConcurrentHashMap<String, Channel> autoReadClosedChannels = new ConcurrentHashMap<>();
    private final BlockingThreadPoolExecutor workExecutor = new BlockingThreadPoolExecutor(4, 128, 1, TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(500), new TaskThreadFactory("MessageHandler-Worker-"));

    @Setter
    private ReceivedDataProcessor processor;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        // 控制客户端发送速率
        if (this.workExecutor.getQueue().size() > this.highWaterMarker) {
            final Channel channel = ctx.channel();
            MessageHandler.log.info("close channel autoread............");
            channel.config().setAutoRead(false);
            this.autoReadClosedChannels.putIfAbsent(channel.toString(), channel);
        }
        this.workExecutor.execute(() -> {
            try {
                this.processor.process(ctx, (ProtocolMessage) msg);
            } catch (final Exception e) {
                MessageHandler.log.error("系统异常", e);
            } finally {
                if ((this.workExecutor.getQueue().size() > this.lowWaterMarker)
                        || this.autoReadClosedChannels.isEmpty()) {
                    return;
                }
                this.autoReadClosedChannels.keySet().forEach(key -> {
                    final Channel c = this.autoReadClosedChannels.remove(key);
                    if (c.isActive()) {
                        c.config().setAutoRead(true);
                        MessageHandler.log.info("open channel autoread............");
                    }
                });
            }
        });
    }

    @PreDestroy
    public void destroy() {
        this.workExecutor.shutdown();
    }
}
