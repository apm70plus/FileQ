package com.apm70.fileq.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface FileStore {

    File addFile(String fileIdentifier, String filename, long fileSize, int chunkCount) throws IOException;

    File getFile(String fileIdentifier) throws FileNotFoundException;

    boolean fileExists(String fileIdentifier);

    boolean isFileComplete(String fileIdentifier) throws FileNotFoundException;

    int breakPointChunkNo(String fileIdentifier) throws FileNotFoundException;

    void mergeFileChunk(String fileIdentifier, int chunkNo, byte[] data) throws IOException;
}
