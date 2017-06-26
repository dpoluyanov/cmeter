package ru.jts.spring.clickhouse.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Camelion
 * @since 22.06.17
 */
public class ClickhouseMetricWriter implements MetricWriter, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ClickhouseMetricWriter.class);
    private final String tableName;
    private final String instanceId;
    private final JdbcTemplate clickhouseJdbcTemplate;

    public ClickhouseMetricWriter(ClickhouseDatasourceProperties clickhouseDatasourceProperties, String applicationName, String instanceId) {
        this.tableName = tablify(applicationName);
        this.instanceId = instanceId;
        this.clickhouseJdbcTemplate = new JdbcTemplate(DataSourceBuilder.create()
                .driverClassName(clickhouseDatasourceProperties.getDriverClassName())
                .url(clickhouseDatasourceProperties.getUrl())
                .username(clickhouseDatasourceProperties.getUsername())
                .password(clickhouseDatasourceProperties.getPassword())
                .build());
    }

    private String tablify(String applicationName) {
        return applicationName.replaceAll("[\\.-]", "_");
    }

    @Override
    public void increment(Delta<?> delta) {
        if (log.isDebugEnabled())
            log.debug("Sending [{}]/[{}] delta to clickhouse", delta.getName(), delta.getValue());
        clickhouseJdbcTemplate.update("" +
                        "INSERT INTO " + tableName + " (timestamp, instance_id, metric, value)\n" +
                        "VALUES(?, ?, ?, ?)",
                delta.getTimestamp(),
                instanceId,
                delta.getName(),
                delta.getValue());
    }

    @Override
    public void reset(String metricName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(Metric<?> metric) {
        if (log.isDebugEnabled())
            log.debug("Sending [{}]/[{}] metric to clickhouse", metric.getName(), metric.getValue());
        clickhouseJdbcTemplate.update("" +
                        "INSERT INTO " + tableName + " (timestamp, instance_id, metric, value)\n" +
                        "VALUES(?, ?, ?, ?)",
                metric.getTimestamp(),
                instanceId,
                metric.getName(),
                metric.getValue());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Create table [{}] for metrics in clickhouse", tableName);
        clickhouseJdbcTemplate.execute("" +
                "CREATE TABLE IF NOT EXISTS " + tableName + "\n" +
                "(\n" +
                " partition Date DEFAULT toDate(timestamp),\n" +
                " timestamp DateTime,\n" +
                " instance_id String,\n" +
                " metric String,\n" +
                " value Float64\n" +
                ") ENGINE = MergeTree(partition, (timestamp, metric, instance_id), 8192)");
    }
}
