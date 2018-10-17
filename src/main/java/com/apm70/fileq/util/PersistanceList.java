package com.apm70.fileq.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;
import com.apm70.fileq.config.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PersistanceList<T> implements IList<T> {

    private final File indexFile;
    private final File storeFile;
    private final BufferedRandomAccessFile indexReader;
    private final BufferedRandomAccessFile indexWriter;
    private final BufferedRandomAccessFile storeReader;
    private final BufferedRandomAccessFile storeWriter;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ReentrantLock readLock = new ReentrantLock();
    private AtomicInteger size;
    private final Class<T> clazzType;
    private final static int INDEX_BYTES = 12;

    public PersistanceList(final File index, final File store, final Class<T> clazzType) throws IOException {
        this.indexFile = index;
        this.storeFile = store;
        this.indexReader = new BufferedRandomAccessFile(this.indexFile, "rw");
        this.indexWriter = new BufferedRandomAccessFile(this.indexFile, "rw");
        this.storeWriter = new BufferedRandomAccessFile(this.storeFile, "rw");
        this.storeReader = new BufferedRandomAccessFile(this.storeFile, "rw");
        this.clazzType = clazzType;
        this.init();
    }

    private void init() throws IOException {
        if (this.indexWriter.length() == 0) {
            this.size = new AtomicInteger(0);
        } else {
            this.size = new AtomicInteger((int) (this.indexWriter.length() / INDEX_BYTES));
        }
        this.indexWriter.seek(this.indexWriter.length());
        this.storeWriter.seek(this.storeWriter.length());
    }

    @Override
    public int size() {
        return this.size.get();
    }

    @Override
    public boolean add(final T object) {
        if (object == null) {
            return false;
        }
        this.writeLock.lock();
        final byte[] bytes = this.toBytes(object);
        try {
            final long writePointer = this.storeWriter.getFilePointer();
            this.storeWriter.write(bytes);
            this.storeWriter.flush();
            this.indexWriter.writeLongb(writePointer);
            this.indexWriter.writeIntb((int) bytes.length);
            this.indexWriter.flush();
            this.size.incrementAndGet();
        } catch (final IOException e) {
            PersistanceList.log.error("写文件失败，消息内容：" + new String(bytes, Constants.defaultCharset), e);
            return false;
        } finally {
            this.writeLock.unlock();
        }
        return true;
    }

    @Override
    public T get(final int i) {
        if ((i + 1) > this.size()) {
            return null;
        }
        this.readLock.lock();
        try {
            this.indexReader.seek(1L * i * INDEX_BYTES);
            final long position = this.indexReader.readLong();
            final int length = this.indexReader.readInt();
            this.storeReader.seek(position);
            final byte[] body = new byte[length];
            this.storeReader.read(body);
            return this.toObject(body);
        } catch (final IOException e) {
            PersistanceList.log.error("读文件失败，索引：" + i, e);
            return null;
        } finally {
            this.readLock.unlock();
        }
    }

    private byte[] toBytes(final T object) {
        if (this.clazzType == Class.class) {
            return ((String) object).getBytes(Constants.defaultCharset);
        } else {
            return JSON.toJSONBytes(object, SerializerFeature.EMPTY);
        }
    }

    @SuppressWarnings("unchecked")
    protected T toObject(final byte[] jsonBytes) {
        final String text = new String(jsonBytes, Constants.defaultCharset);
        if (this.clazzType == String.class) {
            return (T) text;
        } else {
            return JSON.parseObject(text, this.clazzType);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.indexWriter.flush();
            this.storeWriter.flush();
        } finally {
            IOUtils.close(this.indexReader);
            IOUtils.close(this.indexWriter);
            IOUtils.close(this.storeReader);
            IOUtils.close(this.storeWriter);
        }
        super.finalize();
    }
}
