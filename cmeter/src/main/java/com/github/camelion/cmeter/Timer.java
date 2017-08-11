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

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * @author Camelion
 * @since 24.07.17
 * timestamp stored in milliseconds, value stored in nanoseconds
 */
public interface Timer extends Meter {

    default <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> record(supplier);
    }

    default <T> T record(Supplier<T> supplier) {
        long start = Clock.SYSTEM.monotonicTime();
        try {
            return supplier.get();
        } finally {
            long end = Clock.SYSTEM.monotonicTime();
            record(Clock.SYSTEM.wallTime(), end - start);
        }
    }

    /**
     * Writes value to timer
     *
     * @param timestamp the time point for storing metric in milliseconds
     * @param value     amount of ms
     */
    void record(long timestamp, long value);

    default <V> Callable<V> wrap(Callable<V> callable) throws Exception {
        return () -> record(callable);
    }

    default <V> V record(Callable<V> callable) throws Exception {
        long start = Clock.SYSTEM.monotonicTime();
        try {
            return callable.call();
        } finally {
            long end = Clock.SYSTEM.monotonicTime();
            record(Clock.SYSTEM.wallTime(), end - start);
        }
    }

    default Runnable wrap(Runnable run) {
        return () -> record(run);
    }

    default void record(Runnable runnable) {
        long start = Clock.SYSTEM.monotonicTime();
        try {
            runnable.run();
        } finally {
            long end = Clock.SYSTEM.monotonicTime();
            record(Clock.SYSTEM.wallTime(), end - start);
        }
    }
}
