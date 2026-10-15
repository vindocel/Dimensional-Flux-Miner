package dimensionalfluxminer.miner;

/**
 * The Miner's Dimensional Matter buffer, in fixed-point units (1 DM = 10 units), between 0 and
 * {@link MatterValues#BUFFER_UNITS} (5 000 DM). Every mining attempt costs {@link MatterValues#ROLL_COST_UNITS}.
 */
final class MatterBuffer {
    private long units;

    long units() {
        return units;
    }

    /** Sets the buffer, clamped to 0..5 000 DM (saves, tests, invalidation). */
    void set(long units) {
        this.units = Math.max(0, Math.min(MatterValues.BUFFER_UNITS, units));
    }

    long room() {
        return MatterValues.BUFFER_UNITS - units;
    }

    boolean canPayAttempt() {
        return units >= MatterValues.ROLL_COST_UNITS;
    }

    void payAttempt() {
        units -= MatterValues.ROLL_COST_UNITS;
    }

    /** Adds matter that the caller already checked fits (substrate, drained columns). */
    void add(long amount) {
        units += amount;
    }

    /** Adds matter, dropping what exceeds the buffer (filter recovery). */
    void addCapped(long amount) {
        units = Math.min(MatterValues.BUFFER_UNITS, units + amount);
    }
}
