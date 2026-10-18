package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElectricFurnaceGameTests {
    private static final BlockPos MACHINE = new BlockPos(2, 1, 2);

    private ElectricFurnaceGameTests() {
    }

    static ElectricFurnaceBlockEntity place(GameTestHelper helper) {
        BlockState state = DfmBlocks.ELECTRIC_FURNACE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        helper.setBlock(MACHINE, state);
        ElectricFurnaceBlockEntity be = helper.getBlockEntity(MACHINE);
        be.setRate(10);
        be.energyBuffer().setTarget(be.effectivePowerLimit() * 10);
        be.energyBuffer().setStored(be.effectivePowerLimit() * 10);
        return be;
    }

    @GameTest(template = "empty_small")
    public static void rawPairsGiveBulkBonusAndBaseXp(GameTestHelper helper) {
        // Furnace copies made by other alloy machines never turn raw ore into an "alloy" without the bonus.
        var raw = new dimensionalfluxminer.recipe.AlloyRecipeInput(new ItemStack(Items.RAW_IRON), ItemStack.EMPTY, ItemStack.EMPTY);
        for (var h : dimensionalfluxminer.recipe.ExternalAlloying.all(helper.getLevel().getRecipeManager(), helper.getLevel().registryAccess())) {
            helper.assertFalse(h.value().matches(raw, helper.getLevel()), "raw iron is no alloy: " + h.id());
        }
        ElectricFurnaceBlockEntity be = place(helper);
        be.input(0).insert(new ItemStack(Items.RAW_IRON), 4, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.input(0).isEmpty(), "raw consumed");
            long ingots = be.output(0).countOf(new ItemStack(Items.IRON_INGOT));
            helper.assertTrue(ingots == 6, "4 raw -> 6 ingots, got " + ingots);
            // Blasting raw iron gives 0.7 XP per input; the bonus never multiplies XP.
            helper.assertTrue(be.experience().milli() == 2800, "XP milli " + be.experience().milli());
        });
    }

    @GameTest(template = "empty_small")
    public static void upgradeGivesTwoIngotsPerRawAndHalvesPower(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.upgradeSlot().setStackInSlot(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()));
        // Furnace profile, three lanes of 64 items/tick at 25 000 / 3 FE per item.
        helper.assertTrue(be.effectivePowerLimit() == 1_600_000, "Rate 10 with upgrade = 1600000 FE/t, got " + be.effectivePowerLimit());
        be.input(1).insert(new ItemStack(Items.RAW_GOLD), 3, false);
        helper.succeedWhen(() -> helper.assertTrue(be.output(1).countOf(new ItemStack(Items.GOLD_INGOT)) == 6, "3 raw gold -> 6 ingots"));
    }

    @GameTest(template = "empty_small")
    public static void steelAlloyIsOneCombinedOperation(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        helper.assertTrue(be.setMode(ElectricFurnaceMode.ALLOY), "switch to ALLOY with empty outputs");
        be.input(0).insert(new ItemStack(DfmItems.DUSTS.get("coal").get()), 2, false);
        be.input(1).insert(new ItemStack(DfmItems.GRAIN_CALCITE.get()), 2, false);
        be.input(2).insert(new ItemStack(DfmItems.DUSTS.get("iron").get()), 2, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.alloyMain().countOf(new ItemStack(DfmItems.INGOT_STEEL.get())) == 2, "2 steel");
            helper.assertTrue(be.alloyByproduct().countOf(new ItemStack(DfmItems.SLAG.get())) == 2, "2 slag");
            helper.assertTrue(be.experience().milli() == 0, "steel recipe gives no XP");
        });
    }

    /** With Create present: one andesite and one iron or zinc nugget make one andesite alloy. */
    @GameTest(template = "empty_small")
    public static void andesiteAlloyFromAndesiteAndANugget(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("create")) {
            helper.succeed();
            return;
        }
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var alloy = registry.get(net.minecraft.resources.ResourceLocation.parse("create:andesite_alloy"));
        var zinc = registry.get(net.minecraft.resources.ResourceLocation.parse("create:zinc_nugget"));
        ElectricFurnaceBlockEntity be = place(helper);
        be.setMode(ElectricFurnaceMode.ALLOY);
        be.input(0).insert(new ItemStack(Items.ANDESITE), 2, false);
        be.input(1).insert(new ItemStack(Items.IRON_NUGGET), 1, false);
        // Once the iron nugget is used, a zinc nugget takes its lane for the second alloy.
        boolean[] zincAdded = {false};
        helper.onEachTick(() -> {
            if (!zincAdded[0] && be.input(1).isEmpty()) {
                zincAdded[0] = true;
                be.input(1).insert(new ItemStack(zinc), 1, false);
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(be.alloyMain().countOf(new ItemStack(alloy)) == 2,
                "2 andesite alloy (iron, then zinc), got " + be.alloyMain().countOf(new ItemStack(alloy))));
    }

    @GameTest(template = "empty_small")
    public static void slagAggregateRecoversSteel(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setMode(ElectricFurnaceMode.ALLOY);
        be.input(0).insert(new ItemStack(DfmItems.SLAG_AGGREGATE.get()), 5, false);
        helper.succeedWhen(() -> helper.assertTrue(be.alloyMain().countOf(new ItemStack(DfmItems.INGOT_STEEL.get())) == 5, "5 steel"));
    }

    @GameTest(template = "empty_small")
    public static void modesCycleAndKeepOutputsReachable(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        helper.assertTrue(be.mode() == ElectricFurnaceMode.ALL, "new electric furnaces start in alloy + furnace");
        be.output(0).insert(new ItemStack(Items.IRON_INGOT), 1, false);
        be.handleButton(null, ElectricFurnaceBlockEntity.BUTTON_MODE);
        helper.assertTrue(be.mode() == ElectricFurnaceMode.ALLOY, "ALL -> ALLOY");
        be.handleButton(null, ElectricFurnaceBlockEntity.BUTTON_MODE);
        helper.assertTrue(be.mode() == ElectricFurnaceMode.NORMAL, "ALLOY -> furnace only");
        be.handleButton(null, ElectricFurnaceBlockEntity.BUTTON_MODE_BACK);
        helper.assertTrue(be.mode() == ElectricFurnaceMode.ALLOY, "back to ALLOY");
        helper.assertTrue(be.configureSide(RelativeFace.BACK, Transmission.ITEM, SideRoute.ITEM_OUTPUT), "output face");
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(MACHINE),
                RelativeFace.BACK.toDirection(be.facing()));
        boolean found = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            found |= handler.extractItem(slot, 1, true).is(Items.IRON_INGOT);
        }
        helper.assertTrue(found, "furnace output still extractable in ALLOY mode");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void lanesUseAnotherOutputHoldingTheirResult(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setMode(ElectricFurnaceMode.NORMAL);
        // The outputs under the log and the beef hold each other's results (the lanes were swapped).
        be.output(0).insert(new ItemStack(Items.COOKED_BEEF), 3, false);
        be.output(1).insert(new ItemStack(Items.CHARCOAL), 3, false);
        be.input(0).insert(new ItemStack(Items.OAK_LOG), 4, false);
        be.input(1).insert(new ItemStack(Items.BEEF), 4, false);
        be.input(2).insert(new ItemStack(Items.RAW_IRON), 2, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.output(1).countOf(new ItemStack(Items.CHARCOAL)) == 7, "log -> charcoal into the slot holding charcoal");
            helper.assertTrue(be.output(0).countOf(new ItemStack(Items.COOKED_BEEF)) == 7, "beef -> cooked beef into the slot holding it");
            helper.assertTrue(be.output(2).countOf(new ItemStack(Items.IRON_INGOT)) == 3, "iron keeps its own slot");
        });
    }

    @GameTest(template = "empty_small")
    public static void freeOutputStaysReservedForTheLaneAboveIt(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setMode(ElectricFurnaceMode.NORMAL);
        be.output(0).insert(new ItemStack(Items.STONE), 1, false);
        be.input(0).insert(new ItemStack(Items.BEEF), 1, false);
        be.input(1).insert(new ItemStack(Items.RAW_IRON), 64, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(be.output(1).isEmpty() || be.output(1).countOf(new ItemStack(Items.IRON_INGOT)) > 0,
                    "the slot under the iron never receives the beef");
            helper.assertTrue(be.output(2).countOf(new ItemStack(Items.COOKED_BEEF)) == 1, "beef goes to the free slot of the idle lane");
        });
    }

    @GameTest(template = "empty_small")
    public static void autoSplitForcesTheDistribution(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setRate(1);
        be.setMode(ElectricFurnaceMode.NORMAL);
        // Items placed directly in one lane (as a manual insert would) are spread once Auto-Split is ON.
        be.input(0).insert(new ItemStack(Items.RAW_COPPER), 56, false);
        be.handleButton(null, ElectricFurnaceBlockEntity.BUTTON_AUTO_SPLIT);
        helper.runAfterDelay(2, () -> {
            long a = be.input(0).firstCount();
            long b = be.input(1).firstCount();
            long c = be.input(2).firstCount();
            helper.assertTrue(a + b + c == 56, "no item lost: " + a + "/" + b + "/" + c);
            helper.assertTrue(b > 0 && c > 0 && Math.max(a, Math.max(b, c)) - Math.min(a, Math.min(b, c)) <= 2,
                    "spread over the three lanes: " + a + "/" + b + "/" + c);
            helper.assertTrue(a % 2 == 0 && b % 2 == 0 && c % 2 == 0, "raw pairs kept: " + a + "/" + b + "/" + c);
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void takingResultsByHandCollectsTheExperience(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        be.output(0).insert(new ItemStack(Items.IRON_INGOT), 3, false);
        be.experience().setMilli(2100);
        be.handleButton(player, ElectricFurnaceBlockEntity.BUTTON_TAKE_OUTPUT);
        helper.assertTrue(be.experience().milli() == 0, "stored XP collected");
        var orbs = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
                player.getBoundingBox().inflate(2));
        // Orbs of the same value may merge into one orb counting several ("Count" in its data).
        int total = orbs.stream().mapToInt(orb -> orb.getValue()
                * Math.max(1, orb.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getInt("Count"))).sum();
        helper.assertTrue(total == 2 || total == 3, "2.1 XP paid as 2 or 3 points, got " + total);
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void rightClickTakesHalfAndInsertsOne(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setRate(1);
        be.setMode(ElectricFurnaceMode.NORMAL);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        be.input(0).insert(new ItemStack(Items.RAW_IRON), 9, false);
        be.handleButton(player, ElectricFurnaceBlockEntity.BUTTON_TAKE_INPUT_HALF);
        helper.assertTrue(player.containerMenu.getCarried().getCount() == 5 && be.input(0).firstCount() == 4, "took half (rounded up)");
        be.handleButton(player, ElectricFurnaceBlockEntity.BUTTON_INSERT_ONE_LANE + 1);
        helper.assertTrue(player.containerMenu.getCarried().getCount() == 4 && be.input(1).firstCount() == 1, "inserted one");
        helper.succeed();
    }

    @GameTest(template = "empty_small", timeoutTicks = 200)
    public static void alloysOfOtherModsRunInTheElectricFurnace(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var types = dimensionalfluxminer.DfmConfig.externalAlloyTypes();
        long originals = manager.getRecipes().stream()
                .filter(h -> types.contains(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(h.value().getType()))).count();
        var external = dimensionalfluxminer.recipe.ExternalAlloying.all(manager, level.registryAccess()).stream()
                .filter(h -> h.id().getPath().startsWith("external/")).toList();
        if (originals == 0) {
            helper.assertTrue(external.isEmpty(), "nothing converted without the other mods");
            helper.succeed();
            return;
        }
        // Furnace copies some alloy machines keep in their own type are left out on purpose, so only check that some convert.
        helper.assertFalse(external.isEmpty(), "alloys of other mods converted (" + originals + " recipes of the configured types)");
        // A converted alloy whose ingredients all exist in this instance, run for real in Alloy Only mode.
        var recipe = external.stream().map(h -> h.value())
                .filter(r -> r.ingredients().stream().allMatch(s -> s.ingredient().getItems().length > 0)
                        && r.ingredients().stream().allMatch(s -> s.count() <= 64))
                .findFirst().orElseThrow();
        ElectricFurnaceBlockEntity be = place(helper);
        be.setMode(ElectricFurnaceMode.ALLOY);
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            var sized = recipe.ingredients().get(i);
            ItemStack stack = sized.ingredient().getItems()[0];
            helper.assertTrue(be.insertLane(i, stack.copyWithCount(sized.count()), false).isEmpty(), "ingredient " + stack + " accepted");
        }
        helper.succeedWhen(() -> helper.assertTrue(be.alloyMain().countOf(recipe.result()) >= recipe.result().getCount(),
                "converted alloy produced " + recipe.result()));
    }

    @GameTest(template = "empty_small")
    public static void allModeSplitsASmeltableAlloyIngredientWithoutPartner(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setRate(1);
        be.setMode(ElectricFurnaceMode.ALL);
        be.setAutoSplit(true);
        ItemStack ironDust = new ItemStack(DfmItems.DUSTS.get("iron").get());
        helper.assertTrue(be.insertAggregate(ironDust.copyWithCount(60), false).isEmpty(), "all iron dust accepted");
        long a = be.input(0).firstCount();
        long b = be.input(1).firstCount();
        long c = be.input(2).firstCount();
        helper.assertTrue(a == 20 && b == 20 && c == 20, "iron dust alone is spread like a furnace item: " + a + "/" + b + "/" + c);
        // With its alloy partner (coal dust) in a lane, iron dust is kept in one lane for the steel alloy.
        for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
            be.input(lane).clear();
        }
        be.input(1).insert(new ItemStack(DfmItems.DUSTS.get("coal").get()), 4, false);
        be.insertAggregate(ironDust.copyWithCount(10), false);
        helper.assertTrue(be.input(0).firstCount() == 10 && be.input(2).isEmpty(), "iron dust kept in one lane next to coal dust");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void creativeMiddleClickPicksAFullStack(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setRate(1);
        be.setMode(ElectricFurnaceMode.NORMAL);
        be.input(0).insert(new ItemStack(Items.RAW_IRON), 3, false);
        var survival = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertFalse(be.handleButton(survival, ElectricFurnaceBlockEntity.BUTTON_PICK_INPUT), "survival cannot pick");
        var creative = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        be.handleButton(creative, ElectricFurnaceBlockEntity.BUTTON_PICK_INPUT);
        ItemStack carried = creative.containerMenu.getCarried();
        helper.assertTrue(carried.is(Items.RAW_IRON) && carried.getCount() == 64, "full stack to the cursor, got " + carried);
        helper.assertTrue(be.input(0).firstCount() == 3, "the machine keeps its items");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void allModeAlloysFirstOtherwiseSmelts(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.input(0).insert(new ItemStack(DfmItems.SLAG_AGGREGATE.get()), 2, false);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(be.alloyMain().countOf(new ItemStack(DfmItems.INGOT_STEEL.get())) == 2, "alloy recipe runs first");
            be.input(1).insert(new ItemStack(Items.RAW_COPPER), 2, false);
            be.input(2).insert(new ItemStack(Items.SAND), 1, false);
            helper.succeedWhen(() -> {
                helper.assertTrue(be.output(1).countOf(new ItemStack(Items.COPPER_INGOT)) == 3, "raw copper pair -> 3 ingots");
                helper.assertTrue(be.output(2).countOf(new ItemStack(Items.GLASS)) == 1, "furnace-only recipe (sand -> glass)");
            });
        });
    }

    @GameTest(template = "empty_small")
    public static void dedicatedAlloyLaneIsInputOnly(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setRate(1);
        be.setMode(ElectricFurnaceMode.ALLOY);
        helper.assertTrue(be.configureSide(RelativeFace.LEFT, Transmission.ITEM, SideRoute.ITEM_INPUT_2), "dedicated lane 2");
        helper.assertTrue(be.configureSide(RelativeFace.LEFT, Transmission.ENERGY, SideRoute.ENERGY_INPUT), "energy on same face");
        helper.assertTrue(be.configureSide(RelativeFace.LEFT, Transmission.EXPERIENCE, SideRoute.XP_OUTPUT), "xp on same face");
        Direction left = RelativeFace.LEFT.toDirection(be.facing());
        BlockPos abs = helper.absolutePos(MACHINE);
        IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, left);
        helper.assertTrue(handler != null, "item handler");
        helper.assertTrue(handler.insertItem(0, new ItemStack(DfmItems.GRAIN_CALCITE.get(), 4), false).isEmpty(), "inserted");
        helper.assertTrue(be.input(1).firstCount() == 4 && be.input(0).isEmpty() && be.input(2).isEmpty(), "went only to lane 2");
        be.alloyMain().insert(new ItemStack(DfmItems.INGOT_STEEL.get()), 3, false);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            helper.assertTrue(handler.extractItem(slot, 64, true).isEmpty(), "dedicated lane never outputs items");
        }
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, abs, left) != null, "energy coexists");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, abs, left) != null, "xp coexists");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void liquidExperienceIsAVirtualView(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.configureSide(RelativeFace.BACK, Transmission.EXPERIENCE, SideRoute.XP_OUTPUT);
        be.experience().setMilli(1234);
        Direction back = RelativeFace.BACK.toDirection(be.facing());
        IFluidHandler fluid = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(MACHINE), back);
        helper.assertTrue(fluid != null, "fluid handler on XP_OUTPUT face");
        helper.assertTrue(fluid.fill(new FluidStack(DfmFluids.LIQUID_EXPERIENCE.get(), 100), IFluidHandler.FluidAction.EXECUTE) == 0, "no XP input");
        FluidStack drained = fluid.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(drained.getAmount() == 24 && drained.is(DfmFluids.LIQUID_EXPERIENCE.get()), "24 mB drained");
        helper.assertTrue(be.experience().milli() == 34, "fraction below 1 mB stays stored");
        helper.assertTrue(DfmFluids.LIQUID_EXPERIENCE.get().defaultFluidState().is(net.minecraft.tags.FluidTags.create(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "experience"))), "#c:experience");
        helper.succeed();
    }

    private static net.minecraft.nbt.CompoundTag menuData(GameTestHelper helper, ElectricFurnaceBlockEntity be) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        be.writeMenuData(tag, helper.getLevel().registryAccess());
        return tag;
    }

    @GameTest(template = "empty_small")
    public static void itemAndFluidAutomationAreIndependent(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        // The item mode cycle (OFF -> input -> eject -> both) never touches the fluid Auto Eject.
        for (int i = 0; i < 4; i++) {
            be.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_IO_NEXT);
            helper.assertFalse(menuData(helper, be).getBoolean("auto_eject_fluid"), "item cycle leaves fluids off");
        }
        helper.assertTrue(be.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_EJECT_FLUID), "fluid toggle");
        var data = menuData(helper, be);
        helper.assertTrue(data.getBoolean("auto_eject_fluid") && !data.getBoolean("auto_input") && !data.getBoolean("auto_eject"),
                "fluid Auto Eject on, items still off");
        be.handleButton(null, AbstractMachineBlockEntity.BUTTON_AUTO_EJECT_FLUID);
        helper.assertFalse(menuData(helper, be).getBoolean("auto_eject_fluid"), "fluid toggle back off");

        // Items eject on their own: results leave, the liquid XP stays stored while the fluid Auto Eject is off.
        be.configureSide(RelativeFace.BACK, Transmission.ITEM, SideRoute.ITEM_OUTPUT);
        be.configureSide(RelativeFace.BACK, Transmission.EXPERIENCE, SideRoute.XP_OUTPUT);
        BlockPos chest = MACHINE.relative(RelativeFace.BACK.toDirection(be.facing()));
        helper.setBlock(chest, net.minecraft.world.level.block.Blocks.CHEST);
        be.output(0).insert(new ItemStack(Items.IRON_INGOT), 10, false);
        be.experience().setMilli(5000);
        be.setAutoEject(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(be.output(0).isEmpty(), "items ejected into the chest");
            helper.assertTrue(be.experience().milli() == 5000, "XP not moved by the item Auto Eject");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void oldSavesKeepTheirFluidAutoEject(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ElectricFurnaceBlockEntity be = place(helper);
        be.setAutoEject(true);
        net.minecraft.nbt.CompoundTag old = be.saveWithoutMetadata(registries);
        old.remove("auto_eject_fluid");
        be.loadWithComponents(old, registries);
        helper.assertTrue(menuData(helper, be).getBoolean("auto_eject_fluid"), "a save from before the split ejected XP with the items");
        be.setAutoEjectFluid(false);
        be.loadWithComponents(be.saveWithoutMetadata(registries), registries);
        var data = menuData(helper, be);
        helper.assertTrue(data.getBoolean("auto_eject") && !data.getBoolean("auto_eject_fluid"), "new saves keep each setting");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void activeElectricFurnaceEmitsLight14(GameTestHelper helper) {
        ElectricFurnaceBlockEntity be = place(helper);
        be.setRate(1);
        be.input(0).insert(new ItemStack(Items.RAW_COPPER), 64, false);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getLevel().getLightEmission(helper.absolutePos(MACHINE)) == 14, "ACTIVE -> light 14");
            helper.succeed();
        });
    }
}
