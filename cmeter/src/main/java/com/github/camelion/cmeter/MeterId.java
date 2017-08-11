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
 * @since 10.08.17
 */
public final class MeterId {
    private final String name;
    private final Tag[] tags;
    private volatile byte[] nameBytes;
    private volatile byte[][] tagKeys;
    private volatile byte[][] tagValues;

    MeterId(String name, Tag[] tags) {
        this.name = name;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public Tag[] getTags() {
        return tags;
    }

    public byte[] getSerializedName() {
        if (this.nameBytes == null) {
            this.nameBytes = name.getBytes();
        }

        return nameBytes;
    }

    public byte[][] getSerializedTagKeys() {
        if (this.tagKeys == null) {
            this.tagKeys = new byte[tags.length][];
            for (int i = 0; i < tags.length; i++) {
                tagKeys[i] = tags[i].getKey().getBytes();
            }
        }

        return tagKeys;
    }

    public byte[][] getSerializedTagValues() {
        if (this.tagValues == null) {
            this.tagValues = new byte[tags.length][];
            for (int i = 0; i < tags.length; i++) {
                tagValues[i] = tags[i].getValue().getBytes();
            }
        }

        return tagValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeterId meterId = (MeterId) o;
        return Objects.equals(name, meterId.name) &&
                Arrays.equals(tags, meterId.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags);
    }
}
