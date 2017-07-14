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

import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Meter;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;
import org.springframework.metrics.instrument.stats.hist.Histogram;

import java.util.stream.Collectors;

/**
 * @author Camelion
 * @since 02.07.17
 */
public class ClickHouseHistogram implements Meter {
    private final MeterId id;
    private final Histogram<?> histogram;

    ClickHouseHistogram(MeterId id, Histogram<?> histogram) {
        this.id = id;
        this.histogram = histogram;
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
    public Type getType() {
        return Type.Other;
    }

    @Override
    public Iterable<Measurement> measure() {
        return histogram.getBuckets().stream()
                .map(b -> id.withTags(
                        Tag.of("type", "gauge"),
                        Tag.of("bucket", b.getTag(
                                bc -> bc instanceof Double ?
                                        Double.isNaN((Double) bc) ? "NaN"
                                                : Double.toString((Double) bc) : String.valueOf(bc))),
                        Tag.of("statistic", "histogram"))
                        .measurement(b.getValue()))
                .collect(Collectors.toList());
    }
}
