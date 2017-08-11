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


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Camelion
 * @since 25.07.17
 */
final class MetricHouse {
    private final static List<Meter> meters = new CopyOnWriteArrayList<>();

    static <T extends Meter> T registerMeter(T meter) {
        meters.add(meter);
        return meter;
    }

    /**
     * Todo:
     * 1) Special off-heap storage for Java 9
     * 2) Heap storage
     * 3) Java 8 uncontended J8_Store variant for case, where {@code @sun.mics.Contended} restricted in user code
     * 4) Dig into J10 Vector instructions and value types (Panama and Valhalla)
     * 5) Some switch logic between them (per property based, or per meter)
     *
     * @return store for measurements
     */
    static Store createStore() {
        return new J8_Store();
    }

    /**
     * Probably slow reading, but reading is rare in time, and that's normal.
     * May be can read in more threads but need to deal with {@code Cursor} consumers
     *
     * @param cursor metrics acceptor, that consumes id and values
     */
    static void retain(Cursor cursor) {
        meters.forEach(m -> m.retain(cursor));
    }
}