package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.corruption.CorruptionService;
import dimensionalfluxminer.miner.corruption.ProtectionService;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.miner.MinerStructure;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.survey.MinerSurvey;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.smelter.AlloySmelterBlockEntity;
import dimensionalfluxminer.smelter.SmelterMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Performance benchmark of the hot paths (opt-in: runs only with the environment variable {@code DFM_BENCHMARK=1}, and
 * alone in its batch). Every scenario logs one {@code BENCH} line with time per operation, bytes allocated on the server
 * thread, and the work really done (attempts, FE, DM, columns, items), so a faster build can be checked for doing exactly
 * the same work.
 */
@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BenchmarkGameTests {
    private static final long PLENTY = 1_000_000_000_000L;

    private BenchmarkGameTests() {
    }

    private static boolean enabled() {
        return "1".equals(System.getenv("DFM_BENCHMARK"));
    }

    private static long allocated() {
        java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
        return bean instanceof com.sun.management.ThreadMXBean sun ? sun.getCurrentThreadAllocatedBytes() : 0;
    }

    /** Times {@code body} (after a warm-up run) and logs it per operation. */
    private static void bench(String name, long operations, Runnable warmUp, Runnable body, java.util.function.Supplier<String> workText) {
        warmUp.run();
        long bytes = allocated();
        long start = System.nanoTime();
        body.run();
        long nanos = System.nanoTime() - start;
        bytes = allocated() - bytes;
        String work = workText.get();
        DimensionalFluxMiner.LOGGER.info(String.format(Locale.ROOT, "BENCH %-34s ops=%9d ns/op=%10.1f B/op=%9.1f total_ms=%8.2f %s",
                name, operations, nanos / (double) operations, bytes / (double) operations, nanos / 1e6, work.isEmpty() ? "" : "| " + work));
    }

    private static MinerControllerBlockEntity buildMiner(GameTestHelper helper, BlockPos terminal) {
        BlockPos terminalAbs = helper.absolutePos(terminal);
        MinerStructure layout = MinerStructure.layout(terminalAbs, Direction.SOUTH);
        for (MinerStructure.Member m : layout.members()) {
            if (m.slot().part() != Part.MINER_TERMINAL) {
                helper.getLevel().setBlockAndUpdate(m.pos(), MinerStructure.blockFor(m.slot().part()).defaultBlockState());
            }
        }
        helper.getLevel().setBlockAndUpdate(terminalAbs, DfmBlocks.MINER_TERMINAL.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        MinerControllerBlockEntity c = (MinerControllerBlockEntity) helper.getLevel().getBlockEntity(terminalAbs);
        c.tryForm();
        helper.assertTrue(c.formed(), "benchmark miner formed at " + terminal);
        return c;
    }

    private static void refill(MinerControllerBlockEntity c) {
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
            c.output().setStackInSlot(i, ItemStack.EMPTY);
        }
        c.setDmUnits(MatterValues.BUFFER_UNITS);
        c.energyBuffer().setStored(PLENTY);
    }

    @GameTest(template = "empty_large", batch = "zz_benchmark", timeoutTicks = 2400)
    public static void benchmark(GameTestHelper helper) {
        if (!enabled()) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();

        // ---- ore roll
        ResourceProfile profile = ResourceProfiles.get(level);
        RandomSource random = RandomSource.create(1);
        int[] picks = new int[1];
        bench("roll.family", 2_000_000, () -> {
            for (int i = 0; i < 200_000; i++) {
                profile.roll(random);
            }
        }, () -> {
            for (int i = 0; i < 2_000_000; i++) {
                if (profile.roll(random).isPresent()) {
                    picks[0]++;
                }
            }
        }, () -> "families=" + profile.families().size());

        // ---- one Stabilized Miner, Rate 1 and Rate 10 (64 attempts per cycle), and accelerated cycles
        MinerControllerBlockEntity miner = buildMiner(helper, new BlockPos(4, 1, 5));
        miner.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        miner.setRunRequested(true);
        for (int rateLevel : new int[] {1, 10}) {
            miner.setRate(rateLevel);
            int cycles = rateLevel == 1 ? 20_000 : 4_000;
            long[] work = new long[3];
            bench("miner.stabilized.rate" + rateLevel + ".cycle", cycles, () -> {
                for (int i = 0; i < cycles / 10; i++) {
                    refill(miner);
                    miner.acceleratedCycle();
                }
            }, () -> {
                for (int i = 0; i < cycles; i++) {
                    refill(miner);
                    long fe = miner.energyBuffer().stored();
                    miner.acceleratedCycle();
                    work[0] += (MatterValues.BUFFER_UNITS - miner.dmUnits()) / MatterValues.ROLL_COST_UNITS;
                    work[1] += fe - miner.energyBuffer().stored();
                    work[2] += MatterValues.BUFFER_UNITS - miner.dmUnits();
                }
            }, () -> "attempts=" + work[0] + " FE=" + work[1] + " DM_units=" + work[2]);
        }

        // ---- ten Miners at Rate 10, one real tick each plus nine accelerated cycles (Soul Surge style)
        List<MinerControllerBlockEntity> miners = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MinerControllerBlockEntity m = buildMiner(helper, new BlockPos(4 + 8 * (i + 1), 1, 5));
            m.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
            m.setRate(10);
            m.setRunRequested(true);
            miners.add(m);
        }
        long[] tenWork = new long[1];
        bench("miners10.rate10.x10cycles", 400, () -> {
            for (int t = 0; t < 40; t++) {
                for (MinerControllerBlockEntity m : miners) {
                    for (int k = 0; k < 10; k++) {
                        refill(m);
                        m.acceleratedCycle();
                    }
                }
            }
        }, () -> {
            for (int t = 0; t < 400; t++) {
                for (MinerControllerBlockEntity m : miners) {
                    for (int k = 0; k < 10; k++) {
                        refill(m);
                        m.acceleratedCycle();
                        tenWork[0] += (MatterValues.BUFFER_UNITS - m.dmUnits()) / MatterValues.ROLL_COST_UNITS;
                    }
                }
            }
        }, () -> "attempts=" + tenWork[0] + " (per op = 10 miners x 10 cycles)");

        // ---- survey and corruption of real terrain around the test
        BlockPos center = helper.absolutePos(new BlockPos(-200, 0, -200));
        MinerSurvey survey = new MinerSurvey(center);
        int[] inspected = new int[1];
        bench("survey.step256", 200, () -> survey.step(level, 256), () -> {
            for (int i = 0; i < 200; i++) {
                inspected[0] += survey.step(level, 256);
            }
        }, () -> "columns=" + inspected[0] + " candidates=" + survey.knownDrainable());
        List<long[]> columns = new ArrayList<>();
        for (int i = 0; i < 4_000 && survey.hasCandidates(); i++) {
            long packed = survey.pollCandidate();
            columns.add(new long[] {packed >> 32, (int) packed});
        }
        int[] planned = new int[1];
        bench("corruption.plan", columns.size(), () -> {
        }, () -> {
            for (long[] col : columns) {
                if (CorruptionService.plan(level, (int) col[0], (int) col[1]).drainable()) {
                    planned[0]++;
                }
            }
        }, () -> "drainable=" + planned[0]);
        int[] corrupted = new int[1];
        bench("corruption.corrupt", columns.size(), () -> {
        }, () -> {
            for (long[] col : columns) {
                if (CorruptionService.corrupt(level, (int) col[0], (int) col[1], null)) {
                    corrupted[0]++;
                }
            }
        }, () -> "columns=" + corrupted[0]);
        BlockPos probe = helper.absolutePos(new BlockPos(1, 1, 1));
        bench("protection.status", 1_000_000, () -> {
        }, () -> {
            for (int i = 0; i < 1_000_000; i++) {
                ProtectionService.status(level, probe, null);
            }
        }, () -> "adapters=" + ProtectionService.adaptersPresent());

        // ---- Crusher and Smelter at Rate 10
        MechanicalCrusherBlockEntity crusher = CrusherGameTests.place(helper);
        long[] crushed = new long[2];
        bench("crusher.rate10.cycle", 20_000, () -> {
        }, () -> {
            for (int i = 0; i < 20_000; i++) {
                crusher.output().clear();
                crusher.input().clear();
                crusher.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
                crusher.energyBuffer().setStored(PLENTY);
                AbstractMachineBlockEntity.serverTick(level, crusher.getBlockPos(), crusher.getBlockState(), crusher);
                crushed[0] += 64 - crusher.input().firstCount();
            }
        }, () -> "items=" + crushed[0]);
        AlloySmelterBlockEntity smelter = SmelterGameTests.place(helper);
        smelter.setMode(SmelterMode.NORMAL);
        long[] smelted = new long[1];
        bench("smelter.rate10.3lanes.cycle", 20_000, () -> {
        }, () -> {
            for (int i = 0; i < 20_000; i++) {
                for (int lane = 0; lane < AlloySmelterBlockEntity.LANES; lane++) {
                    smelter.input(lane).clear();
                    smelter.output(lane).clear();
                    smelter.input(lane).insert(new ItemStack(Items.SAND), 64, false);
                }
                smelter.energyBuffer().setStored(PLENTY);
                AbstractMachineBlockEntity.serverTick(level, smelter.getBlockPos(), smelter.getBlockState(), smelter);
                for (int lane = 0; lane < AlloySmelterBlockEntity.LANES; lane++) {
                    smelted[0] += smelter.output(lane).countOf(new ItemStack(Items.GLASS));
                }
            }
        }, () -> "items=" + smelted[0]);

        // ---- automation: Auto Input and Auto Eject with chests, Rate 10
        helper.assertTrue(smelter.configureSide(RelativeFace.LEFT, Transmission.ITEM, SideRoute.ITEM_INPUT_ALL)
                && smelter.configureSide(RelativeFace.RIGHT, Transmission.ITEM, SideRoute.ITEM_OUTPUT), "automation sides");
        BlockPos source = smelter.getBlockPos().relative(RelativeFace.LEFT.toDirection(smelter.facing()));
        BlockPos sink = smelter.getBlockPos().relative(RelativeFace.RIGHT.toDirection(smelter.facing()));
        level.setBlockAndUpdate(source, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(sink, Blocks.CHEST.defaultBlockState());
        Container in = (Container) level.getBlockEntity(source);
        Container out = (Container) level.getBlockEntity(sink);
        smelter.setAutoInput(true);
        smelter.setAutoEject(true);
        long[] moved = new long[1];
        bench("smelter.automation.cycle", 5_000, () -> {
        }, () -> {
            for (int i = 0; i < 5_000; i++) {
                for (int s = 0; s < in.getContainerSize(); s++) {
                    in.setItem(s, new ItemStack(Items.SAND, 64));
                }
                for (int s = 0; s < out.getContainerSize(); s++) {
                    moved[0] += out.getItem(s).getCount();
                    out.setItem(s, ItemStack.EMPTY);
                }
                smelter.energyBuffer().setStored(PLENTY);
                AbstractMachineBlockEntity.serverTick(level, smelter.getBlockPos(), smelter.getBlockState(), smelter);
            }
        }, () -> "glass_out=" + moved[0]);
        helper.succeed();
    }
}
