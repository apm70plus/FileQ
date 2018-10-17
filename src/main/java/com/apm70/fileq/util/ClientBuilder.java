package com.apm70.fileq.util;

import java.io.IOException;

import com.apm70.fileq.client.Client;
import com.apm70.fileq.client.conn.ConnectionManager;
import com.apm70.fileq.client.monitor.MonitorAgent;
import com.apm70.fileq.client.publish.FilePublisherImpl;
import com.apm70.fileq.client.publish.PublisherComposite;
import com.apm70.fileq.client.publish.TextPublisherImpl;
import com.apm70.fileq.config.Configuration;

public final class ClientBuilder {

    private final Configuration config = new Configuration();

    public ClientBuilder connKeepalivedSeconds(final int keepalivedSeconds) {
        this.config.setConnKeepalivedSeconds(keepalivedSeconds);
        return this;
    }

    public ClientBuilder clientFileKeepalivedHours(final int keepalivedHours) {
        this.config.setClientFileKeepalivedHours(keepalivedHours);
        return this;
    }

    public ClientBuilder ackTimeout(final int ackTimeout) {
        this.config.setAckTimeout(ackTimeout);
        return this;
    }

    public ClientBuilder storePath(final String storePath) {
        this.config.setClientStorePath(storePath);
        return this;
    }

    public ClientBuilder binaryLogging(final boolean dolog) {
        this.config.setBinaryLogging(dolog);
        return this;
    }

    public ClientBuilder monitorReport(final boolean reporting) {
        this.config.setMonitorReport(reporting);
        return this;
    }

    public ClientBuilder reportHost(final String reportHost) {
        this.config.setReportHost(reportHost);
        return this;
    }

    public ClientBuilder reportPort(final int reportPort) {
        this.config.setReportPort(reportPort);
        return this;
    }

    public ClientBuilder clientIp(final String clientIp) {
        this.config.setClientIp(clientIp);
        return this;
    }

    public ClientBuilder secretKey(final String secretKey) {
        this.config.setSecretKey(secretKey);
        return this;
    }

    public Client build() throws IOException {
        if (this.config.getConnKeepalivedSeconds() == 0) {
            throw new RuntimeException("Connection keepalivedTimeout can not be empty!");
        }
        if (this.config.getAckTimeout() == 0) {
            throw new RuntimeException("ackTimeout can not be empty!");
        }
        if (this.config.getClientStorePath() == null) {
            throw new RuntimeException("storePath can not be empty!");
        }
        if (this.config.getClientFileKeepalivedHours() == 0) {// 默认24小时
            this.config.setClientFileKeepalivedHours(24);
        }
        final CallbackDispatcher callbackDispatcher = new CallbackDispatcher();
        callbackDispatcher.setExpiredInterval(this.config.getAckTimeout());
        DestroyBeanShutdownHook.addLast(callbackDispatcher);

        final ConnectionManager connectionManager = ConnectionManager.instance();
        connectionManager.setConfig(this.config);
        connectionManager.setCallbackDispatcher(callbackDispatcher);

        final Client client = new Client();
        final PublisherComposite publishers = new PublisherComposite();
        final FilePublisherImpl filePublisher = new FilePublisherImpl(
                connectionManager, this.config.getClientStorePath(), this.config.getClientFileKeepalivedHours());
        DestroyBeanShutdownHook.addLast(filePublisher);
        final TextPublisherImpl textPublisher =
                new TextPublisherImpl(connectionManager, this.config.getClientStorePath());
        DestroyBeanShutdownHook.addLast(textPublisher);
        publishers.addPublisher(filePublisher);
        publishers.addPublisher(textPublisher);
        client.setPublisher(publishers);

        if (this.config.isMonitorReport()) {
            final MonitorAgent agent = new MonitorAgent();
            agent.setConnMangager(connectionManager);
            agent.setClientName("大连LQ");
            agent.setServerHost(this.config.getReportHost());
            agent.setServerPort(this.config.getReportPort());
            agent.setIpAddress(this.config.getClientIp());
            agent.setSecretKey(this.config.getSecretKey());
            agent.start();
            DestroyBeanShutdownHook.addLast(agent);
        }
        DestroyBeanShutdownHook.addLast(connectionManager);
        return client;
    }
}
