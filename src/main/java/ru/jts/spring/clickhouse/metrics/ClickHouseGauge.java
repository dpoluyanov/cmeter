package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.Gauge;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.function.ToDoubleFunction;

/**
 * @author Camelion
 * @since 28.06.17
 */
public class ClickHouseGauge<T> implements Gauge {
    private final MeterId id;
    private final WeakReference<T> obj;
    private final ToDoubleFunction<T> value;

    ClickHouseGauge(MeterId id, T obj, ToDoubleFunction<T> value) {
        this.id = id;
        this.obj = new WeakReference<>(obj);
        this.value = value;
    }

    @Override
    public double value() {
        return value.applyAsDouble(obj.get());
    }

    @Override
    public String getName() {
        return id.getName();
    }

    @Override
    public Iterable<Tag> getTags() {
        return id.getTags();
    }

    @Override
    public Iterable<Measurement> measure() {
        return Collections.singleton(id.withTags(Tag.of("type", String.valueOf(getType()))).measurement(value()));
    }
}
