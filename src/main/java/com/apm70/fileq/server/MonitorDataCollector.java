package com.apm70.fileq.server;

import java.util.Map;

public interface MonitorDataCollector {

    void collect(String clientNo, long time, Map<String, Double> metrics);
}
