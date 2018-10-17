package com.apm70.fileq.client.conn;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.apm70.fileq.client.publish.MsgIdGenerator;
import com.apm70.fileq.config.Configuration;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.util.CallbackDispatcher;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Connection extends ChannelInboundHandlerAdapter {
    private final String serverHost;
    private final int serverPort;
    private Channel channel;
    private final EventLoopGroup workerGroup;
    private final QChannelInitializer channelInitializer;
    private final CallbackDispatcher callbackDispatcher;

    private final ReentrantLock lock = new ReentrantLock();

    public Connection(
            final String serverHost,
            final int serverPort,
            final Configuration config,
            final EventLoopGroup workerGroup,
            final CallbackDispatcher callbackDispatcher) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.workerGroup = workerGroup;
        this.callbackDispatcher = callbackDispatcher;
        this.channelInitializer = new QChannelInitializer(
                config.isBinaryLogging(),
                config.getConnKeepalivedSeconds(),
                callbackDispatcher);
    }

    /**
     * 连接服务端
     *
     * @throws InterruptedException
     * @throws Exception
     */
    public void connect() throws Exception {
        if (this.isConnected()) {
            return;
        }
        this.lock.lock();
        try {
            final Bootstrap clientBootStrap = new Bootstrap();
            clientBootStrap
                    .group(this.workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(this.channelInitializer)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.AUTO_READ, true)
                    .option(ChannelOption.SO_KEEPALIVE, true);
            // Start the client.
            final ChannelFuture f = clientBootStrap.connect(this.serverHost, this.serverPort).sync();
            this.channel = f.channel();
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isConnected() {
        return (this.channel != null) && this.channel.isActive();
    }

    public void disconnect() {
        // Wait until the connection is closed.
        try {
            this.channel.closeFuture().sync();
        } catch (final InterruptedException e) {
        }
    }

    public void write(final ProtocolMessage msg, final Consumer<Object> callback, final Object expiredDefaultValue)
            throws Exception {
        if (!this.isConnected()) {
            this.connect();
        }
        msg.setMsgId(MsgIdGenerator.generate());
        this.channel.writeAndFlush(msg);
        this.callbackDispatcher.registerCallback(msg.getMsgId(), callback, expiredDefaultValue);
    }

    public void writeWithoutAck(final ProtocolMessage msg) throws Exception {
        if (!this.isConnected()) {
            this.connect();
        }
        msg.setMsgId(MsgIdGenerator.generate());
        this.channel.writeAndFlush(msg);
    }
}
