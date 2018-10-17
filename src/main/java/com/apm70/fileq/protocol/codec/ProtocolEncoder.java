package com.apm70.fileq.protocol.codec;

import com.apm70.fileq.protocol.message.ProtocolMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolEncoder extends MessageToByteEncoder<ProtocolMessage> {

    @Override
    protected void encode(final ChannelHandlerContext context, final ProtocolMessage msg, final ByteBuf out)
            throws Exception {
        ProtocolEncoder.log.debug("向[{}]发送消息: {}", context.channel().remoteAddress(), msg);
        msg.encode(out);
    }
}
