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

package com.github.camelion.cmeter.exporter.clickhouse;

import com.github.camelion.cmeter.Cursor;
import com.github.camelion.cmeter.MeterId;
import ru.yandex.clickhouse.util.ClickHouseRowBinaryStream;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Camelion
 * @since 09.08.17
 */
final class ClickHouseCursor implements Cursor {
    private final ClickHouseRowBinaryStream stream;
    private final byte[] instanceId;

    ClickHouseCursor(ClickHouseRowBinaryStream stream, String instanceId) {
        this.stream = stream;
        this.instanceId = instanceId.getBytes();
    }

    @Override
    public void consume(MeterId meterId, long timestamp, long value) {
        try {
            stream.writeUInt32(MILLISECONDS.toSeconds(timestamp)); // timestamp as seconds

            writeString(stream, meterId.getSerializedName()); // metric
            writeString(stream, instanceId); // instance_id

            // Nested table with kv Tag
            byte[][] keys = meterId.getSerializedTagKeys();
            stream.writeUnsignedLeb128(keys.length);
            for (byte[] k : keys) {
                writeString(stream, k);
            }

            // Nested table with kv Tag
            byte[][] values = meterId.getSerializedTagValues();
            stream.writeUnsignedLeb128(values.length);
            for (byte[] v : values) {
                writeString(stream, v);
            }

            writeLong(stream, value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeString(ClickHouseRowBinaryStream stream, byte[] values) throws IOException {
        stream.writeUnsignedLeb128(values.length);
        for (byte value : values) {
            stream.writeInt8(value);
        }
    }

    private static void writeLong(ClickHouseRowBinaryStream stream, long l) throws IOException {
        // write `value` as long in LE (workaround CH-JDBC-GUAVA long bug)
        stream.writeInt8((byte) l);
        stream.writeInt8((byte) (l >> 8));
        stream.writeInt8((byte) (l >> 16));
        stream.writeInt8((byte) (l >> 24));
        stream.writeInt8((byte) (l >> 32));
        stream.writeInt8((byte) (l >> 40));
        stream.writeInt8((byte) (l >> 48));
        stream.writeInt8((byte) (l >> 56));
    }
}
