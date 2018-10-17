package com.apm70.fileq.server;

public interface NodeLogsCollector {

    void collect(String clientNo, String[] logs);
}
