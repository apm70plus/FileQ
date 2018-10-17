package com.apm70.fileq.protocol.message;

import com.apm70.fileq.util.MD5Utils;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

/**
 * 文件分片数据
 * 
 * @author liuyg
 */
@Getter
@Setter
public class FileChunk implements ProtocolBody {

    private byte[] fileFlag;

    private int chunkNo;

    private byte[] payload;

    @Override
    public byte getType() {
        return 2;
    }

    @Override
    public int getLength() {
        return 16 + 2 + this.payload.length;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("FileChunk(")
                .append("type=").append(this.getType()).append(", ")
                .append("fileFlag=").append(MD5Utils.encodeHex(this.fileFlag)).append(", ")
                .append("chunkNo=").append(this.chunkNo).append(", ")
                .append("payload=[").append(this.payload.length).append("]bytes array")
                .append(")");
        return builder.toString();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeBytes(this.getFileFlag());
        out.writeShort(this.getChunkNo());
        out.writeBytes(this.getPayload());
    }

    @Override
    public void decode(final ByteBuf in) {
        this.fileFlag = new byte[16];
        in.readBytes(this.fileFlag);
        this.setFileFlag(this.fileFlag);
        this.setChunkNo(in.readUnsignedShort());
        final byte[] payload = new byte[in.readableBytes()];
        in.readBytes(payload);
        this.setPayload(payload);
    }
}
