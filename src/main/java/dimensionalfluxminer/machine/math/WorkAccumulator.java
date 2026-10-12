package dimensionalfluxminer.machine.math;

/**
 * Fractional work accumulator for rates below and above one operation per tick.
 * <p>
 * Each logical tick adds {@code increment} operations. The number of whole operations that may run is then
 * limited by external bottlenecks (inputs, outputs, matter). Partial progress is kept, but never reaches a
 * full operation while the bottleneck prevents it, so the caller only pays for work that can really happen.
 */
public final class WorkAccumulator {
    /** Progress kept when a bottleneck blocks the next operation. */
    private static final double BLOCKED_PROGRESS_CEILING = 0.999999;

    private double progress;

    public double progress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = Double.isFinite(progress) ? Math.max(0, Math.min(BLOCKED_PROGRESS_CEILING, progress)) : 0;
    }

    public void reset() {
        progress = 0;
    }

    /**
     * Plans one logical tick.
     *
     * @param increment operations gained this tick (rate scaled by available energy)
     * @param limit     whole operations allowed by the other bottlenecks
     * @return the plan; call {@link #commit(Plan)} once the operations were executed
     */
    public Plan plan(double increment, long limit) {
        double safeIncrement = Double.isFinite(increment) ? Math.max(0, increment) : 0;
        if (limit <= 0 || safeIncrement <= 0) {
            // Nothing can run: keep the progress and do not charge anything.
            return new Plan(0, progress, 0, limit <= 0);
        }
        double potential = progress + safeIncrement;
        long whole = (long) Math.floor(potential);
        long ops = Math.min(whole, limit);
        boolean limited = ops < whole;
        double next = limited ? BLOCKED_PROGRESS_CEILING : potential - ops;
        double gained = Math.max(0, ops + next - progress);
        return new Plan(ops, next, gained, limited);
    }

    public void commit(Plan plan) {
        this.progress = plan.nextProgress();
    }

    /** Re-plans when execution stopped after {@code executed} of the planned operations (a mid-run bottleneck). */
    public Plan truncate(Plan plan, long executed) {
        if (executed >= plan.operations()) {
            return plan;
        }
        long done = Math.max(0, executed);
        double next = BLOCKED_PROGRESS_CEILING;
        return new Plan(done, next, Math.max(0, done + next - progress), true);
    }

    /**
     * @param operations   whole operations to execute now
     * @param nextProgress progress carried to the next tick
     * @param workDone     operations worth of progress gained this tick (used to charge energy)
     * @param limited      true when a bottleneck cut the work short
     */
    public record Plan(long operations, double nextProgress, double workDone, boolean limited) {
    }
}
