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

package com.github.camelion.micrometer.clickhouse.spring.boot;

import com.github.camelion.micrometer.clickhouse.ClickHouseConfig;
import com.github.camelion.micrometer.clickhouse.ClickHouseMeterRegistry;
import com.netflix.spectator.api.Clock;
import io.micrometer.core.instrument.spectator.SpectatorMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.metrics.EnableMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Camelion
 * @since 27.06.17
 */
@Configuration
@EnableMetrics
@ConditionalOnProperty(value = "clickhouse.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class ClickHouseMetricConfiguration {

    private static final String CLICKHOUSE_JDBC_URL = "jdbc:clickhouse://localhost:8123/default";

    @Bean
    @ConditionalOnMissingBean
    ClickHouseConfig clickHouseConfig(ClickHouseDataSource clickHouseDataSource,
                                      Environment environment) {
        return new ClickHouseConfig(clickHouseDataSource, environment::getProperty);
    }

    @Bean
    @ConditionalOnMissingBean
    ClickHouseDataSource clickHouseDataSource(
            Environment environment,
            ClickHouseProperties clickHouseProperties) {
        String clickhouseUrl = environment.getProperty("clickhouse.metrics.datasource.url",
                CLICKHOUSE_JDBC_URL);

        return new ClickHouseDataSource(clickhouseUrl, clickHouseProperties)
                .withConnectionsCleaning(5, SECONDS);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("clickhouse.metrics.datasource")
    public ClickHouseProperties clickHouseProperties() {
        return new ClickHouseProperties();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    ClickHouseMeterRegistry clickhouseMeterRegistry(ClickHouseConfig clickHouseConfig) {
        return new ClickHouseMeterRegistry(Clock.SYSTEM, clickHouseConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    SpectatorMeterRegistry meterRegistry(ClickHouseMeterRegistry clickHouseMeterRegistry) {
        return new SpectatorMeterRegistry(clickHouseMeterRegistry);
    }
}
