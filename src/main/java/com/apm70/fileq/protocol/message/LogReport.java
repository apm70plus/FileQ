package com.apm70.fileq.protocol.message;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LogReport implements ProtocolBody {

    private byte[] logs;

    @Override
    public byte getType() {
        return 7;
    }

    @Override
    public int getLength() {
        return this.logs.length;
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeBytes(this.getLogs());

    }

    @Override
    public void decode(final ByteBuf in) {
        this.logs = new byte[in.readableBytes()];
        in.readBytes(this.logs);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LogReport(")
                .append("logs=[").append(this.logs.length).append("]bytes array")
                .append(")");
        return builder.toString();
    }
}
