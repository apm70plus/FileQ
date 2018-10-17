package com.apm70.fileq.protocol.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelIdleTimoutHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent e = (IdleStateEvent) evt;
            if ((e.state() == IdleState.READER_IDLE) || (e.state() == IdleState.ALL_IDLE)) {
                ChannelIdleTimoutHandler.log.info("[IdleTimeout] 长时间未收到消息，断开连接！！");
                // 关闭连接 
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.flush();
            }
        } else { // 继续传播事件
            ctx.fireUserEventTriggered(evt);
        }
    }
}
