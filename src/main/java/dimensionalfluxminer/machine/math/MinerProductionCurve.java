package dimensionalfluxminer.machine.math;

/**
 * Production curve of the Dimensional Flux Miner: every Flux Rate level n has a production R(n) and a power P(n).
 * <pre>
 * R(n) = Rmin * (Rmax / Rmin)^((n - 1) / 9)
 * B(n) = Pmin * (Pmax / Pmin)^((n - 1) / 9)
 * P(n) = B(n)                for n = 1..5
 *        2 * B(n)            for n = 6..9 (when that stays under Pmax; otherwise sqrt(P(n - 1) * Pmax))
 *        Pmax                for n = 10
 * </pre>
 * The high levels cost double, so running fast is paid for. Between two levels, production and power are
 * interpolated on a log-log scale; the inverse is used to charge FE for the work that was actually performed.
 */
public final class MinerProductionCurve {
    /** The Miner can never produce more than this per normal logical cycle, whatever the configuration says. */
    public static final double HARD_CAP_PER_CYCLE = 64.0;
    /** Levels from this one up to the level below the maximum cost double the geometric scale. */
    public static final int DOUBLED_FROM_LEVEL = 6;

    private final double minPower;
    private final double maxPower;
    private final double minRate;
    private final double maxRate;
    /** Power and production of each level; index 0 is level 1. */
    private final double[] power = new double[RateScale.MAX_LEVEL];
    private final double[] rate = new double[RateScale.MAX_LEVEL];
    /** Log-log slope between level i + 1 and level i + 2. */
    private final double[] slope = new double[RateScale.MAX_LEVEL - 1];

    public MinerProductionCurve(double minPower, double maxPower, double minRate, double maxRate) {
        if (!(minPower > 0) || !(maxPower > minPower)) {
            throw new IllegalArgumentException("Miner power limits require 0 < min < max");
        }
        double cappedMax = Math.min(maxRate, HARD_CAP_PER_CYCLE);
        if (!(minRate > 0) || !(cappedMax > minRate)) {
            throw new IllegalArgumentException("Miner production limits require 0 < min < max <= 64");
        }
        this.minPower = minPower;
        this.maxPower = maxPower;
        this.minRate = minRate;
        this.maxRate = cappedMax;
        for (int level = RateScale.MIN_LEVEL; level <= RateScale.MAX_LEVEL; level++) {
            int i = level - RateScale.MIN_LEVEL;
            rate[i] = RateScale.value(level, minRate, cappedMax);
            double base = RateScale.value(level, minPower, maxPower);
            if (level == RateScale.MAX_LEVEL) {
                power[i] = maxPower;
            } else if (level >= DOUBLED_FROM_LEVEL) {
                // Strictly between the previous level and the maximum, even with narrow power limits.
                power[i] = 2 * base < maxPower ? 2 * base : Math.sqrt(power[i - 1] * maxPower);
            } else {
                power[i] = base;
            }
        }
        for (int i = 0; i < slope.length; i++) {
            slope[i] = Math.log(rate[i + 1] / rate[i]) / Math.log(power[i + 1] / power[i]);
        }
    }

    public double minPower() {
        return minPower;
    }

    public double maxPower() {
        return maxPower;
    }

    public double minRate() {
        return minRate;
    }

    public double maxRate() {
        return maxRate;
    }

    /** Production rate (resources per logical tick) sustained by {@code power} FE/t. */
    public double rateForPower(double power) {
        if (power < minPower) {
            return 0.0;
        }
        if (power >= maxPower) {
            return maxRate;
        }
        int i = 0;
        while (i < slope.length - 1 && power >= this.power[i + 1]) {
            i++;
        }
        return Math.min(maxRate, rate[i] * Math.pow(power / this.power[i], slope[i]));
    }

    /** FE/t required to perform {@code rate} resources per logical tick. */
    public double powerForRate(double rate) {
        if (rate <= 0) {
            return 0.0;
        }
        int i = 0;
        while (i < slope.length - 1 && rate >= this.rate[i + 1]) {
            i++;
        }
        return power[i] * Math.pow(rate / this.rate[i], 1.0 / slope[i]);
    }

    /** Power limit of a Flux Rate level. */
    public double levelPower(int level) {
        return power[RateScale.clampLevel(level) - RateScale.MIN_LEVEL];
    }

    /** Nominal production of a Flux Rate level at full power. */
    public double levelRate(int level) {
        return rate[RateScale.clampLevel(level) - RateScale.MIN_LEVEL];
    }
}
