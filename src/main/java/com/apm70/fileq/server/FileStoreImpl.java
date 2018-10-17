package com.apm70.fileq.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.apm70.fileq.config.Configuration;
import com.apm70.fileq.config.Constants;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;

import lombok.Getter;
import lombok.Setter;

public class FileStoreImpl implements FileStore {

    @Setter
    private Configuration config;

    private final Cache<String, FileOperator> filesCache =
            CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).maximumSize(5000).build();

    @Override
    public File addFile(final String fileIdentifier, final String filename, final long fileSize, final int chunkCount)
            throws IOException {
        final String datePath = LocalDate.now().toString();
        final File parentPath = new File(
                getStoreRootPath() + File.separator + datePath + File.separator + fileIdentifier);
        if (!parentPath.exists()) {
            parentPath.mkdirs();
        }
        final File file = new File(parentPath, filename);
        try (RandomAccessFile accessFile = new RandomAccessFile(file, "rw")) {
            accessFile.setLength(fileSize);
        }
        final File chunks = new File(parentPath, this.getChunksFileName(fileIdentifier));
        try (RandomAccessFile chunksFile = new RandomAccessFile(chunks, "rw")) {
            chunksFile.setLength(chunkCount);
        }
        this.filesCache.put(fileIdentifier, new FileOperator(file, chunks));
        return file;
    }

    @Override
    public boolean fileExists(final String fileIdentifier) {
        final FileOperator fileOperator = this.getFileOperator(fileIdentifier);
        return fileOperator != null;
    }

    @Override
    public File getFile(final String fileIdentifier) throws FileNotFoundException {
        final FileOperator fileOperator = this.getFileOperator(fileIdentifier);
        if (fileOperator == null) {
            throw new FileNotFoundException(fileIdentifier);
        }
        return fileOperator.getFile();
    }

    @Override
    public boolean isFileComplete(final String fileIdentifier) throws FileNotFoundException {
        final FileOperator fileOperator = this.getFileOperator(fileIdentifier);
        if (fileOperator == null) {
            throw new FileNotFoundException(fileIdentifier);
        }
        return fileOperator.isAllChunksComplete();
    }

    @Override
    public int breakPointChunkNo(final String fileIdentifier) throws FileNotFoundException {
        final FileOperator fileOperator = this.getFileOperator(fileIdentifier);
        if (fileOperator == null) {
            throw new FileNotFoundException(fileIdentifier);
        }
        return fileOperator.firstUncomplateChunkNo();
    }

    @Override
    public void mergeFileChunk(final String fileIdentifier, final int chunkNo, final byte[] data) throws IOException {
        final FileOperator fileOperator = this.getFileOperator(fileIdentifier);
        if (fileOperator == null) {
            throw new FileNotFoundException(fileIdentifier);
        }
        fileOperator.mergeFileChunk(chunkNo, data);
    }

    private FileOperator getFileOperator(final String fileIdentifier) {
        FileOperator fileOperator = this.filesCache.getIfPresent(fileIdentifier);
        if (fileOperator != null) {
            return fileOperator;
        }
        final File fileDir = this.findFileDir(fileIdentifier);
        if (fileDir == null) {
            return null;
        }
        final File[] children = fileDir.listFiles();
        if (children[0].getName().equals(this.getChunksFileName(fileIdentifier))) {
            fileOperator = new FileOperator(children[1], children[0]);
        } else {
            fileOperator = new FileOperator(children[0], children[1]);
        }
        this.filesCache.put(fileIdentifier, fileOperator);
        return fileOperator;
    }

    private File findFileDir(final String fileIdentifier) {

        final File rootDir = new File(getStoreRootPath());
        if (!rootDir.exists()) {
            rootDir.mkdirs();
            return null;
        }
        final String[] dateDirs = rootDir.list();
        if ((dateDirs == null) || (dateDirs.length == 0)) {
            return null;
        }
        Arrays.sort(dateDirs, (a, b) -> b.compareTo(a));

        for (final String dateDir : dateDirs) {
            final File[] files =
                    new File(rootDir, dateDir).listFiles((FilenameFilter) (dir, name) -> name.equals(fileIdentifier));
            if ((files != null) && (files.length == 1)) {
                return files[0];
            }
        }
        return null;
    }

    private String getChunksFileName(final String fileIdentifier) {
        return "chunks-" + fileIdentifier;
    }

    class FileOperator {
        @Getter
        private final File file;
        private MappedByteBuffer fileChunksBuffer;

        public FileOperator(final File file, final File chunks) {
            this.file = file;
            try {
                this.fileChunksBuffer = Files.map(chunks, MapMode.READ_WRITE);
            } catch (final IOException e) {
                throw new RuntimeException("读文件" + chunks.getAbsolutePath() + "失败！", e);
            }
        }

        public int firstUncomplateChunkNo() {
            final int limit = this.fileChunksBuffer.limit();
            for (int i = 0; i < limit; i++) {
                if (this.fileChunksBuffer.get(i) == 0) {
                    return i + 1;
                }
            }
            return -1;
        }

        public void mergeFileChunk(final int chunkNo, final byte[] data) throws IOException {
            // 写文件
            try (RandomAccessFile chunksFile = new RandomAccessFile(this.file, "rw")) {
                final FileChannel channel = chunksFile.getChannel();
                final long writePosition = Constants.ChunckSize * (chunkNo - 1);
                channel.position(writePosition);
                channel.write(ByteBuffer.wrap(data));
                channel.force(true);
            }
            this.fileChunksBuffer.put(chunkNo - 1, (byte) 1);
            this.fileChunksBuffer.force();
        }

        public boolean isAllChunksComplete() {
            final int limit = this.fileChunksBuffer.limit();
            for (int i = limit - 1; i >= 0; i--) {
                if (this.fileChunksBuffer.get(i) == 0) {
                    return false;
                }
            }
            return true;
        }
    }

	private String getStoreRootPath() {
		return this.config.getServerStorePath() + File.separator + "files";
	}

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        final File file = new File("/Users/liuyg/develop/data.dat");
        final long time = System.currentTimeMillis();
        try (RandomAccessFile chunksFile = new RandomAccessFile(file, "rw")) {
            chunksFile.setLength(1024l * 1024l * 1024l * 5l);
        }
        System.out.println(System.currentTimeMillis() - time);
    }
}
