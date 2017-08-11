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

import com.github.camelion.cmeter.MeterRegistry;
import com.github.camelion.cmeter.Tag;
import com.github.camelion.cmeter.Timer;
import com.github.camelion.cmeter.exporter.clickhouse.ClickHouseExporter;
import com.github.camelion.cmeter.exporter.clickhouse.ClickHouseExporterConfig;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import java.util.concurrent.TimeUnit;

/**
 * @author Camelion
 * @since 10.08.17
 */
final class CHExporterSample {
    private static int COUNTER = 0;

    public static void main(String[] args) throws InterruptedException {
        ClickHouseExporterConfig exporterConfig = new ClickHouseExporterConfig(createDatasource());
        exporterConfig.setExportRate(3);

        ClickHouseExporter exporter = new ClickHouseExporter(exporterConfig);

        Timer timer = MeterRegistry.verboseTimer("verbose.timer", Tag.of("my", "timer"));

        exporter.start();
        for (int i = 0; i < 100_000_000; i++) {
            timer.record(CHExporterSample::doWork);
        }

        System.out.println("Job is done");

        TimeUnit.SECONDS.sleep(30); // default exportRate

        exporter.stop();
    }

    private static ClickHouseDataSource createDatasource() {
        ClickHouseProperties clickHouseProperties = new ClickHouseProperties();
        clickHouseProperties.setUser("default");
        clickHouseProperties.setPassword("");
        return new ClickHouseDataSource("jdbc:clickhouse://localhost:8123/default", clickHouseProperties);
    }

    private static void doWork() {
        for (int j = 0; j < 10000; j++) {
            COUNTER--;
        }
        for (int j = 0; j < 10000; j++) {
            COUNTER++;
        }
    }
}
