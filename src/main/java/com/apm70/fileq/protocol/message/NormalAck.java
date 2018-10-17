package com.apm70.fileq.protocol.message;

import com.apm70.fileq.config.Constants;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NormalAck implements ProtocolBody {

    private byte status; // 数据接收状况 0：失败，1:成功，2:重传

    private String cause;

    public boolean isSuccess() {
        return this.status == 1;
    }

    public boolean isFailure() {
        return this.status == 0;
    }

    public boolean isRedo() {
        return this.status == 2;
    }

    @Override
    public byte getType() {
        return 3;
    }

    @Override
    public int getLength() {
        return 1 + (this.cause == null ? 0 : this.cause.getBytes().length);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("NormalAck(")
                .append("type=").append(this.getType()).append(", ")
                .append("status=").append(this.status).append(", ")
                .append("cause=").append(this.cause)
                .append(")");
        return builder.toString();
    }

    @Override
    public void encode(final ByteBuf out) {
        out.writeByte(this.getStatus());
        if (this.getCause() != null) {
            out.writeBytes(this.getCause().getBytes(Constants.defaultCharset));
        }
    }

    @Override
    public void decode(final ByteBuf in) {
        this.setStatus(in.readByte());
        if (in.readableBytes() > 0) {
            final byte[] cause = new byte[in.readableBytes()];
            in.readBytes(cause);
            this.setCause(new String(cause, Constants.defaultCharset));
        }
    }

}
