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

package ru.jts.spring.clickhouse.metrics.boot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.metrics.instrument.Clock;
import ru.jts.spring.clickhouse.metrics.ClickHouseMeterRegistry;
import ru.jts.spring.clickhouse.metrics.ClickHouseMetricWriter;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Camelion
 * @since 27.06.17
 */
@Configuration
public class ClickHouseMetricConfiguration {
    @Value("${clickhouse.metrics.table:${spring.application.name:application}}")
    private String clickhouseTable;

    @Value("${clickhouse.metrics.instance-id:${spring.cloud.consul.discovery.instance-id:${server.port:null}}}")
    private String instanceId;

    @Bean
    ClickHouseMeterRegistry clickhouseMeterRegistry() {
        return new ClickHouseMeterRegistry(Clock.SYSTEM);
    }

    @Bean
    ClickHouseMetricWriter clickhouseMetricWriter(
            @Value("${clickhouse.metrics.step:5000}") Long metricsStep,
            @Value("${clickhouse.datasource.url}") String clickhouseUrl,
            ClickHouseMeterRegistry clickHouseMeterRegistry,
            ClickHouseProperties clickHouseProperties) {

        return new ClickHouseMetricWriter(
                clickHouseMeterRegistry,
                new ClickHouseDataSource(clickhouseUrl, clickHouseProperties)
                        .withConnectionsCleaning(5, SECONDS),
                tablify(clickhouseTable), instanceId, metricsStep);
    }

    private static String tablify(String str) {
        return str.replaceAll("[\\.-]", "_");
    }

    @Bean
    @ConfigurationProperties("clickhouse.datasource")
    ClickHouseProperties clickHouseProperties() {
        return new ClickHouseProperties();
    }
}
