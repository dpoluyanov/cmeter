package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.*;
import org.springframework.metrics.instrument.Timer;
import org.springframework.metrics.instrument.internal.AbstractMeterRegistry;
import org.springframework.metrics.instrument.internal.MapAccess;
import org.springframework.metrics.instrument.internal.MeterId;
import org.springframework.metrics.instrument.stats.hist.Histogram;
import org.springframework.metrics.instrument.stats.quantile.Quantiles;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.ToDoubleFunction;

import static org.springframework.metrics.instrument.internal.MapAccess.computeIfAbsent;

/**
 * @author Camelion
 * @since 27.06.17
 */
public class ClickHouseMeterRegistry extends AbstractMeterRegistry {
    private ConcurrentMap<MeterId, Meter> meterMap = new ConcurrentHashMap<>();

    public ClickHouseMeterRegistry(Clock clock) {
        super(clock);
    }

    @Override
    public Collection<Meter> getMeters() {
        return meterMap.values();
    }

    @Override
    public <M extends Meter> Optional<M> findMeter(Class<M> mClass, String name, Iterable<Tag> tags) {
        Collection<Tag> tagsToMatch = new ArrayList<>();
        tags.forEach(tagsToMatch::add);

        return meterMap.keySet().stream()
                .filter(id -> id.getName().equals(name))
                .filter(id -> id.getTags().containsAll(tagsToMatch))
                .findAny()
                .map(meterMap::get)
                .filter(mClass::isInstance)
                .map(mClass::cast);
    }

    @Override
    public Optional<Meter> findMeter(Meter.Type type, String name, Iterable<Tag> tags) {
        Collection<Tag> tagsToMatch = new ArrayList<>();
        tags.forEach(tagsToMatch::add);

        return meterMap.keySet().stream()
                .filter(id -> id.getName().equals(name))
                .filter(id -> id.getTags().containsAll(tagsToMatch))
                .findAny()
                .map(meterMap::get)
                .filter(m -> m.getType().equals(type));
    }

    @Override
    public Counter counter(String name, Iterable<Tag> tags) {
        MeterId meterId = new MeterId(name, tags);
        return MapAccess.computeIfAbsent(meterMap, meterId, ClickHouseCounter::new);
    }

    @Override
    public LongTaskTimer longTaskTimer(String name, Iterable<Tag> tags) {
        return computeIfAbsent(meterMap, new MeterId(name, tags), id -> new ClickHouseLongTaskTimer(id, getClock()));
    }

    @Override
    public MeterRegistry register(Meter meter) {
        meterMap.put(new MeterId(meter.getName(), meter.getTags()), meter);
        return this;
    }

    @Override
    public <T> T gauge(String name, Iterable<Tag> tags, T obj, ToDoubleFunction<T> f) {
        computeIfAbsent(meterMap, new MeterId(name, tags), id -> new ClickHouseGauge<>(id, obj, f));
        return obj;
    }

    @Override
    protected Timer timer(String name, Iterable<Tag> tags, Quantiles quantiles, Histogram<?> histogram) {
        registerQuantilesGaugeIfNecessary(name, tags, quantiles);
        registerHistogramCounterIfNecessary(name, tags, histogram);
        return computeIfAbsent(meterMap, new MeterId(name, tags), id -> new ClickHouseTimer(id, getClock()));
    }

    @Override
    protected DistributionSummary distributionSummary(String name, Iterable<Tag> tags, Quantiles quantiles, Histogram<?> histogram) {
        registerQuantilesGaugeIfNecessary(name, tags, quantiles);
        registerHistogramCounterIfNecessary(name, tags, histogram);
        return computeIfAbsent(meterMap, new MeterId(name, tags), ClickHouseDistributionSummary::new);
    }

    private void registerQuantilesGaugeIfNecessary(String name, Iterable<Tag> tags, Quantiles quantiles) {
        if (quantiles != null) {
            for (Double q : quantiles.monitored()) {
                List<Tag> quantileTags = new LinkedList<>();
                tags.forEach(quantileTags::add);

                quantileTags.add(Tag.of("quantile", Double.isNaN(q) ? "NaN" : Double.toString(q)));
                quantileTags.add(Tag.of("statistic", "quantile"));

                computeIfAbsent(meterMap, new MeterId(name + ".quantiles", quantileTags),
                        id -> new ClickHouseGauge<>(id, q, quantiles::get));
            }
        }
    }

    private void registerHistogramCounterIfNecessary(String name, Iterable<Tag> tags, Histogram<?> histogram) {
        if (histogram != null) {
            computeIfAbsent(meterMap, new MeterId(name + ".histogram", tags),
                    id -> new ClickHouseHistogram(id, histogram));
        }
    }
}
