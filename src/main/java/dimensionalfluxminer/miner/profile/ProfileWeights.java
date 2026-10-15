package dimensionalfluxminer.miner.profile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Weight transform of the Resource Profile:
 * <pre>
 * rawScore    = sum(veinSize * placementsPerChunk * probability * biomeCoverage)
 * weightBasis = max(rawScore, epsilon) ^ 0.75
 * </pre>
 * normalized to integer weights summing to about 10 000, with a minimum of 1 per family. The exponent keeps every
 * family present while giving rare ores a moderate boost (a square root boosted endgame ores up to 6 to 15 times).
 */
public final class ProfileWeights {
    public static final double EPSILON = 1.0E-6;
    public static final int TARGET_TOTAL = 10_000;
    /** Exponent applied to the raw generation scores: 1 = linear, 0.5 = square root. */
    public static final double EXPONENT = 0.75;

    private ProfileWeights() {
    }

    public static <K> Map<K, Integer> normalize(Map<K, Double> rawScores) {
        Map<K, Integer> out = new LinkedHashMap<>();
        double sum = 0;
        Map<K, Double> basis = new LinkedHashMap<>();
        for (Map.Entry<K, Double> e : rawScores.entrySet()) {
            double b = Math.pow(Math.max(e.getValue(), EPSILON), EXPONENT);
            basis.put(e.getKey(), b);
            sum += b;
        }
        for (Map.Entry<K, Double> e : basis.entrySet()) {
            int w = sum > 0 ? (int) Math.round(e.getValue() / sum * TARGET_TOTAL) : 1;
            out.put(e.getKey(), Math.max(1, w));
        }
        return out;
    }

    /** Empty weight of a profile whose void takes {@code emptyShare} (0..1) of the attempts. */
    public static int emptyWeight(double emptyShare) {
        return (int) Math.round(Math.max(0, Math.min(1, emptyShare)) * TARGET_TOTAL);
    }

    /**
     * Scales the family weights into the part the void leaves ({@code 1 - emptyShare}), keeping their proportions and a
     * minimum of 1 each.
     */
    public static <K> Map<K, Integer> scaleForEmpty(Map<K, Integer> weights, double emptyShare) {
        long total = 0;
        for (int w : weights.values()) {
            total += w;
        }
        int room = TARGET_TOTAL - emptyWeight(emptyShare);
        Map<K, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<K, Integer> e : weights.entrySet()) {
            int w = total > 0 ? (int) Math.round(e.getValue() / (double) total * room) : 0;
            out.put(e.getKey(), Math.max(1, w));
        }
        return out;
    }

    /**
     * Applies datapack share overrides: each override is the fraction (0..1] of the final profile reserved for
     * that family; the remaining families keep their relative weights in the rest.
     */
    public static <K> Map<K, Integer> applyShares(Map<K, Integer> weights, Map<K, Double> shares) {
        if (shares.isEmpty()) {
            return weights;
        }
        double reserved = 0;
        for (double s : shares.values()) {
            reserved += Math.max(0, Math.min(1, s));
        }
        reserved = Math.min(1.0, reserved);
        double rest = 1.0 - reserved;
        long freeTotal = 0;
        for (Map.Entry<K, Integer> e : weights.entrySet()) {
            if (!shares.containsKey(e.getKey())) {
                freeTotal += e.getValue();
            }
        }
        Map<K, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<K, Integer> e : weights.entrySet()) {
            if (!shares.containsKey(e.getKey())) {
                int w = freeTotal > 0 ? (int) Math.round(e.getValue() / (double) freeTotal * rest * TARGET_TOTAL) : 0;
                out.put(e.getKey(), Math.max(1, w));
            }
        }
        for (Map.Entry<K, Double> e : shares.entrySet()) {
            out.put(e.getKey(), Math.max(1, (int) Math.round(Math.max(0, Math.min(1, e.getValue())) * TARGET_TOTAL)));
        }
        return out;
    }
}
