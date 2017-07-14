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
