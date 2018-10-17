package com.apm70.fileq.metrics;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.SigarException;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

public class DiskGaugeSet implements MetricSet {

    private final SigarService sigarService;

    public DiskGaugeSet(final SigarService sigarService) {
        this.sigarService = sigarService;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        try {
            this.sigarService.getLocalDisks().forEach(fs -> {
                String disk = fs.getDevName();
                if (disk.startsWith("/dev/")) {
                    disk = disk.substring(5);
                }
                gauges.put("diskSpace_total_" + disk,
                        (Gauge<Double>) () -> this.sigarService.getDiskTotalKb(fs));
                gauges.put("diskSpace_free_" + disk,
                        (Gauge<Double>) () -> this.sigarService.getDiskFreeKb(fs));
                gauges.put("diskSpace_usePercent_" + disk,
                        (Gauge<Double>) () -> this.sigarService.getDiskUsePercent(fs));
            });
            return gauges;
        } catch (final SigarException e) {
            throw new RuntimeException(e);
        }
    }
}
