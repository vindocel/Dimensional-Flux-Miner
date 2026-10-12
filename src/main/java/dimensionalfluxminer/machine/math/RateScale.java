package dimensionalfluxminer.machine.math;

/**
 * Geometric interpolation between the Rate 1 and Rate 10 values of a machine.
 * <pre>value(n) = min * (max / min)^((n - 1) / 9)</pre>
 */
public final class RateScale {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 10;

    private RateScale() {
    }

    public static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    /** Next (or previous) level, wrapping x10 -> x1 and x1 -> x10. */
    public static int cycleLevel(int level, boolean forward) {
        int span = MAX_LEVEL - MIN_LEVEL + 1;
        return MIN_LEVEL + Math.floorMod(clampLevel(level) - MIN_LEVEL + (forward ? 1 : -1), span);
    }

    public static double value(int level, double min, double max) {
        if (!(min > 0) || !(max > min)) {
            throw new IllegalArgumentException("Rate scale requires 0 < min < max (min=" + min + ", max=" + max + ")");
        }
        int n = clampLevel(level);
        return min * Math.pow(max / min, (n - 1) / 9.0);
    }

    /** FE/t power limit of a level, optionally scaled (Efficiency Upgrade), rounded to the nearest FE. */
    public static long powerLimit(int level, double minFe, double maxFe, double multiplier) {
        return Math.round(value(level, minFe, maxFe) * multiplier);
    }

    /** Ten logical ticks of the effective selected power limit. */
    public static long energyBufferTarget(long effectivePowerLimit) {
        return 10L * effectivePowerLimit;
    }

    /**
     * Alloy Smelter throughput per lane for a recipe with baseline {@code T} ticks:
     * <pre>R(n,T) = (1/T) * (192*T)^((n-1)/9)</pre>
     */
    public static double smelterOperationsPerTick(int level, int baselineTicks, double rateTenItemsPerTick) {
        int t = Math.max(1, baselineTicks);
        int n = clampLevel(level);
        return (1.0 / t) * Math.pow(rateTenItemsPerTick * t, (n - 1) / 9.0);
    }
}
