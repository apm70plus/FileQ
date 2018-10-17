package com.apm70.fileq.protocol.message;

import com.apm70.fileq.config.Constants;
import com.apm70.fileq.util.MD5Utils;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

/**
 * （文件）数据发布请求
 *
 * @author liuyg
 */
@Getter
@Setter
public class FilePublishRequest implements ProtocolBody {

    private byte[] fileFlag;

    private int chunkCount;

    private String filename;

    private long fileSize;

    private String topic;

    private String businessId;

    @Override
    public byte getType() {
        return 0;
    }

    @Override
    public int getLength() {
        return 16 + 2
                + 1 + this.getFilenameLength() + 8
                + 1 + this.getBusinessIdLength()
                + this.topic.getBytes(Constants.defaultCharset).length;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("FilePublishRequest(")
                .append("type=").append(this.getType()).append(", ")
                .append("fileFlag=").append(MD5Utils.encodeHex(this.fileFlag)).append(", ")
                .append("chunkCount=").append(this.chunkCount).append(", ")
                .append("filename=").append(this.filename).append(", ")
                .append("fileSize=").append(this.fileSize).append(", ")
                .append("businessId=").append(this.businessId).append(", ")
                .append("topic=").append(this.topic)
                .append(")");
        return builder.toString();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeBytes(this.getFileFlag());
        out.writeShort(this.getChunkCount());
        final byte filenameLength = this.getFilenameLength();
        out.writeByte(filenameLength);
        out.writeBytes(this.getFilename().getBytes(Constants.defaultCharset));
        out.writeLong(this.getFileSize());
        final byte businessIdLength = this.getBusinessIdLength();
        out.writeByte(businessIdLength);
        out.writeBytes(this.getBusinessId().getBytes(Constants.defaultCharset));
        out.writeBytes(this.getTopic().getBytes(Constants.defaultCharset));
    }

    @Override
    public void decode(final ByteBuf in) {
        // flag
        final byte[] fileFlag = new byte[16];
        in.readBytes(fileFlag);
        this.setFileFlag(fileFlag);
        // chunkCount
        this.setChunkCount(in.readUnsignedShort());
        // filename
        final byte filenameLength = in.readByte();
        final byte[] name = new byte[filenameLength];
        in.readBytes(name);
        this.setFilename(new String(name, Constants.defaultCharset));
        // fileSize
        this.setFileSize(in.readLong());
        // businessId
        final byte businessIdLength = in.readByte();
        final byte[] businessId = new byte[businessIdLength];
        in.readBytes(businessId);
        this.setBusinessId(new String(businessId, Constants.defaultCharset));
        // topic
        final byte[] topic = new byte[in.readableBytes()];
        in.readBytes(topic);
        this.setTopic(new String(topic, Constants.defaultCharset));
    }

    private byte getFilenameLength() {
        return (byte) this.filename.getBytes(Constants.defaultCharset).length;
    }

    private byte getBusinessIdLength() {
        return (byte) this.businessId.getBytes(Constants.defaultCharset).length;
    }
}
