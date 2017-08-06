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


import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Camelion
 * @since 25.07.17
 * <p>
 * Every buffer stores 16 bytes chunks
 * where first 8 byte - timestamp in nanoseconds, next 8 bytes is long/double value of meter on given timestamp.
 * <p>
 * Have two memory regions. First is pre-allocated directBuffers for every meter,
 * that used for storing small amount of actual values.
 * Count of stored values per buffer can be calculated with BUFFER_SIZE/16.
 * Typically it is 256.
 * <p>
 * Second region - is completed directBuffers, that would be collected on next {@link #measurements()} call
 * and would be released after sending to backend storage (but actually drops after next garbage collection)
 * <p>
 * On {@link #measurements()} calling, size of off-heap memory can grow up to 2x of designed size,
 * because all directBuffers recreated with empty size
 */
final class MetricHouse {
    /**
     * 8kb off-heap memory for every metric. ~ 8 MB per 1000 meters.
     */
    static final int BUFFER_SIZE = 128 * 1024 * 1024;
    private final static ConcurrentHashMap<Meter, ByteBuffer> metersMap = new ConcurrentHashMap<>();
    private final static ConcurrentLinkedQueue<Measurement> measurements = new ConcurrentLinkedQueue<>();

    static <T extends Meter> T registerMeter(T meter) {
        metersMap.computeIfAbsent(meter, m -> ByteBuffer.allocateDirect(BUFFER_SIZE));
        return meter;
    }
}