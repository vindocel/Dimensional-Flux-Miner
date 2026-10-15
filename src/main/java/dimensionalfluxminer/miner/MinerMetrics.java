package dimensionalfluxminer.miner;

/**
 * What the Miner shows about its own pace, per real tick (accelerated cycles included): the attempts it could make and
 * the ones it made, the smoothed load that drives the rotation, and the strongest limit met since the previous real
 * tick. Pure bookkeeping: it never changes the work.
 */
final class MinerMetrics {
    /** EMA factor of the load (about 20 ticks). */
    private static final double LOAD_EMA_ALPHA = 0.0952380952;

    private double workThisRealTick;
    private double smoothedLoad;
    private double potentialRate;
    private double actualRate;
    private MinerStatus.LimitingFactor limitSinceRealTick = MinerStatus.LimitingFactor.NONE;
    private MinerStatus.LimitingFactor shownLimit = MinerStatus.LimitingFactor.NONE;

    /** Work (attempts) of one cycle. */
    void addWork(double attempts) {
        workThisRealTick += attempts;
    }

    /** Keeps the strongest limit met since the previous real tick (enum order is the priority). */
    void noteLimit(MinerStatus.LimitingFactor factor) {
        if (factor.ordinal() < limitSinceRealTick.ordinal()) {
            limitSinceRealTick = factor;
        }
    }

    /**
     * Closes a real tick.
     *
     * @param nominal  attempts per cycle of the selected Flux Rate
     * @param cycles   cycles of this game time (1 + accelerated ones)
     * @param formed   whether the machine is formed
     * @param working  whether it worked this tick or the previous one (otherwise the load falls to 0)
     */
    void closeRealTick(double nominal, int cycles, boolean formed, boolean working) {
        double load = nominal > 0 ? Math.max(0, Math.min(1, workThisRealTick / nominal)) : 0;
        if (!working) {
            load = 0;
        }
        potentialRate = formed ? nominal * cycles : 0;
        actualRate = workThisRealTick;
        workThisRealTick = 0;
        shownLimit = limitSinceRealTick;
        limitSinceRealTick = MinerStatus.LimitingFactor.NONE;
        smoothedLoad += LOAD_EMA_ALPHA * (load - smoothedLoad);
    }

    double smoothedLoad() {
        return smoothedLoad;
    }

    double potentialRate() {
        return potentialRate;
    }

    double actualRate() {
        return actualRate;
    }

    MinerStatus.LimitingFactor shownLimit() {
        return shownLimit;
    }
}
