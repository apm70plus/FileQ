package com.apm70.fileq.protocol.message;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProtocolMessage {

    private int msgId;

    private ProtocolBody body;

    public byte getType() {
        return this.body.getType();
    }

    public int getBodyLength() {
        return this.body.getLength();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("{")
                .append("msgId=").append(this.msgId).append(",")
                .append("body=").append(this.body)
                .append("}");
        return builder.toString();
    }

    public void encode(final ByteBuf out) {
        // 写消息头
        out.writeInt(this.getMsgId());
        out.writeByte(this.getType());
        final int bodyLengthPosition = out.writerIndex();
        out.writeInt(0);
        this.getBody().encode(out);
        final int bodyLength = out.writerIndex() - (bodyLengthPosition + 4);
        out.setInt(bodyLengthPosition, bodyLength);
    }

    public void decode(final ByteBuf in) {
        this.setMsgId(in.readInt());
        this.body = this.newBodyInstance(in.readByte());
        in.readInt();// bodyLength
        this.body.decode(in);
    }

    private ProtocolBody newBodyInstance(final byte type) {
        switch (type) {
        case 0:
            return new FilePublishRequest();
        case 1:
            return new FilePublishRequestAck();
        case 2:
            return new FileChunk();
        case 3:
            return new NormalAck();
        case 4:
            return new Text();
        case 5:
            return new Registry();
        case 6:
            return new MonitorReport();
        case 7:
            return new LogReport();
        default:
            throw new RuntimeException("unsupported message type: " + type);
        }
    }
}
