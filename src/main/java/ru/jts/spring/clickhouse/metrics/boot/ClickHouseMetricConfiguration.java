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
            @Value("${clickhouse.datasource.url}") String clickhouseUrl,
            ClickHouseMeterRegistry clickHouseMeterRegistry,
            ClickHouseProperties clickHouseProperties) {

        return new ClickHouseMetricWriter(
                clickHouseMeterRegistry,
                new ClickHouseDataSource(clickhouseUrl, clickHouseProperties),
                tablify(clickhouseTable), instanceId);
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
