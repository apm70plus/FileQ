package com.apm70.fileq.protocol.message;

import io.netty.buffer.ByteBuf;

public interface ProtocolBody {

    byte getType();

    int getLength();

    void encode(ByteBuf out);

    void decode(ByteBuf in);
}
