package dimensionalfluxminer.compat.claims;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.corruption.ProtectionService;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * FTB Chunks (1.21.1, 2101.x) through its public API, by reflection:
 * {@code FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(dimension, chunkPos))} is the claim of a chunk (null =
 * unclaimed); its {@code getTeamData()} tells whether the Miner's owner is a member of the owning team
 * ({@code isTeamMember(UUID)}), and the manager whether the owner bypasses protection ({@code getBypassProtection(UUID)}).
 * Allies are not authorized: their edit rights depend on per-team settings that need an online player to evaluate.
 */
final class FtbChunksAdapter implements ProtectionService.ClaimAdapter {
    static final String MOD_ID = "ftbchunks";

    private volatile Api api;
    private volatile boolean failed;

    private record Api(Method apiAccessor, Method isManagerLoaded, Method getManager, Method getChunk, Constructor<?> chunkDimPos,
                       Method getTeamData, Method isTeamMember, Method getBypassProtection, Method getTeam, Method getTeamId) {
    }

    @Override
    public String name() {
        return "FTB Chunks";
    }

    private Api api() {
        Api current = api;
        if (current != null || failed) {
            return current;
        }
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
            Class<?> manager = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
            Class<?> dimPos = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
            Class<?> claimed = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunk");
            Class<?> teamData = Class.forName("dev.ftb.mods.ftbchunks.api.ChunkTeamData");
            Class<?> team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
            current = new Api(apiClass.getMethod("api"), apiInterface.getMethod("isManagerLoaded"), apiInterface.getMethod("getManager"),
                    manager.getMethod("getChunk", dimPos), dimPos.getConstructor(ResourceKey.class, ChunkPos.class),
                    claimed.getMethod("getTeamData"), teamData.getMethod("isTeamMember", UUID.class),
                    manager.getMethod("getBypassProtection", UUID.class), teamData.getMethod("getTeam"), team.getMethod("getId"));
            api = current;
            return current;
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            DimensionalFluxMiner.LOGGER.warn("FTB Chunks is installed but its API was not found; see corruption.claimFailurePolicy", e);
            return null;
        }
    }

    private Object manager(Api a) throws ReflectiveOperationException {
        Object instance = a.apiAccessor().invoke(null);
        return Boolean.TRUE.equals(a.isManagerLoaded().invoke(instance)) ? a.getManager().invoke(instance) : null;
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
            return manager(a) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private Object claim(Api a, Object manager, ServerLevel level, BlockPos pos) throws ReflectiveOperationException {
        return a.getChunk().invoke(manager, a.chunkDimPos().newInstance(level.dimension(), new ChunkPos(pos)));
    }

    @Override
    public ProtectionService.Status check(ServerLevel level, BlockPos pos, UUID owner) {
        Api a = api();
        if (a == null) {
            throw new IllegalStateException("FTB Chunks API unavailable");
        }
        try {
            Object manager = manager(a);
            if (manager == null) {
                throw new IllegalStateException("FTB Chunks manager not loaded");
            }
            Object chunk = claim(a, manager, level, pos);
            if (chunk == null) {
                return ProtectionService.Status.UNCLAIMED;
            }
            if (owner == null) {
                return ProtectionService.Status.INDETERMINATE;
            }
            Object data = a.getTeamData().invoke(chunk);
            if (data == null) {
                return ProtectionService.Status.INDETERMINATE;
            }
            boolean member = Boolean.TRUE.equals(a.isTeamMember().invoke(data, owner));
            boolean bypass = Boolean.TRUE.equals(a.getBypassProtection().invoke(manager, owner));
            return member || bypass ? ProtectionService.Status.AUTHORIZED : ProtectionService.Status.UNAUTHORIZED;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FTB Chunks claim check failed", e);
        }
    }

    @Override
    public Optional<String> claimant(ServerLevel level, BlockPos pos) {
        Api a = api();
        if (a == null) {
            throw new IllegalStateException("FTB Chunks API unavailable");
        }
        try {
            Object manager = manager(a);
            if (manager == null) {
                throw new IllegalStateException("FTB Chunks manager not loaded");
            }
            Object chunk = claim(a, manager, level, pos);
            if (chunk == null) {
                return Optional.empty();
            }
            Object data = a.getTeamData().invoke(chunk);
            Object team = data == null ? null : a.getTeam().invoke(data);
            Object id = team == null ? null : a.getTeamId().invoke(team);
            if (id == null) {
                throw new IllegalStateException("FTB Chunks claim without a team");
            }
            return Optional.of("ftbchunks:" + id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FTB Chunks claim lookup failed", e);
        }
    }
}
