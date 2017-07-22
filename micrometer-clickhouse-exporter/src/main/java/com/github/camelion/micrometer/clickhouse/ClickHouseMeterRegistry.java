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

package com.github.camelion.micrometer.clickhouse;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.spectator.SpectatorMeterRegistry;

/**
 * @author Camelion
 * @since 22.07.17
 */
public class ClickHouseMeterRegistry extends SpectatorMeterRegistry {

    public ClickHouseMeterRegistry(ClickHouseConfig config) {
        this(Clock.SYSTEM, config);
    }

    public ClickHouseMeterRegistry(Clock clock, ClickHouseConfig config) {
        super(new ClickHouseRegistry(new com.netflix.spectator.api.Clock() {
            @Override
            public long wallTime() {
                return System.currentTimeMillis();
            }

            @Override
            public long monotonicTime() {
                return clock.monotonicTime();
            }
        }, config));
    }

    public void start() {
        ((ClickHouseRegistry) getSpectatorRegistry()).start();
    }

    public void stop() {
        ((ClickHouseRegistry) getSpectatorRegistry()).stop();
    }
}
