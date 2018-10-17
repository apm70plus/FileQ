package com.apm70.fileq.metrics;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.SigarException;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

public class CpuGaugeSet implements MetricSet {

    private final SigarService sigarService;

    public CpuGaugeSet(final SigarService sigarService) {
        this.sigarService = sigarService;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        gauges.put("cpu_user_total", (Gauge<Double>) () -> this.getProperty("user"));
        gauges.put("cpu_sys_total", (Gauge<Double>) () -> this.getProperty("sys"));
        gauges.put("cpu_wait_total", (Gauge<Double>) () -> this.getProperty("wait"));
        gauges.put("cpu_nice_total", (Gauge<Double>) () -> this.getProperty("nice"));
        gauges.put("cpu_idle_total", (Gauge<Double>) () -> this.getProperty("idle"));
        gauges.put("cpu_steal_total", (Gauge<Double>) () -> this.getProperty("steal"));
        gauges.put("cpu_irq_total", (Gauge<Double>) () -> this.getProperty("irq"));
        gauges.put("cpu_softirq_total", (Gauge<Double>) () -> this.getProperty("softirq"));
        return gauges;
    }

    private double getProperty(final String prop) {

        try {
            double value = 0.0d;
            switch (prop) {
            case "user":
                value = this.sigarService.getCpuPerc().getUser();
                break;
            case "sys":
                value = this.sigarService.getCpuPerc().getSys();
                break;
            case "wait":
                value = this.sigarService.getCpuPerc().getWait();
                break;
            case "nice":
                value = this.sigarService.getCpuPerc().getNice();
                break;
            case "idle":
                value = this.sigarService.getCpuPerc().getIdle();
                break;
            case "steal":
                value = this.sigarService.getCpuPerc().getStolen();
                break;
            case "irq":
                value = this.sigarService.getCpuPerc().getIrq();
                break;
            case "softirq":
                value = this.sigarService.getCpuPerc().getSoftIrq();
                break;
            default:
                value = 0.0d;
            }
            return value;
        } catch (final SigarException e) {
            return 0.0D;
        }
    }
}
