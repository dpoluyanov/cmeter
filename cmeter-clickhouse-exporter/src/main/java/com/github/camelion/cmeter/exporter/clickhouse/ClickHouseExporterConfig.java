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

import ru.yandex.clickhouse.ClickHouseDataSource;

/**
 * @author Camelion
 * @since 09.08.17
 */
public final class ClickHouseExporterConfig {
    private static final int DEFAULT_EXPORT_RATE = 30; // seconds
    private static final String DEFAULT_TABLE = "app_metrics";
    private final ClickHouseDataSource dataSource;
    private int exportRate = DEFAULT_EXPORT_RATE;
    private String table = DEFAULT_TABLE;
    private String instanceId = "";

    public ClickHouseExporterConfig(ClickHouseDataSource dataSource) {
        this.dataSource = dataSource;
    }

    int getExportRate() {
        return exportRate;
    }

    /**
     * Sets export rate for metric collecting
     *
     * @param exportRate interval of metric storing (in seconds)
     */
    public void setExportRate(int exportRate) {
        this.exportRate = exportRate;
    }

    String getTable() {
        return table;
    }

    /**
     * Sets table for metric exporting.
     * Can be in two formats `table`, and `database.table`.
     *
     * @param table database table name for metric storing
     */
    public void setTable(String table) {
        this.table = table;
    }

    String getInstanceId() {
        return instanceId;
    }

    /**
     * Sets instance identifier of running application.
     * There are no need to set it, it's optional.
     *
     * @param instanceId identifier of application instance
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    ClickHouseDataSource getDataSource() {
        return dataSource;
    }
}
