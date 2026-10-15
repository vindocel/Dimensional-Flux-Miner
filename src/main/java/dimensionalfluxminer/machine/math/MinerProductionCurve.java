package dimensionalfluxminer.machine.math;

/**
 * Production curve of the Dimensional Flux Miner: every Flux Rate level n has a production R(n) and a cost per attempt
 * E(n).
 * <pre>
 * R(n) = Rmin * (Rmax / Rmin)^((n - 1) / 9)     or a configured table of the ten levels
 * E(n) = E                                     or P(n) / R(n) with a configured table of FE/t P(n)
 * P(n) = R(n) * E(n)
 * </pre>
 * By default the cost of an attempt is the same at every level. The FE charged is always the work really performed
 * times the cost of the level it was performed at.
 */
public final class MinerProductionCurve {
    /** The Miner can never produce more than this per normal logical cycle, whatever the configuration says. */
    public static final double HARD_CAP_PER_CYCLE = 64.0;

    /** Production of each level; index 0 is level 1. */
    private final double[] rate = new double[RateScale.MAX_LEVEL];
    /** FE of one attempt at each level. */
    private final double[] cost = new double[RateScale.MAX_LEVEL];

    /** The default curve: geometric production and the same cost per attempt at every level. */
    public MinerProductionCurve(double minRate, double maxRate, double energyPerAttempt) {
        this(geometric(minRate, maxRate), null, energyPerAttempt);
    }

    /**
     * @param rates            production of the ten levels (each capped at {@link #HARD_CAP_PER_CYCLE})
     * @param power            FE/t of the ten levels, or null for {@code energyPerAttempt} at every level
     * @param energyPerAttempt FE of one attempt when {@code power} is null
     */
    public MinerProductionCurve(LevelTable rates, LevelTable power, double energyPerAttempt) {
        if (power == null && (!(energyPerAttempt > 0) || !Double.isFinite(energyPerAttempt))) {
            throw new IllegalArgumentException("Miner energy per attempt must be positive");
        }
        for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
            int i = level - RateScale.MIN_LEVEL;
            rate[i] = Math.min(rates.at(level), HARD_CAP_PER_CYCLE);
            cost[i] = power == null ? energyPerAttempt : power.at(level) / rate[i];
        }
    }

    private static LevelTable geometric(double minRate, double maxRate) {
        double cappedMax = Math.min(maxRate, HARD_CAP_PER_CYCLE);
        if (!(minRate > 0) || !(cappedMax > minRate)) {
            throw new IllegalArgumentException("Miner production limits require 0 < min < max <= 64");
        }
        return LevelTable.geometric(minRate, cappedMax);
    }

    /** FE of one attempt at {@code level}. */
    public double energyPerAttempt(int level) {
        return cost[RateScale.clampLevel(level) - RateScale.MIN_LEVEL];
    }

    /** The lowest FE/t of the ten levels. */
    public double minPower() {
        double min = Double.MAX_VALUE;
        for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
            min = Math.min(min, levelPower(level));
        }
        return min;
    }

    /** Attempts per logical tick that {@code power} FE/t pays for at {@code level} (never above the level's production). */
    public double rateForPower(int level, double power) {
        if (!(power > 0)) {
            return 0.0;
        }
        return Math.min(levelRate(level), power / energyPerAttempt(level));
    }

    /** FE/t required to perform {@code rate} attempts per logical tick at {@code level}. */
    public double powerForRate(int level, double rate) {
        return RateScale.power(rate, energyPerAttempt(level));
    }

    /** FE/t of a Flux Rate level at full speed. */
    public double levelPower(int level) {
        return powerForRate(level, levelRate(level));
    }

    /** Nominal production of a Flux Rate level at full power. */
    public double levelRate(int level) {
        return rate[RateScale.clampLevel(level) - RateScale.MIN_LEVEL];
    }
}
