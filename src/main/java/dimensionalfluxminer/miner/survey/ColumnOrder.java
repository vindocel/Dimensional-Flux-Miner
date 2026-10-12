package dimensionalfluxminer.miner.survey;

import java.util.Arrays;

/**
 * All X/Z offsets inside the circular drain radius, ordered from the center outward. Shared by every survey.
 */
public final class ColumnOrder {
    public static final int RADIUS = 300;
    private static volatile ColumnOrder instance;

    private final int[] dx;
    private final int[] dz;

    private ColumnOrder(int radius) {
        long r2 = (long) radius * radius;
        int side = radius * 2 + 1;
        long[] packed = new long[side * side];
        int n = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                long d2 = (long) x * x + (long) z * z;
                if (d2 <= r2) {
                    // distance in the high bits keeps the sort stable by distance, then by position.
                    packed[n++] = (d2 << 22) | ((long) (x + radius) << 11) | (z + radius);
                }
            }
        }
        packed = Arrays.copyOf(packed, n);
        Arrays.sort(packed);
        dx = new int[n];
        dz = new int[n];
        for (int i = 0; i < n; i++) {
            dx[i] = (int) ((packed[i] >> 11) & 0x7FF) - radius;
            dz[i] = (int) (packed[i] & 0x7FF) - radius;
        }
    }

    public static ColumnOrder get() {
        ColumnOrder local = instance;
        if (local == null) {
            synchronized (ColumnOrder.class) {
                local = instance;
                if (local == null) {
                    instance = local = new ColumnOrder(RADIUS);
                }
            }
        }
        return local;
    }

    /** Theoretical number of columns inside the radius (about pi * 300^2). */
    public int size() {
        return dx.length;
    }

    public int dx(int index) {
        return dx[index];
    }

    public int dz(int index) {
        return dz[index];
    }
}
