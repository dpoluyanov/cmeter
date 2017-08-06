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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Camelion
 * @since 29.07.17
 */
final class ByteBufferPool {
    static final int BUFFER_SIZE = 8192;
    static final Queue<ByteBuffer> directBuffers = new ConcurrentLinkedQueue<>();
    private static final ExecutorService arenaRecreation;
    static  {
        arenaRecreation = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(arenaRecreation::shutdownNow));
    }

    private static ArenaRecreationTask arenaRecreationTask = new ArenaRecreationTask();

    static ByteBuffer acquireBuffer() {
        ByteBuffer direct = directBuffers.poll();
        if (direct == null) {
            arenaRecreation.submit(arenaRecreationTask);
            return ByteBuffer.allocate(BUFFER_SIZE);
        }
        return direct;
    }

    static void release(ByteBuffer byteBuffer) {
        if (byteBuffer.isDirect()) {
            directBuffers.offer(byteBuffer);
        }
    }

    static class ArenaRecreationTask implements Runnable {
        private ArenaRecreationTask() {
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ByteBufferPool.directBuffers.add(ByteBuffer.allocateDirect(BUFFER_SIZE));
                }
            } catch (OutOfMemoryError e) {
                // ignored
            }
        }
    }
}
