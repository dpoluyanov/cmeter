package ru.jts.spring.clickhouse.metrics.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.jts.spring.clickhouse.metrics.ClickhouseDatasourceProperties;
import ru.jts.spring.clickhouse.metrics.ClickhouseMetricWriter;

/**
 * @author Camelion
 * @since 22.06.17
 */
@Configuration
@ConditionalOnProperty(value = "spring.metrics.export.clickhouse.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ClickhouseDatasourceProperties.class)
class ClickhouseMetricAutoConfiguration {

    @Value("${spring.application.name:application}")
    private String springApplicationName;

    @Value("${spring.application.instance-id:${spring.cloud.consul.discovery.instance-id:${server.port:null}}}")
    private String instanceId;

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("clickhouse.datasource.url")
    @ExportMetricWriter
    ClickhouseMetricWriter clickhouseMetricWriter(
            ClickhouseDatasourceProperties clickhouseDatasourceProperties) {
        return new ClickhouseMetricWriter(clickhouseDatasourceProperties, springApplicationName, instanceId);
    }

    @Bean
    ClickhouseDatasourceProperties clickhouseDatasourceProperties() {
        return new ClickhouseDatasourceProperties();
    }
}
