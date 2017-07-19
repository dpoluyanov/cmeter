package ru.jts.spring.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.metrics.annotation.Timed;
import org.springframework.metrics.instrument.Counter;
import org.springframework.metrics.instrument.DistributionSummary;
import org.springframework.metrics.instrument.MeterRegistry;
import org.springframework.metrics.instrument.Timer;
import org.springframework.metrics.instrument.binder.JvmGcMetrics;
import org.springframework.metrics.instrument.binder.JvmMemoryMetrics;
import org.springframework.metrics.instrument.scheduling.MetricsSchedulingAspect;
import org.springframework.metrics.instrument.stats.hist.NormalHistogram;
import org.springframework.metrics.instrument.stats.quantile.GKQuantiles;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.jts.spring.clickhouse.metrics.boot.EnableClickHouseMetrics;

import java.util.concurrent.ThreadLocalRandom;

@EnableScheduling
@EnableClickHouseMetrics
@SpringBootApplication
public class SpringClickHouseMetricsSampleApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SpringClickHouseMetricsSampleApplication.class);
    private final Counter requestCounter;
    private final DistributionSummary responseSizes;
    private final Timer requestTimer;

    @Autowired
    public SpringClickHouseMetricsSampleApplication(MeterRegistry meterRegistry) {
        // meter registry provides typical meters, e.g. counters, timers, gauges
        this.requestCounter = meterRegistry.counter("requests");
        this.responseSizes = meterRegistry.summaryBuilder("response-size")
                // provides 10 buckets with size 2000 for every bucket
                .histogram(new NormalHistogram<>(NormalHistogram.linear(0, 20000, 10)))
                .create();
        this.requestTimer = meterRegistry.timerBuilder("request-timer")
                .quantiles(GKQuantiles.quantiles(0.5, 0.95, 0.97, 0.99).create())
                .create();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringClickHouseMetricsSampleApplication.class, args);
    }

    // calculates time for every long task execution
    @Timed(value = "longTaskExecutor", longTask = true, extraTags = {"executors", "long-task-exec"})
    @Scheduled(fixedDelay = 10000)
    public void longTaskExecutor() throws InterruptedException {
        int doWorkFor = Math.abs(ThreadLocalRandom.current().nextInt()) % 10000;
        LOG.info("Start doing work for {} ms", doWorkFor);
        Thread.sleep(doWorkFor);
        LOG.info("Ok, job is done!");
    }

    @Scheduled(fixedRate = 10)
    public void doRequest() throws InterruptedException {

        int bytesReceived = requestTimer.record(this::request);

        // save response size to distribution summary
        responseSizes.record(bytesReceived);
    }

    // calculates quantilies for request execution timings
    private int request() {
        int doWorkFor = Math.abs(ThreadLocalRandom.current().nextInt()) % 50;

        try {
            Thread.sleep(doWorkFor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // increment requests counter
        requestCounter.increment();

        return Math.abs(ThreadLocalRandom.current().nextInt()) % 20000;
    }


    @Bean
    JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * Temporally, until <a href="https://github.com/spring-projects/spring-metrics/pull/47">wouldn't merged</a>
     *
     * @param registry
     * @return
     */
    @Bean
    public MetricsSchedulingAspect metricsSchedulingAspect(MeterRegistry registry) {
        return new MetricsSchedulingAspect(registry);
    }
}
