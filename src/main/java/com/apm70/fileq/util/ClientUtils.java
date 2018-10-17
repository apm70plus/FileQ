package com.apm70.fileq.util;

import com.apm70.fileq.protocol.message.NormalAck;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.server.service.NodeRegistryService.NodeInfo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class ClientUtils {

    private static final AttributeKey<NodeInfo> CLIENT_KEY = AttributeKey.valueOf("CLIENT");

    public static NodeInfo getClientInfo(final Channel channel) {
        final Attribute<NodeInfo> value = channel.attr(CLIENT_KEY);
        return value.get();
    }

    public static void setClientInfo(final Channel channel, final NodeInfo info) {
        channel.attr(CLIENT_KEY).set(info);
    }

    public static void sendFailureAck(final ChannelHandlerContext ctx, final int messageId, final String cause) {
        final NormalAck errorAck = new NormalAck();
        errorAck.setStatus((byte) 0);
        errorAck.setCause(cause);
        sendAck(ctx, errorAck, messageId);
    }

    public static void sendSuccessAck(final ChannelHandlerContext ctx, final int messageId) {
        final NormalAck ack = new NormalAck();
        ack.setStatus((byte) 1);
        sendAck(ctx, ack, messageId);
    }

    public static void sendAck(final ChannelHandlerContext ctx, final NormalAck ack, final int messageId) {
        final ProtocolMessage msg = new ProtocolMessage();
        msg.setMsgId(messageId);
        msg.setBody(ack);
        ctx.writeAndFlush(msg);
    }
}
