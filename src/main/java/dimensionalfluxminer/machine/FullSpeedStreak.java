package dimensionalfluxminer.machine;

/**
 * Counts how many game ticks in a row a machine ran at full production ("Redline"). A tick below full production,
 * or a gap with no tick at all (chunk unloaded), restarts the count; the goal is reported once per streak.
 */
public final class FullSpeedStreak {
    /** Ten seconds. */
    public static final int REQUIRED_TICKS = 200;

    private final int requiredTicks;
    private long lastTick = Long.MIN_VALUE;
    private int ticks;

    public FullSpeedStreak(int requiredTicks) {
        this.requiredTicks = requiredTicks;
    }

    /** Records one game tick; true only on the tick the streak reaches the required length. */
    public boolean record(long gameTime, boolean fullSpeed) {
        if (!fullSpeed || gameTime != lastTick + 1) {
            ticks = 0;
        }
        lastTick = gameTime;
        if (!fullSpeed || ticks >= requiredTicks) {
            return false;
        }
        return ++ticks == requiredTicks;
    }
}
