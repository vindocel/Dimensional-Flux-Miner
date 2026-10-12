package dimensionalfluxminer.smelter;

/**
 * Smelter experience kept in fixed point (1 XP = 1000 milli). The liquid view exposes whole millibuckets only:
 * 1 XP = 20 mB, so 1 mB = 50 milli-XP; fractions below 1 mB stay stored.
 */
public final class StoredExperience {
    public static final long MILLI_PER_XP = 1000;
    public static final long MILLI_PER_MB = 50;

    private long milli;

    public long milli() {
        return milli;
    }

    public void setMilli(long milli) {
        this.milli = Math.max(0, milli);
    }

    public void add(double xp) {
        if (xp > 0 && Double.isFinite(xp)) {
            milli += Math.round(xp * MILLI_PER_XP);
        }
    }

    public double xp() {
        return milli / (double) MILLI_PER_XP;
    }

    public long availableMillibuckets() {
        return milli / MILLI_PER_MB;
    }

    /** Drains up to {@code mb} whole millibuckets; returns the amount drained. */
    public long drain(long mb, boolean simulate) {
        long drained = Math.max(0, Math.min(mb, availableMillibuckets()));
        if (!simulate) {
            milli -= drained * MILLI_PER_MB;
        }
        return drained;
    }

    /** Whole XP points for an orb payout, rounding the fraction probabilistically like a vanilla furnace. */
    public static int payout(long milli, double random) {
        long whole = milli / MILLI_PER_XP;
        double fraction = (milli % MILLI_PER_XP) / (double) MILLI_PER_XP;
        long total = whole + (random < fraction ? 1 : 0);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
}
