package dimensionalfluxminer.smelter;

/**
 * Distribution of an incoming amount across compatible lanes when Auto-Split is ON.
 * <p>
 * Bulk-aware mode (Raw without Efficiency Upgrade) preserves pairs: odd lane counts are completed first, pairs are
 * then spread as evenly as possible and a single unmatched item is placed only after all pairs were assigned.
 */
public final class AutoSplit {
    private AutoSplit() {
    }

    /**
     * @param counts   current item count per candidate lane
     * @param free     free capacity per candidate lane
     * @param amount   items to distribute
     * @param pairAware keep Raw pairs together
     * @return amount assigned to each lane
     */
    public static long[] distribute(long[] counts, long[] free, long amount, boolean pairAware) {
        int n = counts.length;
        long[] add = new long[n];
        long left = amount;
        if (pairAware) {
            for (int i = 0; i < n && left > 0; i++) {
                if (counts[i] % 2 == 1 && free[i] > 0) {
                    add[i]++;
                    left--;
                }
            }
        }
        long unit = pairAware ? 2 : 1;
        while (left >= unit) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (free[i] - add[i] >= unit && (best < 0 || counts[i] + add[i] < counts[best] + add[best])) {
                    best = i;
                }
            }
            if (best < 0) {
                break;
            }
            add[best] += unit;
            left -= unit;
        }
        if (left > 0) {
            // Remaining single item(s): least-loaded lane with space.
            while (left > 0) {
                int best = -1;
                for (int i = 0; i < n; i++) {
                    if (free[i] - add[i] >= 1 && (best < 0 || counts[i] + add[i] < counts[best] + add[best])) {
                        best = i;
                    }
                }
                if (best < 0) {
                    break;
                }
                add[best]++;
                left--;
            }
        }
        return add;
    }
}
