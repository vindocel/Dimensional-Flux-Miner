package dimensionalfluxminer.gametest;

import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.Transfer;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.ItemQueue;
import dimensionalfluxminer.machine.math.DynamicEnergyBuffer;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceMode;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Rate-10 throughput of the Electric Furnace: 64 operations per lane and 192 in total, really reached and sustained when
 * the inputs are fed and the outputs drained. Every slot holds 64 items at most.
 */
public final class ThroughputGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);
    private static final long PLENTY = 1_000_000_000_000L;

    private ThroughputGameTests() {
    }

    private static void cycle(GameTestHelper helper, AbstractMachineBlockEntity be) {
        AbstractMachineBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(MACHINE), be.getBlockState(), be);
    }

    private static double spent(DynamicEnergyBuffer buffer, long storedBefore, double debtBefore) {
        return (storedBefore - buffer.stored()) + (buffer.debt() - debtBefore);
    }

    private static void clearAll(ElectricFurnaceBlockEntity be) {
        for (int i = 0; i < ElectricFurnaceBlockEntity.LANES; i++) {
            be.input(i).clear();
            be.output(i).clear();
        }
    }

    /** Furnace mode, Rate 10, plenty of energy, empty lanes and progress cleared. */
    private static ElectricFurnaceBlockEntity furnace(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        be.setMode(ElectricFurnaceMode.ALLOY);
        be.setMode(ElectricFurnaceMode.NORMAL);
        be.setRate(10);
        clearAll(be);
        be.energyBuffer().setStored(PLENTY);
        return be;
    }

    @GameTest(template = "empty_small")
    public static void eachLaneAloneRuns64PerTick(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                ElectricFurnaceBlockEntity m = furnace(helper);
                long made = 0;
                for (int tick = 0; tick < 20; tick++) {
                    m.input(lane).clear();
                    m.input(lane).insert(new ItemStack(Items.SAND), 64, false);
                    m.output(lane).clear();
                    cycle(helper, m);
                    long glass = m.output(lane).countOf(new ItemStack(Items.GLASS));
                    helper.assertTrue(glass == 64, "lane " + (lane + 1) + " tick " + tick + ": 64 glass, got " + glass);
                    made += glass;
                }
                helper.assertTrue(made == 20 * 64, "lane " + (lane + 1) + " sustained 64 per tick");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void threeLanesSustain192PerTickAt3200000Fe(GameTestHelper helper) {
        ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            ElectricFurnaceBlockEntity be = furnace(helper);
            long stored = be.energyBuffer().stored();
            double debt = be.energyBuffer().debt();
            long made = 0;
            int ticks = 200;
            for (int tick = 0; tick < ticks; tick++) {
                for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                    be.input(lane).clear();
                    be.input(lane).insert(new ItemStack(Items.SAND), 64, false);
                    be.output(lane).clear();
                }
                cycle(helper, be);
                for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                    made += be.output(lane).countOf(new ItemStack(Items.GLASS));
                }
            }
            double perTick = spent(be.energyBuffer(), stored, debt) / ticks;
            helper.assertTrue(made == 192L * ticks, "192 operations per tick over " + ticks + " ticks, got " + made);
            helper.assertTrue(Math.abs(perTick - 3_200_000) < 1, "3 200 000 FE/t, got " + perTick);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void autoInputAndAutoEjectSustain192PerTick(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        be.setMode(ElectricFurnaceMode.NORMAL);
        clearAll(be);
        helper.assertTrue(be.configureSide(RelativeFace.LEFT, Transmission.ITEM, SideRoute.ITEM_INPUT_ALL), "left input");
        helper.assertTrue(be.configureSide(RelativeFace.RIGHT, Transmission.ITEM, SideRoute.ITEM_OUTPUT), "right output");
        BlockPos source = MACHINE.relative(RelativeFace.LEFT.toDirection(be.facing()));
        BlockPos sink = MACHINE.relative(RelativeFace.RIGHT.toDirection(be.facing()));
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(sink, Blocks.CHEST);
        Container in = (Container) TestSupport.blockEntity(helper, source);
        Container out = (Container) TestSupport.blockEntity(helper, sink);
        be.setAutoInput(true);
        be.setAutoEject(true);
        long[] made = {0};
        int[] tick = {0};
        // Every tick: a full chest of sand, an empty output chest and the energy refilled, like real supply and extraction.
        helper.onEachTick(() -> {
            for (int i = 0; i < out.getContainerSize(); i++) {
                ItemStack stack = out.getItem(i);
                if (stack.is(Items.GLASS) && tick[0] >= 20) {
                    made[0] += stack.getCount();
                }
                out.setItem(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < in.getContainerSize(); i++) {
                in.setItem(i, new ItemStack(Items.SAND, 64));
            }
            be.energyBuffer().setStored(be.energyBuffer().target());
            tick[0]++;
        });
        helper.runAfterDelay(120, () -> {
            // 100 measured ticks; one tick of tolerance at each edge of the window.
            long expected = 100L * 192;
            helper.assertTrue(Math.abs(made[0] - expected) <= 192, "about 192 glass per tick through real automation: " + made[0] + " / " + expected);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void rawBonusesStopAtTheSixtyFourItemOutputWithoutLosingItems(GameTestHelper helper) {
        ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            for (boolean upgrade : new boolean[] {false, true}) {
                ElectricFurnaceBlockEntity be = furnace(helper);
                be.upgradeSlot().setStackInSlot(0, upgrade ? new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()) : ItemStack.EMPTY);
                long raw = 0;
                long ingots = 0;
                for (int tick = 0; tick < 20; tick++) {
                    clearAll(be);
                    for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                        be.input(lane).insert(new ItemStack(Items.RAW_IRON), 64, false);
                    }
                    cycle(helper, be);
                    for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                        raw += 64 - be.input(lane).firstCount();
                        long out = be.output(lane).countOf(new ItemStack(Items.IRON_INGOT));
                        helper.assertTrue(out <= 64, "no output slot ever holds more than 64, got " + out);
                        ingots += out;
                    }
                }
                // Each output holds one stack of 64: pairs make 21 x 3 = 63 ingots from 42 raw, the upgrade 32 x 2 = 64
                // from 32 raw; the rest of the raw waits in the input, nothing is lost.
                long rawPerLane = upgrade ? 32 : 42;
                long ingotsPerLane = upgrade ? 64 : 63;
                helper.assertTrue(raw == 20L * 3 * rawPerLane, (upgrade ? "upgrade" : "pairs") + ": " + rawPerLane + " raw per lane per tick, got " + raw);
                helper.assertTrue(ingots == 20L * 3 * ingotsPerLane, (upgrade ? "upgrade" : "pairs") + ": " + ingotsPerLane + " ingots per lane per tick, got " + ingots);
                helper.assertTrue(ingots * 2 == raw * (upgrade ? 4 : 3), "the bonus never changes the operation count");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void alloyRuns64ExecutionsWithItsByproduct(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            be.setMode(ElectricFurnaceMode.ALLOY);
            be.setRate(10);
            be.energyBuffer().setStored(PLENTY);
            long stored = be.energyBuffer().stored();
            double debt = be.energyBuffer().debt();
            long steel = 0;
            long slag = 0;
            int ticks = 50;
            for (int tick = 0; tick < ticks; tick++) {
                clearAll(be);
                be.input(0).insert(new ItemStack(DfmItems.DUSTS.get("iron").get()), 64, false);
                be.input(1).insert(new ItemStack(DfmItems.DUSTS.get("coal").get()), 64, false);
                be.input(2).insert(new ItemStack(DfmItems.GRAIN_CALCITE.get()), 64, false);
                cycle(helper, be);
                steel += be.alloyMain().countOf(new ItemStack(DfmItems.INGOT_STEEL.get()));
                slag += be.alloyByproduct().countOf(new ItemStack(DfmItems.SLAG.get()));
            }
            helper.assertTrue(steel == 64L * ticks && slag == 64L * ticks, "64 steel + 64 slag per tick: " + steel + " / " + slag);
            double perTick = spent(be.energyBuffer(), stored, debt) / ticks;
            helper.assertTrue(Math.abs(perTick - 1_920_000) < 1, "1 920 000 FE/t, got " + perTick);
            // A recipe asking for two units of an ingredient runs half the executions (and costs twice per execution).
            AlloyingRecipe twice = new AlloyingRecipe(List.of(new SizedIngredient(Ingredient.of(Items.IRON_INGOT), 2),
                    new SizedIngredient(Ingredient.of(Items.COAL), 1)), new ItemStack(Items.IRON_BLOCK), Optional.empty(), 200, 0F);
            helper.assertTrue(Math.abs(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyExecutionsPerTick(10, twice) - 32) < 1e-9, "two units: 32 executions per tick");
            helper.assertTrue(Math.abs(dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyExecutionsPerTick(10, twice) * dimensionalfluxminer.furnace.ElectricFurnaceEnergy.alloyExecutionEnergy(twice, 10, false)
                    - 1_920_000) < 1e-3, "same FE/t: 32 x 60 000");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void emptyPartialAndFullOutputSlots(GameTestHelper helper) {
        ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            ElectricFurnaceBlockEntity be = furnace(helper);
            ItemStack glass = new ItemStack(Items.GLASS);
            // The other two outputs are full of something else, so lane 1 can only use its own output.
            be.output(1).insert(new ItemStack(Items.STONE), 64, false);
            be.output(2).insert(new ItemStack(Items.STONE), 64, false);
            // Partly occupied: 40 of 64 taken, so only 24 sand can be smelted; the rest stays in the input.
            be.output(0).insert(glass, 40, false);
            be.input(0).insert(new ItemStack(Items.SAND), 64, false);
            long stored = be.energyBuffer().stored();
            double debt = be.energyBuffer().debt();
            cycle(helper, be);
            helper.assertTrue(be.output(0).countOf(glass) == 64 && be.input(0).firstCount() == 40,
                    "24 smelted into the free room, 40 left: " + be.output(0).countOf(glass) + " / " + be.input(0).firstCount());
            double fe = spent(be.energyBuffer(), stored, debt);
            helper.assertTrue(fe <= 25 * 50_000 / 3.0 + 1, "FE only for the work that fit: " + fe);
            // Full: nothing consumed, nothing spent.
            stored = be.energyBuffer().stored();
            debt = be.energyBuffer().debt();
            for (int i = 0; i < 3; i++) {
                cycle(helper, be);
            }
            helper.assertTrue(be.input(0).firstCount() == 40 && spent(be.energyBuffer(), stored, debt) == 0, "full output: no input or FE used");
            helper.assertTrue("output_blocked".equals(be.status()), "status output_blocked, got " + be.status());
            // Automation sees three ordinary slots of at most 64.
            be.configureSide(RelativeFace.BACK, Transmission.ITEM, SideRoute.ITEM_OUTPUT);
            ItemSlots handler = Transfer.items(helper.getLevel(), helper.absolutePos(MACHINE),
                    RelativeFace.BACK.toDirection(be.facing()));
            // One slot per output first; the cable probe slots of the inputs come after them.
            helper.assertTrue(handler.getSlots() > ElectricFurnaceBlockEntity.LANES, "one slot per output, then the probe slots");
            int taken = 0;
            for (int slot = 0; slot < ElectricFurnaceBlockEntity.LANES; slot++) {
                helper.assertTrue(handler.getSlotLimit(slot) == 64 && handler.getStackInSlot(slot).getCount() <= 64, "slot " + slot + " holds 64 at most");
                taken += handler.extractItem(slot, 64, false).getCount();
            }
            helper.assertTrue(taken == 3 * 64 && be.output(0).isEmpty(), "one pass empties the outputs: " + taken);
            // Empty again: a whole tick of work.
            be.input(0).insert(new ItemStack(Items.SAND), 24, false);
            cycle(helper, be);
            helper.assertTrue(be.output(0).countOf(glass) == 64, "empty output takes a full tick, got " + be.output(0).countOf(glass));
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void acceleratedCyclesEachRunAFull192(GameTestHelper helper) {
        ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            ElectricFurnaceBlockEntity be = furnace(helper);
            long stored = be.energyBuffer().stored();
            double debt = be.energyBuffer().debt();
            long made = 0;
            // Four calls in the same game time: the normal one and three accelerated ones.
            for (int call = 0; call < 4; call++) {
                for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                    be.input(lane).clear();
                    be.input(lane).insert(new ItemStack(Items.SAND), 64, false);
                    be.output(lane).clear();
                }
                cycle(helper, be);
                for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                    made += be.output(lane).countOf(new ItemStack(Items.GLASS));
                }
            }
            helper.assertTrue(made == 4 * 192, "every accelerated cycle runs 192: " + made);
            helper.assertTrue(Math.abs(spent(be.energyBuffer(), stored, debt) - 4 * 3_200_000) < 1, "and pays for it");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void insufficientEnergySlowsDownWithoutLosingItems(GameTestHelper helper) {
        ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            ElectricFurnaceBlockEntity be = furnace(helper);
            be.energyBuffer().setDebt(0);
            // A third of the FE of a Rate-10 tick: about 21 of the 64 items of each lane.
            be.energyBuffer().setStored(3_200_000 / 3);
            for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                be.input(lane).insert(new ItemStack(Items.SAND), 64, false);
            }
            cycle(helper, be);
            long glass = 0;
            long sand = 0;
            for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                glass += be.output(lane).countOf(new ItemStack(Items.GLASS));
                sand += be.input(lane).firstCount();
            }
            helper.assertTrue(glass + sand == 192, "no item lost or created: " + glass + " + " + sand);
            helper.assertTrue(glass >= 60 && glass <= 66, "a third of the work, got " + glass);
            helper.assertTrue(be.energyBuffer().available() < 1, "all the FE used");
            cycle(helper, be);
            long after = 0;
            for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
                after += be.output(lane).countOf(new ItemStack(Items.GLASS));
            }
            helper.assertTrue(after == glass, "nothing more without FE, status " + be.status());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void partlyFilledSlotsSurviveSaveAndLoad(GameTestHelper helper) {
        ElectricFurnaceGameTests.place(helper);
        helper.runAfterDelay(1, () -> {
            ElectricFurnaceBlockEntity be = furnace(helper);
            be.output(0).insert(new ItemStack(Items.GLASS), 50, false);
            be.output(2).insert(new ItemStack(Items.IRON_INGOT), 13, false);
            be.input(1).insert(new ItemStack(Items.SAND), 37, false);
            helper.assertTrue(be.output(0).insert(new ItemStack(Items.GLASS), 100, false) == 14, "a slot never takes more than 64");
            var registries = helper.getLevel().registryAccess();
            CompoundTag saved = be.saveWithoutMetadata(registries);
            clearAll(be);
            TestSupport.load(be, saved, registries);
            helper.assertTrue(be.output(0).countOf(new ItemStack(Items.GLASS)) == 64, "64 glass kept");
            helper.assertTrue(be.output(2).countOf(new ItemStack(Items.IRON_INGOT)) == 13, "13 ingots kept");
            helper.assertTrue(be.input(1).firstCount() == 37, "37 sand kept");
            helper.assertTrue(be.input(0).capacity() == ItemQueue.SLOT_LIMIT && be.output(0).capacity() == ItemQueue.SLOT_LIMIT, "every slot is 64");
            helper.succeed();
        });
    }
}
