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

import java.lang.ref.WeakReference;
import java.util.function.ToDoubleFunction;

/**
 * @author Camelion
 * @since 14.08.17
 */
class StepGauge<T> implements Gauge {
    private final MeterId meterId;
    private final Store store;
    private final WeakReference<T> ref;
    private final ToDoubleFunction<T> asDouble;

    StepGauge(MeterId meterId, WeakReference<T> ref, ToDoubleFunction<T> asDouble) {
        this.meterId = meterId;
        this.store = MetricHouse.createStore();
        this.ref = ref;
        this.asDouble = asDouble;
    }

    @Override
    public Double getValue() {
        T obj;
        if ((obj = ref.get()) != null)
            return asDouble.applyAsDouble(obj);
        return null;
    }

    @Override
    public void record(long timestamp, double value) {
        store.write(timestamp, value);
    }

    @Override
    public void retain(Cursor cursor) {
        store.retain(meterId, cursor);
    }
}
