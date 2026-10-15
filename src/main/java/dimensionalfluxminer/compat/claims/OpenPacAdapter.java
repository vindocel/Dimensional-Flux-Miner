package dimensionalfluxminer.compat.claims;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.corruption.ProtectionService;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Open Parties and Claims (1.21.1) through its public server API, by reflection:
 * {@code OpenPACServerAPI.get(server).getServerClaimsManager().get(dimension, blockPos)} is the claim of a chunk
 * (null = wilderness) and {@code getChunkProtection().hasChunkAccess(UUID, dimension, chunkX, chunkZ)} says whether the
 * Miner's owner may act there, following the claim's own party and ally settings.
 */
final class OpenPacAdapter implements ProtectionService.ClaimAdapter {
    static final String MOD_ID = "openpartiesandclaims";

    private volatile Api api;
    private volatile boolean failed;

    private record Api(Method get, Method claimsManager, Method claimAt, Method protection, Method hasChunkAccess, Method playerId) {
    }

    @Override
    public String name() {
        return "Open Parties and Claims";
    }

    private Api api() {
        Api current = api;
        if (current != null || failed) {
            return current;
        }
        try {
            Class<?> server = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
            Class<?> claims = Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI");
            Class<?> protection = Class.forName("xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI");
            Class<?> claim = Class.forName("xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI");
            current = new Api(server.getMethod("get", MinecraftServer.class), server.getMethod("getServerClaimsManager"),
                    claims.getMethod("get", ResourceLocation.class, BlockPos.class), server.getMethod("getChunkProtection"),
                    protection.getMethod("hasChunkAccess", UUID.class, ResourceLocation.class, int.class, int.class),
                    claim.getMethod("getPlayerId"));
            api = current;
            return current;
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            DimensionalFluxMiner.LOGGER.warn("Open Parties and Claims is installed but its API was not found; see corruption.claimFailurePolicy", e);
            return null;
        }
    }

    @Override
    public boolean chunkBased() {
        return true;
    }

    @Override
    public boolean available(ServerLevel level) {
        Api a = api();
        if (a == null) {
            return false;
        }
        try {
            return a.get().invoke(null, level.getServer()) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private Object claim(Api a, Object instance, ServerLevel level, BlockPos pos) throws ReflectiveOperationException {
        return a.claimAt().invoke(a.claimsManager().invoke(instance), level.dimension().location(), pos);
    }

    @Override
    public ProtectionService.Status check(ServerLevel level, BlockPos pos, UUID owner) {
        Api a = api();
        if (a == null) {
            throw new IllegalStateException("Open Parties and Claims API unavailable");
        }
        try {
            Object instance = a.get().invoke(null, level.getServer());
            if (claim(a, instance, level, pos) == null) {
                return ProtectionService.Status.UNCLAIMED;
            }
            if (owner == null) {
                return ProtectionService.Status.INDETERMINATE;
            }
            Object access = a.hasChunkAccess().invoke(a.protection().invoke(instance), owner, level.dimension().location(),
                    pos.getX() >> 4, pos.getZ() >> 4);
            return Boolean.TRUE.equals(access) ? ProtectionService.Status.AUTHORIZED : ProtectionService.Status.UNAUTHORIZED;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Open Parties and Claims claim check failed", e);
        }
    }

    @Override
    public Optional<String> claimant(ServerLevel level, BlockPos pos) {
        Api a = api();
        if (a == null) {
            throw new IllegalStateException("Open Parties and Claims API unavailable");
        }
        try {
            Object claim = claim(a, a.get().invoke(null, level.getServer()), level, pos);
            if (claim == null) {
                return Optional.empty();
            }
            Object id = a.playerId().invoke(claim);
            if (id == null) {
                throw new IllegalStateException("Open Parties and Claims claim without an owner");
            }
            return Optional.of("openpac:" + id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Open Parties and Claims claim lookup failed", e);
        }
    }
}
