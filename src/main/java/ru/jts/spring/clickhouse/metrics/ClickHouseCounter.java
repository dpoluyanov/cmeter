package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.Counter;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;

import java.util.Collections;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * @author Camelion
 * @since 27.06.17
 */
public class ClickHouseCounter implements Counter {
    private final MeterId id;

    private DoubleAdder count = new DoubleAdder();

    ClickHouseCounter(MeterId id) {
        this.id = id;
    }

    @Override
    public void increment() {
        count.add(1);
    }

    @Override
    public void increment(double amount) {
        count.add(amount);
    }

    @Override
    public double count() {
        return count.doubleValue();
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
        return Collections.singleton(id.withTags(Tag.of("type", String.valueOf(getType()))).measurement(count()));
    }
}
