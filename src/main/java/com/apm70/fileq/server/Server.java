package com.apm70.fileq.server;

import com.apm70.fileq.config.Configuration;
import com.apm70.fileq.protocol.codec.ProtocolDecoder;
import com.apm70.fileq.protocol.codec.ProtocolEncoder;
import com.apm70.fileq.protocol.handler.ChannelIdleTimoutHandler;
import com.apm70.fileq.protocol.handler.MessageHandler;
import com.apm70.fileq.protocol.handler.ServerExceptionHandler;
import com.apm70.fileq.server.topic.TopicSubscriber;
import com.apm70.fileq.server.topic.TopicMessageQueue;
import com.apm70.fileq.util.Destroyable;
import com.apm70.fileq.util.FlowLimiter;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server implements Destroyable {

    @Setter
    private Configuration config;
    @Setter
    private MessageHandler messageHandler;
    @Setter
    private ServerExceptionHandler serverExceptionHandler;
    @Setter
    private TopicMessageQueue topicMessageQueue;
    @Setter
    private FlowLimiter flowLimiter;

    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;
    private final LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
    private boolean started;

    public void start() throws Exception {
        if (this.started) {
            return;
        }
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        final ServerBootstrap serverBotStrap = new ServerBootstrap();
        serverBotStrap.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        final int keepalivedTimeout = Server.this.config.getConnKeepalivedSeconds();
                        final ProtocolDecoder decoder = new ProtocolDecoder();
                        decoder.setFlowLimiter(Server.this.flowLimiter);
                        final ChannelPipeline pipeline = ch.pipeline();
                        if (Server.this.config.isBinaryLogging()) {
                            pipeline.addLast("loggingHandler", Server.this.loggingHandler);
                        }
                        pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
                        pipeline.addLast("idleStateHandler",
                                new IdleStateHandler(keepalivedTimeout, keepalivedTimeout, keepalivedTimeout));
                        pipeline.addLast("idleEventHandler", new ChannelIdleTimoutHandler());
                        pipeline.addLast("decoder", decoder);
                        pipeline.addLast("encoder", new ProtocolEncoder());
                        pipeline.addLast("messageHandler", Server.this.messageHandler);
                        pipeline.addLast("serverExceptionHandler", Server.this.serverExceptionHandler);
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.AUTO_READ, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        serverBotStrap.bind(this.config.getServerHost(), this.config.getServerPort()).sync();
        Server.log.info("Server started, listening on {}:{}", this.config.getServerHost(),
                this.config.getServerPort());
        this.started = true;

    }

    public void stop() {
        if (!this.started) {
            return;
        }
        try {
            this.started = false;
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        this.stop();
    }

    public TopicSubscriber newTopicSubscriber() {
        final TopicSubscriber subscriber = new TopicSubscriber();
        subscriber.setTopicMessageQueue(this.topicMessageQueue);
        return subscriber;
    }
}
