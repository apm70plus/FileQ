package com.apm70.fileq.client.conn;

import com.apm70.fileq.protocol.codec.ProtocolDecoder;
import com.apm70.fileq.protocol.codec.ProtocolEncoder;
import com.apm70.fileq.protocol.handler.ChannelIdleTimoutHandler;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.util.CallbackDispatcher;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final boolean binaryLogging;
    private final int keepalivedTimeout;
    private final CallbackDispatcher callbackDispatcher;

    public QChannelInitializer(final boolean binaryLogging, final int keepalivedTimeout,
            final CallbackDispatcher callbackDispatcher) {
        this.binaryLogging = binaryLogging;
        this.keepalivedTimeout = keepalivedTimeout;
        this.callbackDispatcher = callbackDispatcher;
    }

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        if (this.binaryLogging) {
            pipeline.addLast("loggingHandler", new LoggingHandler());
        }
        pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        pipeline.addLast("idleStateHandler",
                new IdleStateHandler(this.keepalivedTimeout, this.keepalivedTimeout, this.keepalivedTimeout));
        pipeline.addLast("idleEventHandler", new ChannelIdleTimoutHandler());
        pipeline.addLast("decoder", new ProtocolDecoder());
        pipeline.addLast("encoder", new ProtocolEncoder());
        pipeline.addLast("messageHandler", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object message) {
                final ProtocolMessage msg = (ProtocolMessage) message;
                switch (msg.getType()) {
                case 1:
                case 3:
                    QChannelInitializer.this.callbackDispatcher.fireCallback(msg.getMsgId(), msg);
                    break;
                default:
                    // unsupports
                }
            }
        });
        pipeline.addLast("channelInactiveHandler", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
                QChannelInitializer.log.info("客户端连接 [{}:{}] 已经断开！");
            }
        });
    }

}
