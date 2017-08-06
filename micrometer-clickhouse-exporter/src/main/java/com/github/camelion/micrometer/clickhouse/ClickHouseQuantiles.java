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
import io.micrometer.core.instrument.Observer;
import io.micrometer.core.instrument.util.MeterId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Camelion
 * @since 24.07.17
 */
public class ClickHouseQuantiles implements Observer {
    private final MeterId meterId;
    private final Clock clock;
    private BlockingQueue<TVPair> measures = new LinkedBlockingQueue<>();

    public ClickHouseQuantiles(MeterId meterId, Clock clock) {
        this.meterId = meterId;
        this.clock = clock;
    }

    @Override
    public void observe(double value) {
        measures.add(new TVPair(clock.monotonicTime(), value));
    }

    public CHBucket measure() {
        List<TVPair> tvs = new ArrayList<>();
        measures.drainTo(tvs);

        return new CHBucket(meterId, tvs);
    }

    public static class TVPair {
        private final long timestamp;
        private final double value;

        private TVPair(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long timestamp() {
            return timestamp;
        }

        public double value() {
            return value;
        }
    }

    public static class CHBucket {
        private MeterId meterId;
        private List<TVPair> tvPairs;
        private CHBucket(MeterId meterId, List<TVPair> tvPairs) {
            this.meterId = meterId;
            this.tvPairs = tvPairs;
        }

        public MeterId meterId() {
            return meterId;
        }

        public List<TVPair> pairs() {
            return tvPairs;
        }
    }
}
