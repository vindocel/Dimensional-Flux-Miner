package dimensionalfluxminer.machine;

/**
 * Distinguishes the normal ticker call of a server tick from additional calls made in the same
 * {@code gameTime} by external tick accelerators. The number of extra calls is never assumed: each one is
 * simply reported as an extra logical cycle.
 */
public final class LogicalTickCounter {
    private long lastGameTime = Long.MIN_VALUE;
    private int callsThisTick;

    /** @return true for the first call of this game time, false for every additional call */
    public boolean register(long gameTime) {
        if (gameTime != lastGameTime) {
            lastGameTime = gameTime;
            callsThisTick = 1;
            return true;
        }
        callsThisTick++;
        return false;
    }

    public int callsThisTick() {
        return callsThisTick;
    }
}
