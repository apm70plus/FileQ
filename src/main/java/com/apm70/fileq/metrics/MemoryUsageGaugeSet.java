package com.apm70.fileq.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.RatioGauge;

public class MemoryUsageGaugeSet implements MetricSet {

    private final MemoryMXBean mxBean;

    public MemoryUsageGaugeSet() {
        this(ManagementFactory.getMemoryMXBean(), ManagementFactory.getMemoryPoolMXBeans());
    }

    public MemoryUsageGaugeSet(final MemoryMXBean mxBean,
            final Collection<MemoryPoolMXBean> memoryPools) {
        this.mxBean = mxBean;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("memory_init_heap", (Gauge<Long>) () -> this.mxBean.getHeapMemoryUsage().getInit());
        gauges.put("memory_used_heap", (Gauge<Long>) () -> this.mxBean.getHeapMemoryUsage().getUsed());
        gauges.put("memory_max_heap", (Gauge<Long>) () -> this.mxBean.getHeapMemoryUsage().getMax());
        gauges.put("memory_committed_heap", (Gauge<Long>) () -> this.mxBean.getHeapMemoryUsage().getCommitted());
        gauges.put("memory_usage_heap", new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                final MemoryUsage usage = MemoryUsageGaugeSet.this.mxBean.getHeapMemoryUsage();
                return Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        gauges.put("memory_init_noheap", (Gauge<Long>) () -> this.mxBean.getNonHeapMemoryUsage().getInit());
        gauges.put("memory_used_noheap", (Gauge<Long>) () -> this.mxBean.getNonHeapMemoryUsage().getUsed());
        gauges.put("memory_max_noheap", (Gauge<Long>) () -> this.mxBean.getNonHeapMemoryUsage().getMax());
        gauges.put("memory_committed_noheap", (Gauge<Long>) () -> this.mxBean.getNonHeapMemoryUsage().getCommitted());

        return Collections.unmodifiableMap(gauges);
    }
}
