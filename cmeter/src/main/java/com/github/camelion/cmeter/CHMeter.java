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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Camelion
 * @since 30.07.17
 */
abstract class CHMeter implements Meter {
    private static final Unsafe U;
    private static final long PROBE;

    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    private static final int RESIZE_STEP = 1 << 10; // step by 1k
    private static final int LLS = Long.BYTES * 2;
    private static final long CELLSBUSY;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);

            U = (Unsafe) theUnsafe.get(null);
            Class<?> tk = Thread.class;
            PROBE = U.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
            Class<?> cmk = CHMeter.class;
            CELLSBUSY = U.objectFieldOffset
                    (cmk.getDeclaredField("cellsBusy"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private final BP bp;
    private volatile BP[] bps;
    private volatile int cellsBusy;

    CHMeter() {
        bp = new BP(U.allocateMemory(RESIZE_STEP), RESIZE_STEP);
    }

    public Measurement measure() {
        ByteBuffer measure;

        return new Measurement(null, null);
    }

    protected final void write(long p1, long p2) {
        BP[] as;
        int m;
        BP a;
        if ((as = bps) != null || !bp.casPutNext(p1, p2)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                    (a = as[getProbe() & m]) == null ||
                    !(uncontended = a.casPutNext(p1, p2)))
                longWrite(p1, p2, uncontended);
        }
    }

    private static int getProbe() {
        return U.getInt(Thread.currentThread(), PROBE);
    }

    private void longWrite(long p1, long p2, boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current();
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;
        for (; ; ) {
            BP[] as;
            BP a;
            int n;
            if ((as = bps) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {
                                BP[] rs;
                                int m, j;
                                if ((rs = bps) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = new BP(U.allocateMemory(RESIZE_STEP), RESIZE_STEP);
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)
                    wasUncontended = true;
                else if (a.casPutNext(p1, p2))
                    break;
                else if (n >= NCPU || bps != as)
                    collide = false;
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (bps == as) {
                            BP[] rs = new BP[n << 1];
                            System.arraycopy(as, 0, rs, 0, n);
                            bps = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;
                }
                h = advanceProbe(h);
            } else if (cellsBusy == 0 && bps == as && casCellsBusy()) {
                boolean init = false;
                try {
                    if (bps == as) {
                        BP[] rs = new BP[2];
                        rs[h & 1] = new BP(U.allocateMemory(RESIZE_STEP), RESIZE_STEP);
                        bps = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (bp.casPutNext(p1, p2))
                break;
        }
    }

    private boolean casCellsBusy() {
        return U.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    private static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        U.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * Doesn't work without -XX:-RestrictContended JVM Flag
     */
    @sun.misc.Contended
    static class BP {
        private static final sun.misc.Unsafe U;
        private static final long lockOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);

                U = (Unsafe) theUnsafe.get(null);
                Class<?> ak = BP.class;
                lockOffset = U.objectFieldOffset
                        (ak.getDeclaredField("lock"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        volatile long address;
        volatile long length;
        volatile long pointer;
        volatile int lock;

        BP(long address, long length) {
            this.address = address;
            this.length = length;
        }

        boolean casPutNext(long p1, long p2) {
            // should be fast, because uncollided previously
            if (U.compareAndSwapInt(this, lockOffset, 0, 1)) {
                // resize if need
                if (pointer + LLS >= length) {
                    address = U.reallocateMemory(address, (length = length + RESIZE_STEP));
                }

                // todo deal with unaligned
                U.putLong(address + pointer, p1);
                U.putLong(address + pointer + Long.BYTES, p2);

                pointer += LLS;

                lock = 0;
                return true;
            }

            return false;
        }

        boolean compact(long[] copied) {
            if (U.compareAndSwapInt(this, lockOffset, 0, 1)
                    && pointer > 0) {
                long copyLength = pointer;
                long copyAddr = U.allocateMemory(copyLength);
                U.copyMemory(address, copyAddr, copyLength);
                address = U.reallocateMemory(address, Math.max(RESIZE_STEP, length - RESIZE_STEP));
                pointer = 0;

                copied[0] = copyAddr;
                copied[1] = copyLength;
                return true;
            }

            return false;
        }

        void dealloc() {
            U.freeMemory(address);
        }
    }
}
