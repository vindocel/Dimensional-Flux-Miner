package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.corruption.CorruptionService;
import dimensionalfluxminer.miner.corruption.ProtectionService;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmItems;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Territorial protection of the Dimensional Death with a claim system of the test: positions it claims belong to one
 * player, may be listed as failing, or the whole system may be unavailable. Each test registers its own instance,
 * limited to its own area, and removes it at the end; the tests that claim everything run in their own batch.
 */
@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProtectionGameTests {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000b0b");

    private ProtectionGameTests() {
    }

    /** A claim system of the test: {@code claimed} positions belong to {@code claimOwner}. */
    static final class TestClaims implements ProtectionService.ClaimAdapter {
        final Predicate<BlockPos> claimed;
        final UUID claimOwner;
        Predicate<BlockPos> failing = p -> false;
        boolean available = true;

        TestClaims(Predicate<BlockPos> claimed, UUID claimOwner) {
            this.claimed = claimed;
            this.claimOwner = claimOwner;
        }

        @Override
        public String name() {
            return "test claims";
        }

        @Override
        public boolean available(ServerLevel level) {
            return available;
        }

        @Override
        public ProtectionService.Status check(ServerLevel level, BlockPos pos, UUID owner) {
            if (failing.test(pos)) {
                throw new IllegalStateException("test failure");
            }
            if (!claimed.test(pos)) {
                return ProtectionService.Status.UNCLAIMED;
            }
            if (owner == null) {
                return ProtectionService.Status.INDETERMINATE;
            }
            return claimOwner.equals(owner) ? ProtectionService.Status.AUTHORIZED : ProtectionService.Status.UNAUTHORIZED;
        }

        @Override
        public Optional<String> claimant(ServerLevel level, BlockPos pos) {
            if (failing.test(pos)) {
                throw new IllegalStateException("test failure");
            }
            return claimed.test(pos) ? Optional.of("test:" + claimOwner) : Optional.empty();
        }
    }

    /** Claims the columns of the test area with local x >= {@code fromX}. */
    private static Predicate<BlockPos> area(GameTestHelper helper, int fromX) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return p -> p.getX() >= origin.getX() + fromX && p.getX() < origin.getX() + 16 && p.getZ() >= origin.getZ() - 1
                && p.getZ() < origin.getZ() + 16;
    }

    /** Grass on dirt and stone at local (x, z), with a short grass plant on top. */
    private static void grassColumn(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        helper.setBlock(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(x, 3, z), Blocks.POPPY);
    }

    private static boolean corrupt(GameTestHelper helper, int x, int z, UUID owner) {
        BlockPos abs = helper.absolutePos(new BlockPos(x, 0, z));
        return CorruptionService.corrupt(helper.getLevel(), abs.getX(), abs.getZ(), owner);
    }

    @GameTest(template = "empty_large", batch = "protection", skyAccess = true)
    public static void claimsDecideWhoMayCorrupt(GameTestHelper helper) {
        TestClaims claims = new TestClaims(area(helper, 4), ALICE);
        ProtectionService.register(claims);
        try {
            for (int z = 1; z <= 5; z++) {
                grassColumn(helper, 1, z);
                grassColumn(helper, 5, z);
            }
            // Unclaimed land: anyone, even a Miner without an owner.
            helper.assertTrue(corrupt(helper, 1, 1, BOB), "unclaimed: allowed");
            helper.assertTrue(corrupt(helper, 1, 2, null), "unclaimed: allowed without an owner");
            // Alice's claim: Alice yes, Bob no, unknown owner no.
            helper.assertTrue(corrupt(helper, 5, 1, ALICE), "owner's claim: allowed");
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS.get(), new BlockPos(5, 2, 1));
            helper.assertFalse(corrupt(helper, 5, 2, BOB), "someone else's claim: denied");
            helper.assertFalse(corrupt(helper, 5, 3, null), "claimed, owner unknown: denied");
            // A failing check on a claimed position denies too.
            claims.failing = p -> p.equals(helper.absolutePos(new BlockPos(5, 2, 4)));
            helper.assertFalse(corrupt(helper, 5, 4, ALICE), "adapter failure: denied");
            claims.failing = p -> false;
            for (int z = 2; z <= 4; z++) {
                helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(5, 2, z));
                helper.assertBlockPresent(Blocks.DIRT, new BlockPos(5, 1, z));
                helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 0, z));
                helper.assertBlockPresent(Blocks.POPPY, new BlockPos(5, 3, z));
            }
            // The system cannot be queried: denied everywhere (default policy "deny").
            claims.available = false;
            helper.assertTrue(DfmConfig.claimFailureDenies(), "default policy denies");
            helper.assertFalse(corrupt(helper, 1, 5, BOB), "claim system unavailable: denied even on unclaimed land");
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(1, 2, 5));
        } finally {
            ProtectionService.unregister(claims);
        }
        helper.succeed();
    }

    @GameTest(template = "empty_large", batch = "protection", skyAccess = true)
    public static void aColumnIsAuthorizedOrDeniedAsAWhole(GameTestHelper helper) {
        // Only the upper half of a tall plant is claimed by Bob (a claim with height limits): nothing of the column changes.
        BlockPos upper = helper.absolutePos(new BlockPos(2, 4, 2));
        TestClaims claims = new TestClaims(p -> p.equals(upper), BOB);
        ProtectionService.register(claims);
        try {
            helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.DIRT);
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.GRASS_BLOCK);
            helper.setBlock(new BlockPos(2, 3, 2), Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
            helper.setBlock(new BlockPos(2, 4, 2), Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
            helper.assertFalse(corrupt(helper, 2, 2, ALICE), "protected upper half: the whole column is denied");
            helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(2, 2, 2));
            helper.assertBlockPresent(Blocks.TALL_GRASS, new BlockPos(2, 3, 2));
            helper.assertBlockPresent(Blocks.TALL_GRASS, new BlockPos(2, 4, 2));
            // Bob's own Miner corrupts it, the two-block plant at once.
            helper.assertTrue(corrupt(helper, 2, 2, BOB), "owner of the protected part: allowed");
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS.get(), new BlockPos(2, 2, 2));
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS_PLANT.get(), new BlockPos(2, 3, 2));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 4, 2));
        } finally {
            ProtectionService.unregister(claims);
        }
        helper.succeed();
    }

    @GameTest(template = "empty_large", batch = "protection", timeoutTicks = 120)
    public static void corruptedWaterNeverCrossesIntoAClaim(GameTestHelper helper) {
        // Bob claims local x >= 5. Corrupted Water at x = 3 on a stone floor flows toward the border.
        TestClaims claims = new TestClaims(area(helper, 5), BOB);
        ProtectionService.register(claims);
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        // A shaft inside Bob's claim, with vanilla water under the claimed column x = 5.
        helper.setBlock(new BlockPos(5, 0, 2), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 1, 2), DfmBlocks.CORRUPTED_WATER.get());
        helper.runAfterDelay(80, () -> {
            try {
                helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(4, 1, 2))).getType()
                        .isSame(dimensionalfluxminer.registry.DfmFluids.CORRUPTED_WATER.get()), "it flows on unclaimed land");
                for (int x = 5; x <= 8; x++) {
                    for (int z = 0; z <= 4; z++) {
                        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(x, 1, z))).isEmpty(),
                                "no Corrupted Water inside the claim at " + x + "," + z);
                    }
                }
                helper.assertBlockPresent(Blocks.WATER, new BlockPos(5, 0, 2));
            } finally {
                ProtectionService.unregister(claims);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small", batch = "protection", timeoutTicks = 120)
    public static void corruptedWaterDoesNotSinkIntoAProtectedColumn(GameTestHelper helper) {
        // The vanilla water below is claimed by Bob: the Corrupted Water above it never turns it.
        BlockPos below = helper.absolutePos(new BlockPos(2, 1, 2));
        TestClaims claims = new TestClaims(p -> p.equals(below), BOB);
        ProtectionService.register(claims);
        for (int y = 0; y <= 3; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        helper.setBlock(new BlockPos(2 + dx, y, 2 + dz), Blocks.STONE);
                    }
                }
            }
        }
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.WATER);
        helper.setBlock(new BlockPos(2, 2, 2), DfmBlocks.CORRUPTED_WATER.get());
        helper.runAfterDelay(60, () -> {
            try {
                helper.assertBlockPresent(Blocks.WATER, new BlockPos(2, 1, 2));
            } finally {
                ProtectionService.unregister(claims);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small", batch = "protection")
    public static void serverRulesProtectSpawnAndRegions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int radius = level.getServer().getSpawnProtectionRadius();
        BlockPos spawn = level.getSharedSpawnPos();
        helper.assertTrue(ProtectionService.inProtectedRegion(level, spawn) == (radius > 0), "spawn protection follows the server (" + radius + ")");
        helper.assertFalse(ProtectionService.inProtectedRegion(level, spawn.offset(radius + 1, 0, 0)), "outside the spawn protection");
        DfmConfig.Region region = DfmConfig.Region.parse("minecraft:overworld, 10, -5, -10, 5");
        helper.assertTrue(region != null && region.contains(level.dimension().location(), 0, 0) && region.contains(level.dimension().location(), -10, 5),
                "region corners included: " + region);
        helper.assertFalse(region.contains(level.dimension().location(), 11, 0) || region.contains(net.minecraft.world.level.Level.NETHER.location(), 0, 0),
                "outside the region or in another dimension");
        helper.assertTrue(DfmConfig.Region.parse("minecraft:overworld,1,2,3") == null && DfmConfig.Region.parse("x,a,1,2,3") == null, "invalid entries refused");
        helper.assertTrue(DfmConfig.corruptionEnabled() && ProtectionService.corruptionAllowed(level), "corruption on by default");
        helper.succeed();
    }

    @GameTest(template = "empty_large", batch = "protection")
    public static void theOwnerSurvivesARestart(GameTestHelper helper) {
        MinerControllerBlockEntity c = MinerGameTests.build(helper);
        c.setOwner(ALICE);
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = c.saveWithoutMetadata(registries);
        c.setOwner(BOB);
        c.loadWithComponents(saved, registries);
        helper.assertTrue(ALICE.equals(c.owner()), "owner kept: " + c.owner());
        helper.succeed();
    }

    /** Calls a method declared by an interface of an optional mod (implementations may be non-public classes). */
    private static Object call(Object target, String type, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        return Class.forName(type).getMethod(name, types).invoke(target, args);
    }

    /**
     * With FTB Chunks installed: a real claim, through the real adapter. A server team of FTB Teams claims the test's
     * chunk; nobody is a member of a server team, so every owner is unauthorized there. Runs alone, and the chunk is
     * released at the end, so the other tests never meet it.
     */
    @GameTest(template = "empty_small", batch = "protection_ftb")
    public static void ftbChunksClaimsAreRespected(GameTestHelper helper) throws ReflectiveOperationException {
        if (!net.neoforged.fml.ModList.get().isLoaded("ftbchunks")) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        var source = level.getServer().createCommandSourceStack();
        BlockPos inside = helper.absolutePos(new BlockPos(2, 1, 2));
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(inside);
        BlockPos outside = new BlockPos(chunk.getMinBlockX() - 20, inside.getY(), chunk.getMinBlockZ() - 20);
        String teamsApi = "dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API";
        String teamManager = "dev.ftb.mods.ftbteams.api.TeamManager";
        String chunksApi = "dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API";
        String chunkManager = "dev.ftb.mods.ftbchunks.api.ClaimedChunkManager";
        String teamData = "dev.ftb.mods.ftbchunks.api.ChunkTeamData";
        Class<?> team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
        Class<?> color = Class.forName("dev.ftb.mods.ftblibrary.icon.Color4I");
        Class<?> dimPos = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
        Object pos = dimPos.getConstructor(net.minecraft.resources.ResourceKey.class, net.minecraft.world.level.ChunkPos.class)
                .newInstance(level.dimension(), chunk);
        Object teams = call(Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null), teamsApi, "getManager", new Class<?>[0]);
        Object serverTeam = call(teams, teamManager, "createServerTeam", new Class<?>[] {net.minecraft.commands.CommandSourceStack.class, String.class, String.class, color},
                source, "dfm_protection_test", "", color.getField("WHITE").get(null));
        Object chunks = call(Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI").getMethod("api").invoke(null), chunksApi, "getManager", new Class<?>[0]);
        Object data = call(chunks, chunkManager, "getOrCreateData", new Class<?>[] {team}, serverTeam);
        Object result = call(data, teamData, "claim", new Class<?>[] {net.minecraft.commands.CommandSourceStack.class, dimPos, boolean.class}, source, pos, false);
        helper.assertTrue(Boolean.TRUE.equals(call(result, "dev.ftb.mods.ftbchunks.api.ClaimResult", "isSuccess", new Class<?>[0])),
                "test chunk claimed by a server team");
        try {
            helper.assertTrue(ProtectionService.status(level, inside, BOB) == ProtectionService.Status.UNAUTHORIZED,
                    "not a member: unauthorized, got " + ProtectionService.status(level, inside, BOB));
            helper.assertTrue(ProtectionService.status(level, inside, null) == ProtectionService.Status.INDETERMINATE, "no owner: indeterminate");
            helper.assertTrue(ProtectionService.status(level, outside, BOB) == ProtectionService.Status.UNCLAIMED, "outside: unclaimed");

            helper.assertFalse(ProtectionService.waterMayCross(level, outside, inside), "water never flows into the claim");
            helper.assertTrue(ProtectionService.waterMayCross(level, inside, inside.below()), "water inside the same claim");
        } finally {
            call(data, teamData, "unclaim", new Class<?>[] {net.minecraft.commands.CommandSourceStack.class, dimPos, boolean.class}, source, pos, false);
        }
        helper.assertTrue(ProtectionService.status(level, inside, BOB) == ProtectionService.Status.UNCLAIMED, "released");
        helper.succeed();
    }
    /** Runs alone: every column around is Bob's, so Alice's Basic Miner finds no matter and produces nothing. */
    @GameTest(template = "empty_large", batch = "protection_denied", timeoutTicks = 200)
    public static void basicCoreGetsNoMatterFromProtectedLand(GameTestHelper helper) {
        TestClaims claims = new TestClaims(p -> true, BOB);
        ProtectionService.register(claims);
        MinerControllerBlockEntity c = MinerGameTests.build(helper);
        c.setOwner(ALICE);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.BASIC_DIMENSIONAL_CORE.get()));
        MinerGameTests.powerUp(c, 10);
        c.setRunRequested(true);
        helper.runAfterDelay(150, () -> {
            try {
                helper.assertTrue(c.dmUnits() == 0 && c.columnsDrained() == 0, "no DM without corruption: " + c.dmUnits());
                for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
                    helper.assertTrue(c.output().getStackInSlot(i).isEmpty(), "nothing produced");
                }
            } finally {
                ProtectionService.unregister(claims);
            }
            helper.succeed();
        });
    }

    /** Runs alone: authorized corruption pays exactly 64 DM per column, nothing else. */
    @GameTest(template = "empty_large", batch = "protection_reward", timeoutTicks = 300)
    public static void authorizedCorruptionPaysExactly64Dm(GameTestHelper helper) {
        MinerControllerBlockEntity c = MinerGameTests.build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.BASIC_DIMENSIONAL_CORE.get()));
        MinerGameTests.powerUp(c, 3);
        c.setRunRequested(true);
        // A Rate-3 buffer holds less than one 200 000 FE attempt: keep it fed like a generator would.
        helper.onEachTick(() -> c.energyBuffer().setStored(c.powerLimit() * 10));
        helper.succeedWhen(() -> {
            int produced = 0;
            for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
                produced += c.output().getStackInSlot(i).getCount();
            }
            helper.assertTrue(produced > 0 && c.columnsDrained() > 0, "worked: status " + c.status() + ", DM " + c.dmUnits());
            // Matter in = 64 DM per drained column; matter out = 2 DM per attempt (every attempt here kept its ore).
            long expected = c.columnsDrained() * MatterValues.COLUMN_UNITS - (long) produced * MatterValues.ROLL_COST_UNITS;
            helper.assertTrue(c.dmUnits() == expected, "DM " + c.dmUnits() + " = " + c.columnsDrained() + " x 640 - " + produced + " x 20");
        });
    }
}
