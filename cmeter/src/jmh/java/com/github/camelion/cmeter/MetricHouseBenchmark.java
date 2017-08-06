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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jmh.annotations.Threads.MAX;

/**
 * Current results (only order! repeat tests on your machine!)
 * MetricHouseBenchmark.writeMetric  thrpt   15    0,011 ±  0,002  ops/ns
 * MetricHouseBenchmark.writeMetric   avgt   15  315,585 ± 67,408   ns/op
 * <p>
 * In case of out of memory error, you can need increase `MaxDirectMemorySize`,
 * (because count of method invocations per iteration is can't be controlled by JMH,
 * and it can be larger, that real system does).
 *
 * @author Camelion
 * @since 26.07.17
 */
@Fork(value = 0, jvmArgs = {"-server"/*, "-XX:-RestrictContended"*/})
@Warmup(iterations = 15)
@BenchmarkMode(value = {Mode.Throughput, Mode.AverageTime})
@org.openjdk.jmh.annotations.Measurement(iterations = 15)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Threads(value = MAX)
public class MetricHouseBenchmark {

    private CHTimer meter;
    private Long timestamp;
    private Long value;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MetricHouseBenchmark.class.getSimpleName())
                .warmupIterations(15)
                .measurementIterations(15)
                .mode(Mode.Throughput).mode(Mode.AverageTime)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setUp() {
        meter = new CHTimer("test.timer", new Tag[0]);
        timestamp = System.nanoTime();
        value = 10L;
    }

    /**
     * Cleanups queue and deallocate directBuffers manually
     *
     * @return
     */
    @TearDown
    public void tearDown() {
        // force release memory
//        meter.clean();
    }

    /**
     * Tests recording value for single metric
     * Warning! Buffers not cleaning between measures.
     * Large amount of iterations can produce {@link OutOfMemoryError}, for this we increase
     * size of MaxDirectMemorySize, allowed to allocate for JVM with {@code -XX:MaxDirectMemorySize}
     * For testing on large iterations need another test, that does
     * method calls like a real system would does
     */
    @Benchmark
    public void writeMetric() {
        meter.record(timestamp, value);
    }
//    @Benchmark
//    public void writeMetric() {
//        buf.putLong(0, timestamp).putLong(8, value);
//    }
}
