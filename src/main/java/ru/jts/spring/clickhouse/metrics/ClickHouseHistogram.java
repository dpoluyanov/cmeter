package ru.jts.spring.clickhouse.metrics;

import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Meter;
import org.springframework.metrics.instrument.Tag;
import org.springframework.metrics.instrument.internal.MeterId;
import org.springframework.metrics.instrument.stats.hist.Histogram;

import java.util.stream.Collectors;

/**
 * @author Dmitry Poluyanov
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
