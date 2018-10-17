package com.apm70.fileq.config;

import java.nio.charset.Charset;

import com.apm70.fileq.protocol.message.NormalAck;
import com.apm70.fileq.protocol.message.ProtocolMessage;

public final class Constants {

    public static Charset defaultCharset = Charset.forName("UTF-8");

    /**
     * 文件分块儿大小
     */
    public static int ChunckSize = 512 * 1024;

    /**
     * 应答超时默认ACK消息
     */
    public static Object EXPIRED_ACK;

    static {
        final ProtocolMessage msg = new ProtocolMessage();
        final NormalAck ack = new NormalAck();
        ack.setStatus((byte) 2);
        ack.setCause("ACK应答超时");
        msg.setBody(ack);
        EXPIRED_ACK = msg;
    }
}
