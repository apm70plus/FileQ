package com.apm70.fileq.protocol.codec;

import java.util.List;

import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.util.FlowLimiter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolDecoder extends ByteToMessageDecoder {

    private static final int HEADER_LEN = 9;

    @Setter
    private FlowLimiter flowLimiter;

    @Override
    protected void decode(final ChannelHandlerContext context, final ByteBuf in, final List<Object> out)
            throws Exception {
        while (true) {
            final int readableBytes = in.readableBytes();
            if (readableBytes < ProtocolDecoder.HEADER_LEN) {
                return;
            }
            final int readerIndex = in.readerIndex();
            final int bodyLength = in.getInt(readerIndex + 5);
            if (readableBytes < (ProtocolDecoder.HEADER_LEN + bodyLength)) {
                context.read();
                return;
            }

            if (this.flowLimiter != null) {
                this.flowLimiter.flowIn(ProtocolDecoder.HEADER_LEN + bodyLength);
            }
            final ProtocolMessage msg = new ProtocolMessage();
            final ByteBuf buf = in.readSlice(ProtocolDecoder.HEADER_LEN + bodyLength);
            msg.decode(buf);
            ProtocolDecoder.log.debug("收到[{}]的消息: {}", context.channel().remoteAddress(), msg);
            out.add(msg);
        }
    }
}
