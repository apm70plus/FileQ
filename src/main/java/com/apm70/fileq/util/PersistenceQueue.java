package com.apm70.fileq.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 持久化队列
 *
 * @author liuyg
 */
@Slf4j
public class PersistenceQueue<T> implements IQueue<T> {

    private static final Charset utf8 = Charset.forName("UTF-8");
    private static final byte[] END_MARK = ("@END@" + System.lineSeparator()).getBytes(PersistenceQueue.utf8);
    private final File metaFile;
    private final File storeFile;
    private BufferedRandomAccessFile metaData;
    private BufferedRandomAccessFile storeReader;
    private BufferedRandomAccessFile storeWriter;
    private final ReentrantLock lock = new ReentrantLock();
    private AtomicInteger size;
    private final Class<T> clazzType;

    /**
     * 构造函数
     *
     * @param metadata
     * @param store
     * @throws IOException
     */
    public PersistenceQueue(final File metadata, final File store, final Class<T> clazzType) throws IOException {
        this.metaFile = metadata;
        this.storeFile = store;
        this.metaData = new BufferedRandomAccessFile(this.metaFile, "rw");
        this.storeWriter = new BufferedRandomAccessFile(this.storeFile, "rw");
        this.storeReader = new BufferedRandomAccessFile(this.storeFile, "rw");
        this.clazzType = clazzType;
        this.init();
    }

    private void init() throws IOException {
        long readPosition;
        long writePosition;
        if (this.metaData.length() == 0) {
            this.size = new AtomicInteger(0);
            readPosition = 0L;
            writePosition = 0L;
        } else {
            this.size = new AtomicInteger(this.metaData.readIntb());
            if (this.size.get() < 0) {
                PersistenceQueue.log.error("消息队列元数据读取错误，队列size小于0.");
                throw new RuntimeException("消息队列元数据读取错误，队列size小于0.");
            }
            readPosition = this.metaData.readLong();
            writePosition = this.metaData.readLong();
        }
        this.storeReader.seek(readPosition);
        this.storeWriter.seek(writePosition);
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
        this.lock.lock();
        if (this.size() <= 0) {
            this.tryArchive();
        }
        final byte[] bytes = this.toBytes(object);
        try {
            this.storeWriter.writeIntb(bytes.length);
            this.storeWriter.write(bytes);
            this.storeWriter.write(PersistenceQueue.END_MARK);
            this.storeWriter.flush();
            this.size.incrementAndGet();
            this.persistMetaData();
        } catch (final IOException e) {
            PersistenceQueue.log.error("写文件失败，消息内容：" + new String(bytes, PersistenceQueue.utf8), e);
            return false;
        } finally {
            this.lock.unlock();
        }
        return true;
    }

    @Override
    public T peek() {
        if (this.size() <= 0) {
            this.tryArchive();
            return null;
        }

        this.lock.lock();
        try {
            final long readStartPosition = this.storeReader.getFilePointer();
            final byte[] value = this.nextValue();
            if (value == null) {
                return null;
            }
            this.storeReader.seek(readStartPosition);
            return this.toObject(value);
        } catch (final IOException e) {
            return null;
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public T poll() {
        if (this.size() <= 0) {
            this.tryArchive();
            return null;
        }
        this.lock.lock();
        try {
            final byte[] value = this.nextValue();
            this.size.decrementAndGet();
            this.persistMetaData();
            return this.toObject(value);
        } catch (final IOException e) {
            PersistenceQueue.log.error("读文件失败", e);
            return null;
        } finally {
            this.lock.unlock();
        }
    }

    public void close() {
        this.lock.lock();
        try {
            IOUtils.close(this.metaData);
            IOUtils.close(this.storeReader);
            IOUtils.close(this.storeWriter);
        } finally {
            this.lock.unlock();
        }
    }

    private void tryArchive() {
        try {
            if (this.storeWriter.getFilePointer() < 102400000L) {
                return;
            }
        } catch (final Exception e) {
        }
        this.lock.lock();
        try {
            if (this.size.get() > 0) {
                return;
            }
            IOUtils.close(this.metaData);
            IOUtils.close(this.storeReader);
            IOUtils.close(this.storeWriter);
            final String time = LocalDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
            //final String time = DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd'T'HH:mm:ss");
            File targetFile = new File(this.metaFile.getParent(), this.metaFile.getName() + "-" + time);
            Files.move(this.metaFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            targetFile = new File(this.storeFile.getParent(), this.storeFile.getName() + "-" + time);
            Files.move(this.storeFile.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            this.metaData = new BufferedRandomAccessFile(this.metaFile, "rw");
            this.storeWriter = new BufferedRandomAccessFile(this.storeFile, "rw");
            this.storeReader = new BufferedRandomAccessFile(this.storeFile, "rw");
        } catch (final IOException e) {
            PersistenceQueue.log.warn("文件归档失败", e);
        } finally {
            this.lock.unlock();
        }
    }

    private byte[] toBytes(final T object) {
        if (this.clazzType == Class.class) {
            return ((String) object).getBytes(PersistenceQueue.utf8);
        } else {
            return JSON.toJSONBytes(object, SerializerFeature.EMPTY);
        }
    }

    @SuppressWarnings("unchecked")
    protected T toObject(final byte[] jsonBytes) {
        final String text = new String(jsonBytes, PersistenceQueue.utf8);
        if (this.clazzType == Class.class) {
            return (T) text;
        } else {
            return JSON.parseObject(text, this.clazzType);
        }
    }

    private void persistMetaData() {
        try {
            this.metaData.seek(0);
            this.metaData.writeIntb(this.size.get());
            this.metaData.writeLongb(this.storeReader.getFilePointer());
            this.metaData.writeLongb(this.storeWriter.getFilePointer());
            this.metaData.flush();
        } catch (final IOException e) {
        }
    }

    private byte[] nextValue() throws IOException {
        final int size = this.storeReader.readIntb();
        if (size == 0) {
            return null;
        }
        final byte[] value = new byte[size];
        this.storeReader.read(value);
        this.storeReader.seek(this.storeReader.getFilePointer() + PersistenceQueue.END_MARK.length);
        return value;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.metaData.flush();
            this.storeWriter.flush();
        } finally {
            IOUtils.close(this.metaData);
            IOUtils.close(this.storeReader);
            IOUtils.close(this.storeWriter);
        }
        super.finalize();
    }
}
