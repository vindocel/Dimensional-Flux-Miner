package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.RedstoneMode;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrusherGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private CrusherGameTests() {
    }

    static MechanicalCrusherBlockEntity place(GameTestHelper helper) {
        BlockState state = DfmBlocks.MECHANICAL_CRUSHER.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        helper.setBlock(MACHINE, state);
        MechanicalCrusherBlockEntity be = helper.getBlockEntity(MACHINE);
        be.setRate(10);
        be.energyBuffer().setTarget(be.effectivePowerLimit() * 10);
        be.energyBuffer().setStored(be.effectivePowerLimit() * 10);
        return be;
    }

    @GameTest(template = "empty_small")
    public static void crushesIronIngotIntoDust(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        be.input().insert(new ItemStack(Items.IRON_INGOT), 10, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.output().countOf(new ItemStack(DfmItems.DUSTS.get("iron").get())) == 10, "expected 10 iron dust");
            helper.assertTrue(be.input().isEmpty(), "input should be consumed");
        });
    }

    @GameTest(template = "empty_small")
    public static void oreYieldsTwoOrThreeWithUpgrade(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        be.input().insert(new ItemStack(Items.IRON_ORE), 4, false);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.output().countOf(new ItemStack(Items.RAW_IRON)) == 8, "4 ores -> 8 raw iron");
            be.output().clear();
            be.upgradeSlot().setStackInSlot(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()));
            be.input().insert(new ItemStack(Items.DIAMOND_ORE), 4, false);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(be.output().countOf(new ItemStack(Items.DIAMOND)) == 12, "4 diamond ores -> 12 diamonds with upgrade");
                helper.assertTrue(be.effectivePowerLimit() == 640_000, "upgrade halves FE at Rate 10 (64 x 10 000)");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty_small")
    public static void sneakClickInstallsTheUpgrade(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack upgrades = new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get(), 2);
        BlockPos pos = helper.absolutePos(MACHINE);
        helper.assertTrue(dimensionalfluxminer.machine.QuickInstall.install(player, helper.getLevel(), pos,
                new ItemStack(DfmItems.BASIC_DIMENSIONAL_CORE.get())) == null, "a machine takes no core");
        dimensionalfluxminer.machine.QuickInstall.install(player, helper.getLevel(), pos, upgrades);
        helper.assertTrue(be.upgradeSlot().getStackInSlot(0).is(DfmItems.EFFICIENCY_UPGRADE.get()) && upgrades.getCount() == 1,
                "upgrade installed from the held stack");
        dimensionalfluxminer.machine.QuickInstall.install(player, helper.getLevel(), pos, upgrades);
        helper.assertTrue(upgrades.getCount() == 1, "the slot holds one upgrade");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void metalDustsSmeltIntoTheInstalledIngots(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getRecipeManager();
        for (String metal : java.util.List.of("iron", "gold", "copper", "tin", "lead", "silver", "uranium", "aluminum", "nickel", "osmium", "zinc")) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "ingots/" + metal));
            boolean hasIngot = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tag).map(set -> set.size() > 0).orElse(false);
            var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(DfmItems.DUSTS.get(metal).get()));
            for (var type : java.util.List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING, net.minecraft.world.item.crafting.RecipeType.BLASTING)) {
                var recipe = recipes.getRecipeFor(type, input, level);
                if (!hasIngot) {
                    helper.assertTrue(recipe.isEmpty(), metal + " dust has no " + type + " recipe without an ingot");
                    continue;
                }
                helper.assertTrue(recipe.isPresent(), metal + " dust has a " + type + " recipe");
                ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
                helper.assertTrue(result.is(tag), metal + " dust " + type + " gives a c:ingots/" + metal + " item, got " + result);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void outputBlockedConsumesNothing(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        // Fill all output kinds so nothing new fits.
        be.output().insert(new ItemStack(Items.STONE), 96, false);
        be.output().insert(new ItemStack(Items.DIRT), 96, false);
        be.output().insert(new ItemStack(Items.SAND), 96, false);
        be.output().insert(new ItemStack(Items.GRAVEL), 96, false);
        be.input().insert(new ItemStack(Items.COAL), 5, false);
        long energyBefore = be.energyBuffer().stored();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(be.input().firstCount() == 5, "input must not be consumed while output is blocked");
            helper.assertTrue(be.energyBuffer().stored() == energyBefore, "no operational FE while output is blocked");
            helper.assertTrue("output_blocked".equals(be.status()), "status should be output_blocked");
            helper.assertBlockProperty(MACHINE, DfmStateProperties.MACHINE_STATE, MachineState.IDLE);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void sideRoutesExposeOnlyConfiguredCapabilities(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        BlockPos abs = helper.absolutePos(MACHINE);
        // Fresh machine: every face except front and top accepts energy; the top is a fixed item input; front exposes nothing.
        // The configurable faces have an item handler for the cables to find, but with no route it moves nothing.
        Direction front = be.facing();
        be.output().insert(new ItemStack(Items.GRAVEL), 4, false);
        for (Direction dir : Direction.values()) {
            boolean energyExpected = dir != front && dir != Direction.UP;
            helper.assertTrue((helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, abs, dir) != null) == energyExpected,
                    "default energy on " + dir);
            IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, dir);
            helper.assertTrue((items != null) == (dir != front), "item handler on " + dir);
            if (items != null && dir != Direction.UP) {
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    helper.assertTrue(items.extractItem(slot, 64, false).isEmpty(), "no route: nothing leaves through " + dir);
                    helper.assertTrue(items.insertItem(slot, new ItemStack(Items.COAL), false).getCount() == 1, "no route: nothing enters through " + dir);
                }
            }
        }
        be.output().clear();
        IItemHandler top = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, Direction.UP);
        helper.assertTrue(top.insertItem(0, new ItemStack(Items.COAL), true).isEmpty(), "top accepts inputs");
        helper.assertFalse(be.configureSide(RelativeFace.UP, Transmission.ITEM, SideRoute.ITEM_OUTPUT), "top never outputs");
        helper.assertFalse(be.configureSide(RelativeFace.FRONT, Transmission.ITEM, SideRoute.ITEM_INPUT_ALL), "front is locked");
        helper.assertFalse(be.configureSide(RelativeFace.UP, Transmission.ENERGY, SideRoute.ENERGY_INPUT), "top has no energy");
        Direction left = RelativeFace.LEFT.toDirection(be.facing());
        helper.assertTrue(be.configureSide(RelativeFace.LEFT, Transmission.ITEM, SideRoute.ITEM_INPUT_OUTPUT_ALL), "left item route");
        helper.assertTrue(be.configureSide(RelativeFace.LEFT, Transmission.ENERGY, SideRoute.ENERGY_INPUT), "left energy route");

        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, left);
        IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, abs, left);
        helper.assertTrue(handler != null && energy != null, "left exposes item + energy at once");
        helper.assertTrue(energy.canReceive() && !energy.canExtract(), "energy is input-only");
        // Invalid process inputs are rejected; the upgrade is never swallowed as input.
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "dirt is not a valid input");
        helper.assertTrue(handler.insertItem(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()), true).getCount() == 1, "upgrade is not an input");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.COAL, 3), false).isEmpty(), "coal is accepted");
        // Unprocessed input can never be extracted through the combined route.
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            helper.assertTrue(handler.extractItem(slot, 64, true).isEmpty() || !handler.getStackInSlot(slot).is(Items.COAL), "input not extractable");
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small", timeoutTicks = 400)
    public static void cablesThatPullOrPushTakeTheRoute(GameTestHelper helper) {
        // As on the Miner ports: a cable that keeps pulling from a face gets the item output, one that pushes the input.
        MechanicalCrusherBlockEntity be = place(helper);
        be.output().insert(new ItemStack(Items.GRAVEL), 8, false);
        var level = helper.getLevel();
        BlockPos abs = helper.absolutePos(MACHINE);
        Direction left = RelativeFace.LEFT.toDirection(be.facing());
        Direction back = RelativeFace.BACK.toDirection(be.facing());
        helper.assertTrue(pull(level.getCapability(Capabilities.ItemHandler.BLOCK, abs, left)) == 0, "nothing leaves before the route");
        helper.assertTrue(push(level.getCapability(Capabilities.ItemHandler.BLOCK, abs, back), new ItemStack(Items.COAL)) == 0,
                "nothing enters before the route");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.route(RelativeFace.LEFT, Transmission.ITEM) == SideRoute.ITEM_OUTPUT, "the pulling cable got the output");
            helper.assertTrue(be.route(RelativeFace.BACK, Transmission.ITEM) == SideRoute.ITEM_INPUT_ALL, "the pushing cable got the input");
            int both = (1 << RelativeFace.LEFT.ordinal()) | (1 << RelativeFace.BACK.ordinal());
            helper.assertTrue(be.cableSides() == both && be.sides().locked(RelativeFace.LEFT, Transmission.ITEM)
                    && be.sides().locked(RelativeFace.BACK, Transmission.ITEM), "locked while the cables are active");
            helper.assertFalse(be.sides().cycle(RelativeFace.LEFT, Transmission.ITEM, be.itemProfile(), true), "a locked face does not cycle");
            helper.assertTrue(pull(level.getCapability(Capabilities.ItemHandler.BLOCK, abs, left)) > 0, "the pulling cable now gets items");
            helper.assertTrue(push(level.getCapability(Capabilities.ItemHandler.BLOCK, abs, back), new ItemStack(Items.COAL)) > 0,
                    "the pushing cable now feeds the input");
        });
        // Quiet cables free the faces; the routes stay.
        helper.runAfterDelay(20 + dimensionalfluxminer.machine.CableActivity.ACTIVE_TICKS + 20, () -> {
            helper.assertTrue(be.cableSides() == 0 && !be.sides().locked(RelativeFace.LEFT, Transmission.ITEM), "freed");
            helper.assertTrue(be.route(RelativeFace.LEFT, Transmission.ITEM) == SideRoute.ITEM_OUTPUT
                    && be.route(RelativeFace.BACK, Transmission.ITEM) == SideRoute.ITEM_INPUT_ALL, "routes kept");
            helper.succeed();
        });
    }

    /** What a pulling cable takes from every slot of a face. */
    static int pull(IItemHandler handler) {
        int taken = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            taken += handler.extractItem(slot, 64, false).getCount();
        }
        return taken;
    }

    /** What a pushing cable puts into a face, trying every slot. */
    static int push(IItemHandler handler, ItemStack stack) {
        ItemStack left = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
            left = handler.insertItem(slot, left, false);
        }
        return stack.getCount() - left.getCount();
    }

    @GameTest(template = "empty_small")
    public static void extraTickerCallsRunExtraLogicalCycles(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        helper.runAfterDelay(2, () -> {
            be.output().clear();
            be.input().clear();
            ItemStack dust = new ItemStack(DfmItems.DUSTS.get("iron").get());
            be.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
            be.energyBuffer().setStored(be.energyBuffer().target());
            BlockPos abs = helper.absolutePos(MACHINE);
            // Two calls within the same game time: both run a complete Rate-10 cycle (64 operations each).
            // Every slot holds 64 items, so the first result is taken out and the input refilled in between.
            AbstractMachineBlockEntity.serverTick(helper.getLevel(), abs, be.getBlockState(), be);
            long produced = be.output().countOf(dust);
            be.output().clear();
            be.input().insert(new ItemStack(Items.IRON_INGOT), 64, false);
            AbstractMachineBlockEntity.serverTick(helper.getLevel(), abs, be.getBlockState(), be);
            produced += be.output().countOf(dust);
            helper.assertTrue(produced == 128, "two logical cycles in one game time, got " + produced);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void redstoneModeGatesOperation(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_REDSTONE_NEXT);
        helper.assertTrue(be.redstoneMode() == RedstoneMode.HIGH, "IGNORED -> HIGH");
        be.input().insert(new ItemStack(Items.COAL), 5, false);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.input().firstCount() == 5, "HIGH without signal -> halted");
            helper.assertTrue("redstone_halt".equals(be.status()), "status redstone_halt");
            // Any side of the block counts (here the front, which has no side configuration at all).
            BlockPos front = MACHINE.relative(be.facing());
            helper.setBlock(front, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
            helper.succeedWhen(() -> helper.assertTrue(be.input().isEmpty(), "signal allows processing"));
        });
    }

    @GameTest(template = "empty_small")
    public static void invertedRedstoneHaltsWhilePowered(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_REDSTONE_PREVIOUS);
        helper.assertTrue(be.redstoneMode() == RedstoneMode.LOW, "IGNORED -> LOW going back");
        helper.setBlock(MACHINE.relative(be.facing().getOpposite()), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        be.input().insert(new ItemStack(Items.COAL), 5, false);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.input().firstCount() == 5, "LOW while powered -> halted");
            helper.setBlock(MACHINE.relative(be.facing().getOpposite()), net.minecraft.world.level.block.Blocks.AIR);
            helper.succeedWhen(() -> helper.assertTrue(be.input().isEmpty(), "no signal allows processing"));
        });
    }

    @GameTest(template = "empty_small")
    public static void ingotsAndGemsCrushIntoTheirDust(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        var level = helper.getLevel();
        helper.assertTrue(dimensionalfluxminer.crusher.CrusherProcessing.resolve(level, new ItemStack(Items.GOLD_INGOT), false)
                .map(r -> r.result().is(DfmItems.DUSTS.get("gold").get()) && r.result().getCount() == 1).orElse(false), "gold ingot -> gold dust");
        helper.assertTrue(dimensionalfluxminer.crusher.CrusherProcessing.resolve(level, new ItemStack(Items.DIAMOND), false)
                .map(r -> r.result().is(DfmItems.DUSTS.get("diamond").get())).orElse(false), "diamond -> diamond dust");
        helper.assertTrue(dimensionalfluxminer.crusher.CrusherProcessing.resolve(level, new ItemStack(Items.RAW_COPPER), false)
                .map(r -> r.result().is(DfmItems.DUSTS.get("copper").get())).orElse(false), "raw copper -> copper dust");
        helper.assertTrue(dimensionalfluxminer.crusher.CrusherProcessing.resolve(level, new ItemStack(DfmItems.DUSTS.get("gold").get()), false)
                .isEmpty(), "a dust is never crushed again");
        be.input().insert(new ItemStack(Items.GOLD_INGOT), 5, false);
        helper.succeedWhen(() -> helper.assertTrue(be.output().countOf(new ItemStack(DfmItems.DUSTS.get("gold").get())) == 5, "5 gold dust"));
    }

    @GameTest(template = "empty_small")
    public static void modOreFeaturesAreReadFromTheirConfiguration(GameTestHelper helper) {
        // Every ore feature, vanilla or with its own configuration type from another mod, yields its target blocks.
        var registries = helper.getLevel().registryAccess();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registries);
        var features = registries.registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
        int checked = 0;
        for (var entry : features.entrySet()) {
            var feature = entry.getValue();
            boolean declaresTargets = net.minecraft.world.level.levelgen.feature.ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, feature).result()
                    .filter(json -> json.isJsonObject() && json.getAsJsonObject().get("config") instanceof com.google.gson.JsonObject config
                            && config.get("targets") instanceof com.google.gson.JsonArray targets && !targets.isEmpty()
                            && targets.get(0) instanceof com.google.gson.JsonObject first && first.has("state"))
                    .isPresent();
            if (!declaresTargets) {
                continue;
            }
            var shape = dimensionalfluxminer.miner.profile.ProfileBuilder.oreShape(feature, ops);
            helper.assertTrue(shape.isPresent() && !shape.get().targets().isEmpty() && shape.get().size() > 0,
                    "ore targets of " + entry.getKey().location());
            checked++;
        }
        helper.assertTrue(checked > 0, "vanilla ore features checked");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void slotsHoldSixtyFourAndFullOutputSpillsThenStops(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        ItemStack dust = new ItemStack(DfmItems.DUSTS.get("iron").get());
        helper.assertTrue(be.input().insert(new ItemStack(Items.IRON_INGOT), 100, false) == 64, "input slot holds 64");
        be.input().clear();
        helper.assertTrue(be.output().insert(dust, 100, false) == 100, "100 dust fit");
        helper.assertTrue(be.output().kinds() == 2 && be.output().count(0) == 64 && be.output().count(1) == 36,
                "a full slot continues in the next free slot");
        helper.assertTrue(be.output().insert(dust, 1000, false) == 156, "four slots hold 256");
        be.input().insert(new ItemStack(Items.IRON_INGOT), 10, false);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.input().firstCount() == 10, "all output slots full stops the machine");
            helper.assertTrue("output_blocked".equals(be.status()), "status output_blocked, got " + be.status());
            be.output().remove(dust, 64, false);
            helper.succeedWhen(() -> helper.assertTrue(be.input().isEmpty(), "freed output lets it run again"));
        });
    }

    @GameTest(template = "empty_small")
    public static void sidesUsedByANetworkNodeAreLocked(GameTestHelper helper) {
        // What a Logistics Networks node on the machine sets (read from its channels when the mod is installed).
        MechanicalCrusherBlockEntity be = place(helper);
        be.sides().set(RelativeFace.LEFT, Transmission.ITEM, 0, SideRoute.ITEM_OUTPUT);
        be.sides().setLocks(Transmission.ITEM, 1 << RelativeFace.LEFT.ordinal());
        int left = AbstractMachineBlockEntity.BUTTON_SIDE_BASE + RelativeFace.LEFT.ordinal() * 16 + Transmission.ITEM.ordinal() * 2;
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertFalse(be.handleButton(player, left), "a node's side does not cycle");
        be.handleButton(player, AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + Transmission.ITEM.ordinal() * 2);
        be.handleButton(player, AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL);
        helper.assertTrue(be.sides().get(RelativeFace.LEFT, Transmission.ITEM, 0) == SideRoute.ITEM_OUTPUT, "batch and clear keep it");
        be.sides().setLocks(Transmission.ITEM, 0);
        helper.assertTrue(be.handleButton(player, left), "free again without the node");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void rateCycleAndSideBatchButtons(GameTestHelper helper) {
        MechanicalCrusherBlockEntity be = place(helper);
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE);
        helper.assertTrue(be.rate() == 1, "x10 wraps to x1");
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE_BACK);
        helper.assertTrue(be.rate() == 10, "x1 wraps back to x10");

        int item = AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + Transmission.ITEM.ordinal() * 2;
        be.handleButton(null, item);
        helper.assertTrue(be.sides().get(RelativeFace.BACK, Transmission.ITEM, 0) == SideRoute.ITEM_INPUT_ALL, "batch: NONE -> input");
        helper.assertTrue(be.sides().get(RelativeFace.FRONT, Transmission.ITEM, 0) == SideRoute.NONE, "front never configurable");
        be.handleButton(null, item);
        helper.assertTrue(be.sides().get(RelativeFace.DOWN, Transmission.ITEM, 0) == SideRoute.ITEM_OUTPUT, "batch: input -> output");
        be.handleButton(null, item + 1);
        helper.assertTrue(be.sides().get(RelativeFace.LEFT, Transmission.ITEM, 0) == SideRoute.ITEM_INPUT_ALL, "batch back: output -> input");
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_SIDE_BASE + RelativeFace.LEFT.ordinal() * 16 + Transmission.ITEM.ordinal() * 2);
        be.handleButton(null, item);
        helper.assertTrue(be.sides().get(RelativeFace.RIGHT, Transmission.ITEM, 0) == SideRoute.NONE, "differing faces batch to NONE");

        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + Transmission.ENERGY.ordinal() * 2);
        helper.assertTrue(!be.sides().anyFace(Transmission.ENERGY, 0, SideRoute.ENERGY_INPUT), "energy input wraps to NONE");
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + Transmission.ENERGY.ordinal() * 2);
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL);
        helper.assertTrue(!be.sides().anyFace(Transmission.ENERGY, 0, SideRoute.ENERGY_INPUT), "Shift clear resets every transmission");
        helper.assertTrue(be.sides().get(RelativeFace.UP, Transmission.ITEM, 0) == SideRoute.ITEM_INPUT_ALL, "fixed top input is kept");

        // Automation button: OFF -> input -> eject -> both -> OFF; right click goes back.
        boolean[][] expected = {{true, false}, {false, true}, {true, true}, {false, false}};
        for (boolean[] step : expected) {
            be.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_IO_NEXT);
            CompoundTag data = new CompoundTag();
            be.writeMenuData(data, helper.getLevel().registryAccess());
            helper.assertTrue(data.getBoolean("auto_input") == step[0] && data.getBoolean("auto_eject") == step[1],
                    "automation cycle step " + java.util.Arrays.toString(step));
        }
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_IO_PREVIOUS);
        CompoundTag back = new CompoundTag();
        be.writeMenuData(back, helper.getLevel().registryAccess());
        helper.assertTrue(back.getBoolean("auto_input") && back.getBoolean("auto_eject"), "OFF goes back to input + eject");
        helper.succeed();
    }
}