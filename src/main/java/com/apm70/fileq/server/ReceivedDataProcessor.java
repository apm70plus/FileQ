package com.apm70.fileq.server;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.apm70.fileq.protocol.message.LogReport;
import com.apm70.fileq.protocol.message.MonitorReport;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.server.service.NodeRegistryService;
import com.apm70.fileq.server.service.FilePublishService;
import com.apm70.fileq.server.service.TextPublishService;
import com.apm70.fileq.server.service.NodeRegistryService.NodeInfo;
import com.apm70.fileq.util.ClientUtils;

import io.netty.channel.ChannelHandlerContext;
import lombok.Setter;

public class ReceivedDataProcessor {

    @Setter
    private FilePublishService filePublishService;
    @Setter
    private TextPublishService textPublishService;
    @Setter
    private NodeRegistryService nodeRegistryService;
    @Setter
    private MonitorDataCollector monitorDataCollector;
    @Setter
    private NodeLogsCollector nodeLogsCollector;

    public void process(final ChannelHandlerContext ctx, final ProtocolMessage msg) {

        switch (msg.getType()) {
        case 0:
            this.filePublishService.handleFilePublishReq(ctx, msg);
            break;
        case 2:
            this.filePublishService.handleFileChunk(ctx, msg);
            break;
        case 4:
            this.textPublishService.handleTextPublish(ctx, msg);
            break;
        case 5:
            this.nodeRegistryService.registry(ctx, msg);
            break;
        case 6:
            this.handleMonitorReport(ctx, msg);
            break;
        case 7:
            this.handleLogReport(ctx, msg);
        default:
            // unsupports
        }
    }

    private void handleLogReport(final ChannelHandlerContext ctx, final ProtocolMessage msg) {
        final NodeInfo client = ClientUtils.getClientInfo(ctx.channel());
        if (client == null) {
            return;
        }
        if (this.nodeLogsCollector == null) {
            return;
        }
        final LogReport report = (LogReport) msg.getBody();
        try {
            final String[] logs = JSON.parseObject(report.getLogs(), String[].class);
            this.nodeLogsCollector.collect(client.getNodeNo(), logs);
        } catch (final Throwable e) {
        }
    }

    private void handleMonitorReport(final ChannelHandlerContext ctx, final ProtocolMessage msg) {
        final NodeInfo client = ClientUtils.getClientInfo(ctx.channel());
        if (client == null) {
            return;
        }
        client.updateLatestActiveTime(); // 更新客户端最新活跃时间
        final MonitorReport report = (MonitorReport) msg.getBody();
        if (this.monitorDataCollector == null) {
            return;
        }
        try {
            final Map<String, Double> metrics =
                    JSON.parseObject(report.getContent(), new TypeReference<HashMap<String, Double>>() {
                    });
            this.monitorDataCollector.collect(client.getNodeNo(), report.getTime(), metrics);
        } catch (final Throwable e) {
        }
    }
}
