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

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Camelion
 * @since 24.07.17
 */
class CHTimer extends CHMeter implements Timer {
    private final String name;
    private final Tag[] tags;

    CHTimer(String name, Tag[] tags) {
        this.name = name;
        this.tags = tags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CHTimer chTimer = (CHTimer) o;
        return Objects.equals(name, chTimer.name) &&
                Arrays.equals(tags, chTimer.tags);
    }

    @Override
    public void record(long timestamp, long value) {
        write(timestamp, value);
    }
}
