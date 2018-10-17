package com.apm70.fileq.client.publish;

import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.util.CallbackDispatcher;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Setter;

public class ReceiveMsgProcessor extends ChannelInboundHandlerAdapter {
    @Setter
    private CallbackDispatcher callbackDispatcher;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        final ProtocolMessage msg = (ProtocolMessage) message;
        switch (msg.getType()) {
        case 1:
        case 3:
            this.callbackDispatcher.fireCallback(msg.getMsgId(), msg);
            break;
        default:
            // unsupports
        }
    }
}
