package ru.jts.spring.clickhouse.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import org.springframework.scheduling.annotation.Scheduled;
import ru.yandex.clickhouse.ClickHouseDataSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Camelion
 * @since 22.06.17
 */
public class ClickHouseMetricWriter implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(ClickHouseMetricWriter.class);
    private final ClickHouseMeterRegistry clickHouseMeterRegistry;
    private final String tableName;
    private final String instanceId;
    private final JdbcTemplate clickHouseJdbcTemplate;

    public ClickHouseMetricWriter(ClickHouseMeterRegistry clickHouseMeterRegistry,
                                  ClickHouseDataSource clickHouseDataSource,
                                  String tableName, String instanceId) {
        this.clickHouseMeterRegistry = clickHouseMeterRegistry;
        this.tableName = tableName;
        this.instanceId = instanceId;
        this.clickHouseJdbcTemplate = new JdbcTemplate(clickHouseDataSource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Create table [{}] for metrics in ClickHouse", tableName);

        clickHouseJdbcTemplate.execute("" +
                "CREATE TABLE IF NOT EXISTS " + tableName + "\n" +
                "(\n" +
                " partition Date DEFAULT toDate(timestamp),\n" +
                " timestamp DateTime DEFAULT now(),\n" +
                " instance_id String,\n" +
                " metric String,\n" +
                " value Float64\n" +
                ") ENGINE = MergeTree(partition, (timestamp, metric, instance_id), 8192)");
    }

    @Scheduled(fixedRateString = "${clickhouse.metrics.step:5000}")
    public void sendMetrics() {
        List<Measurement> batch = clickHouseMeterRegistry.getMeters()
                .parallelStream()
                .flatMap(meter -> StreamSupport.stream(meter.measure().spliterator(), false))
                .collect(Collectors.toList());

        if (!batch.isEmpty()) {
            store(batch);
        } else if (log.isDebugEnabled()) {
            log.debug("Ignore empty measurements batch");
        }
    }

    private void store(List<Measurement> batch) {
        if (log.isDebugEnabled()) {
            log.debug("Sending measurements batch with size {} to ClickHouse", batch.size());
        }

        List<Object[]> batchArgs = batch.stream()
                .map(m -> new Object[]{instanceId, toMetric(m.getName(), m.getTags()), m.getValue()})
                .collect(Collectors.toList());

        if (log.isTraceEnabled()) {
            for (Object[] batchArg : batchArgs) {
                log.debug("send measure {} with value {} to ClickHouse", batchArg[1], batchArg[2]);
            }
        }

        clickHouseJdbcTemplate.batchUpdate(
                "INSERT INTO " + tableName + " (instance_id, metric, value)\n" +
                        "VALUES(?, ?, ?)", batchArgs);
    }

    private String toMetric(String name, Set<Tag> tags) {
        return Stream.concat(Stream.of(name),
                tags.stream().flatMap(tag -> Stream.of(tag.getKey(), tag.getValue())))
                .collect(Collectors.joining("."));
    }
}
