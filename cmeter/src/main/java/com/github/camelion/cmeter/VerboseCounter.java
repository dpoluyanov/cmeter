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

import java.util.Objects;

/**
 * @author Camelion
 * @since 07.08.17
 * Stores every increment with it timestamp in storage
 */
final class VerboseCounter implements Counter {
    private final Store store;
    private final MeterId meterId;

    VerboseCounter(MeterId meterId) {
        this.meterId = meterId;
        this.store = MetricHouse.createStore();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerboseCounter that = (VerboseCounter) o;
        return Objects.equals(meterId, that.meterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meterId);
    }

    /**
     * Stores every timestamp/increment amount pair in store.
     * total sum should be calculated later on backend storage (with different slices)
     *
     * @param timestamp time mark of incrementation
     * @param amount    value for increase
     */
    @Override
    public void increment(long timestamp, long amount) {
        if (amount < 0)
            throw new IllegalArgumentException("Amount should be positive");
        store.write(timestamp, amount);
    }

    @Override
    public void retain(Cursor cursor) {
        store.retain(meterId, cursor);
    }
}
