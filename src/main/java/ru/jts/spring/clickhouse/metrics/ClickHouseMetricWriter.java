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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.metrics.instrument.Measurement;
import org.springframework.metrics.instrument.Tag;
import ru.yandex.clickhouse.ClickHouseDataSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Camelion
 * @since 22.06.17
 */
public class ClickHouseMetricWriter implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(ClickHouseMetricWriter.class);
    private final ClickHouseMeterRegistry clickHouseMeterRegistry;
    private final String tableName;
    private final String instanceId;
    private final JdbcTemplate clickHouseJdbcTemplate;
    private final Long metricsStep;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> metricsDelivery;

    public ClickHouseMetricWriter(ClickHouseMeterRegistry clickHouseMeterRegistry,
                                  ClickHouseDataSource clickHouseDataSource,
                                  String tableName, String instanceId,
                                  Long metricsStep) {
        this.clickHouseMeterRegistry = clickHouseMeterRegistry;
        this.tableName = tableName;
        this.instanceId = instanceId;
        this.clickHouseJdbcTemplate = new JdbcTemplate(clickHouseDataSource);
        this.metricsStep = metricsStep;
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
                " tags Nested\n" +
                " (\n" +
                "   key String,\n" +
                "   value String\n" +
                " ),\n" +
                " value Float64\n" +
                ") ENGINE = MergeTree(partition, (timestamp, metric, tags.key, tags.value, instance_id), 8192)");

        metricsDelivery = scheduledExecutorService
                .scheduleAtFixedRate(this::sendMetrics, metricsStep, metricsStep, TimeUnit.MILLISECONDS);
    }

    private void sendMetrics() {
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
                .map(m -> {
                    Map<String, String> tags = toTagsMap(m.getTags());
                    return new Object[]{instanceId, m.getName(), tags.keySet(), tags.values(), m.getValue()};
                })
                .collect(Collectors.toList());

        if (log.isTraceEnabled()) {
            for (Object[] batchArg : batchArgs) {
                log.debug("send measure {} with value {} to ClickHouse", batchArg[1], batchArg[2]);
            }
        }

        clickHouseJdbcTemplate.batchUpdate(
                "INSERT INTO " + tableName + " (instance_id, metric, tags.key, tags.value, value)\n" +
                        "VALUES(?, ?, ?, ?, ?)", batchArgs);
    }

    private Map<String, String> toTagsMap(Set<Tag> tags) {
        return tags.stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }

    @Override
    public void destroy() throws Exception {
        if (metricsDelivery != null) {
            metricsDelivery.cancel(true);
        }
        scheduledExecutorService.shutdownNow();
    }
}
