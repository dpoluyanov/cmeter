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

import org.springframework.metrics.instrument.Counter;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * @author Camelion
 * @since 27.06.17
 * Note: Counter additionally returns count_sample since last measure
 */
public class ClickHouseCounter implements Counter {
    private static final Tag TYPE_TAG = Tag.of("type", String.valueOf(Type.Counter));
    private static final Tag STATISTIC_COUNT_TAG = Tag.of("statistic", "count");
    private static final Tag STATISTIC_COUNT_SAMPLE_TAG = Tag.of("statistic", "count_sample");

    private final MeterId originalId;
    private final MeterId counterId;
    private final MeterId counterSampleId;

    private DoubleAdder count = new DoubleAdder();
    private volatile double lastCount;

    ClickHouseCounter(MeterId id) {
        this.originalId = id;
        this.counterId = originalId.withTags(TYPE_TAG, STATISTIC_COUNT_TAG);
        this.counterSampleId = originalId.withTags(TYPE_TAG, STATISTIC_COUNT_SAMPLE_TAG);
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
        return originalId.getName();
    }

    @Override
    public Iterable<Tag> getTags() {
        return originalId.getTags();
    }

    @Override
    public Iterable<Measurement> measure() {
        double cnt = count();

        List<Measurement> measurements = Arrays.asList(
                counterId.measurement(cnt),
                counterSampleId.measurement(cnt - lastCount));

        lastCount = cnt;

        return measurements;
    }
}
