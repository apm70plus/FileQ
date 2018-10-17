package com.apm70.fileq.client.conn;

public interface ConnStateListener {

    void onConnStateChanged(Connection conn, boolean isActive);
}
