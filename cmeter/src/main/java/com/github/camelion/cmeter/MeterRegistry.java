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

package com.github.camelion.cmeter;

/**
 * @author Camelion
 * @since 26.07.17
 */
public final class MeterRegistry {

    /**
     * Creates verbose timer that provides api for recording, or measuring execution of some code
     * Verbose timer registers all executions in storage
     *
     * @param name metric name
     * @param tags zero or more tags for this metric
     * @return new verbose timer instance
     */
    public static Timer verboseTimer(String name, Tag... tags) {
        return MetricHouse.registerMeter(new VerboseTimer(new MeterId(name, tags)));
    }

    /**
     * Creates verbose counter that stores every increment
     * Verbose timer registers all executions in storage
     *
     * @param name metric name
     * @param tags zero or more tags for this metric
     * @return new verbose counter instance
     */
    public static Counter verboseCounter(String name, Tag... tags) {
        return MetricHouse.registerMeter(new VerboseCounter(new MeterId(name, tags)));
    }
}
