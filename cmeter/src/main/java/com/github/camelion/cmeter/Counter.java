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

/**
 * @author Camelion
 * @since 07.08.17
 */
public interface Counter extends Meter {

    default <V> Callable<V> wrap(Callable<V> callable) throws Exception {
        return () -> invocations(callable);
    }

    default <V> V invocations(Callable<V> callable) throws Exception {
        try {
            return callable.call();
        } finally {
            increment();
        }
    }

    default void increment() {
        increment(Clock.SYSTEM.wallTime(), 1L);
    }

    /**
     * Increments counter on given amount at provided timestamp.
     *
     * @param timestamp time-mark, for given amount value
     * @param amount    value for increment counter
     */
    void increment(long timestamp, long amount);

    default Runnable wrap(Runnable runnable) {
        return () -> invocations(runnable);
    }

    default void invocations(Runnable runnable) {
        try {
            runnable.run();
        } finally {
            increment();
        }
    }

    default void increment(long amount) {
        increment(Clock.SYSTEM.wallTime(), amount);
    }
}
