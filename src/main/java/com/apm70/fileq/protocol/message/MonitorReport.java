package com.apm70.fileq.protocol.message;

import com.apm70.fileq.config.Constants;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MonitorReport implements ProtocolBody {

    private long time;
    private String content;

    @Override
    public byte getType() {
        return 6;
    }

    @Override
    public int getLength() {
        return 8 + this.content.length();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeLong(this.time);
        out.writeBytes(this.content.getBytes(Constants.defaultCharset));
    }

    @Override
    public void decode(final ByteBuf in) {
        this.time = in.readLong();
        final byte[] content = new byte[in.readableBytes()];
        in.readBytes(content);
        this.setContent(new String(content, Constants.defaultCharset));
    }

}
