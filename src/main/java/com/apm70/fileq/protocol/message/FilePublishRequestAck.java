package com.apm70.fileq.protocol.message;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FilePublishRequestAck implements ProtocolBody {

    private int chunkNo;

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public int getLength() {
        return 2;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("FilePublishRequestAck(")
                .append("type=").append(this.getType()).append(", ")
                .append("chunkNo=").append(this.chunkNo)
                .append(")");
        return builder.toString();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeShort(this.getChunkNo());
    }

    @Override
    public void decode(final ByteBuf in) {
        this.setChunkNo(in.readUnsignedShort());
    }
}
