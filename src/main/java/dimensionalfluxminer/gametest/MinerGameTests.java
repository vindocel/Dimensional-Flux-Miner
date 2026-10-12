package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.miner.MinerAssembly;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerLayout.PortRole;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.miner.MinerStatus;
import dimensionalfluxminer.miner.MinerStructure;
import dimensionalfluxminer.miner.corruption.CorruptionService;
import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MinerGameTests {
    private static final BlockPos TERMINAL = new BlockPos(4, 1, 5);

    private MinerGameTests() {
    }

    /** Places the 27 blocks for a south-facing terminal and forms the machine. */
    static MinerControllerBlockEntity build(GameTestHelper helper) {
        BlockPos terminalAbs = helper.absolutePos(TERMINAL);
        MinerStructure layout = MinerStructure.layout(terminalAbs, Direction.SOUTH);
        for (MinerStructure.Member m : layout.members()) {
            if (m.slot().part() == Part.MINER_TERMINAL) {
                continue;
            }
            helper.setBlock(TERMINAL.offset(m.pos().subtract(terminalAbs)), MinerStructure.blockFor(m.slot().part()));
        }
        helper.setBlock(TERMINAL, DfmBlocks.MINER_TERMINAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        MinerControllerBlockEntity controller = helper.getBlockEntity(TERMINAL);
        var problem = controller.tryForm();
        helper.assertTrue(controller.formed(), "structure should form: " + (problem == null ? "no message" : problem.getString()));
        return controller;
    }

    static void powerUp(MinerControllerBlockEntity c, int rate) {
        c.setRate(rate);
        c.energyBuffer().setTarget(c.powerLimit() * 10);
        c.energyBuffer().setStored(c.powerLimit() * 10);
    }

    static BlockPos port(GameTestHelper helper, MinerControllerBlockEntity c, PortRole role) {
        return c.structure().port(role).orElseThrow().pos();
    }

    @GameTest(template = "empty_large")
    public static void formsAndExposesPortsOnOutwardFacesOnly(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        for (MinerStructure.Member m : c.structure().members()) {
            helper.assertTrue(helper.getLevel().getBlockState(m.pos()).getValue(DfmStateProperties.FORMED), "participant formed at " + m.pos());
        }
        MinerStructure.Member energy = c.structure().port(PortRole.ENERGY_INPUT).orElseThrow();
        helper.assertTrue(energy.outward() == Direction.WEST, "south front -> energy faces west");
        helper.assertTrue(c.structure().port(PortRole.ITEM_OUTPUT).orElseThrow().outward() == Direction.EAST, "output faces east");
        helper.assertTrue(c.structure().port(PortRole.MATTER_INPUT).orElseThrow().outward() == Direction.NORTH, "matter faces north");
        var level = helper.getLevel();
        helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, energy.pos(), Direction.WEST) != null, "energy on outward face");
        helper.assertTrue(level.getCapability(Capabilities.EnergyStorage.BLOCK, energy.pos(), Direction.UP) == null, "no energy on other faces");
        BlockPos out = port(helper, c, PortRole.ITEM_OUTPUT);
        IItemHandler outHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, out, Direction.EAST);
        helper.assertTrue(outHandler != null && outHandler.getSlots() == 27, "27-slot extraction view");
        helper.assertTrue(!outHandler.insertItem(0, new ItemStack(Items.STONE), false).isEmpty(), "output never accepts insertion");
        // No Core: the matter input capability is unavailable, the core slot is never exposed.
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, port(helper, c, PortRole.MATTER_INPUT), Direction.NORTH) == null, "matter input needs Stabilized");
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.BASIC_DIMENSIONAL_CORE.get()));
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, port(helper, c, PortRole.MATTER_INPUT), Direction.NORTH) == null, "Basic keeps matter input disabled");
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK, port(helper, c, PortRole.MATTER_INPUT), Direction.NORTH) != null, "Stabilized enables matter input");
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void terminalClicksPlaceTheMissingPartsFromTheInventory(GameTestHelper helper) {
        helper.setBlock(TERMINAL, DfmBlocks.MINER_TERMINAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        BlockPos terminalAbs = helper.absolutePos(TERMINAL);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos away = helper.absolutePos(TERMINAL.south(4));
        player.moveTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
        var empty = MinerAssembly.placeNext(helper.getLevel(), terminalAbs, player);
        helper.assertTrue(empty != null && MinerAssembly.missing(helper.getLevel(), terminalAbs).size() == 26,
                "nothing placed without items");
        java.util.Map<Part, Integer> needed = new java.util.EnumMap<>(Part.class);
        for (MinerStructure.Member m : MinerStructure.layout(terminalAbs, Direction.SOUTH).members()) {
            if (m.slot().part() != Part.MINER_TERMINAL) {
                needed.merge(m.slot().part(), 1, Integer::sum);
            }
        }
        needed.forEach((part, count) -> player.getInventory().add(new ItemStack(MinerStructure.blockFor(part), count)));
        for (int click = 0; click < 26; click++) {
            MinerAssembly.placeNext(helper.getLevel(), terminalAbs, player);
        }
        MinerControllerBlockEntity controller = helper.getBlockEntity(TERMINAL);
        helper.assertTrue(controller.formed(), "the 26 clicks build and form the machine");
        helper.assertTrue(player.getInventory().isEmpty(), "every part was taken from the inventory");
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void sneakClickInstallsCoreAndUpgradeThroughAnyPart(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos chassis = c.structure().members().stream().filter(m -> m.slot().part() == Part.STEEL_CHASSIS)
                .findFirst().orElseThrow().pos();
        ItemStack cores = new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get(), 2);
        ItemStack upgrades = new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get(), 3);
        dimensionalfluxminer.machine.QuickInstall.install(player, helper.getLevel(), chassis, cores);
        dimensionalfluxminer.machine.QuickInstall.install(player, helper.getLevel(), chassis, upgrades);
        helper.assertTrue(c.coreSlot().getStackInSlot(0).is(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()) && cores.getCount() == 1,
                "core installed from the held stack");
        helper.assertTrue(c.upgradeSlot().getStackInSlot(0).is(DfmItems.EFFICIENCY_UPGRADE.get()) && upgrades.getCount() == 2,
                "upgrade installed from the held stack");
        dimensionalfluxminer.machine.QuickInstall.install(player, helper.getLevel(), chassis, cores);
        helper.assertTrue(cores.getCount() == 1 && c.coreSlot().getStackInSlot(0).getCount() == 1, "an occupied slot takes nothing");
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void autoInputFillsTheMatterBufferWhileTheMachineIsOff(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        c.setAutoInput(true);
        MinerStructure.Member port = c.structure().port(PortRole.MATTER_INPUT).orElseThrow();
        BlockPos chestPos = port.pos().relative(port.outward());
        helper.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos);
        for (int i = 0; i < 4; i++) {
            chest.setItem(i, new ItemStack(Items.STONE, 64));
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(c.status() == MinerStatus.MANUAL_STOP, "the machine stays off: " + c.status());
            helper.assertTrue(chest.isEmpty() && c.dmUnits() > 0, "the stone was pulled while off, DM units " + c.dmUnits());
        });
    }

    @GameTest(template = "empty_large")
    public static void acceleratedMinerRunsAtFullRateWithAnEndlessSource(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 10);
        c.setAutoInput(true);
        c.setRunRequested(true);
        // One kept family: the other rolls still spend DM but never need the output, so only matter can limit.
        c.filter().add(ResourceProfiles.get(helper.getLevel()).families().getFirst().key());
        MinerStructure.Member port = c.structure().port(PortRole.MATTER_INPUT).orElseThrow();
        BlockPos chestPos = port.pos().relative(port.outward());
        helper.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos);
        // Each tick: an endless stone source, an emptied output, full energy and ten accelerated cycles. With no item
        // limit, matter always keeps up: only energy could hold the machine back.
        Runnable refill = () -> {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                chest.setItem(i, new ItemStack(Items.STONE, 64));
            }
        };
        helper.onEachTick(() -> {
            for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
                c.output().setStackInSlot(i, ItemStack.EMPTY);
            }
            c.energyBuffer().setStored(c.powerLimit() * 10);
            // Refilled before every cycle, like a creative storage: the source never runs dry.
            for (int i = 0; i < 10; i++) {
                refill.run();
                c.acceleratedCycle();
            }
            refill.run();
            c.energyBuffer().setStored(c.powerLimit() * 10);
        });
        helper.runAfterDelay(4, () -> {
            var data = new net.minecraft.nbt.CompoundTag();
            c.writeMenuData(data);
            helper.assertTrue("NONE".equals(data.getString("limit")), "nothing limits it: " + data.getString("limit"));
            double nominal = data.getDouble("nominal");
            helper.assertTrue(Math.abs(data.getDouble("potential") - nominal * 11) < 1e-6,
                    "potential counts the 11 cycles: " + data.getDouble("potential") + " for " + nominal);
            helper.assertTrue(Math.abs(data.getDouble("actual") - nominal * 11) < 1e-6,
                    "actual sums every cycle at full rate: " + data.getDouble("actual"));
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void redstoneHaltFreezesAutoInput(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        c.setAutoInput(true);
        c.setRedstoneControl(true);
        MinerStructure.Member port = c.structure().port(PortRole.MATTER_INPUT).orElseThrow();
        BlockPos chestPos = port.pos().relative(port.outward());
        helper.getLevel().setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.STONE, 64));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(chest.getItem(0).getCount() == 64 && c.dmUnits() == 0, "no pull without the redstone signal");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void newMachineIsStoppedAndRedstoneNeverStartsIt(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 1);
        c.setDmUnits(1000);
        helper.assertFalse(c.runRequested(), "default STOPPED");
        helper.setBlock(TERMINAL.north().north().west().west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(c.status() == MinerStatus.MANUAL_STOP, "redstone cannot start a stopped miner: " + c.status());
            c.setRedstoneControl(true);
            c.setRunRequested(true);
            helper.setBlock(TERMINAL.north().north().west().west(), Blocks.AIR);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(c.status() == MinerStatus.REDSTONE_HALT, "no signal -> REDSTONE HALT: " + c.status());
                helper.assertTrue(c.runRequested(), "halt keeps runRequested");
                // Signal on a lower-layer receiver (the chassis next to it).
                helper.setBlock(TERMINAL.west().west(), Blocks.REDSTONE_BLOCK);
                helper.succeedWhen(() -> helper.assertTrue(c.status() == MinerStatus.RUNNING, "signal resumes: " + c.status()));
            });
        });
    }

    @GameTest(template = "empty_large")
    public static void matterInputConvertsSubstrateImmediately(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        IItemHandler matter = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, port(helper, c, PortRole.MATTER_INPUT), Direction.NORTH);
        helper.assertTrue(matter.insertItem(0, new ItemStack(Items.STONE, 10), false).isEmpty(), "stone accepted");
        helper.assertTrue(c.dmUnits() == 50, "10 stone = 5.0 DM, got " + c.dmUnits());
        helper.assertTrue(matter.insertItem(0, new ItemStack(Items.DIRT, 10), false).isEmpty(), "dirt accepted");
        helper.assertTrue(c.dmUnits() == 60, "10 dirt = 1.0 DM");
        helper.assertTrue(matter.insertItem(0, new ItemStack(Items.OAK_PLANKS, 3), false).getCount() == 3, "invalid substrate rejected");
        c.upgradeSlot().setStackInSlot(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()));
        matter.insertItem(0, new ItemStack(Items.DIRT, 1), false);
        helper.assertTrue(c.dmUnits() == 66, "dirt with upgrade = 0.6 DM (no cap on matter input)");
        // Only what fits in the 5 000 DM buffer is accepted; the rest stays with the source.
        c.setDmUnits(MatterValues.BUFFER_UNITS - 25);
        ItemStack rest = matter.insertItem(0, new ItemStack(Items.STONE, 64), false);
        helper.assertTrue(rest.getCount() == 62 && c.dmUnits() == MatterValues.BUFFER_UNITS - 5, "partial acceptance, got rest " + rest.getCount());
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void productionSpendsTwoDmPerRollAndOutputsOreBlocks(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 10);
        c.setDmUnits(200);
        c.setRunRequested(true);
        helper.runAfterDelay(2, () -> {
            int produced = 0;
            ResourceProfile profile = ResourceProfiles.get(helper.getLevel());
            for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
                ItemStack stack = c.output().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    produced += stack.getCount();
                    helper.assertTrue(profile.familyOf(stack.getItem()).isPresent(), "output is a profile ore block: " + stack);
                }
            }
            helper.assertTrue(produced == 10, "200 units = 10 rolls, produced " + produced);
            helper.assertTrue(c.dmUnits() == 0, "DM consumed: " + c.dmUnits());
            helper.assertTrue(c.status() == MinerStatus.WAITING_FOR_MATTER, "then waits for matter: " + c.status());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void filterKeepsSelectedFamiliesAndRecoversCappedDm(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        c.upgradeSlot().setStackInSlot(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()));
        powerUp(c, 10);
        ResourceProfile profile = ResourceProfiles.get(helper.getLevel());
        helper.assertTrue(profile.families().size() >= 2, "overworld profile has several families: " + profile.families().size());
        ResourceFamily kept = profile.families().getFirst();
        c.filter().add(kept.key());
        c.setDmUnits(2000);
        c.setRunRequested(true);
        helper.runAfterDelay(1, () -> {
            for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
                ItemStack stack = c.output().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    helper.assertTrue(kept.variants().stream().anyMatch(v -> v.item() == stack.getItem()), "only the selected family is kept: " + stack);
                }
            }
            // Each roll costs 20 units and a rejected one returns at most 20: DM can never grow from filtering.
            helper.assertTrue(c.dmUnits() <= 2000, "no net DM gain from filter recovery");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void nearlyFullOutputKeepsWorkingAndOnlyPaysForKeptResults(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 10);
        c.setDmUnits(2000);
        ResourceProfile profile = ResourceProfiles.get(helper.getLevel());
        ResourceFamily family = profile.families().stream().max(java.util.Comparator.comparingInt(ResourceFamily::weight)).orElseThrow();
        ItemStack room = new ItemStack(family.icon());
        // Every slot taken: only the last one, holding one ore of the heaviest family, still has room.
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS - 1; i++) {
            c.output().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        c.output().setStackInSlot(MinerControllerBlockEntity.OUTPUT_SLOTS - 1, room.copy());
        c.setRunRequested(true);
        helper.runAfterDelay(1, () -> {
            int produced = c.output().getStackInSlot(MinerControllerBlockEntity.OUTPUT_SLOTS - 1).getCount() - 1;
            helper.assertTrue(c.status() == MinerStatus.RUNNING, "rolls that do not fit do not stop it: " + c.status());
            helper.assertTrue(c.visualStage().processing(), "animation keeps running");
            helper.assertTrue(c.dmUnits() == 2000 - produced * MatterValues.ROLL_COST_UNITS,
                    "only kept results are paid: produced " + produced + ", DM " + c.dmUnits());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large", timeoutTicks = 900)
    public static void lostRollsKeepThePaceSoTheOreThatFitsIsNotFarmed(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 1);
        c.setDmUnits(2000);
        ResourceProfile profile = ResourceProfiles.get(helper.getLevel());
        // Only the rarest ore still fits: lost rolls retried at once would yield one of it every 80 ticks.
        ResourceFamily rarest = profile.families().stream().min(java.util.Comparator.comparingInt(ResourceFamily::weight)).orElseThrow();
        double chance = rarest.weight() / (double) profile.totalWeight();
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS - 1; i++) {
            c.output().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        c.output().setStackInSlot(MinerControllerBlockEntity.OUTPUT_SLOTS - 1, new ItemStack(rarest.icon()));
        c.setRunRequested(true);
        helper.onEachTick(() -> c.energyBuffer().setStored(c.powerLimit() * 10));
        // Rate 1 rolls once every 80 ticks: 10 rolls in 800 ticks, each keeping its own chance (about 0.3 rare ores
        // expected); retrying lost rolls would yield one about every 120 ticks (6 or more).
        helper.runAfterDelay(800, () -> {
            int produced = c.output().getStackInSlot(MinerControllerBlockEntity.OUTPUT_SLOTS - 1).getCount() - 1;
            helper.assertTrue(chance < 0.15, "test needs a rare family, chance " + chance);
            helper.assertTrue(produced <= 2, "lost rolls must not be retried at once: produced " + produced);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void acceleratedCyclesThatDrainTheBufferStillShowAWorkingMachine(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        // Rate 1: the matter lasts the whole test even with ten accelerated cycles per game time.
        powerUp(c, 1);
        c.setDmUnits(2000);
        c.setRunRequested(true);
        // Every game time: the accelerated cycles use all the energy before the normal tick of the terminal.
        helper.onEachTick(() -> {
            c.energyBuffer().setStored(c.powerLimit() * 10);
            for (int i = 0; i < 10; i++) {
                c.acceleratedCycle();
            }
            c.energyBuffer().setStored(0);
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(c.visualStage().processing(), "working machine looks working: " + c.visualStage());
            helper.assertTrue(c.status() == MinerStatus.RUNNING, "status RUNNING, got " + c.status());
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void energyInputAlsoFeedsTheAcceleratedCycles(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.setRate(10);
        c.energyBuffer().setStored(0);
        for (int i = 0; i < 20; i++) {
            c.acceleratedCycle();
        }
        MinerStructure.Member port = c.structure().port(PortRole.ENERGY_INPUT).orElseThrow();
        IEnergyStorage input = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, port.pos(), port.outward());
        long cycle = c.powerLimit();
        long accepted = input.receiveEnergy(Integer.MAX_VALUE, false);
        helper.assertTrue(accepted == 10 * cycle + 20 * cycle,
                "one refill covers the 10 x FE/t buffer and the 20 accelerated cycles: " + accepted + " for " + cycle + " FE/t");
        helper.assertTrue(c.energyBuffer().target() == 10 * cycle, "the buffer itself is unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void fullOutputBlocksBeforeSpendingAnything(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 10);
        c.setDmUnits(1000);
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
            c.output().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        long fe = c.energyBuffer().stored();
        c.setRunRequested(true);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(c.status() == MinerStatus.OUTPUT_BLOCKED, "OUTPUT BLOCKED: " + c.status());
            helper.assertTrue(c.dmUnits() == 1000 && c.energyBuffer().stored() == fe, "no DM/FE consumed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void outputPortHasNoItemLimitPerTick(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
            c.output().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        IItemHandler out = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, port(helper, c, PortRole.ITEM_OUTPUT), Direction.EAST);
        int taken = 0;
        for (int slot = 0; slot < out.getSlots(); slot++) {
            taken += out.extractItem(slot, 64, false).getCount();
        }
        helper.assertTrue(taken == MinerControllerBlockEntity.OUTPUT_SLOTS * 64, "the whole buffer leaves in one tick, got " + taken);
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void lowerChassisAccelerationRunsExtraCyclesTerminalDoesNot(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        powerUp(c, 10);
        c.setRunRequested(true);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
                c.output().setStackInSlot(i, ItemStack.EMPTY);
            }
            c.setDmUnits(MatterValues.BUFFER_UNITS);
            c.energyBuffer().setStored(c.energyBuffer().target());
            var level = helper.getLevel();
            BlockPos chassis = c.structure().members().stream().filter(m -> m.slot().accelerationReceiver()).findFirst().orElseThrow().pos();
            MinerLinkBlockEntity link = (MinerLinkBlockEntity) level.getBlockEntity(chassis);
            int before = countOutput(c);
            MinerLinkBlockEntity.serverTick(level, chassis, level.getBlockState(chassis), link);
            MinerLinkBlockEntity.serverTick(level, chassis, level.getBlockState(chassis), link);
            int afterChassis = countOutput(c);
            helper.assertTrue(afterChassis - before >= 64, "extra chassis call = one more 64-roll cycle, got " + (afterChassis - before));
            BlockPos terminal = helper.absolutePos(TERMINAL);
            MinerControllerBlockEntity.serverTick(level, terminal, level.getBlockState(terminal), c);
            MinerControllerBlockEntity.serverTick(level, terminal, level.getBlockState(terminal), c);
            helper.assertTrue(countOutput(c) - afterChassis <= 64, "terminal calls never add accelerated cycles");
            helper.succeed();
        });
    }

    private static int countOutput(MinerControllerBlockEntity c) {
        int n = 0;
        for (int i = 0; i < MinerControllerBlockEntity.OUTPUT_SLOTS; i++) {
            n += c.output().getStackInSlot(i).getCount();
        }
        return n;
    }

    @GameTest(template = "empty_large")
    public static void breakingAPartDropsContentsOnceAndStops(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        c.output().setStackInSlot(0, new ItemStack(Items.IRON_ORE, 7));
        powerUp(c, 3);
        c.setRunRequested(true);
        BlockPos vent = c.structure().members().stream().filter(m -> m.slot().part() == Part.VENTILATION_UNIT).findFirst().orElseThrow().pos();
        helper.getLevel().setBlock(vent, Blocks.AIR.defaultBlockState(), 3);
        helper.assertFalse(c.formed(), "invalidated");
        helper.assertFalse(c.runRequested(), "forced STOPPED");
        helper.assertTrue(c.energyBuffer().stored() == 0 && c.dmUnits() == 0, "FE and DM lost");
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(9, 6, 9);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
        long cores = drops.stream().filter(e -> e.getItem().is(DfmItems.STABILIZED_DIMENSIONAL_CORE.get())).mapToInt(e -> e.getItem().getCount()).sum();
        long ores = drops.stream().filter(e -> e.getItem().is(Items.IRON_ORE)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(cores == 1 && ores == 7, "core and output dropped exactly once: cores=" + cores + " ores=" + ores);
        for (MinerStructure.Member m : MinerStructure.layout(helper.absolutePos(TERMINAL), Direction.SOUTH).members()) {
            var state = helper.getLevel().getBlockState(m.pos());
            if (state.hasProperty(DfmStateProperties.FORMED)) {
                helper.assertFalse(state.getValue(DfmStateProperties.FORMED), "renders restored at " + m.pos());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_large", skyAccess = true)
    public static void columnCorruptionFollowsDepthRules(GameTestHelper helper) {
        // Column A: short grass over grass / dirt / stone / stone (the 4th block stays normal).
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.DIRT);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.SHORT_GRASS);
        // Column B: a flower becomes a wither rose and does not consume depth.
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.DIRT);
        helper.setBlock(new BlockPos(3, 3, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(3, 4, 1), Blocks.POPPY);
        // Column C: two water layers over sand and dirt: water, sand (into sediment) and dirt converted.
        helper.setBlock(new BlockPos(5, 1, 1), Blocks.DIRT);
        helper.setBlock(new BlockPos(5, 2, 1), Blocks.SAND);
        helper.setBlock(new BlockPos(5, 3, 1), Blocks.WATER);
        helper.setBlock(new BlockPos(5, 4, 1), Blocks.WATER);
        // Column D: ground covered by a tree and a roof is corrupted through the cover.
        helper.setBlock(new BlockPos(7, 1, 1), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(7, 2, 1), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(7, 3, 1), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(7, 4, 1), Blocks.OAK_PLANKS);
        // Column E: natural ground that does not corrupt (sandstone) is not drainable.
        helper.setBlock(new BlockPos(1, 3, 3), Blocks.SANDSTONE);
        // Column F: a fern (a weed) becomes corrupted grass; Column G: gravel becomes corrupted sediment.
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.FERN);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.GRAVEL);

        var level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(1, 0, 1));
        BlockPos b = helper.absolutePos(new BlockPos(3, 0, 1));
        BlockPos cPos = helper.absolutePos(new BlockPos(5, 0, 1));
        BlockPos d = helper.absolutePos(new BlockPos(7, 0, 1));
        BlockPos e = helper.absolutePos(new BlockPos(1, 0, 3));
        BlockPos f = helper.absolutePos(new BlockPos(3, 0, 3));
        BlockPos g = helper.absolutePos(new BlockPos(5, 0, 3));
        helper.assertFalse(CorruptionService.plan(level, e.getX(), e.getZ()).drainable(), "sandstone column is not drainable");
        helper.assertTrue(CorruptionService.corrupt(level, d.getX(), d.getZ(), null), "covered column D corrupted");
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS.get(), new BlockPos(7, 1, 1));
        helper.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(7, 2, 1));
        helper.assertTrue(CorruptionService.corrupt(level, f.getX(), f.getZ(), null), "column F corrupted");
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS_PLANT.get(), new BlockPos(3, 3, 3));
        helper.assertTrue(CorruptionService.corrupt(level, g.getX(), g.getZ(), null), "column G corrupted");
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_SEDIMENT.get(), new BlockPos(5, 2, 3));
        helper.assertFalse(CorruptionService.corrupt(level, g.getX(), g.getZ(), null), "corrupted sediment is never corrupted again");
        helper.assertTrue(CorruptionService.corrupt(level, a.getX(), a.getZ(), null), "column A corrupted");
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS.get(), new BlockPos(1, 3, 1));
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_DIRT.get(), new BlockPos(1, 2, 1));
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_STONE.get(), new BlockPos(1, 1, 1));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 0, 1));
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_GRASS_PLANT.get(), new BlockPos(1, 4, 1));
        helper.assertTrue(CorruptionService.corrupt(level, b.getX(), b.getZ(), null), "column B corrupted");
        helper.assertBlockPresent(Blocks.WITHER_ROSE, new BlockPos(3, 4, 1));
        helper.assertTrue(CorruptionService.corrupt(level, cPos.getX(), cPos.getZ(), null), "column C corrupted");
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_WATER.get(), new BlockPos(5, 4, 1));
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_WATER.get(), new BlockPos(5, 3, 1));
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_SEDIMENT.get(), new BlockPos(5, 2, 1));
        helper.assertBlockPresent(DfmBlocks.CORRUPTED_DIRT.get(), new BlockPos(5, 1, 1));
        helper.assertFalse(CorruptionService.corrupt(level, a.getX(), a.getZ(), null), "an already corrupted column gives nothing");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void resourceProfileFamiliesAreWeightedAndCrushable(GameTestHelper helper) {
        ResourceProfile profile = ResourceProfiles.get(helper.getLevel());
        int total = profile.totalWeight();
        DimensionalFluxMiner.LOGGER.info("Resource Profile {} (total {}): {}", profile.dimension().location(), total,
                profile.families().stream().map(f -> f.key() + "=" + f.weight() + "->" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(f.crusherOutput())).toList());
        helper.assertTrue(Math.abs(total - 10_000) <= profile.families().size(), "weights normalized to ~10000: " + total);
        for (ResourceFamily family : profile.families()) {
            helper.assertTrue(family.weight() >= 1, "minimum weight 1");
            for (ResourceFamily.Variant v : family.variants()) {
                var resolution = dimensionalfluxminer.crusher.CrusherProcessing.resolve(helper.getLevel(), new ItemStack(v.item()), false);
                helper.assertTrue(resolution.isPresent() && resolution.get().ore() && resolution.get().result().getCount() == 2,
                        "every producible ore is processed by the Crusher (2x): " + v.item());
            }
        }
        helper.assertTrue(profile.family(net.minecraft.resources.ResourceLocation.withDefaultNamespace("iron")).isPresent(), "iron family present");
        helper.succeed();
    }

    /** Runs alone: the Basic drain corrupts terrain inside its 300-block radius. */
    @GameTest(template = "empty_large", batch = "basic_drain", timeoutTicks = 200)
    public static void basicCoreSurveysDrainsAndProduces(GameTestHelper helper) {
        MinerControllerBlockEntity c = build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.BASIC_DIMENSIONAL_CORE.get()));
        powerUp(c, 10);
        c.setRunRequested(true);
        helper.assertFalse(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                port(helper, c, PortRole.MATTER_INPUT), Direction.NORTH) != null, "Basic never exposes matter input");
        helper.succeedWhen(() -> {
            helper.assertTrue(countOutput(c) > 0, "Basic produced from drained matter (status " + c.status() + ")");
            // Every produced roll was paid with DM from corrupted columns (64 DM each), never from free matter.
            helper.assertTrue(dimensionalfluxminer.miner.corruption.BlightData.get(helper.getLevel()).chunks().size() > 0,
                    "corrupted columns recorded for the Blight");
        });
    }

    @GameTest(template = "empty_large", timeoutTicks = 120)
    public static void corruptedWaterHealsMonsters(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), DfmBlocks.CORRUPTED_WATER.get());
                helper.setBlock(new BlockPos(x, 2, z), DfmBlocks.CORRUPTED_WATER.get());
            }
        }
        Spider spider = helper.spawn(EntityType.SPIDER, new BlockPos(4, 1, 4));
        spider.setHealth(4.0F);
        helper.succeedWhen(() -> helper.assertTrue(spider.getHealth() > 4.0F, "monster healed in corrupted water"));
    }

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void corruptedWaterSinksThroughTheWaterBelowIt(GameTestHelper helper) {
        // A 1 x 1 shaft: dirt floor, a two-block kelp, three water sources, Corrupted Water on top.
        for (int y = 0; y <= 7; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        helper.setBlock(new BlockPos(2 + dx, y, 2 + dz), Blocks.STONE);
                    }
                }
            }
        }
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.DIRT);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.KELP_PLANT);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.KELP);
        for (int y = 3; y <= 5; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.WATER);
        }
        helper.setBlock(new BlockPos(2, 6, 2), DfmBlocks.CORRUPTED_WATER.get());
        helper.succeedWhen(() -> {
            for (int y = 3; y <= 6; y++) {
                helper.assertBlockPresent(DfmBlocks.CORRUPTED_WATER.get(), new BlockPos(2, y, 2));
            }
            // The kelp stays, now holding Corrupted Water.
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_KELP.get(), new BlockPos(2, 2, 2));
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_KELP_PLANT.get(), new BlockPos(2, 1, 2));
            helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(new BlockPos(2, 1, 2))).getType()
                    .isSame(dimensionalfluxminer.registry.DfmFluids.CORRUPTED_WATER.get()), "kelp holds Corrupted Water");
        });
    }

    @GameTest(template = "empty_large")
    public static void tallSeagrassKeepsBothHalvesWhenCorrupted(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.DIRT);
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.TALL_SEAGRASS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.TALL_SEAGRASS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        helper.assertTrue(CorruptionService.corruptWaterAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4))), "upper half converted");
        helper.runAfterDelay(3, () -> {
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_TALL_SEAGRASS.get(), new BlockPos(4, 1, 4));
            helper.assertBlockPresent(DfmBlocks.CORRUPTED_TALL_SEAGRASS.get(), new BlockPos(4, 2, 4));
            helper.succeed();
        });
    }

    @GameTest(template = "empty_large")
    public static void corruptedWaterHurtsPassiveMobs(GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), DfmBlocks.CORRUPTED_WATER.get());
                helper.setBlock(new BlockPos(x, 2, z), DfmBlocks.CORRUPTED_WATER.get());
            }
        }
        var cow = helper.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        float full = cow.getMaxHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.isInWater(), "corrupted water counts as water (splash, swimming, sounds)");
            helper.assertTrue(cow.getHealth() < full, "passive mob hurt by corrupted water");
        });
    }

    @GameTest(template = "empty_small")
    public static void corruptedSedimentFallsAndSmeltsIntoGlass(GameTestHelper helper) {
        var level = helper.getLevel();
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(DfmItems.CORRUPTED_SEDIMENT.get()));
        var recipe = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
        helper.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input, level.registryAccess()).is(Items.GLASS),
                "corrupted sediment smelts into glass");
        helper.assertTrue(DfmBlocks.CORRUPTED_SEDIMENT.get().defaultBlockState().is(net.minecraft.tags.BlockTags.SAND), "it is sand");
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 4, 2), DfmBlocks.CORRUPTED_SEDIMENT.get());
        helper.succeedWhen(() -> helper.assertBlockPresent(DfmBlocks.CORRUPTED_SEDIMENT.get(), new BlockPos(2, 2, 2)));
    }

    /** Own batch: neighbouring tests clearing their area could otherwise discard the mob mid-test. */
    @GameTest(template = "empty_large", batch = "corrupted_water_drowned")
    public static void waterBreathingMobsDoNotDrownInCorruptedWater(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                for (int y = 0; y <= 5; y++) {
                    boolean inside = x >= 3 && x <= 5 && z >= 3 && z <= 5 && y >= 1 && y <= 4;
                    helper.setBlock(new BlockPos(x, y, z), inside ? DfmBlocks.CORRUPTED_WATER.get() : Blocks.STONE);
                }
            }
        }
        var drowned = helper.spawn(EntityType.DROWNED, new BlockPos(4, 1, 4));
        drowned.setPersistenceRequired();
        drowned.setAirSupply(0);
        // Out of air, a mob that drowns loses air below 0 and takes drowning damage after 20 ticks; a drowned does not.
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(drowned.isAlive() && drowned.getAirSupply() >= 0 && drowned.getLastDamageSource() == null,
                    "a drowned breathes in Corrupted Water like in water, air " + drowned.getAirSupply() + ", damage "
                            + (drowned.getLastDamageSource() == null ? "none" : drowned.getLastDamageSource().getMsgId()));
            helper.succeed();
        });
    }

    /** Own batch: neighbouring tests clearing their area could otherwise discard the Warden mid-test. */
    @GameTest(template = "empty_large", batch = "corrupted_water_warden")
    public static void corruptedWaterHealsTheWardenOneHeartAtATime(GameTestHelper helper) {
        // A closed 3 x 3 x 4 tank: the Warden stays fully in Corrupted Water.
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                for (int y = 0; y <= 5; y++) {
                    boolean inside = x >= 3 && x <= 5 && z >= 3 && z <= 5 && y >= 1 && y <= 4;
                    helper.setBlock(new BlockPos(x, y, z), inside ? DfmBlocks.CORRUPTED_WATER.get() : Blocks.STONE);
                }
            }
        }
        var warden = helper.spawn(EntityType.WARDEN, new BlockPos(4, 1, 4));
        // The test server has no player: without this the Warden may despawn in the middle of the test.
        warden.setPersistenceRequired();
        // Spawned without the normal spawn setup, a Warden has no dig cooldown and digs away (vanilla) almost at once.
        warden.getBrain().setMemoryWithExpiry(net.minecraft.world.entity.ai.memory.MemoryModuleType.DIG_COOLDOWN,
                net.minecraft.util.Unit.INSTANCE, 1200L);
        warden.setHealth(100.0F);
        // Each heal is checked on its own (the test server may stop ticking the Warden before the end of a fixed
        // window): every heal is one heart (2 HP) and two heals are exactly 10 game ticks apart.
        java.util.List<long[]> heals = new java.util.ArrayList<>();
        float[] last = {100.0F};
        helper.onEachTick(() -> {
            float health = warden.getHealth();
            if (health != last[0]) {
                heals.add(new long[] {helper.getLevel().getGameTime(), Math.round((health - last[0]) * 10)});
                last[0] = health;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(heals.size() >= 4, "four heals seen, got " + heals.size() + " removed " + warden.getRemovalReason()
                    + " alive " + warden.isAlive() + " health " + warden.getHealth() + " ticks " + warden.tickCount + " air " + warden.getAirSupply()
                    + " pos " + helper.relativePos(warden.blockPosition()) + " inCW " + warden.isInFluidType(dimensionalfluxminer.registry.DfmFluids.CORRUPTED_WATER_TYPE.get())
                    + " pose " + warden.getPose());
            for (int i = 0; i < heals.size(); i++) {
                helper.assertTrue(heals.get(i)[1] == 20, "each heal is one heart (2 HP), got " + heals.get(i)[1] / 10.0);
                if (i > 0) {
                    long gap = heals.get(i)[0] - heals.get(i - 1)[0];
                    helper.assertTrue(gap == 10, "heals 10 ticks apart, got " + gap);
                }
            }
        });
    }
}
