package com.apm70.fileq.client.conn;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import com.apm70.fileq.config.Configuration;
import com.apm70.fileq.util.CallbackDispatcher;
import com.apm70.fileq.util.Destroyable;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import lombok.Setter;

public class ConnectionManager implements Destroyable {

    private static ConnectionManager instance = new ConnectionManager();

    @Setter
    @Getter
    private Configuration config;
    @Setter
    private CallbackDispatcher callbackDispatcher;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final Map<String, Connection> connectionPool = new HashMap<>();

    private ConnectionManager() {
    }

    public static ConnectionManager instance() {
        return instance;
    }

    public Connection getConnection(final String serverHost, final int port) {
        Connection conn = this.connectionPool.get(serverHost + ":" + port);
        if (conn != null) {
            return conn;
        }
        synchronized (this.connectionPool) {
            conn = this.connectionPool.get(serverHost + ":" + port);
            if (conn != null) {
                return conn;
            }
            conn = new Connection(serverHost, port, this.config, this.workerGroup, this.callbackDispatcher);
            this.connectionPool.put(serverHost + ":" + port, conn);
            return conn;
        }
    }

    @PreDestroy
    public void destroy() {
        this.workerGroup.shutdownGracefully();
    }
}
