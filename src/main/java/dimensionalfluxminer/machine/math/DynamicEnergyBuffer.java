package dimensionalfluxminer.machine.math;

/**
 * FE buffer whose target capacity follows the selected Rate ({@code 10 x effective FE/t}).
 * <p>
 * Lowering the target never deletes stored energy: insertion is simply refused until consumption brings the
 * stored amount back under the new target.
 * <p>
 * The headroom lets the input go above the target by what the externally accelerated cycles of one game time use:
 * the target (the capacity shown) stays the same, but a source that refills once per game time also feeds them.
 */
public final class DynamicEnergyBuffer {
    private long stored;
    private long target;
    private long headroom;

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
        long limit = target + headroom;
        if (amount <= 0 || stored >= limit) {
            return 0;
        }
        long accepted = Math.min(amount, limit - stored);
        if (!simulate) {
            stored += accepted;
        }
        return accepted;
    }

    /** Consumes up to {@code amount} FE for machine work; returns what was actually consumed. */
    public long consume(long amount) {
        if (amount <= 0) {
            return 0;
        }
        long used = Math.min(amount, stored);
        stored -= used;
        return used;
    }
}
