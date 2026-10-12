package dimensionalfluxminer.machine;

/**
 * Counts the extra (externally accelerated) logical cycles of each game time, so the energy input can also feed them.
 * The rate of the current game time is used when it is already higher, otherwise the one of the previous game time;
 * one game time without extra cycles brings it back to zero.
 */
public final class AccelerationDemand {
    private long gameTime = Long.MIN_VALUE;
    private int current;
    private int previous;

    /** One extra logical cycle in {@code gameTime}. */
    public void recordExtra(long gameTime) {
        roll(gameTime);
        current++;
    }

    /** Extra logical cycles per game time, as seen in {@code gameTime}. */
    public int extraPerTick(long gameTime) {
        roll(gameTime);
        return Math.max(current, previous);
    }

    private void roll(long time) {
        if (time != gameTime) {
            previous = time == gameTime + 1 ? current : 0;
            current = 0;
            gameTime = time;
        }
    }
}
