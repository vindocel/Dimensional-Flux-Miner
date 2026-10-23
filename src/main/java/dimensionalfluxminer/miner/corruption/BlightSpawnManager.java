package dimensionalfluxminer.miner.corruption;

import dimensionalfluxminer.registry.DfmTags;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Centralized Blight Spawn Manager: there is no spawner or ticker per corrupted block. Every 40 ticks, loaded
 * corrupted chunks near players receive {@code 4 * density^2} bonus spawn attempts (fraction = probability of
 * one more), capped at 64 attempts per level per pass. Natural rules, weights, mob cap, Peaceful, player
 * distance and collision are preserved; only types tagged {@code blight_light_bypass_eligible} may ignore light.
 */
public final class BlightSpawnManager {
    public static final int INTERVAL_TICKS = 40;
    public static final int MAX_ATTEMPTS_PER_PASS = 64;
    public static final double ATTEMPTS_AT_FULL_DENSITY = 4.0;
    private static final double ACTIVE_PLAYER_RADIUS = 128.0;
    private static final double MIN_PLAYER_DISTANCE = 24.0;
    private static final int COLUMNS_PER_CHUNK = 256;
    /** Natural spawner's chunk-area divisor (17 x 17). */
    private static final int MOB_CAP_AREA = 289;

    private BlightSpawnManager() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL_TICKS != 0) {
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        BlightData data = BlightData.get(level);
        List<ServerPlayer> players = level.players().stream().filter(p -> !p.isSpectator()).toList();
        if (players.isEmpty()) {
            return;
        }
        List<Long> candidates = new ArrayList<>();
        for (long chunk : data.chunks()) {
            int cx = ChunkPos.getX(chunk);
            int cz = ChunkPos.getZ(chunk);
            if (!level.hasChunk(cx, cz)) {
                continue;
            }
            double x = (cx << 4) + 8;
            double z = (cz << 4) + 8;
            if (players.stream().anyMatch(p -> p.distanceToSqr(x, p.getY(), z) <= ACTIVE_PLAYER_RADIUS * ACTIVE_PLAYER_RADIUS)) {
                candidates.add(chunk);
            }
        }
        Collections.shuffle(candidates, new java.util.Random(level.getRandom().nextLong()));
        int budget = MAX_ATTEMPTS_PER_PASS;
        for (long chunk : candidates) {
            if (budget <= 0) {
                break;
            }
            double density = density(level, data, ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
            double expected = ATTEMPTS_AT_FULL_DENSITY * density * density;
            int attempts = (int) Math.floor(expected);
            if (level.getRandom().nextDouble() < expected - attempts) {
                attempts++;
            }
            for (int i = 0; i < attempts && budget > 0; i++) {
                budget--;
                trySpawn(level, data, chunk);
            }
        }
    }

    /** corruptedEligibleSurfaceColumns / eligibleSurfaceColumns in the loaded 3 x 3 chunk neighborhood. */
    static double density(ServerLevel level, BlightData data, int cx, int cz) {
        int corrupted = 0;
        int loaded = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.hasChunk(cx + dx, cz + dz)) {
                    loaded++;
                    corrupted += data.count(ChunkPos.pack(cx + dx, cz + dz));
                }
            }
        }
        return loaded == 0 ? 0 : Math.min(1.0, corrupted / (double) (loaded * COLUMNS_PER_CHUNK));
    }

    /** Spawn pressure (density squared) around a position, shown as BLIGHT THREAT. */
    public static double threatAround(ServerLevel level, BlockPos pos) {
        double d = density(level, BlightData.get(level), pos.getX() >> 4, pos.getZ() >> 4);
        return d * d;
    }

    private static void trySpawn(ServerLevel level, BlightData data, long chunk) {
        LongArrayList columns = data.columns(chunk);
        if (columns.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        long packed = columns.getLong(random.nextInt(columns.size()));
        int x = BlightData.unpackX(packed);
        int z = BlightData.unpackZ(packed);
        // The corrupted ground may be under cover (trees, buildings): spawn on it, the collision checks below decide.
        BlockPos ground = CorruptionService.ground(level, x, z);
        if (ground == null) {
            data.remove(x, z);
            return;
        }
        int y = ground.getY() + 1;
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos.below()).is(DfmTags.Blocks.CORRUPTED_TERRAIN)) {
            // The surface is no longer corrupted (mined, covered or replaced): forget it.
            data.remove(x, z);
            return;
        }
        if (!level.getFluidState(pos).isEmpty()) {
            return;
        }
        double nearest = level.players().stream().filter(p -> !p.isSpectator())
                .mapToDouble(p -> p.distanceToSqr(x + 0.5, y, z + 0.5)).min().orElse(Double.MAX_VALUE);
        if (nearest < MIN_PLAYER_DISTANCE * MIN_PLAYER_DISTANCE || nearest > ACTIVE_PLAYER_RADIUS * ACTIVE_PLAYER_RADIUS) {
            return;
        }
        NaturalSpawner.SpawnState spawnState = level.getChunkSource().getLastSpawnState();
        if (spawnState != null) {
            int cap = MobCategory.MONSTER.getMaxInstancesPerChunk() * spawnState.getSpawnableChunkCount() / MOB_CAP_AREA;
            if (spawnState.getMobCategoryCounts().getInt(MobCategory.MONSTER) >= cap) {
                return;
            }
        }
        WeightedList<MobSpawnSettings.SpawnerData> entries = level.getChunkSource().getGenerator()
                .getMobsAt(level, level.structureManager(), MobCategory.MONSTER, pos);
        MobSpawnSettings.SpawnerData entry = entries.getRandom(random).orElse(null);
        if (entry == null || entry.type().builtInRegistryHolder().is(DfmTags.Entities.BLIGHT_SPAWN_BLACKLIST)) {
            return;
        }
        EntityType<?> type = entry.type();
        if (!SpawnPlacements.isSpawnPositionOk(type, level, pos) || !level.noCollision(type.getSpawnAABB(x + 0.5, y, z + 0.5))) {
            return;
        }
        if (!rulesAllow(level, type, pos, random)) {
            return;
        }
        Entity entity = type.create(level, EntitySpawnReason.NATURAL);
        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return;
        }
        mob.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        if (!mob.checkSpawnObstruction(level)) {
            mob.discard();
            return;
        }
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        level.addFreshEntityWithPassengers(mob);
    }

    /**
     * Normal spawn rules first. Only explicitly eligible monsters may fall back to the light-independent
     * monster rule, which still keeps Peaceful and the spawn-support checks.
     */
    @SuppressWarnings("unchecked")
    static boolean rulesAllow(ServerLevel level, EntityType<?> type, BlockPos pos, RandomSource random) {
        if (SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)) {
            return true;
        }
        if (!type.builtInRegistryHolder().is(DfmTags.Entities.BLIGHT_LIGHT_BYPASS_ELIGIBLE) || type.getCategory() != MobCategory.MONSTER) {
            return false;
        }
        Entity probe = type.create(level, EntitySpawnReason.NATURAL);
        boolean monster = probe instanceof Monster;
        if (probe != null) {
            probe.discard();
        }
        return monster && Monster.checkAnyLightMonsterSpawnRules((EntityType<? extends Monster>) type, level, EntitySpawnReason.NATURAL, pos, random);
    }
}
