package com.apm70.fileq.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class PersistanceCounter {
    private static final String filename = "Counter.data";
    private final AtomicLong count = new AtomicLong();
    private final FileOutputStream writer;
    private final byte[] buf = new byte[8];
    private final ByteBuffer buffer = ByteBuffer.wrap(this.buf);

    public PersistanceCounter(final String storeDir) throws IOException {
        this.writer = new FileOutputStream(storeDir + File.separator + PersistanceCounter.filename, false);
        try (FileInputStream reader = new FileInputStream(storeDir + File.separator + PersistanceCounter.filename)) {
            final int size = reader.read(this.buf);
            if (size <= 0) {
                this.count.set(0L);
            } else {
                this.count.set(this.buffer.getLong(0));
            }
        }
    }

    public long getAndIncrement() {
        final long value = this.count.incrementAndGet();
        this.buffer.putLong(0, value);
        try {
            this.writer.write(this.buf);
            if (this.buf[7] == 0) {
                this.writer.flush();
            }
        } catch (final IOException e) {
        }
        return value - 1;
    }

    public long get() {
        return this.count.get();
    }

    /**
     * 析构函数，刷写操作到文件，关闭文件
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            this.writer.flush();
        } catch (final Exception e) {
        } finally {
            try {
                this.writer.close();
            } catch (final IOException e) {
            }
        }
        super.finalize();
    }
}
