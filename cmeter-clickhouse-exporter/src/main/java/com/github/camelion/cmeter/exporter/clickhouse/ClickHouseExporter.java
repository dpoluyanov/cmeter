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

package com.github.camelion.cmeter.exporter.clickhouse;

import com.github.camelion.cmeter.MetricCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseConnectionImpl;
import ru.yandex.clickhouse.ClickHouseStatement;

import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Camelion
 * @since 09.08.17
 */
public final class ClickHouseExporter {
    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseExporter.class);
    private final ScheduledExecutorService exporter;
    private final MetricCollector collector;
    private final ClickHouseExporterConfig exporterConfig;
    private ScheduledFuture<?> scheduledFuture;

    public ClickHouseExporter(ClickHouseExporterConfig exporterConfig) {
        this(new MetricCollector(), exporterConfig);
    }

    public ClickHouseExporter(MetricCollector collector, ClickHouseExporterConfig exporterConfig) {
        this.collector = collector;
        this.exporterConfig = exporterConfig;
        exporter = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            final AtomicInteger threadNum = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                String name = "CMeter-CH-Exporter-" + threadNum.getAndIncrement();
                return new Thread(Thread.currentThread().getThreadGroup(), r, name);
            }
        });
    }

    public void start() {
        try (ClickHouseConnection connection = exporterConfig.getDataSource().getConnection()) {
            try (ClickHouseStatement stmt = connection.createStatement()) {
                stmt.execute("" +
                        "CREATE TABLE IF NOT EXISTS `" + exporterConfig.getTable() + "`\n" +
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
                        ") ENGINE = MergeTree(partition, (timestamp, metric), 8192)");

                LOG.info("Created table {} for application metrics", exporterConfig.getTable());
            }
        } catch (SQLException e) {
            LOG.error("Error in creating CH table", e);
        }

        scheduledFuture = exporter.scheduleWithFixedDelay(
                this::sendMeasures,
                exporterConfig.getExportRate() * 2, exporterConfig.getExportRate(), TimeUnit.SECONDS);
    }

    public void stop() throws InterruptedException {
        scheduledFuture.cancel(false);
        exporter.shutdown();
    }

    /**
     * Metrics delivery worker
     */
    private void sendMeasures() {
        try (ClickHouseConnectionImpl connection =
                     (ClickHouseConnectionImpl) exporterConfig.getDataSource().getConnection()) {
            try (ClickHouseStatement statement = connection.createStatement()) {
                statement.sendRowBinaryStream("INSERT INTO `" + exporterConfig.getTable() + "`" +
                                " (timestamp,  metric, instance_id, tags.key, tags.value, value)",
                        stream -> collector.obtainMetrics(
                                new ClickHouseCursor(stream, exporterConfig.getInstanceId())));
            }
        } catch (SQLException e) {
            LOG.error("Problems with metric storing", e);
            throw new RuntimeException(e);
        }
    }
}
