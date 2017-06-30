package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.Clock;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.AbstractTimer;
import org.springframework.metrics.instrument.internal.MeterId;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.springframework.metrics.instrument.internal.TimeUtils.nanosToUnit;

/**
 * @author Camelion
 * @since 28.06.17
 */
public class ClickHouseTimer extends AbstractTimer {
    private LongAdder count = new LongAdder();
    private LongAdder totalTime = new LongAdder();

    ClickHouseTimer(MeterId id, Clock clock) {
        super(id, clock);
    }

    @Override
    public void record(long amount, TimeUnit unit) {
        if (amount >= 0) {
            count.increment();
            totalTime.add(TimeUnit.NANOSECONDS.convert(amount, unit));
        }
    }

    @Override
    public long count() {
        return count.longValue();
    }

    @Override
    public double totalTime(TimeUnit unit) {
        return nanosToUnit(totalTime.doubleValue(), unit);
    }

    @Override
    public Iterable<Measurement> measure() {
        return Arrays.asList(
                id.withTags(Tag.of("type", String.valueOf(getType())), Tag.of("statistic", "count")).measurement(count()),
                id.withTags(Tag.of("type", String.valueOf(getType())), Tag.of("statistic", "amount")).measurement(totalTime(TimeUnit.NANOSECONDS))
        );
    }
}
