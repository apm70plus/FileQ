package com.apm70.fileq.protocol.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class ServerExceptionHandler extends ChannelHandlerAdapter {

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final Throwable cause) throws Exception {
        ServerExceptionHandler.log.error("Netty服务异常", cause);
        ctx.close();
    }
}
