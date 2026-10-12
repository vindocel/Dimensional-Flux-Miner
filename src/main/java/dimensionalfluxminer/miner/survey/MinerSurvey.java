package dimensionalfluxminer.miner.survey;

import dimensionalfluxminer.miner.corruption.CorruptionService;
import java.util.ArrayDeque;
import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Progressive Dimensional Survey of one Basic-Core Miner. Columns are inspected from the center outward,
 * only in loaded chunks; drainable columns are cached and revalidated right before corruption.
 */
public final class MinerSurvey {
    public static final int BOOTSTRAP_COLUMNS = 256;
    /** Unloaded columns are cheap to skip, but skipping is still bounded per call. */
    private static final int SKIP_FACTOR = 8;

    private final BlockPos center;
    private final ColumnOrder order = ColumnOrder.get();
    private final BitSet seen = new BitSet();
    private final BitSet inspectedThisPass = new BitSet();
    private final ArrayDeque<Long> candidates = new ArrayDeque<>();
    private int pointer;
    private int seenCount;
    private boolean candidateFoundThisPass;

    public MinerSurvey(BlockPos center) {
        this.center = center;
    }

    public BlockPos center() {
        return center;
    }

    /** Inspects up to {@code budget} loaded columns; returns the number of columns inspected. */
    public int step(ServerLevel level, int budget) {
        int inspected = 0;
        int steps = 0;
        int maxSteps = budget * SKIP_FACTOR;
        while (inspected < budget && steps < maxSteps) {
            if (pointer >= order.size()) {
                pointer = 0;
                if (!candidateFoundThisPass && candidates.isEmpty()) {
                    // Nothing drainable known: start a fresh pass so changed terrain/chunks are noticed.
                    inspectedThisPass.clear();
                }
                candidateFoundThisPass = false;
            }
            int index = pointer++;
            steps++;
            if (inspectedThisPass.get(index)) {
                continue;
            }
            int x = center.getX() + order.dx(index);
            int z = center.getZ() + order.dz(index);
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            inspectedThisPass.set(index);
            if (!seen.get(index)) {
                seen.set(index);
                seenCount++;
            }
            inspected++;
            if (CorruptionService.plan(level, x, z).drainable()) {
                candidates.add(((long) x << 32) | (z & 0xFFFFFFFFL));
                candidateFoundThisPass = true;
            }
        }
        return inspected;
    }

    /** Bootstrap: at least 256 inspected columns and at least one drainable target. */
    public boolean ready() {
        return seenCount >= Math.min(BOOTSTRAP_COLUMNS, order.size()) && !candidates.isEmpty();
    }

    public boolean hasCandidates() {
        return !candidates.isEmpty();
    }

    public int knownDrainable() {
        return candidates.size();
    }

    public double coverage() {
        return seenCount / (double) order.size();
    }

    public int seenColumns() {
        return seenCount;
    }

    /** Takes the next cached target; the caller revalidates it before corrupting. */
    public long pollCandidate() {
        Long next = candidates.poll();
        return next == null ? Long.MIN_VALUE : next;
    }
}
