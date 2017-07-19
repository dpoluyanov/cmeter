/*
 * Copyright 2017 JTS-Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private static final Tag TYPE_TAG = Tag.of("type", String.valueOf(Type.LongTaskTimer));
    private final Clock clock;
    private final ConcurrentMap<Long, Long> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextTask = new AtomicLong(0L);

    private final MeterId originalId;
    private final MeterId activeTasksId;
    private final MeterId durationId;

    ClickHouseLongTaskTimer(MeterId id, Clock clock) {
        this.originalId = id;
        this.activeTasksId = originalId.withTags(TYPE_TAG, Tag.of("statistic", "activeTasks"));
        this.durationId = originalId.withTags(TYPE_TAG, Tag.of("statistic", "duration"));
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
        return originalId.getName();
    }

    @Override
    public Iterable<Tag> getTags() {
        return originalId.getTags();
    }

    @Override
    public Iterable<Measurement> measure() {
        return Arrays.asList(
                activeTasksId.measurement(activeTasks()),
                durationId.measurement(duration()));
    }
}
