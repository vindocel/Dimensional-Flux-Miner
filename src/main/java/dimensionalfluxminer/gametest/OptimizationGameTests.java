package dimensionalfluxminer.gametest;

import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.SlotStorage;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.corruption.CorruptionService;
import dimensionalfluxminer.miner.corruption.ProtectionService;
import dimensionalfluxminer.miner.profile.ProfileBuilder;
import dimensionalfluxminer.miner.profile.ProfileDimensionRule;
import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.miner.profile.VoidCurve;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Regression tests of the optimizations: each fast path gives exactly the answer of the code it replaced. */
public final class OptimizationGameTests {
    private OptimizationGameTests() {
    }

    /** The roll before the optimization: walk the families one by one. */
    private static int referenceRoll(ResourceProfile profile, RandomSource random) {
        int total = profile.totalWeight() + Math.max(0, profile.emptyWeight());
        int r = random.nextInt(Math.max(1, total));
        List<ResourceFamily> families = profile.families();
        for (int i = 0; i < families.size(); i++) {
            r -= families.get(i).weight();
            if (r < 0) {
                return i;
            }
        }
        return families.isEmpty() || profile.emptyWeight() > 0 ? -1 : families.size() - 1;
    }

    @GameTest(template = "empty_small")
    public static void rollsAreIdenticalToTheOneByOneWalk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceProfile overworld = ResourceProfiles.get(level);
        List<ResourceProfile> profiles = List.of(overworld, ResourceProfiles.get(level.getServer().getLevel(Level.NETHER)),
                ResourceProfiles.get(level.getServer().getLevel(Level.END)),
                ProfileBuilder.build(level, List.of(), List.of(new ProfileDimensionRule(level.dimension(), 0.97, 1000, VoidCurve.STEP))),
                // Zero weights must never be picked, as before.
                new ResourceProfile(level.dimension(), List.of(
                        new ResourceFamily(overworld.families().get(0).key(), overworld.families().get(0).variants(), 0, Items.COAL),
                        new ResourceFamily(overworld.families().get(1).key(), overworld.families().get(1).variants(), 5, Items.COAL),
                        new ResourceFamily(overworld.families().get(2).key(), overworld.families().get(2).variants(), 0, Items.COAL))));
        for (ResourceProfile profile : profiles) {
            RandomSource a = RandomSource.create(42);
            RandomSource b = RandomSource.create(42);
            for (int i = 0; i < 200_000; i++) {
                int fast = profile.rollIndex(a);
                int slow = referenceRoll(profile, b);
                if (fast != slow) {
                    helper.fail("roll " + i + " of " + profile + ": " + fast + " instead of " + slow);
                    return;
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void outputSlotIsTheOneTheInsertHelperWouldUse(GameTestHelper helper) {
        RandomSource random = RandomSource.create(7);
        List<Item> items = List.of(Items.IRON_ORE, Items.COAL_ORE, Items.DIAMOND_ORE, Items.ENDER_PEARL, Items.DIAMOND_SWORD);
        for (int round = 0; round < 200; round++) {
            SlotStorage fast = new SlotStorage(MinerControllerBlockEntity.OUTPUT_SLOTS);
            SlotStorage slow = new SlotStorage(MinerControllerBlockEntity.OUTPUT_SLOTS);
            for (int slot = 0; slot < MinerControllerBlockEntity.OUTPUT_SLOTS; slot++) {
                if (random.nextInt(3) > 0) {
                    Item item = items.get(random.nextInt(items.size()));
                    int count = 1 + random.nextInt(new ItemStack(item).getMaxStackSize());
                    fast.setStackInSlot(slot, new ItemStack(item, count));
                    slow.setStackInSlot(slot, new ItemStack(item, count));
                }
            }
            for (int i = 0; i < 400; i++) {
                ItemStack stack = new ItemStack(items.get(random.nextInt(items.size())));
                boolean slowFits = ItemSlots.insert(slow, stack.copy(), true).isEmpty();
                int slot = dimensionalfluxminer.miner.MinerOutput.insertionSlot(fast, stack);
                helper.assertTrue((slot >= 0) == slowFits, "fits the same: round " + round + ", insert " + i);
                if (slot >= 0) {
                    ItemSlots.insert(slow, stack.copy(), false);
                    fast.insertItem(slot, stack.copy(), false);
                }
                for (int s = 0; s < MinerControllerBlockEntity.OUTPUT_SLOTS; s++) {
                    if (!ItemStack.matches(fast.getStackInSlot(s), slow.getStackInSlot(s))) {
                        helper.fail("slot " + s + " differs after insert " + i + " of round " + round);
                        return;
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_large", skyAccess = true)
    public static void surveyAnswerMatchesTheFullPlan(GameTestHelper helper) {
        // Varied columns: water, corrupted water, cover, sandstone, stone, grass with plants, empty air.
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.WATER);
        helper.setBlock(new BlockPos(2, 1, 1), DfmBlocks.CORRUPTED_WATER.get());
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.SANDSTONE);
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(4, 2, 1), Blocks.POPPY);
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(6, 1, 1), DfmBlocks.CORRUPTED_STONE.get());
        helper.setBlock(new BlockPos(7, 1, 1), Blocks.GLASS);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        int checked = 0;
        for (int x = -40; x < 40; x++) {
            for (int z = -40; z < 40; z++) {
                int ax = origin.getX() + x;
                int az = origin.getZ() + z;
                if (!level.hasChunk(ax >> 4, az >> 4)) {
                    continue;
                }
                helper.assertTrue(CorruptionService.drainable(level, ax, az) == CorruptionService.plan(level, ax, az).drainable(),
                        "drainable differs at " + ax + "," + az);
                checked++;
            }
        }
        helper.assertTrue(checked > 100, "columns checked: " + checked);
        helper.succeed();
    }

    @GameTest(template = "empty_small", batch = "protection")
    public static void columnCheckMatchesThePerPositionCheck(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID owner = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        List<BlockPos> column = new ArrayList<>();
        for (int y = 0; y < 6; y++) {
            column.add(base.above(y));
        }
        for (boolean chunkBased : new boolean[] {false, true}) {
            for (int protectedY = -1; protectedY < 6; protectedY++) {
                BlockPos protectedPos = protectedY < 0 ? null : base.above(protectedY);
                ProtectionService.ClaimAdapter adapter = new ProtectionService.ClaimAdapter() {
                    @Override
                    public String name() {
                        return "regression";
                    }

                    @Override
                    public boolean available(ServerLevel l) {
                        return true;
                    }

                    @Override
                    public ProtectionService.Status check(ServerLevel l, BlockPos pos, UUID o) {
                        // A chunk-based system gives one answer for the whole chunk, as the contract says.
                        boolean hit = chunkBased ? protectedPos != null && net.minecraft.world.level.ChunkPos.containing(pos).equals(net.minecraft.world.level.ChunkPos.containing(protectedPos))
                                : pos.equals(protectedPos);
                        return hit ? ProtectionService.Status.UNAUTHORIZED : ProtectionService.Status.UNCLAIMED;
                    }

                    @Override
                    public Optional<String> claimant(ServerLevel l, BlockPos pos) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean chunkBased() {
                        return chunkBased;
                    }
                };
                ProtectionService.register(adapter);
                try {
                    boolean perPosition = column.stream().allMatch(p -> ProtectionService.status(level, p, owner).allows());
                    boolean whole = ProtectionService.allowsAll(level, column, owner);
                    helper.assertTrue(whole == perPosition, "chunkBased=" + chunkBased + " protected y=" + protectedY + ": " + whole);
                } finally {
                    ProtectionService.unregister(adapter);
                }
            }
        }
        helper.succeed();
    }
}
