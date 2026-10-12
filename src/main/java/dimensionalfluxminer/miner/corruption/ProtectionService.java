package dimensionalfluxminer.miner.corruption;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Claim protection for environmental corruption. There is no universal claim API, so optional adapters register
 * here. A position claimed by a supported system may only be corrupted when the Miner owner may modify it; any
 * adapter error on a claimed position denies the corruption (no change, no DM).
 */
public final class ProtectionService {
    public enum Result { ALLOW, DENY, UNKNOWN }

    /** Adapter for one claim system; returns UNKNOWN when the position is not claimed by it. */
    public interface ClaimAdapter {
        String name();

        Result check(ServerLevel level, BlockPos pos, UUID owner);
    }

    private static final List<ClaimAdapter> ADAPTERS = new CopyOnWriteArrayList<>();

    private ProtectionService() {
    }

    public static void register(ClaimAdapter adapter) {
        ADAPTERS.add(adapter);
        DimensionalFluxMiner.LOGGER.info("Claim protection adapter registered: {}", adapter.name());
    }

    public static boolean adaptersPresent() {
        return !ADAPTERS.isEmpty();
    }

    public static Result check(ServerLevel level, BlockPos pos, UUID owner) {
        for (ClaimAdapter adapter : ADAPTERS) {
            Result result;
            try {
                result = adapter.check(level, pos, owner);
            } catch (RuntimeException e) {
                DimensionalFluxMiner.LOGGER.warn("Claim adapter {} failed at {}; corruption denied", adapter.name(), pos, e);
                return Result.DENY;
            }
            if (result == Result.DENY) {
                return Result.DENY;
            }
        }
        return Result.ALLOW;
    }
}
