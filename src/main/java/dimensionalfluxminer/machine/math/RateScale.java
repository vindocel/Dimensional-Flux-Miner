package dimensionalfluxminer.machine.math;

/**
 * Geometric interpolation of a machine's speed between Rate 1 and Rate 10, and the energy derived from it.
 * <pre>value(n) = min * (max / min)^((n - 1) / 9)
 * FE/t(n)  = operationsPerTick(n) * energyPerOperation</pre>
 * Every operation costs the same FE at every Rate, so a faster level asks for more FE/t in the same proportion.
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

    /** FE/t of {@code operationsPerTick} operations costing {@code energyPerOperation} FE each. */
    public static double power(double operationsPerTick, double energyPerOperation) {
        if (!(operationsPerTick > 0) || !(energyPerOperation > 0)) {
            return 0.0;
        }
        return operationsPerTick * energyPerOperation;
    }

    /** FE/t rounded to the nearest FE, saturated so it never overflows a long. */
    public static long powerLimit(double power) {
        if (!(power > 0)) {
            return 0L;
        }
        return power >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(power);
    }

    /** Ten logical ticks of the effective selected power limit. */
    public static long energyBufferTarget(long effectivePowerLimit) {
        return saturatedMultiply(10L, effectivePowerLimit);
    }

    /** {@code a * b} for non-negative values, saturated at {@link Long#MAX_VALUE}. */
    public static long saturatedMultiply(long a, long b) {
        if (a <= 0 || b <= 0) {
            return 0L;
        }
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    /**
     * Alloy Smelter throughput of one lane for a recipe with baseline {@code T} ticks (64 per lane at Rate 10):
     * <pre>R(n,T) = (1/T) * (64*T)^((n-1)/9)</pre>
     */
    public static double smelterOperationsPerTick(int level, int baselineTicks, double rateTenOperationsPerLane) {
        int t = Math.max(1, baselineTicks);
        int n = clampLevel(level);
        return (1.0 / t) * Math.pow(rateTenOperationsPerLane * t, (n - 1) / 9.0);
    }

    /**
     * Alloy Smelter lane throughput with an optional table of the ten levels for a standard recipe of
     * {@code standardTicks}: a recipe of another duration keeps the proportion of the default curve,
     * {@code table(n) * R(n,T) / R(n,standard)}, never above {@code rateTenOperationsPerLane}. Without a table it is R(n,T).
     */
    public static double smelterOperationsPerTick(int level, int baselineTicks, double rateTenOperationsPerLane, LevelTable standard,
                                                  int standardTicks) {
        double curve = smelterOperationsPerTick(level, baselineTicks, rateTenOperationsPerLane);
        if (standard == null) {
            return curve;
        }
        double reference = smelterOperationsPerTick(level, standardTicks, rateTenOperationsPerLane);
        return Math.min(rateTenOperationsPerLane, standard.at(level) * curve / reference);
    }
}
