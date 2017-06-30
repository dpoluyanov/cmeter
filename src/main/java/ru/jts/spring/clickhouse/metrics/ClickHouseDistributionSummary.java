package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.DistributionSummary;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;

import java.util.Arrays;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Camelion
 * @since 28.06.17
 */
public class ClickHouseDistributionSummary implements DistributionSummary {
    private final MeterId id;
    private LongAdder count = new LongAdder();
    private DoubleAdder amount = new DoubleAdder();

    ClickHouseDistributionSummary(MeterId id) {
        this.id = id;
    }

    @Override
    public void record(double amount) {
        if (amount >= 0) {
            count.increment();
            this.amount.add(amount);
        }
    }

    @Override
    public long count() {
        return count.longValue();
    }

    @Override
    public double totalAmount() {
        return amount.doubleValue();
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
        return Arrays.asList(
                id.withTags(Tag.of("type", String.valueOf(getType())), Tag.of("statistic", "count")).measurement(count()),
                id.withTags(Tag.of("type", String.valueOf(getType())), Tag.of("statistic", "amount")).measurement(totalAmount())
        );
    }
}
