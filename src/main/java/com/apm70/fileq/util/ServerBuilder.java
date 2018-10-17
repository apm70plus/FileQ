package com.apm70.fileq.util;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import com.apm70.fileq.config.Configuration;
import com.apm70.fileq.protocol.handler.MessageHandler;
import com.apm70.fileq.protocol.handler.ServerExceptionHandler;
import com.apm70.fileq.protocol.message.Registry;
import com.apm70.fileq.server.NodeLogsCollector;
import com.apm70.fileq.server.FileStoreImpl;
import com.apm70.fileq.server.Server;
import com.apm70.fileq.server.MonitorDataCollector;
import com.apm70.fileq.server.ReceivedDataProcessor;
import com.apm70.fileq.server.service.NodeRegistryService;
import com.apm70.fileq.server.service.FilePublishService;
import com.apm70.fileq.server.service.TextPublishService;
import com.apm70.fileq.server.service.TopicPublishService;
import com.apm70.fileq.server.topic.DefaultTopicClearStrategy;
import com.apm70.fileq.server.topic.TopicMessageQueue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServerBuilder {

    private final Configuration config = new Configuration();

    public ServerBuilder bind(final String serverHost, final int serverPort) {
        this.config.setServerHost(serverHost);
        this.config.setServerPort(serverPort);
        return this;
    }

    public ServerBuilder connKeepalivedSeconds(final int connKeepalivedSeconds) {
        this.config.setConnKeepalivedSeconds(connKeepalivedSeconds);
        return this;
    }

    public ServerBuilder storePath(final String storePath) {
        this.config.setServerStorePath(storePath);
        return this;
    }

    public ServerBuilder binaryLogging(final boolean dolog) {
        this.config.setBinaryLogging(dolog);
        return this;
    }

    public ServerBuilder topicMsgKeepalivedFragments(final int topicMsgKeepalivedFragments) {
        this.config.setTopicMsgKeepalivedFragments(topicMsgKeepalivedFragments);
        return this;
    }

    public ServerBuilder topicMsgKeepalivedHours(final int topicMsgKeepalivedHours) {
        this.config.setTopicMsgKeepalivedHours(topicMsgKeepalivedHours);
        return this;
    }

    public ServerBuilder serverInflowSpeedLimit(final int limit) {
        this.config.setServerInflowSpeedLimit(limit);
        return this;
    }

    public ServerBuilder monitorDataCollector(final MonitorDataCollector monitorDataCollector) {
        this.config.setMonitorDataCollector(monitorDataCollector);
        return this;
    }
    
    public ServerBuilder clientNoSupplier(Function<Registry, String> clientNoSupplier) {
    		this.config.setClientNoSupplier(clientNoSupplier);
    		return this;
    }
    
    public ServerBuilder secretKey(final String secretKey) {
        this.config.setSecretKey(secretKey);
        return this;
    }

    public Server build() throws IOException {
        if (this.config.getServerHost() == null) {
            this.config.setServerHost("0.0.0.0");
        }
        if (this.config.getServerPort() == 0) {
            throw new RuntimeException("LqServer listener port can not be empty!");
        }
        if (this.config.getConnKeepalivedSeconds() == 0) {
            throw new RuntimeException("Connection keepalivedSeconds can not be empty!");
        }
        if (this.config.getServerStorePath() == null) {
            throw new RuntimeException("storePath can not be empty!");
        }

        // topic message index store
        final String topicMsgQueueDir = this.config.getServerStorePath() + File.separator + "topics";
        final TopicMessageQueue topicMessageQueue = new TopicMessageQueue(topicMsgQueueDir);
        final DefaultTopicClearStrategy clearStrategy = new DefaultTopicClearStrategy(topicMsgQueueDir,
                this.config.getTopicMsgKeepalivedFragments(), this.config.getTopicMsgKeepalivedHours());
        topicMessageQueue.setClearStrategy(clearStrategy);
        final TopicPublishService topicPublishService = new TopicPublishService();
        topicPublishService.setTopicMessageQueue(topicMessageQueue);
        
        DestroyBeanShutdownHook.addLast(topicMessageQueue);
        DestroyBeanShutdownHook.addLast(clearStrategy);

        // 文件发布服务
        final FileStoreImpl fileStore = new FileStoreImpl();
        fileStore.setConfig(this.config);
        final FilePublishService filePublishService = new FilePublishService();
        filePublishService.setFileStore(fileStore);
        filePublishService.setTopicPublishService(topicPublishService);
        

        final TextPublishService textPublishService = new TextPublishService();
        textPublishService.setTopicPublishService(topicPublishService);

        NodeRegistryService nodeRegistryService = new NodeRegistryService();
        Function<Registry, String> clientNoSupplier = config.getClientNoSupplier();
		if (clientNoSupplier == null) {
			clientNoSupplier = r -> { return r.getNetwork().replace(".", ""); };
        }
		nodeRegistryService.setNodeNoGetter(clientNoSupplier);
		nodeRegistryService.setSecretKey(config.getSecretKey());
		
        final ReceivedDataProcessor dataProcessor = new ReceivedDataProcessor();
        dataProcessor.setFilePublishService(filePublishService);
        dataProcessor.setTextPublishService(textPublishService);
		dataProcessor.setNodeRegistryService(nodeRegistryService);
		
        MonitorDataCollector collector = this.config.getMonitorDataCollector();
        if (collector == null) {
            collector = (no, time, json) -> {
                log.info("收到客户端 [{}] 监控上报", no);
            };
        }
        dataProcessor.setMonitorDataCollector(collector);
        NodeLogsCollector logCollector = this.config.getClientLogsCollector();
        if (logCollector == null) {
            logCollector = (no, array) -> {
                log.info("收到客户端 [{}] 日志上报", no);
            };
        }
        dataProcessor.setNodeLogsCollector(logCollector);

        final MessageHandler handler = new MessageHandler();
        handler.setProcessor(dataProcessor);
        DestroyBeanShutdownHook.addLast(handler);
        
        final Server server = new Server();
        server.setConfig(this.config);
        server.setMessageHandler(handler);
        server.setServerExceptionHandler(new ServerExceptionHandler());
        server.setTopicMessageQueue(topicMessageQueue);
        if (this.config.getServerInflowSpeedLimit() > 0) {
            final FlowLimiter flowLimiter = new FlowLimiter(this.config.getServerInflowSpeedLimit());
            server.setFlowLimiter(flowLimiter);
        }
        
        DestroyBeanShutdownHook.addFirst(server);
        return server;
    }
}
