package com.apm70.fileq.client.monitor;

import java.util.concurrent.TimeUnit;

import com.apm70.fileq.client.conn.Connection;
import com.apm70.fileq.client.conn.ConnectionManager;
import com.apm70.fileq.config.Constants;
import com.apm70.fileq.metrics.CpuGaugeSet;
import com.apm70.fileq.metrics.DiskGaugeSet;
import com.apm70.fileq.metrics.LQReporter;
import com.apm70.fileq.metrics.MemoryUsageGaugeSet;
import com.apm70.fileq.metrics.NetInterfaceGaugeSet;
import com.apm70.fileq.metrics.SigarService;
import com.apm70.fileq.protocol.message.MonitorReport;
import com.apm70.fileq.protocol.message.NormalAck;
import com.apm70.fileq.protocol.message.ProtocolMessage;
import com.apm70.fileq.protocol.message.Registry;
import com.apm70.fileq.util.Destroyable;
import com.apm70.fileq.util.MD5Utils;
import com.codahale.metrics.MetricRegistry;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitorAgent implements Destroyable {

    @Setter
    private ConnectionManager connMangager;
    @Setter
    private String serverHost;
    @Setter
    private int serverPort;
    @Setter
    private String clientName;
    @Setter
    private String ipAddress;
    @Setter
    private String secretKey;
    private LQReporter reporter;
    private boolean registry;

    public void start() {
        final SigarService sigar = new SigarService();
        final MetricRegistry registry = new MetricRegistry();
        this.reporter = LQReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .outputTo(this::report)
                .build();
        registry.registerAll(new CpuGaugeSet(sigar));
        registry.registerAll(new MemoryUsageGaugeSet());
        registry.registerAll(new DiskGaugeSet(sigar));
        registry.registerAll(new NetInterfaceGaugeSet(sigar));
        this.reporter.start(10, TimeUnit.SECONDS);
    }

    private void report(final String json) {
        if (!this.hasRegistry()) {
            this.tryRegistry();
        } else {
            final ProtocolMessage msg = new ProtocolMessage();
            final MonitorReport report = new MonitorReport();
            report.setContent(json);
            report.setTime(System.currentTimeMillis());
            msg.setBody(report);
            try {
                this.getConnection().writeWithoutAck(msg);
            } catch (final Exception e) {
                log.warn("监控信息上报失败", e);
            }
        }
    }

    private Connection getConnection() {
        return this.connMangager.getConnection(this.serverHost, this.serverPort);
    }

    private void tryRegistry() {
        this.registry = false;
        final ProtocolMessage msg = new ProtocolMessage();
        final Registry registry = new Registry();
        registry.setClientName(this.clientName);
        registry.setNetwork(this.ipAddress);
        registry.setTimestamp(System.currentTimeMillis());
        final byte[] signature = MD5Utils.signature(this.ipAddress, this.secretKey, registry.getTimestamp());
        registry.setSignature(signature);
        msg.setBody(registry);
        try {
            this.getConnection().write(msg, resp -> {
                final NormalAck ack = (NormalAck) ((ProtocolMessage) resp).getBody();
                if (ack.isSuccess()) {
                    this.registry = true;
                    log.info("客户端注册成功！！！");
                } else {
                    log.warn("LQ客户端注册失败，请检查是否客户端签名错误");
                }
            }, Constants.EXPIRED_ACK);
        } catch (final Exception e) {
            log.error("LQ客户端注册失败", e);
        }
    }

    private boolean hasRegistry() {
        return this.getConnection().isConnected() && this.registry;
    }

    @Override
    public void destroy() {
        this.reporter.close();
    }

}
