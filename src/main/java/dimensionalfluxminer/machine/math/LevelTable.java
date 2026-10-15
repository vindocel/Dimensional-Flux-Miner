package dimensionalfluxminer.machine.math;

import java.util.Arrays;

/** One value per speed level (1 to 10), immutable: a configured table or the geometric curve between two ends. */
public final class LevelTable {
    private final double[] values;

    /** @param values exactly ten values, index 0 being level 1 */
    public LevelTable(double[] values) {
        if (values.length != RateScale.MAX_LEVEL - RateScale.MIN_LEVEL + 1) {
            throw new IllegalArgumentException("A level table needs one value per level, got " + values.length);
        }
        for (double value : values) {
            if (!(value > 0) || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Level table values must be positive, got " + value);
            }
        }
        this.values = values.clone();
    }

    /** The geometric curve {@link RateScale#value} between {@code min} (level 1) and {@code max} (level 10). */
    public static LevelTable geometric(double min, double max) {
        double[] values = new double[RateScale.MAX_LEVEL - RateScale.MIN_LEVEL + 1];
        for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
            values[level - RateScale.MIN_LEVEL] = RateScale.value(level, min, max);
        }
        return new LevelTable(values);
    }

    /** Value of {@code level}, clamped to 1..10. */
    public double at(int level) {
        return values[RateScale.clampLevel(level) - RateScale.MIN_LEVEL];
    }

    /** The lowest value of the ten levels. */
    public double min() {
        return Arrays.stream(values).min().orElseThrow();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LevelTable other && Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
