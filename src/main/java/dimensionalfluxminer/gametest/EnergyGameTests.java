package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.math.DynamicEnergyBuffer;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceMode;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Energy per operation: the same FE for every operation at every Rate, charged only for the work really done. Every
 * check drives the logical cycles directly inside one callback, so the normal ticks of the test never interleave.
 */
public final class EnergyGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    /** Plenty of FE, far above any buffer target: the work is never energy limited unless a test says so. */
    private static final long PLENTY = 1_000_000_000_000L;

    private EnergyGameTests() {
    }

    /** Places an AE2 creative energy cell; false when AE2 is not installed. */
    private static boolean placeAe2Cell(GameTestHelper helper, BlockPos pos) {
        var cell = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("ae2", "creative_energy_cell"));
        cell.ifPresent(block -> helper.setBlock(pos, block));
        return cell.isPresent();
    }

    /** With Applied Energistics 2, a machine next to the network draws AE as FE. */
    @GameTest(template = "empty_small", timeoutTicks = 100)
    public static void machinesDrawEnergyFromAe2(GameTestHelper helper) {
        var crusher = CrusherGameTests.place(helper);
        crusher.energyBuffer().setStored(0);
        if (!placeAe2Cell(helper, new BlockPos(2, 2, 2)) || !placeAe2Cell(helper, new BlockPos(1, 1, 2))) {
            helper.succeed();
            return;
        }
        helper.succeedWhen(() -> helper.assertTrue(crusher.energyBuffer().stored() > 0, "crusher filled from AE2"));
    }

    /** With Applied Energistics 2, the Miner draws AE through an energy port. */
    @GameTest(template = "empty_large", timeoutTicks = 100)
    public static void minerDrawsEnergyFromAe2(GameTestHelper helper) {
        MinerControllerBlockEntity c = MinerGameTests.build(helper);
        c.setRate(1);
        c.energyBuffer().setTarget(c.powerLimit() * 10);
        BlockPos port = MinerGameTests.portPos(c, dimensionalfluxminer.miner.MinerPorts.Face.WEST_CENTER).west();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        if (!placeAe2Cell(helper, port.subtract(origin))) {
            helper.succeed();
            return;
        }
        helper.succeedWhen(() -> helper.assertTrue(c.energyBuffer().stored() > 0, "Miner filled from AE2"));
    }
    /** FE spent since {@code storedBefore}/{@code debtBefore}, fractions included. */
    private static double spent(DynamicEnergyBuffer buffer, long storedBefore, double debtBefore) {
        return (storedBefore - buffer.stored()) + (buffer.debt() - debtBefore);
    }

    private static void cycle(GameTestHelper helper, AbstractMachineBlockEntity be) {
        AbstractMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(MACHINE), be.getBlockState(), be);
    }

    private static boolean near(double expected, double actual) {
        return Math.abs(expected - actual) <= Math.max(1.0, Math.abs(expected) * 1e-6);
    }

    // ---------------------------------------------------------------- level tables

    /**
     * Optional tables of the ten levels: a speed table and a power table replace the default curve, a table of the wrong
     * size is ignored. The configuration is changed in memory and restored inside one callback, so no other test sees it.
     */
    @GameTest(template = "empty_small")
    public static void levelTablesReplaceTheDefaultCurves(GameTestHelper helper) {
        MechanicalCrusherBlockEntity crusher = CrusherGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            List<Double> speed = List.of(0.01, 0.02, 0.05, 0.1, 0.5, 1.0, 2.0, 8.0, 16.0, 32.0);
            List<Long> power = List.of(500L, 900L, 2_000L, 3_000L, 10_000L, 15_000L, 25_000L, 80_000L, 120_000L, 200_000L);
            try {
                dimensionalfluxminer.DfmConfig.CRUSHER_SPEED_BY_LEVEL.set(speed);
                dimensionalfluxminer.DfmConfig.CRUSHER_POWER_BY_LEVEL.set(power);
                dimensionalfluxminer.DfmConfig.MINER_POWER_BY_LEVEL.set(List.of(1L, 2L, 3L));
                dimensionalfluxminer.DfmConfig.invalidateCaches();
                for (int level = 1; level <= 10; level++) {
                    crusher.setRate(level);
                    helper.assertTrue(MechanicalCrusherBlockEntity.operationsPerTick(level) == speed.get(level - 1), "speed of level " + level);
                    helper.assertTrue(crusher.effectivePowerLimit() == power.get(level - 1),
                            "FE/t of level " + level + " = " + power.get(level - 1) + ", got " + crusher.effectivePowerLimit());
                }
                crusher.upgradeSlot().setStackInSlot(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()));
                helper.assertTrue(crusher.effectivePowerLimit() == 100_000, "the upgrade halves a configured level too");
                crusher.upgradeSlot().setStackInSlot(0, ItemStack.EMPTY);

                // One cycle at level 10 crushes 32 items and pays exactly the level's FE/t.
                crusher.setRate(10);
                crusher.energyBuffer().setStored(PLENTY);
                crusher.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
                long stored = crusher.energyBuffer().stored();
                double debt = crusher.energyBuffer().debt();
                cycle(helper, crusher);
                helper.assertTrue(near(200_000, spent(crusher.energyBuffer(), stored, debt)), "one tick at level 10 costs its FE/t");
                helper.assertTrue(crusher.input().firstCount() == 32, "32 operations per tick, left " + crusher.input().firstCount());

                // A table that does not list the ten levels never mixes with the default curve.
                double defaultPower = MinerControllerBlockEntity.curve().levelPower(10);
                helper.assertTrue(near(12_800_000, defaultPower), "partial Miner table ignored, got " + defaultPower);
            } finally {
                dimensionalfluxminer.DfmConfig.CRUSHER_SPEED_BY_LEVEL.set(List.of());
                dimensionalfluxminer.DfmConfig.CRUSHER_POWER_BY_LEVEL.set(List.of());
                dimensionalfluxminer.DfmConfig.MINER_POWER_BY_LEVEL.set(List.of());
                dimensionalfluxminer.DfmConfig.invalidateCaches();
            }
            helper.assertTrue(MechanicalCrusherBlockEntity.operationsPerTick(10) == 64.0, "defaults restored");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- Crusher

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void crusherChargesTheSameFePerOperationAtEveryRate(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = CrusherGameTests.place(helper);
        ItemStack dust = new ItemStack(DfmItems.DUSTS.get("iron").get());
        helper.runAfterDelay(1, () -> {
            for (boolean upgrade : new boolean[] {false, true}) {
                be.upgradeSlot().setStackInSlot(0, upgrade ? new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()) : ItemStack.EMPTY);
                double cost = upgrade ? 10_000 : 20_000;
                for (int level = 1; level <= 10; level++) {
                    be.setRate(level);
                    double ops = MechanicalCrusherBlockEntity.operationsPerTick(level);
                    helper.assertTrue(be.effectivePowerLimit() == RateScale.powerLimit(ops * cost),
                            "Rate " + level + " FE/t = ops x " + cost + ", got " + be.effectivePowerLimit());
                    be.energyBuffer().setStored(PLENTY);
                    long stored = be.energyBuffer().stored();
                    double debt = be.energyBuffer().debt();
                    double progress = be.workProgress();
                    long done = 0;
                    int calls = (int) Math.min(600, Math.max(1, Math.ceil(2.0 / ops)));
                    for (int i = 0; i < calls; i++) {
                        be.output().clear();
                        be.input().clear();
                        be.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
                        double before = spent(be.energyBuffer(), stored, debt);
                        cycle(helper, be);
                        done += 64 - be.input().firstCount();
                        if (i == 0) {
                            double tick = spent(be.energyBuffer(), stored, debt) - before;
                            helper.assertTrue(near(ops * cost, tick), "Rate " + level + " one tick costs " + ops * cost + ", got " + tick);
                        }
                    }
                    double work = done + be.workProgress() - progress;
                    double perOperation = spent(be.energyBuffer(), stored, debt) / work;
                    helper.assertTrue(done >= 1 && Math.abs(perOperation - cost) < 1e-3 * cost,
                            "Rate " + level + (upgrade ? " (upgrade)" : "") + ": " + perOperation + " FE per operation over " + done);
                    helper.assertTrue(be.output().countOf(dust) <= 64, "products are not billed");
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void crusherWithoutEnoughEnergyKeepsFractionalProgressAndNeverWorksForFree(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = CrusherGameTests.place(helper);
        ItemStack dust = new ItemStack(DfmItems.DUSTS.get("iron").get());
        helper.runAfterDelay(1, () -> {
            be.output().clear();
            be.input().clear();
            be.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
            be.energyBuffer().setDebt(0);
            // Rate 10 asks for 1 280 000 FE/t; 30 000 FE pay for one operation and a half.
            be.energyBuffer().setStored(30_000);
            double progress = be.workProgress();
            cycle(helper, be);
            long made = be.output().countOf(dust);
            double work = made + be.workProgress() - progress;
            helper.assertTrue(near(1.5, work), "1.5 operations of work for 30 000 FE, got " + work);
            helper.assertTrue(be.energyBuffer().available() < 1.0, "everything was spent on that work");
            for (int i = 0; i < 5; i++) {
                cycle(helper, be);
            }
            helper.assertTrue(be.output().countOf(dust) == made, "no product without energy");
            helper.assertTrue("no_energy".equals(be.status()), "status no_energy, got " + be.status());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void crusherAcceleratedCyclesPayEachOneAndNeverDuplicate(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = CrusherGameTests.place(helper);
        ItemStack dust = new ItemStack(DfmItems.DUSTS.get("iron").get());
        helper.runAfterDelay(1, () -> {
            be.output().clear();
            be.input().clear();
            be.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
            be.energyBuffer().setDebt(0);
            // Exactly one Rate-10 cycle of energy: 64 x 20 000 FE.
            be.energyBuffer().setStored(1_280_000);
            cycle(helper, be);
            long first = be.output().countOf(dust);
            be.output().clear();
            be.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
            for (int i = 0; i < 4; i++) {
                cycle(helper, be);
            }
            long more = be.output().countOf(dust);
            helper.assertTrue(first + more <= 64 && first >= 63, "the energy of one cycle crushes one cycle: " + first + " + " + more);
            helper.assertTrue(be.energyBuffer().stored() == 0, "and all of it is spent");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void crusherProgressAndFractionSurviveSaveAndLoad(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = CrusherGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            be.setRate(1);
            be.output().clear();
            be.input().clear();
            be.input().insert(new ItemStack(Items.IRON_INGOT), 8, false);
            be.energyBuffer().setStored(PLENTY);
            for (int i = 0; i < 70; i++) {
                cycle(helper, be);
            }
            var registries = helper.getLevel().registryAccess();
            double progress = be.workProgress();
            long stored = be.energyBuffer().stored();
            double debt = be.energyBuffer().debt();
            CompoundTag saved = be.saveWithoutMetadata(registries);
            be.energyBuffer().setStored(0);
            be.energyBuffer().setDebt(0);
            be.input().clear();
            TestSupport.load(be, saved, registries);
            helper.assertTrue(progress > 0 && Math.abs(be.workProgress() - progress) < 1e-12, "progress kept: " + be.workProgress());
            helper.assertTrue(be.energyBuffer().stored() == stored && Math.abs(be.energyBuffer().debt() - debt) < 1e-12, "FE and its fraction kept");
            helper.assertTrue(be.input().firstCount() == 8, "input kept");
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- Electric Furnace

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void furnaceLanesChargeOnlyTheLanesThatWork(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            be.setMode(ElectricFurnaceMode.NORMAL);
            for (int level : new int[] {1, 5, 8}) {
                be.setRate(level);
                double lane = RateScale.electricFurnaceOperationsPerTick(level, ElectricFurnaceBlockEntity.STANDARD_COOKING_TICKS,
                        ElectricFurnaceBlockEntity.RATE_TEN_OPERATIONS_PER_LANE);
                for (int lanes = 1; lanes <= 3; lanes++) {
                    for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                        be.input(i).clear();
                        be.output(i).clear();
                    }
                    for (int i = 0; i < lanes; i++) {
                        be.input(i).insert(new ItemStack(Items.SAND), 64, false);
                    }
                    be.energyBuffer().setStored(PLENTY);
                    long stored = be.energyBuffer().stored();
                    double debt = be.energyBuffer().debt();
                    cycle(helper, be);
                    double tick = spent(be.energyBuffer(), stored, debt);
                    double expected = lanes * lane * 50_000 / 3.0;
                    helper.assertTrue(near(expected, tick), "Rate " + level + ", " + lanes + " lane(s): " + expected + " FE/t, got " + tick);
                }
            }
            be.setRate(1);
            helper.assertTrue(be.effectivePowerLimit() == 250, "Rate 1 furnace profile: 250 FE/t, got " + be.effectivePowerLimit());
            be.setRate(10);
            helper.assertTrue(be.effectivePowerLimit() == 3_200_000, "Rate 10 furnace profile (3 x 64 per tick): 3 200 000 FE/t, got " + be.effectivePowerLimit());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void furnaceItemCostIsTheSameForEveryDurationAndHalvedByTheUpgrade(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            be.setMode(ElectricFurnaceMode.NORMAL);
            // Sand smelts in 200 ticks; raw iron blasts in 100 ticks (and runs in pairs of 2 raw -> 3 ingots without the upgrade).
            for (boolean upgrade : new boolean[] {false, true}) {
                be.upgradeSlot().setStackInSlot(0, upgrade ? new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()) : ItemStack.EMPTY);
                double cost = (upgrade ? 25_000 : 50_000) / 3.0;
                for (ItemStack item : List.of(new ItemStack(Items.SAND), new ItemStack(Items.RAW_IRON))) {
                    for (int level : new int[] {1, 4, 7, 10}) {
                        be.setRate(level);
                        // Changing the mode clears the progress, so every measure starts with this item alone.
                        be.setMode(ElectricFurnaceMode.ALL);
                        be.setMode(ElectricFurnaceMode.NORMAL);
                        for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                            be.input(i).clear();
                            be.output(i).clear();
                        }
                        be.energyBuffer().setStored(PLENTY);
                        long stored = be.energyBuffer().stored();
                        double debt = be.energyBuffer().debt();
                        double progress = be.laneWorkProgress(0);
                        long items = 0;
                        int calls = level == 1 ? 450 : level == 4 ? 30 : 3;
                        for (int c = 0; c < calls; c++) {
                            be.output(0).clear();
                            be.input(0).clear();
                            be.input(0).insert(item, 64, false);
                            cycle(helper, be);
                            items += 64 - be.input(0).firstCount();
                        }
                        int unitItems = item.is(Items.RAW_IRON) && !upgrade ? 2 : 1;
                        double work = items + (be.laneWorkProgress(0) - progress) * unitItems;
                        double perItem = spent(be.energyBuffer(), stored, debt) / work;
                        helper.assertTrue(items > 0 && Math.abs(perItem - cost) < 1e-3 * cost,
                                item + " at Rate " + level + (upgrade ? " (upgrade)" : "") + ": " + perItem + " FE per item, expected " + cost);
                    }
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void alloyChargesPerExecutionByItsUnitsOfWork(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(be.setMode(ElectricFurnaceMode.ALLOY), "ALLOY mode");
            helper.assertTrue(be.effectivePowerLimit() == 1_920_000, "Rate 10 alloy profile (64 per tick): 1 920 000 FE/t, got " + be.effectivePowerLimit());
            be.setRate(1);
            helper.assertTrue(be.effectivePowerLimit() == 150, "Rate 1 alloy profile: 150 FE/t, got " + be.effectivePowerLimit());
            // An execution asking for two units of an ingredient is two units of work and costs twice as much.
            AlloyingRecipe single = new AlloyingRecipe(List.of(new SizedIngredient(Ingredient.of(Items.IRON_INGOT), 1)),
                    new ItemStack(Items.IRON_BLOCK), Optional.empty(), 200, 0F);
            AlloyingRecipe twice = new AlloyingRecipe(List.of(new SizedIngredient(Ingredient.of(Items.IRON_INGOT), 2),
                    new SizedIngredient(Ingredient.of(Items.COAL), 1)), new ItemStack(Items.IRON_BLOCK), Optional.empty(), 50, 0F);
            helper.assertTrue(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyExecutionEnergy(single, 10, false) == 30_000, "standard execution 30 000 FE");
            helper.assertTrue(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyExecutionEnergy(single, 10, true) == 15_000, "upgrade: 15 000 FE");
            helper.assertTrue(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyWorkUnits(twice) == 2 && dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyExecutionEnergy(twice, 10, false) == 60_000,
                    "two units of work: 60 000 FE, whatever the duration");
            // Steel (100 ticks) run for real: 30 000 FE per execution at every Rate, 15 000 with the upgrade.
            for (boolean upgrade : new boolean[] {false, true}) {
                be.upgradeSlot().setStackInSlot(0, upgrade ? new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()) : ItemStack.EMPTY);
                double cost = upgrade ? 15_000 : 30_000;
                for (int level : new int[] {1, 3, 6, 9}) {
                    be.setRate(level);
                    be.energyBuffer().setStored(PLENTY);
                    long stored = be.energyBuffer().stored();
                    double debt = be.energyBuffer().debt();
                    double progress = be.alloyWorkProgress();
                    long executions = 0;
                    int calls = level == 1 ? 250 : level == 3 ? 60 : 3;
                    for (int c = 0; c < calls; c++) {
                        refillSteel(be);
                        cycle(helper, be);
                        executions += 64 - be.input(0).firstCount();
                    }
                    double work = executions + be.alloyWorkProgress() - progress;
                    double perExecution = spent(be.energyBuffer(), stored, debt) / work;
                    helper.assertTrue(executions > 0 && Math.abs(perExecution - cost) < 1e-3 * cost,
                            "steel at Rate " + level + (upgrade ? " (upgrade)" : "") + ": " + perExecution + " FE per execution");
                }
            }
            helper.succeed();
        });
    }

    private static void refillSteel(ElectricFurnaceBlockEntity be) {
        for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
            be.input(i).clear();
            be.output(i).clear();
        }
        be.input(0).insert(new ItemStack(DfmItems.DUSTS.get("iron").get()), 64, false);
        be.input(1).insert(new ItemStack(DfmItems.DUSTS.get("coal").get()), 64, false);
        be.input(2).insert(new ItemStack(DfmItems.GRAIN_CALCITE.get()), 64, false);
    }

    @GameTest(template = "empty_small")
    public static void hybridModeChargesTheProfileOfTheWorkDone(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(be.mode() == ElectricFurnaceMode.ALL, "alloy + furnace");
            be.setRate(4);
            for (int round = 0; round < 2; round++) {
                // The lanes hold an alloy: only the alloy profile is billed.
                for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                    be.input(i).clear();
                    be.output(i).clear();
                }
                be.input(0).insert(new ItemStack(DfmItems.SLAG_AGGREGATE.get()), 64, false);
                be.energyBuffer().setStored(PLENTY);
                long stored = be.energyBuffer().stored();
                double debt = be.energyBuffer().debt();
                cycle(helper, be);
                double alloy = spent(be.energyBuffer(), stored, debt);
                double expectedAlloy = RateScale.electricFurnaceOperationsPerTick(4, 100, ElectricFurnaceBlockEntity.RATE_TEN_OPERATIONS_PER_LANE) * 30_000;
                helper.assertTrue(near(expectedAlloy, alloy), "alloy profile " + expectedAlloy + ", got " + alloy);
                // The lanes smelt on their own: only the furnace profile is billed.
                be.input(0).clear();
                for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                    be.input(i).insert(new ItemStack(Items.SAND), 64, false);
                }
                stored = be.energyBuffer().stored();
                debt = be.energyBuffer().debt();
                cycle(helper, be);
                double furnace = spent(be.energyBuffer(), stored, debt);
                double expectedFurnace = 3 * RateScale.electricFurnaceOperationsPerTick(4, 200, ElectricFurnaceBlockEntity.RATE_TEN_OPERATIONS_PER_LANE) * 50_000 / 3.0;
                helper.assertTrue(near(expectedFurnace, furnace), "furnace profile " + expectedFurnace + ", got " + furnace);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void electricFurnaceBlockedOutputOrNoEnergySpendsNothing(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            be.setMode(ElectricFurnaceMode.NORMAL);
            for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                be.input(i).clear();
                be.output(i).clear();
                be.output(i).insert(new ItemStack(Items.STONE), 64, false);
            }
            be.input(0).insert(new ItemStack(Items.SAND), 10, false);
            be.energyBuffer().setStored(1_000_000);
            long stored = be.energyBuffer().stored();
            double debt = be.energyBuffer().debt();
            for (int i = 0; i < 5; i++) {
                cycle(helper, be);
            }
            helper.assertTrue(spent(be.energyBuffer(), stored, debt) == 0 && be.input(0).firstCount() == 10, "blocked output: nothing spent");
            helper.assertTrue("output_blocked".equals(be.status()), "status output_blocked, got " + be.status());
            for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                be.output(i).clear();
            }
            be.energyBuffer().setStored(0);
            be.energyBuffer().setDebt(0);
            for (int i = 0; i < 5; i++) {
                cycle(helper, be);
            }
            helper.assertTrue(be.output(0).isEmpty() && be.input(0).firstCount() == 10, "no energy: no glass");
            helper.assertTrue("no_energy".equals(be.status()), "status no_energy, got " + be.status());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void electricFurnaceProgressSurvivesSaveAndLoad(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            be.setMode(ElectricFurnaceMode.NORMAL);
            be.setRate(1);
            for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
                be.input(i).clear();
                be.output(i).clear();
                be.input(i).insert(new ItemStack(Items.SAND), 4, false);
            }
            be.energyBuffer().setStored(PLENTY);
            for (int i = 0; i < 30; i++) {
                cycle(helper, be);
            }
            var registries = helper.getLevel().registryAccess();
            double lane = be.laneWorkProgress(1);
            CompoundTag saved = be.saveWithoutMetadata(registries);
            TestSupport.load(be, new CompoundTag(), registries);
            helper.assertTrue(be.laneWorkProgress(1) == 0, "a save without progress starts from zero");
            TestSupport.load(be, saved, registries);
            helper.assertTrue(lane > 0 && Math.abs(be.laneWorkProgress(1) - lane) < 1e-12, "lane progress kept: " + be.laneWorkProgress(1));
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- Miner

    private static MinerControllerBlockEntity runningMiner(GameTestHelper helper) {
        MinerControllerBlockEntity c = MinerGameTests.build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        MinerGameTests.powerUp(c, 1);
        c.setRunRequested(true);
        // One kept family: every attempt then spends its 2 DM (kept or filtered) and a result never misses a full output,
        // whatever ore mods are installed, so the DM counts the attempts exactly.
        c.filter().add(dimensionalfluxminer.miner.profile.ResourceProfiles.get(helper.getLevel()).families().getFirst().key());
        return c;
    }

    /** One accelerated cycle with an emptied output and a full DM buffer; returns the attempts made (2 DM each). */
    private static long minerCycle(MinerControllerBlockEntity c) {
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
            c.output().setStackInSlot(i, ItemStack.EMPTY);
        }
        c.setDmUnits(MatterValues.BUFFER_UNITS);
        c.acceleratedCycle();
        return (MatterValues.BUFFER_UNITS - c.dmUnits()) / MatterValues.ROLL_COST_UNITS;
    }

    @GameTest(template = "empty_large", timeoutTicks = 200)
    public static void minerChargesTheSameFePerAttemptAtEveryFluxRate(GameTestHelper helper) {
        MinerControllerBlockEntity c = runningMiner(helper);
        long[] expected = {2_500, 6_458, 16_681, 43_089, 111_302, 287_505, 742_654, 1_918_350, 4_955_288, 12_800_000};
        helper.runAfterDelay(1, () -> {
            for (int level = 1; level <= 10; level++) {
                c.setRate(level);
                helper.assertTrue(c.powerLimit() == expected[level - 1], "Flux Rate " + level + ": " + expected[level - 1] + " FE/t, got " + c.powerLimit());
                c.energyBuffer().setStored(PLENTY);
                long stored = c.energyBuffer().stored();
                double debt = c.energyBuffer().debt();
                double progress = c.workProgress();
                double rate = MinerControllerBlockEntity.curve().levelRate(level);
                long attempts = 0;
                int calls = (int) Math.min(400, Math.max(1, Math.ceil(2.0 / rate)));
                for (int i = 0; i < calls; i++) {
                    double before = spent(c.energyBuffer(), stored, debt);
                    attempts += minerCycle(c);
                    if (i == 0) {
                        double tick = spent(c.energyBuffer(), stored, debt) - before;
                        helper.assertTrue(near(rate * 200_000, tick), "Flux Rate " + level + " one cycle costs " + rate * 200_000 + ", got " + tick);
                    }
                }
                double work = attempts + c.workProgress() - progress;
                double perAttempt = spent(c.energyBuffer(), stored, debt) / work;
                helper.assertTrue(attempts > 0 && Math.abs(perAttempt - 200_000) < 0.2, "Flux Rate " + level + ": " + perAttempt + " FE per attempt");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void minerAttemptsOnlyWhatTheEnergyPaysForEvenWhenAccelerated(GameTestHelper helper) {
        MinerControllerBlockEntity c = runningMiner(helper);
        helper.runAfterDelay(1, () -> {
            c.setRate(10);
            c.energyBuffer().setDebt(0);
            // 12 800 000 FE pay for exactly one Rate-10 cycle (64 attempts), however many cycles are requested.
            c.energyBuffer().setStored(12_800_000);
            double progress = c.workProgress();
            long attempts = 0;
            for (int i = 0; i < 6; i++) {
                attempts += minerCycle(c);
            }
            double work = attempts + c.workProgress() - progress;
            helper.assertTrue(Math.abs(work - 64) < 1e-6 && attempts <= 64, "64 attempts of work, got " + work + " (" + attempts + " whole)");
            helper.assertTrue(c.energyBuffer().available() < 1.0, "the energy is all spent");
            // Half an attempt of energy: half an attempt of progress, nothing for free.
            c.energyBuffer().setStored(100_000);
            double half = c.workProgress();
            long more = minerCycle(c);
            double gained = more + c.workProgress() - half;
            helper.assertTrue(Math.abs(gained - 0.5) < 1e-6, "100 000 FE = half an attempt, got " + gained);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void minerFilteredAttemptsStillCostTheirEnergy(GameTestHelper helper) {
        MinerControllerBlockEntity c = runningMiner(helper);
        helper.runAfterDelay(1, () -> {
            var profile = dimensionalfluxminer.miner.profile.ResourceProfiles.get(helper.getLevel());
            c.filter().add(profile.families().getFirst().key());
            c.setRate(10);
            c.energyBuffer().setStored(PLENTY);
            long stored = c.energyBuffer().stored();
            double debt = c.energyBuffer().debt();
            double progress = c.workProgress();
            long attempts = minerCycle(c);
            double work = attempts + c.workProgress() - progress;
            helper.assertTrue(near(work * 200_000, spent(c.energyBuffer(), stored, debt)), "every attempt is billed, kept or filtered");
            helper.assertTrue(attempts == 64, "a Rate-10 cycle makes 64 attempts, got " + attempts);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void minerProgressAndFractionSurviveSaveAndLoad(GameTestHelper helper) {
        MinerControllerBlockEntity c = runningMiner(helper);
        helper.runAfterDelay(1, () -> {
            c.setRate(1);
            c.energyBuffer().setStored(PLENTY);
            for (int i = 0; i < 30; i++) {
                minerCycle(c);
            }
            var registries = helper.getLevel().registryAccess();
            double progress = c.workProgress();
            long stored = c.energyBuffer().stored();
            double debt = c.energyBuffer().debt();
            CompoundTag saved = c.saveWithoutMetadata(registries);
            c.energyBuffer().setStored(0);
            c.energyBuffer().setDebt(0);
            TestSupport.load(c, saved, registries);
            helper.assertTrue(progress > 0 && Math.abs(c.workProgress() - progress) < 1e-12, "progress kept: " + c.workProgress());
            helper.assertTrue(c.energyBuffer().stored() == stored && Math.abs(c.energyBuffer().debt() - debt) < 1e-12, "FE and its fraction kept");
            helper.succeed();
        });
    }
}
