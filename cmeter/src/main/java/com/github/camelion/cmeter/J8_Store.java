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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Camelion
 * @since 30.07.17
 * TODO May be should do efficient store with page-aligned memory
 * TODO Need cleaner for garbage collected meters
 * currently can lead to off-heap memory leak
 * but it's not actual problem,
 * because in this time there are no way to deregister meter, and store is persistent in memory
 */
final class J8_Store extends Store {
    private static final Unsafe U;
    private static final long PROBE;

    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    private static final int RESIZE_STEP; // step by page size
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
            Class<?> cmk = J8_Store.class;
            CELLSBUSY = U.objectFieldOffset
                    (cmk.getDeclaredField("cellsBusy"));
            RESIZE_STEP = U.pageSize();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private final BP bp;
    private volatile BP[] bps;
    private volatile int cellsBusy;

    J8_Store() {
        bp = new BP(U.allocateMemory(RESIZE_STEP), RESIZE_STEP);
    }

    @Override
    final void write(long p1, long p2) {
        p2 = Double.doubleToLongBits(p2);
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

    @Override
    void write(long p1, double p2) {
        write(p1, Double.doubleToLongBits(p2));
    }

    @Override
    void retain(MeterId meterId, Cursor cursor) {
        long[] al = new long[2];

        // read base
        compactAndConsume(meterId, cursor, bp, al);

        // read nodes
        BP[] cbps = bps;
        BP cbp;
        if (cbps != null) {
            for (int i = 0; i < cbps.length; ++i) {
                if ((cbp = cbps[i]) != null) {
                    compactAndConsume(meterId, cursor, cbp, al);
                }
            }
        }
    }

    private void compactAndConsume(MeterId meterId, Cursor cursor, BP inbp, long[] al) {
        al[0] = al[1] = 0;
        // resize and copy al[1] amount of data to al[0] address
        while (inbp.pointer > 0 && !inbp.compact(al)) ;

        // have data
        if (al[1] > 0) {
            try {
                for (long i = 0; i < al[1]; i += LLS) {
                    cursor.consume(meterId, U.getLong(al[0] + i), U.getLong(al[0] + i + Long.BYTES));
                }
            } finally {
                U.freeMemory(al[0]);
            }
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
                                    rs[j] = new BP(U.allocateMemory(RESIZE_STEP), RESIZE_STEP, p1, p2);
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
                        rs[h & 1] = new BP(U.allocateMemory(RESIZE_STEP), RESIZE_STEP, p1, p2);
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
     * And doesn't work in j9, because this annotation was moved to another package
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

        private volatile long address;
        private volatile long length;
        private volatile long pointer;
        private volatile int lock;

        // preinitialized
        BP(long address, long length, long p1, long p2) {
            this(address, length);
            U.putLong(address + pointer, p1);
            U.putLong(address + pointer + Long.BYTES, p2);
            pointer = LLS;
        }

        BP(long address, long length) {
            this.address = address;
            this.length = length;
        }

        /**
         * @param p1 - possible timestamp mark
         * @param p2 - possible value
         * @return {@code true} when value is stored in memory otherwise return {@code false}
         */
        boolean casPutNext(long p1, long p2) {
            if (U.compareAndSwapInt(this, lockOffset, 0, 1)) {
                // resize if need
                if (pointer + LLS >= length) {
                    long prevLength = length;
                    try {
                        address = U.reallocateMemory(address, (length = prevLength + RESIZE_STEP));
                    } catch (OutOfMemoryError e) {
                        length = prevLength;
                        lock = 0;
                        throw e;
                    }
                }

                // todo deal with unaligned and other platforms
                U.putLong(address + pointer, p1);
                U.putLong(address + pointer + Long.BYTES, p2);

                pointer += LLS;

                lock = 0;
                return true;
            }

            return false;
        }

        /**
         * @param copied incoming buffer, that wants two values.
         *               first - address from should start reading,
         *               second - length of data on this address
         * @return - true, if bp was been compacted, false, when
         */
        boolean compact(long[] copied) {
            if (U.compareAndSwapInt(this, lockOffset, 0, 1)) {
                // may be need compact only
                if (pointer == 0) {
                    // should decrease size
                    if (length > RESIZE_STEP) {
                        // usually same address, without last bytes
                        address = U.reallocateMemory(address, length = Math.max(RESIZE_STEP, length - RESIZE_STEP));
                    }
                    copied[0] = address;
                    copied[1] = 0; // nothing to read

                    lock = 0;
                    return true;
                }

                long copyLength = pointer;
                long copyAddr;
                try {
                    copyAddr = U.allocateMemory(copyLength);
                } catch (OutOfMemoryError e) {
                    // force unlock on oom
                    lock = 0;
                    throw e;
                }
                // TODO need copy through safe-point gap (e.g. 1MB, similar to DirectBuffer)
                U.copyMemory(address, copyAddr, copyLength);

                // try to decrease meter memory by one step on every reading
                address = U.reallocateMemory(address, length = Math.max(RESIZE_STEP, length - RESIZE_STEP));
                pointer = 0;

                copied[0] = copyAddr;
                copied[1] = copyLength;

                lock = 0;
                return true;
            }

            return false;
        }

        public final void dealloc() {
            U.freeMemory(address);
        }
    }
}
