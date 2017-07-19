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

import org.springframework.metrics.instrument.Gauge;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.function.ToDoubleFunction;

/**
 * @author Camelion
 * @since 28.06.17
 */
public class ClickHouseGauge<T> implements Gauge {
    private final MeterId originalId;
    private final WeakReference<T> obj;
    private final ToDoubleFunction<T> value;
    private final MeterId gaugeId;

    ClickHouseGauge(MeterId id, T obj, ToDoubleFunction<T> value) {
        this.originalId = id;
        this.gaugeId = originalId.withTags(Tag.of("type", String.valueOf(Type.Gauge)));
        this.obj = new WeakReference<>(obj);
        this.value = value;
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
        return Collections.singleton(gaugeId.measurement(value()));
    }

    @Override
    public double value() {
        return value.applyAsDouble(obj.get());
    }
}
