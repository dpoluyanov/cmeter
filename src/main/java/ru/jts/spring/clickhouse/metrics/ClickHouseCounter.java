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
