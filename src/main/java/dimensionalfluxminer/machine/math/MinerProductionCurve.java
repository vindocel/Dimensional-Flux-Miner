package dimensionalfluxminer.machine.math;

/**
 * Production curve of the Dimensional Flux Miner.
 * <pre>
 * R(P) = Rmin * (P / Pmin)^k,   k = ln(Rmax / Rmin) / ln(Pmax / Pmin)
 * P(R) = Pmin * (R / Rmin)^(1/k)
 * </pre>
 * The inverse is used to charge FE for the work that was actually performed.
 */
public final class MinerProductionCurve {
    /** The Miner can never produce more than this per normal logical cycle, whatever the configuration says. */
    public static final double HARD_CAP_PER_CYCLE = 64.0;

    private final double minPower;
    private final double maxPower;
    private final double minRate;
    private final double maxRate;
    private final double k;

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
        this.k = Math.log(this.maxRate / minRate) / Math.log(maxPower / minPower);
    }

    public double k() {
        return k;
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
        return Math.min(maxRate, minRate * Math.pow(power / minPower, k));
    }

    /** FE/t required to perform {@code rate} resources per logical tick. */
    public double powerForRate(double rate) {
        if (rate <= 0) {
            return 0.0;
        }
        return minPower * Math.pow(rate / minRate, 1.0 / k);
    }

    /** Power limit of a Flux Rate level. */
    public double levelPower(int level) {
        return RateScale.value(level, minPower, maxPower);
    }

    /** Nominal production of a Flux Rate level at full power. */
    public double levelRate(int level) {
        return rateForPower(levelPower(level));
    }
}
