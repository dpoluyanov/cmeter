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
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.AbstractTimer;
import org.springframework.metrics.instrument.internal.MeterId;
import org.springframework.metrics.instrument.stats.hist.Histogram;
import org.springframework.metrics.instrument.stats.quantile.Quantiles;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.springframework.metrics.instrument.internal.TimeUtils.nanosToUnit;

/**
 * @author Camelion
 * @since 28.06.17
 * Note: Measure additionally returns count_sample and amount_sample, values since last measure
 */
public class ClickHouseTimer extends AbstractTimer {
    private static final Tag TYPE_TAG = Tag.of("type", String.valueOf(Type.Timer));

    private final MeterId countId;
    private final MeterId amountId;
    private final MeterId countSampleId;
    private final MeterId amountSampleId;
    private final Quantiles quantiles;
    private final Histogram<?> histogram;

    private LongAdder count = new LongAdder();
    private LongAdder totalTime = new LongAdder();
    private long lastCount;
    private double lastTotalTime;

    ClickHouseTimer(MeterId id, Clock clock, Quantiles quantiles, Histogram<?> histogram) {
        super(id, clock);
        this.countId = id.withTags(TYPE_TAG, Tag.of("statistic", "count"));
        this.amountId = id.withTags(TYPE_TAG, Tag.of("statistic", "amount"));
        this.countSampleId = id.withTags(TYPE_TAG, Tag.of("statistic", "count_sample"));
        this.amountSampleId = id.withTags(TYPE_TAG, Tag.of("statistic", "amount_sample"));
        this.quantiles = quantiles;
        this.histogram = histogram;
    }

    @Override
    public void record(long amount, TimeUnit unit) {
        if (amount >= 0) {
            count.increment();
            totalTime.add(TimeUnit.NANOSECONDS.convert(amount, unit));

            if(quantiles != null)
                quantiles.observe(amount);
            if(histogram != null)
                histogram.observe(amount);
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
        long cnt = count();
        double time = totalTime(TimeUnit.NANOSECONDS);

        List<Measurement> measurements = Arrays.asList(
                countId.measurement(cnt),
                amountId.measurement(time),
                countSampleId.measurement(cnt - lastCount),
                amountSampleId.measurement(time - lastTotalTime));

        lastCount = cnt;
        lastTotalTime = time;

        return measurements;
    }
}
