package dimensionalfluxminer.miner.profile;

import java.util.Locale;

/**
 * How the void of a dimension shrinks as more ore families are born there, from {@code emptyShare} with one family to
 * none from {@code minFamilies} families on.
 */
public enum VoidCurve {
    /**
     * The chance of finding something grows by the same factor with every family:
     * <pre>found(n) = (1 - s) * (1 / (1 - s))^((n - 1) / (min - 1)),  void(n) = 1 - found(n)</pre>
     */
    GEOMETRIC,
    /** The void shrinks by the same amount with every family. */
    LINEAR,
    /** The full void below the minimum, none from it on. */
    STEP;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static VoidCurve byName(String name) {
        for (VoidCurve curve : values()) {
            if (curve.serializedName().equals(name)) {
                return curve;
            }
        }
        throw new IllegalArgumentException("Unknown void curve: " + name);
    }

    /** Share of the attempts that find nothing in a dimension of {@code families} families. */
    public double voidShare(double emptyShare, int minFamilies, int families) {
        double s = Math.max(0, Math.min(0.9999, emptyShare));
        if (families <= 0 || families >= minFamilies || s <= 0) {
            return 0.0;
        }
        if (this == STEP || minFamilies <= 1) {
            return s;
        }
        double t = (families - 1) / (double) (minFamilies - 1);
        return switch (this) {
            case LINEAR -> s * (1 - t);
            case GEOMETRIC -> 1 - (1 - s) * Math.pow(1 / (1 - s), t);
            case STEP -> s;
        };
    }
}
