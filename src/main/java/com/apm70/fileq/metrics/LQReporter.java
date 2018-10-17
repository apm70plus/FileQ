package com.apm70.fileq.metrics;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

/**
 * 采集数据定时上报器
 * @author liuyg
 *
 */
public class LQReporter extends ScheduledReporter {

    private final Consumer<String> output;

    private LQReporter(
            final MetricRegistry registry,
            final Consumer<String> output,
            final Locale locale,
            final Clock clock,
            final TimeZone timeZone,
            final TimeUnit rateUnit, final TimeUnit durationUnit, final MetricFilter filter,
            final ScheduledExecutorService executor,
            final boolean shutdownExecutorOnStop, final Set<MetricAttribute> disabledMetricAttributes) {
        super(registry, "LQ-reporter", filter, rateUnit, durationUnit, executor, shutdownExecutorOnStop,
                disabledMetricAttributes);
        this.output = output;
    }

    public static Builder forRegistry(final MetricRegistry registry) {
        return new Builder(registry);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void report(
            final SortedMap<String, Gauge> gauges,
            final SortedMap<String, Counter> counters,
            final SortedMap<String, Histogram> histograms,
            final SortedMap<String, Meter> meters,
            final SortedMap<String, Timer> timers) {

        final JSONObject output = new JSONObject();
        if (!gauges.isEmpty()) {
            for (final Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                output.put(entry.getKey(), entry.getValue().getValue());
            }
        }

        if (!counters.isEmpty()) {
            for (final Map.Entry<String, Counter> entry : counters.entrySet()) {
                output.put(entry.getKey(), entry.getValue().getCount());
            }
        }

        if (!histograms.isEmpty()) {
            throw new UnsupportedOperationException();
        }

        if (!meters.isEmpty()) {
            throw new UnsupportedOperationException();
        }

        if (!timers.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        final String gaugesJson = JSON.toJSONString(output);

        this.output.accept(gaugesJson);
    }

    public static class Builder {
        private final MetricRegistry registry;
        private Consumer<String> output;
        private Locale locale;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private ScheduledExecutorService executor;
        private boolean shutdownExecutorOnStop;
        private Set<MetricAttribute> disabledMetricAttributes;

        private Builder(final MetricRegistry registry) {
            this.registry = registry;
            this.output = null;
            this.locale = Locale.getDefault();
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.executor = null;
            this.shutdownExecutorOnStop = true;
            this.disabledMetricAttributes = Collections.emptySet();
        }

        public Builder shutdownExecutorOnStop(final boolean shutdownExecutorOnStop) {
            this.shutdownExecutorOnStop = shutdownExecutorOnStop;
            return this;
        }

        public Builder scheduleOn(final ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder outputTo(final Consumer<String> output) {
            this.output = output;
            return this;
        }

        public Builder formattedFor(final Locale locale) {
            this.locale = locale;
            return this;
        }

        public Builder withClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder formattedFor(final TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder convertRatesTo(final TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        public Builder convertDurationsTo(final TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        public Builder filter(final MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder disabledMetricAttributes(final Set<MetricAttribute> disabledMetricAttributes) {
            this.disabledMetricAttributes = disabledMetricAttributes;
            return this;
        }

        public LQReporter build() {
            return new LQReporter(this.registry,
                    this.output,
                    this.locale,
                    this.clock,
                    this.timeZone,
                    this.rateUnit,
                    this.durationUnit,
                    this.filter,
                    this.executor,
                    this.shutdownExecutorOnStop,
                    this.disabledMetricAttributes);
        }
    }
}
