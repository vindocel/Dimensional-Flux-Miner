package dimensionalfluxminer.machine.math;

/**
 * FE buffer whose target capacity follows the selected Rate ({@code 10 x effective FE/t}).
 * <p>
 * Lowering the target never deletes stored energy: insertion is simply refused until consumption brings the
 * stored amount back under the new target.
 * <p>
 * The headroom lets the input go above the target by what the externally accelerated cycles of one game time use:
 * the target (the capacity shown) stays the same, but a source that refills once per game time also feeds them.
 * <p>
 * Work is charged in exact fractions of FE ({@link #charge}): the part below one FE is kept as a debt and paid from the
 * next whole FE, so fractional costs (a third of a furnace batch, slow rates) never round up or down over time.
 */
public final class DynamicEnergyBuffer {
    private long stored;
    private long target;
    private long headroom;
    /** FE already charged for work but not yet taken from {@link #stored}; normally below one FE. */
    private double debt;

    public DynamicEnergyBuffer(long target) {
        this.target = Math.max(0, target);
    }

    public long stored() {
        return stored;
    }

    public long target() {
        return target;
    }

    /** Capacity shown to the player and to external handlers. */
    public long capacity() {
        return Math.max(target, stored);
    }

    public void setTarget(long target) {
        this.target = Math.max(0, target);
    }

    public void setStored(long stored) {
        this.stored = Math.max(0, stored);
    }

    /** Extra input allowed above the target for the accelerated cycles of one game time (0 without acceleration). */
    public void setHeadroom(long headroom) {
        this.headroom = Math.max(0, headroom);
    }

    public long headroom() {
        return headroom;
    }

    public long receive(long amount, boolean simulate) {
        long limit = target > Long.MAX_VALUE - headroom ? Long.MAX_VALUE : target + headroom;
        if (amount <= 0 || stored >= limit) {
            return 0;
        }
        long accepted = Math.min(amount, limit - stored);
        if (!simulate) {
            stored += accepted;
        }
        return accepted;
    }

    /** Consumes up to {@code amount} whole FE; returns what was actually consumed. */
    public long consume(long amount) {
        if (amount <= 0) {
            return 0;
        }
        long used = Math.min(amount, stored);
        stored -= used;
        return used;
    }

    /** FE that work may still use now (stored minus the fraction already charged). */
    public double available() {
        return Math.max(0.0, stored - debt);
    }

    /**
     * Charges {@code fe} FE of work performed. Whole FE leave the buffer at once; the fraction below one FE stays as a
     * debt paid by the next charge. Callers never plan more work than {@link #available()} pays for.
     */
    public void charge(double fe) {
        if (!(fe > 0) || !Double.isFinite(fe)) {
            return;
        }
        debt += fe;
        long whole = debt >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(debt);
        if (whole > 0) {
            debt -= consume(whole);
        }
    }

    public double debt() {
        return debt;
    }

    /** Restores a saved debt (invalid values become zero). */
    public void setDebt(double debt) {
        this.debt = Double.isFinite(debt) && debt > 0 ? debt : 0.0;
    }
}
