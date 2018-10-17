package com.apm70.fileq.protocol.message;

import com.apm70.fileq.config.Constants;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Registry implements ProtocolBody {

    private byte[] signature;
    private long timestamp;
    private String network;
    private String clientName;

    @Override
    public byte getType() {
        return 5;
    }

    @Override
    public int getLength() {
        return 16 + 8 + 4 + this.getClientNameLength();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeBytes(this.signature);
        out.writeLong(this.timestamp);
        for (final String value : this.network.split("\\.")) {
            out.writeByte(Integer.parseInt(value));
        }
        out.writeBytes(this.clientName.getBytes(Constants.defaultCharset));
    }

    @Override
    public void decode(final ByteBuf in) {
        this.signature = new byte[16];
        in.readBytes(this.signature);
        this.timestamp = in.readLong();
        for (int i = 0; i < 4; i++) {
            if (this.network == null) {
                this.network = "";
            } else {
                this.network += ".";
            }
            this.network += String.valueOf(in.readUnsignedByte());
        }
    }

    private int getClientNameLength() {
        return this.clientName.getBytes(Constants.defaultCharset).length;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Registry(")
                //.append("network=[").append(this.network.length).append("]bytes array")
                .append(")");
        return builder.toString();
    }

}
