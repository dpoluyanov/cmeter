package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.Clock;
import org.springframework.metrics.instrument.LongTaskTimer;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Camelion
 * @since 28.06.17
 */
public class ClickHouseLongTaskTimer implements LongTaskTimer {
    private final MeterId id;
    private final Clock clock;
    private final ConcurrentMap<Long, Long> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextTask = new AtomicLong(0L);

    ClickHouseLongTaskTimer(MeterId id, Clock clock) {
        this.id = id;
        this.clock = clock;
    }

    @Override
    public long start() {
        long task = nextTask.getAndIncrement();
        tasks.put(task, clock.monotonicTime());
        return task;
    }

    @Override
    public long stop(long task) {
        Long startTime = tasks.get(task);
        if (startTime != null) {
            tasks.remove(task);
            return clock.monotonicTime() - startTime;
        } else {
            return -1L;
        }
    }

    @Override
    public long duration(long task) {
        Long startTime = tasks.get(task);
        return (startTime != null) ? (clock.monotonicTime() - startTime) : -1L;
    }

    @Override
    public long duration() {
        long now = clock.monotonicTime();
        long sum = 0L;
        for (long startTime : tasks.values()) {
            sum += now - startTime;
        }
        return sum;
    }

    @Override
    public int activeTasks() {
        return tasks.size();
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
                id.withTags(Tag.of("type", String.valueOf(getType())), Tag.of("statistic", "activeTasks")).measurement(activeTasks()),
                id.withTags(Tag.of("type", String.valueOf(getType())), Tag.of("statistic", "duration")).measurement(duration())
        );
    }
}
