package com.apm70.fileq.client.publish;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.apm70.fileq.client.conn.ConnectionManager;
import com.apm70.fileq.util.Destroyable;
import com.apm70.fileq.util.LQWaiter;
import com.apm70.fileq.util.MD5Utils;
import com.apm70.fileq.util.PersistenceQueue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilePublisherImpl extends AbstractPublisher implements Destroyable {

    private final String fileTmpPath;
    private final PersistenceQueue<FilePublishContext> publishFilesQueue;
    private final PersistenceQueue<FilePublishContext> publishFailedQueue;
    private final LQWaiter publishTaskWaiter = new LQWaiter("filePublishTaskWaiter");
    private final LQWaiter failedTaskRetryWaiter = new LQWaiter("failedFileTaskRetryWaiter");
    private volatile boolean stoped;
    private final TmpFileClear clearTask;

    public FilePublisherImpl(final ConnectionManager connectionManager, final String fileTmpPath,
            final int fileKeepalivedHours) throws IOException {
        this.fileTmpPath = fileTmpPath;
        this.connectionManager = connectionManager;
        final File dir = new File(fileTmpPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final File store = new File(fileTmpPath, "files-publishing-store.dat");
        final File meta = new File(fileTmpPath, "files-publishing-meta.dat");
        this.publishFilesQueue = new PersistenceQueue<>(meta, store, FilePublishContext.class);
        final File failedStore = new File(fileTmpPath, "files-failed-store.dat");
        final File failedMeta = new File(fileTmpPath, "files-failed-meta.dat");
        this.publishFailedQueue = new PersistenceQueue<>(failedMeta, failedStore, FilePublishContext.class);
        this.stoped = false;
        this.clearTask = new TmpFileClear(fileTmpPath, fileKeepalivedHours);
        this.clearTask.start();
        this.startPublishing();
        this.startPublishFiledRetry();
    }

    @Override
    public void publishAsync(final PublishContext context) {
        this.persistant((FilePublishContext) context);
        this.publishTaskWaiter.signal();
    }

    /**
     * 发送文件之前，先将文件拷贝副本到临时目录,并计算MD5值，持久化文件信息到队列
     *
     * @param context
     */
    private void persistant(final FilePublishContext context) {
        // 文件拷贝副本到临时目录,并计算MD5值
        final File dir = new File(this.fileTmpPath + File.separator + LocalDate.now().toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final File copy = new File(dir, String.valueOf(LocalDateTime.now().getNano()));
        if (context.getFilePath() != null) {
            this.persistFile(context, copy);
        } else if (context.getFileContent() != null) {
            this.persistBytes(context, copy);
        }
    }

    /**
     * 持久化字节码到临时文件
     *
     * @param context
     * @param copy
     */
    private void persistBytes(final FilePublishContext context, final File copy) {
        try (RandomAccessFile target = new RandomAccessFile(copy, "rw")) {
            final FileChannel writeChannel = target.getChannel();
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            ByteBuffer bf = ByteBuffer.allocate(8);
            final int length = context.getFileContent().length;
            bf.putLong(length);
            bf.flip();
            digest.update(bf);
            bf.clear();
            bf = ByteBuffer.wrap(context.getFileContent());
            bf.mark();
            digest.update(bf);
            bf.reset();
            writeChannel.write(bf);
            bf.clear();
            context.setFileSize(length);
            context.setFilePath(copy.getAbsolutePath());
            context.setFileContent(null);
            context.setFileMD5(MD5Utils.encodeHex(digest.digest()));
            context.setFilename("Text2File.dat");
            // 持久化文件信息到队列
            this.publishFilesQueue.add(context);
            this.notifyAllListener(PublishState.queuing(context.topicNo, context.businessId));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + "MD5" + "\"", e);
        } catch (final IOException e) {
            throw new IllegalStateException("Read file error", e);
        }
    }

    /**
     * 持久化文件到临时目录
     *
     * @param context
     * @param copy
     */
    private void persistFile(final FilePublishContext context, final File copy) {
        final File file = new File(context.getFilePath());
        try (RandomAccessFile source = new RandomAccessFile(file, "r");
                RandomAccessFile target = new RandomAccessFile(copy, "rw")) {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            final ByteBuffer bf = ByteBuffer.allocate(64 * 1024 * 1024);
            bf.putLong(source.length());
            bf.flip();
            digest.update(bf);
            bf.clear();
            final FileChannel channel = source.getChannel();
            final FileChannel writeChannel = target.getChannel();
            while (channel.read(bf) > 0) {
                bf.flip();
                bf.mark();
                digest.update(bf);
                bf.reset();
                writeChannel.write(bf);
                bf.clear();
            }
            context.setFileSize(file.length());
            context.setFilePath(copy.getAbsolutePath());
            context.setFileMD5(MD5Utils.encodeHex(digest.digest()));
            context.setFilename(file.getName());
            // 持久化文件信息到队列
            this.publishFilesQueue.add(context);
            this.notifyAllListener(PublishState.queuing(context.topicNo, context.businessId));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + "MD5" + "\"", e);
        } catch (final IOException e) {
            throw new IllegalStateException("Read file error", e);
        }
    }

    private void startPublishing() {
        final FilePublishTask task = new FilePublishTask();
        task.setConnectionManager(this.connectionManager);
        new Thread((Runnable) () -> {
            while (!FilePublisherImpl.this.stoped) {
                final FilePublishContext context = FilePublisherImpl.this.publishFilesQueue.peek();
                if (context == null) {
                    FilePublisherImpl.this.publishTaskWaiter.await();
                    continue;
                }
                try {
                    // 通知业务开始发送
                    this.notifyAllListener(PublishState.publishing(context.topicNo, context.businessId));

                    final PublishState state = task.publish(context);
                    FilePublisherImpl.this.notifyAllListener(state);// 通知所有监听发布状态
                    if (state.getState() == 3) {
                        // 发布失败，写发布任务到失败队列
                        FilePublisherImpl.log.error("LQCLIENT 文件发布失败。失败原因：{}", state.getError());
                        FilePublisherImpl.this.publishFailedQueue.add(context);
                    }
                } catch (final Exception e1) {
                    // 发布失败，写发布任务到失败队列
                    FilePublisherImpl.log.error("LQCLIENT 文件发布失败。", e1);
                    FilePublisherImpl.this.publishFailedQueue.add(context);
                } finally {
                    try {
                        FilePublisherImpl.this.publishFilesQueue.poll();
                    } catch (final Exception e2) {
                        FilePublisherImpl.log.error("", e2);
                    }
                }
            }
        }, "file-publishing-thread").start();
    }

    private void startPublishFiledRetry() {
        final FilePublishTask task = new FilePublishTask();
        task.setConnectionManager(this.connectionManager);
        new Thread((Runnable) () -> {
            while (!FilePublisherImpl.this.stoped) {
                FilePublisherImpl.this.failedTaskRetryWaiter.await(30);
                final FilePublishContext context = FilePublisherImpl.this.publishFailedQueue.peek();
                if (context == null) {
                    continue;
                }

                boolean isCancelPublish = false;
                for (final WaitCancelRepublish w : AbstractPublisher.cancelRepublish) {
                    if (w.getBusinessId().equals(context.getBusinessId())
                            && w.getTopicNo().equals(context.getTopicNo())) {
                        isCancelPublish = true;
                        AbstractPublisher.cancelRepublish.remove(w);
                        FilePublisherImpl.this.publishFailedQueue.poll();
                    }
                    final Date today = new Date();
                    if (today.getTime() - w.getCreateDate().getTime() > 24 * 3600000) {
                        AbstractPublisher.cancelRepublish.remove(w);
                    }
                }
                if (!isCancelPublish) {
                    try {
                        if (!new File(context.getFilePath()).exists()) {
                            // 文件不存在，应该是文件存放超时被清理，放弃发布
                            FilePublisherImpl.log.warn("LQCLIENT 文件本地存放时间超时，放弃发布，发布信息：{}", context);
                            this.notifyAllListener(
                                    PublishState.failure(context.topicNo, context.businessId, "文件不存在，可能是重试超时，放弃发布"));
                            continue;
                        }
                        final PublishState state = task.publish(context);
                        FilePublisherImpl.this.notifyAllListener(state);// 通知所有监听发布状态
                        if (state.getState() == 3) {
                            // 发布失败，写发布任务到失败队列
                            FilePublisherImpl.log.error("LQCLIENT 文件发布失败。失败原因：{}", state.getError());
                            FilePublisherImpl.this.publishFailedQueue.add(context);
                        }
                    } catch (final Exception e1) {
                        // 发布失败，写发布任务到失败队列
                        FilePublisherImpl.log.error("LQCLIENT 文件发布失败。", e1);
                        FilePublisherImpl.this.publishFailedQueue.add(context);
                    } finally {
                        try {
                            FilePublisherImpl.this.publishFailedQueue.poll();
                        } catch (final Exception e2) {
                            FilePublisherImpl.log.error("", e2);
                        }
                    }
                }
            }
        }, "file-publish-retry-thread").start();
    }

    @Override
    public void destroy() {
        this.stoped = true;
        this.publishTaskWaiter.signal();
        this.failedTaskRetryWaiter.signal();
        this.clearTask.stop();
    }

    @Override
    public boolean matches(final PublishContext context) {
        return context instanceof FilePublishContext;
    }

    class TmpFileClear {
        private Timer timer;
        private final String scanPath;
        private final long keepalivedTime;

        public TmpFileClear(final String scanPath, final int keepaliveHours) {
            this.scanPath = scanPath;
            this.keepalivedTime = TimeUnit.HOURS.toMillis(keepaliveHours);
        }

        public void start() {
            this.timer = new Timer("clear-temp-files");
            this.timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        final File root = new File(TmpFileClear.this.scanPath);
                        if (!root.exists()) {
                            return;
                        }
                        final File[] dirs = root.listFiles((FileFilter) f -> f.isDirectory());
                        for (final File dir : dirs) {
                            TmpFileClear.this.scanAndClearDir(dir);
                        }
                    } catch (final Throwable e) {
                        FilePublisherImpl.log.error(e.getMessage(), e);
                    }
                }
            }, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1));
        }

        private void scanAndClearDir(final File dir) {
            final File[] files = dir.listFiles();
            if (files.length == 0) {
                dir.delete();
            }
            for (final File f : files) {
                if (f.lastModified() + this.keepalivedTime < System.currentTimeMillis()) {
                    f.delete();
                }
            }
        }

        public void stop() {
            this.timer.cancel();
        }
    }
}
