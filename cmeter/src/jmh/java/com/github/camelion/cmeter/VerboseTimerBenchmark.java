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
import java.util.concurrent.atomic.LongAdder;

import static org.openjdk.jmh.annotations.Threads.MAX;

/**
 * Current results (only order! repeat tests on your machine!)
 * <pre>{@code
 * Benchmark                           Mode  Cnt   Score    Error   Units
 * VerboseTimerBenchmark.writeMetric  thrpt    5   0,103 ±  0,050  ops/ns
 * VerboseTimerBenchmark.writeMetric   avgt    5  36,887 ± 24,976   ns/op
 * }</pre>
 * <p>
 *
 * @author Camelion
 * @since 26.07.17
 */
@Fork(value = 1, jvmArgs = {"-server", "-XX:-RestrictContended"/*, "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintAssembly"*/})
@Warmup(iterations = 3)
@BenchmarkMode(value = {Mode.Throughput, Mode.AverageTime})
@org.openjdk.jmh.annotations.Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Threads(value = MAX)
public class VerboseTimerBenchmark {
    private VerboseTimer meter;
    private Long timestamp;
    private Long value;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VerboseTimerBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(5)
                .mode(Mode.Throughput).mode(Mode.AverageTime)
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setUp() {
        meter = new VerboseTimer(new MeterId("test.timer", new Tag[0]));
        timestamp = System.nanoTime();
        value = 10L;
    }

    /**
     * Cleanups queue and deallocate directBuffers manually
     */
    @TearDown(Level.Iteration)
    public void tearDown() {
        LongAdder counter = new LongAdder();
        // cleanup
        meter.retain((meterId, timestamp, value) -> counter.increment());

        // cheat cleanup to initial size
        long cnt = counter.sum();
        for (int i = 0; i < cnt / 4096; i++)
            meter.retain((meterId, timestamp, value) -> counter.increment());

        System.out.println("processed: " + counter.sum() + " measures");
    }

    /**
     * Tests recording value for single metric
     */
    @Benchmark
    public void writeMetric() {
        meter.record(timestamp, value);
    }
}
