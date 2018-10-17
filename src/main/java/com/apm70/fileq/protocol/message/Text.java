package com.apm70.fileq.protocol.message;

import com.apm70.fileq.config.Constants;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Text implements ProtocolBody {

    private String topic;

    private String businessId;

    private byte[] payload;

    @Override
    public byte getType() {
        return 4;
    }

    @Override
    public int getLength() {
        return this.payload.length
                + 1 + this.getTopicLength()
                + 1 + this.getBusinessIdLength();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeByte(this.getTopicLength());
        out.writeBytes(this.topic.getBytes(Constants.defaultCharset));
        out.writeByte(this.getBusinessIdLength());
        out.writeBytes(this.businessId.getBytes(Constants.defaultCharset));
        out.writeBytes(this.payload);
    }

    @Override
    public void decode(final ByteBuf in) {
        final int topicLen = in.readByte();
        final byte[] topicBs = new byte[topicLen];
        in.readBytes(topicBs);
        this.topic = new String(topicBs, Constants.defaultCharset);
        final int businessIdLen = in.readByte();
        final byte[] bs = new byte[businessIdLen];
        in.readBytes(bs);
        this.businessId = new String(bs, Constants.defaultCharset);
        this.payload = new byte[in.readableBytes()];
        in.readBytes(this.payload);
    }

    private int getBusinessIdLength() {
        return this.businessId.getBytes(Constants.defaultCharset).length;
    }

    private int getTopicLength() {
        return this.topic.getBytes(Constants.defaultCharset).length;
    }
}
