package com.apm70.fileq.metrics;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.SigarException;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

public class NetInterfaceGaugeSet implements MetricSet {

    private final SigarService sigarService;

    private final Meter preRxPackets = new Meter();
    private final Meter preTxPackets = new Meter();
    private final Meter preRxBytes = new Meter();
    private final Meter preTxBytes = new Meter();
    private final Meter preRxErrors = new Meter();
    private final Meter preTxErrors = new Meter();
    private final Meter preRxDropped = new Meter();
    private final Meter preTxDropped = new Meter();

    public NetInterfaceGaugeSet(final SigarService sigarService) {
        this.sigarService = sigarService;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        try {
            this.sigarService.getNetInterfaces().forEach(ifName -> {
                gauges.put("ifstat_RxPackets_" + ifName,
                        (Gauge<Double>) () -> {
                            final long rxPackets = this.sigarService.getNetIfStat(ifName).getRxPackets();
                            return this.preRxPackets.getRate(rxPackets);
                        });
                gauges.put("ifstat_TxPackets_" + ifName,
                        (Gauge<Double>) () -> {
                            final long txPackets = this.sigarService.getNetIfStat(ifName).getTxPackets();
                            return this.preTxPackets.getRate(txPackets);
                        });
                gauges.put("ifstat_RxBytes_" + ifName,
                        (Gauge<Double>) () -> {
                            final long rxBytes = this.sigarService.getNetIfStat(ifName).getRxBytes();
                            return this.preRxBytes.getRate(rxBytes);
                        });
                gauges.put("ifstat_TxBytes_" + ifName,
                        (Gauge<Double>) () -> {
                            final long txBytes = this.sigarService.getNetIfStat(ifName).getTxBytes();
                            return this.preTxBytes.getRate(txBytes);
                        });
                gauges.put("ifstat_RxErrors_" + ifName,
                        (Gauge<Double>) () -> {
                            final long rxErrors = this.sigarService.getNetIfStat(ifName).getRxErrors();
                            return this.preRxErrors.getRate(rxErrors);
                        });
                gauges.put("ifstat_TxErrors_" + ifName,
                        (Gauge<Double>) () -> {
                            final long txErrors = this.sigarService.getNetIfStat(ifName).getTxErrors();
                            return this.preTxErrors.getRate(txErrors);
                        });
                gauges.put("ifstat_RxDropped_" + ifName,
                        (Gauge<Double>) () -> {
                            final long rxDropped = this.sigarService.getNetIfStat(ifName).getRxDropped();
                            return this.preRxDropped.getRate(rxDropped);
                        });
                gauges.put("ifstat_TxDropped_" + ifName,
                        (Gauge<Double>) () -> {
                            final long txDropped = this.sigarService.getNetIfStat(ifName).getTxDropped();
                            return this.preTxDropped.getRate(txDropped);
                        });
            });
            return gauges;
        } catch (final SigarException e) {
            throw new RuntimeException(e);
        }
    }

    class Meter {
        private long preValue = 0L;
        private long preTime = 0L;

        public synchronized double getRate(final long value) {
            final long time = System.currentTimeMillis();
            long rate = 0L;
            final long duration = (time - this.preTime) / 1000L;
            if ((this.preTime != 0) && (value >= this.preValue) && (duration >= 1)) {
                try {
                    rate = (value - this.preValue) / duration;
                } catch (final Exception e) {
                    System.out.println(e);
                }
            }
            this.preValue = value;
            this.preTime = time;
            return rate;
        }
    }
}
