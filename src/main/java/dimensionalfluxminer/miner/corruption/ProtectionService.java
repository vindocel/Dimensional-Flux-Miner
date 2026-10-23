package dimensionalfluxminer.miner.corruption;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmTags;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Territorial protection of the environmental corruption. There is no universal claim API, so optional adapters
 * register here ({@link ClaimAdapters}); server rules (corruption on/off, blocked dimensions, spawn protection,
 * protected regions) come from {@link DfmConfig}.
 * <ul>
 *     <li>unclaimed position: allowed (unless a server rule forbids it);</li>
 *     <li>claimed and the Miner's owner may build there: allowed;</li>
 *     <li>claimed by someone else: denied;</li>
 *     <li>claimed but the check failed or could not decide (no owner, adapter error): denied.</li>
 * </ul>
 * A Miner's owner is always the server-side UUID of the player who placed or formed it, never data from a client.
 */
public final class ProtectionService {
    /** Outcome of one position. Only {@link #UNCLAIMED} and {@link #AUTHORIZED} let the corruption through. */
    public enum Status {
        UNCLAIMED, AUTHORIZED, UNAUTHORIZED, INDETERMINATE;

        public boolean allows() {
            return this == UNCLAIMED || this == AUTHORIZED;
        }
    }

    /** One claim system. */
    public interface ClaimAdapter {
        String name();

        /** False when the claim system is installed but cannot be queried (its API changed or is not ready). */
        boolean available(ServerLevel level);

        /**
         * {@link Status#UNCLAIMED} when this system has no claim there; never {@link Status#UNCLAIMED} for a claimed
         * position. {@code owner} may be null (unknown owner). Exceptions count as {@link Status#INDETERMINATE}.
         */
        Status check(ServerLevel level, BlockPos pos, UUID owner);

        /** Stable identity of the claim covering {@code pos} (team or player), empty when unclaimed. */
        Optional<String> claimant(ServerLevel level, BlockPos pos);

        /** True when claims cover whole chunks (every position of a chunk has the same answer). */
        default boolean chunkBased() {
            return false;
        }
    }

    private static final List<ClaimAdapter> ADAPTERS = new CopyOnWriteArrayList<>();
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ProtectionService() {
    }

    public static void register(ClaimAdapter adapter) {
        ADAPTERS.add(adapter);
        DimensionalFluxMiner.LOGGER.info("Claim protection adapter registered: {}", adapter.name());
    }

    public static void unregister(ClaimAdapter adapter) {
        ADAPTERS.remove(adapter);
    }

    public static boolean adaptersPresent() {
        return !ADAPTERS.isEmpty();
    }

    /**
     * True when the server lets the corruption happen in this dimension at all: not turned off, not in the blocked
     * dimensions of the configuration and not of a dimension type in {@code #dimensionalfluxminer:corruption_blocked}.
     */
    public static boolean corruptionAllowed(Level level) {
        return DfmConfig.corruptionEnabled() && !DfmConfig.corruptionBlockedDimensions().contains(level.dimension().identifier())
                && !level.dimensionTypeRegistration().is(DfmTags.DimensionTypes.CORRUPTION_BLOCKED);
    }

    /** Spawn protection and the administrative regions of the configuration. */
    public static boolean inProtectedRegion(ServerLevel level, BlockPos pos) {
        if (DfmConfig.corruptionRespectsSpawnProtection() && level.dimension() == Level.OVERWORLD) {
            int radius = level.getServer() instanceof net.minecraft.server.dedicated.DedicatedServer dedicated ? dedicated.spawnProtectionRadius() : 16;
            BlockPos spawn = level.getRespawnData().pos();
            if (radius > 0 && Math.max(Math.abs(pos.getX() - spawn.getX()), Math.abs(pos.getZ() - spawn.getZ())) <= radius) {
                return true;
            }
        }
        for (DfmConfig.Region region : DfmConfig.corruptionProtectedRegions()) {
            if (region.contains(level.dimension().identifier(), pos.getX(), pos.getZ())) {
                return true;
            }
        }
        return false;
    }

