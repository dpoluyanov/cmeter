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

import java.util.concurrent.TimeUnit;

/**
 * @author Camelion
 * @since 24.07.17
 */
public interface Timer extends Meter {
    default void record(long timestamp, long value, TimeUnit unit) {
        record(timestamp, TimeUnit.NANOSECONDS.convert(value, unit));
    }

    /**
     * Writes nanosecond to timer
     *
     * @param timestamp the time point for storing metric (in seconds)
     * @param value     amount of ns
     */
    void record(long timestamp, long value);
}
