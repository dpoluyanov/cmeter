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

package com.github.camelion.micrometer.clickhouse;


import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Tag;
import io.micrometer.core.instrument.spectator.step.AbstractStepRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.clickhouse.*;
import ru.yandex.clickhouse.settings.ClickHouseQueryParam;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author Camelion
 * @since 27.06.17
 */
public class ClickHouseMeterRegistry extends AbstractStepRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseMeterRegistry.class);

    private final ClickHouseDataSource chDatasource;
    private final String metricsTable;
    private final ClickHouseConfig config;
    private final String instanceId;
    private Map<ClickHouseQueryParam, String> clickHouseParams;

    public ClickHouseMeterRegistry(Clock clock, ClickHouseConfig config) {
        super(clock, config);
        this.chDatasource = config.dataSource();
        this.metricsTable = config.get(config.prefix() + ".table");
        this.config = config;
        // require non null string for clickhouse column
        String instId = config.get(config.prefix() + ".instance-id");
        this.instanceId = instId == null ? "" : instId;
    }

    @Override
    public void start() {
        if (config.enabled()) {
            try {
                executeSql("CREATE TABLE IF NOT EXISTS `" + metricsTable + "`\n" +
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
                        ") ENGINE = MergeTree(partition, " +
                        "(timestamp, metric, tags.key, tags.value, instance_id), 8192)");
                // TODO successful logger for created/not changed table
            } catch (SQLException e) {
                LOG.error("Failed to create ClickHouse table [" + metricsTable + "]", e);
                throw new RuntimeException(e);
            }
        }
        super.start();
    }

    @Override
    protected void pushMetrics() {
        List<Measurement> measurements = this.getBatches().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        try {
            this.sendMeasurementsBatch("INSERT INTO `" + metricsTable + "`" +
                    " (timestamp, instance_id, metric, tags.key, tags.value, value)\n" +
                    "VALUES(?, ?, ?, ?, ?, ?)", measurements);
        } catch (SQLException e) {
            LOG.error("Failed to send measurements to ClickHouse table [" + metricsTable + "]", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends batch with metrics to clickhouse storage
     *
     * @param sql
     * @throws SQLException
     */
    private void sendMeasurementsBatch(String sql, List<Measurement> batch) throws SQLException {
        ClickHouseConnectionImpl connection = null;
        ClickHousePreparedStatement stmt = null;
        try {
            connection = (ClickHouseConnectionImpl) chDatasource.getConnection();
            stmt = connection.createClickHousePreparedStatement(sql);
            Map<ClickHouseQueryParam, String> params = getClickHouseParams();

            for (Measurement measurement : batch) {
                Map<String, String> tags = toTagsMap(measurement.id().tags());
                stmt.setTimestamp(1, new Timestamp(measurement.timestamp()));
                stmt.setString(2, instanceId);
                stmt.setString(3, measurement.id().name());
                stmt.setArray(4, tags.keySet());
                stmt.setArray(5, tags.values());
                stmt.setDouble(6, measurement.value());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } finally {
            if (connection != null)
                connection.close();
            if (stmt != null)
                stmt.close();
        }
    }

    /**
     * Converts tag to equivalent map of k/v
     *
     * @param tags
     * @return
     */
    private static Map<String, String> toTagsMap(Iterable<Tag> tags) {
        Map<String, String> tMap = new HashMap<>();
        for (Tag t : tags) {
            tMap.put(t.key(), t.value());
        }
        return tMap;
    }

    /**
     * Sends sql request to server
     *
     * @param sql
     * @throws SQLException
     */
    private void executeSql(String sql) throws SQLException {
        ClickHouseConnection connection = null;
        ClickHouseStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = chDatasource.getConnection();
            stmt = connection.createStatement();
            Map<ClickHouseQueryParam, String> params = getClickHouseParams();
            rs = stmt.executeQuery(sql, params);
        } finally {
            if (connection != null)
                connection.close();
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }

    private Map<ClickHouseQueryParam, String> getClickHouseParams() {
        if (clickHouseParams == null) {
            clickHouseParams = new HashMap<>();

            for (ClickHouseQueryParam param : ClickHouseQueryParam.values()) {
                String val = config.get(config.prefix() + "." + param.getKey());
                if (val != null)
                    clickHouseParams.put(param, val);
            }

            clickHouseParams.put(ClickHouseQueryParam.CONNECT_TIMEOUT, String.valueOf(connectTimeout / 1000));
            clickHouseParams.put(ClickHouseQueryParam.RECEIVE_TIMEOUT, String.valueOf(readTimeout / 1000));
            clickHouseParams.put(ClickHouseQueryParam.SEND_TIMEOUT, String.valueOf(readTimeout / 1000));
        }

        return clickHouseParams;
    }
}