    /** Status of one position for a Miner of {@code owner} (null = unknown owner). */
    public static Status status(ServerLevel level, BlockPos pos, UUID owner) {
        if (!corruptionAllowed(level) || inProtectedRegion(level, pos)) {
            return Status.UNAUTHORIZED;
        }
        Status result = Status.UNCLAIMED;
        for (ClaimAdapter adapter : ADAPTERS) {
            Status status;
            try {
                if (!adapter.available(level)) {
                    if (DfmConfig.claimFailureDenies()) {
                        warnOnce(adapter.name(), "Claim system {} is installed but cannot be queried; corruption denied everywhere "
                                + "(corruption.claimFailurePolicy = deny)");
                        return Status.INDETERMINATE;
                    }
                    warnOnce(adapter.name(), "Claim system {} is installed but cannot be queried; it is ignored "
                            + "(corruption.claimFailurePolicy = ignore)");
                    continue;
                }
                status = adapter.check(level, pos, owner);
            } catch (RuntimeException | LinkageError e) {
                DimensionalFluxMiner.LOGGER.warn("Claim adapter {} failed at {}; corruption denied", adapter.name(), pos, e);
                return Status.INDETERMINATE;
            }
            if (status == null || status == Status.INDETERMINATE || status == Status.UNAUTHORIZED) {
                return status == null ? Status.INDETERMINATE : status;
            }
            if (status == Status.AUTHORIZED) {
                result = Status.AUTHORIZED;
            }
        }
        return result;
    }

    /**
     * True when every position may be corrupted: one column is authorized or denied as a whole. Same answer as
     * {@link #status} for each position, but a chunk-based claim system is asked once per chunk.
     */
    public static boolean allowsAll(ServerLevel level, Collection<BlockPos> positions, UUID owner) {
        if (positions.isEmpty()) {
            return true;
        }
        if (!corruptionAllowed(level)) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (inProtectedRegion(level, pos)) {
                return false;
            }
        }
        for (ClaimAdapter adapter : ADAPTERS) {
            long lastChunk = Long.MIN_VALUE;
            try {
                if (!adapter.available(level)) {
                    if (DfmConfig.claimFailureDenies()) {
                        warnOnce(adapter.name(), "Claim system {} is installed but cannot be queried; corruption denied everywhere "
                                + "(corruption.claimFailurePolicy = deny)");
                        return false;
                    }
                    warnOnce(adapter.name(), "Claim system {} is installed but cannot be queried; it is ignored "
                            + "(corruption.claimFailurePolicy = ignore)");
                    continue;
                }
                for (BlockPos pos : positions) {
                    if (adapter.chunkBased()) {
                        long chunk = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
                        if (chunk == lastChunk) {
                            continue;
                        }
                        lastChunk = chunk;
                    }
                    Status status = adapter.check(level, pos, owner);
                    if (status == null || status == Status.INDETERMINATE || status == Status.UNAUTHORIZED) {
                        return false;
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                DimensionalFluxMiner.LOGGER.warn("Claim adapter {} failed; corruption denied", adapter.name(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * Whether Corrupted Water at {@code from} may act on {@code to} (flow there or corrupt the water there) on a later
     * tick, when its Miner is long gone: the water never leaves the claim it is in. Allowed when {@code to} is
     * unclaimed, or claimed by the same claimant as {@code from}; denied across a claim border, into a protected region,
     * where the corruption is off, and whenever a check fails.
     */
    public static boolean waterMayCross(ServerLevel level, BlockPos from, BlockPos to) {
        if (!corruptionAllowed(level)) {
            return false;
        }
        if (inProtectedRegion(level, to) && !inProtectedRegion(level, from)) {
            return false;
        }
        for (ClaimAdapter adapter : ADAPTERS) {
            try {
                if (!adapter.available(level)) {
                    if (DfmConfig.claimFailureDenies()) {
                        return false;
                    }
                    continue;
                }
                Optional<String> target = adapter.claimant(level, to);
                if (target == null) {
                    return false;
                }
                if (target.isPresent() && !target.equals(adapter.claimant(level, from))) {
                    return false;
                }
            } catch (RuntimeException | LinkageError e) {
                warnOnce(adapter.name() + ":water", "Claim adapter {} failed while checking Corrupted Water; it does not spread");
                return false;
            }
        }
        return true;
    }

    private static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            DimensionalFluxMiner.LOGGER.warn(message, key);
        }
    }
}
