package ru.jts.spring.clickhouse.metrics.boot;

import org.springframework.context.annotation.Import;
import org.springframework.metrics.boot.EnableMetrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Camelion
 * @since 27.06.17
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableMetrics
@Import(ClickHouseMetricConfiguration.class)
public @interface EnableClickHouseMetrics {
}
