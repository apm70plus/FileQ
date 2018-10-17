package com.apm70.fileq.client.publish;

import java.util.concurrent.atomic.AtomicInteger;

public final class MsgIdGenerator {

    private static final AtomicInteger generator = new AtomicInteger();

    public static int generate() {
        return MsgIdGenerator.generator.incrementAndGet();
    }
}
