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

import org.springframework.metrics.instrument.DistributionSummary;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;
import org.springframework.metrics.instrument.stats.hist.Histogram;
import org.springframework.metrics.instrument.stats.quantile.Quantiles;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Camelion
 * @since 28.06.17
 * Note: distribution summary additionally returns count_sample and amount_sample since last measure
 */
public class ClickHouseDistributionSummary implements DistributionSummary {
    private static final Tag TYPE_TAG = Tag.of("type", String.valueOf(Type.DistributionSummary));
    private static final Tag STATISTIC_COUNT_TAG = Tag.of("statistic", "count");
    private static final Tag STATISTIC_AMOUNT_TAG = Tag.of("statistic", "amount");
    private static final Tag STATISTIC_COUNT_SAMPLE_TAG = Tag.of("statistic", "count_sample");
    private static final Tag STATISTIC_AMOUNT_SAMPLE_TAG = Tag.of("statistic", "amount_sample");

    private final MeterId originalId;
    private final MeterId countId;
    private final MeterId amountId;
    private final MeterId countSampleId;
    private final MeterId amountSampleId;
    private final Quantiles quantilies;
    private final Histogram<?> histogram;

    private LongAdder count = new LongAdder();
    private DoubleAdder amount = new DoubleAdder();

    private volatile long lastCount;
    private volatile double lastAmount;

    ClickHouseDistributionSummary(MeterId id, Quantiles quantiles, Histogram<?> histogram) {
        this.originalId = id;
        this.countId = id.withTags(TYPE_TAG, STATISTIC_COUNT_TAG);
        this.amountId = id.withTags(TYPE_TAG, STATISTIC_AMOUNT_TAG);
        this.countSampleId = id.withTags(TYPE_TAG, STATISTIC_COUNT_SAMPLE_TAG);
        this.amountSampleId = id.withTags(TYPE_TAG, STATISTIC_AMOUNT_SAMPLE_TAG);
        this.quantilies = quantiles;
        this.histogram = histogram;
    }

    @Override
    public void record(double amount) {
        if (amount >= 0) {
            count.increment();
            this.amount.add(amount);

            if (quantilies != null)
                quantilies.observe(amount);
            if (histogram != null)
                histogram.observe(amount);
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
        return originalId.getName();
    }

    @Override
    public Iterable<Tag> getTags() {
        return originalId.getTags();
    }

    @Override
    public Iterable<Measurement> measure() {
        long cnt = count();
        double amount = totalAmount();

        List<Measurement> measurements = Arrays.asList(
                countId.measurement(count()),
                amountId.measurement(totalAmount()),
                countSampleId.measurement(cnt - lastCount),
                amountSampleId.measurement(amount - lastAmount));

        lastCount = cnt;
        lastAmount = amount;
        return measurements;
    }
}
