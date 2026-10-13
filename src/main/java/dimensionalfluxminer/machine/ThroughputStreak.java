package dimensionalfluxminer.machine;

/**
 * Counts how many whole seconds in a row a machine kept at least {@code threshold} operations per second. Work is
 * recorded per game time (accelerated cycles included); a second with less work, or a gap with no work at all,
 * restarts the count.
 */
public final class ThroughputStreak {
    private static final int TICKS_PER_SECOND = 20;

    private final double threshold;
    private final int requiredSeconds;
    private long currentSecond = Long.MIN_VALUE;
    private double sum;
    private int streak;

    public ThroughputStreak(double threshold, int requiredSeconds) {
        this.threshold = threshold;
        this.requiredSeconds = requiredSeconds;
    }

    /** Adds work done at {@code gameTime}; true once the required number of full seconds has been reached. */
    public boolean record(long gameTime, double operations) {
        long second = Math.floorDiv(gameTime, TICKS_PER_SECOND);
        if (second != currentSecond) {
            boolean previousCounted = second == currentSecond + 1 && sum >= threshold;
            streak = previousCounted ? streak + 1 : 0;
            currentSecond = second;
            sum = 0;
        }
        sum += operations;
        return streak >= requiredSeconds;
    }

    public int streak() {
        return streak;
    }
}
