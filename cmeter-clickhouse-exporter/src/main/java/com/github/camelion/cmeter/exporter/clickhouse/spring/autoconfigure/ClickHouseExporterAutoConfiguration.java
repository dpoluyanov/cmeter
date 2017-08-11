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

package com.github.camelion.cmeter.exporter.clickhouse.spring.autoconfigure;

import com.github.camelion.cmeter.exporter.clickhouse.ClickHouseExporter;
import com.github.camelion.cmeter.exporter.clickhouse.ClickHouseExporterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

/**
 * @author Camelion
 * @since 11.08.17
 */
@Configuration
@ConditionalOnClass(name = "ru.yandex.clickhouse.ClickHouseDataSource")
@ConditionalOnMissingBean(ClickHouseProperties.class)
@EnableConfigurationProperties(ClickHouseProperties.class)
@ConditionalOnProperty(value = "clickhouse.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class ClickHouseExporterAutoConfiguration {
    private static final String CLICKHOUSE_JDBC_URL = "jdbc:clickhouse://localhost:8123/default";

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("clickhouse.metrics.datasource")
    public ClickHouseProperties clickHouseProperties() {
        return new ClickHouseProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClickHouseDataSource clickHouseDataSource(Environment environment,
                                                     ClickHouseProperties clickHouseProperties) {
        return new ClickHouseDataSource(
                environment.getProperty("clickhouse.metrics.datasource.url", CLICKHOUSE_JDBC_URL),
                clickHouseProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClickHouseExporterConfig clickHouseExporterConfig(ClickHouseDataSource clickHouseDataSource,
                                                             @Value("${clickhouse.metrics.table:app_metrics}")
                                                                     String table,
                                                             @Value("${clickhouse.metrics.instance-id:default}")
                                                                     String instanceId,
                                                             @Value("${clickhouse.metrics.export-rate:30}")
                                                                     int exportRate) {
        ClickHouseExporterConfig exporterConfig = new ClickHouseExporterConfig(clickHouseDataSource);

        exporterConfig.setTable(table);
        exporterConfig.setInstanceId(instanceId);
        exporterConfig.setExportRate(exportRate);

        return exporterConfig;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public ClickHouseExporter clickHouseExporter(ClickHouseExporterConfig exporterConfig) {
        return new ClickHouseExporter(exporterConfig);
    }
}
