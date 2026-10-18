package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionalFluxMiner.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {
    private ContentGameTests() {
    }

    /** Every terrain conversion is data driven: each corruptible tag holds the vanilla blocks it replaces by default. */
    @GameTest(template = "empty_small")
    public static void corruptionConversionsAreTags(GameTestHelper helper) {
        helper.assertTrue(Blocks.DIRT.defaultBlockState().is(dimensionalfluxminer.registry.DfmTags.Blocks.CORRUPTIBLE_DIRT), "dirt");
        helper.assertTrue(Blocks.GRASS_BLOCK.defaultBlockState().is(dimensionalfluxminer.registry.DfmTags.Blocks.CORRUPTIBLE_GRASS), "grass block");
        helper.assertTrue(Blocks.POPPY.defaultBlockState().is(dimensionalfluxminer.registry.DfmTags.Blocks.CORRUPTIBLE_FLOWERS), "flowers");
        helper.assertFalse(Blocks.COARSE_DIRT.defaultBlockState().is(dimensionalfluxminer.registry.DfmTags.Blocks.CORRUPTIBLE_DIRT),
                "coarse dirt is not corrupted unless a datapack adds it");
        helper.assertTrue(Blocks.DIRT.defaultBlockState().is(dimensionalfluxminer.registry.DfmTags.Blocks.CORRUPTION_GROUND), "dirt is ground");
        helper.succeed();
    }

    /**
     * 14 crafting + 3 crushing + 10 ore_crushing + 2 alloying = 29 definitions, plus the compatibility recipes of the
     * mods present: Malum (5 ore_crushing), Forbidden Arcanus (2 ore_crushing) and Create (1 alloying).
     */
    @GameTest(template = "empty_small")
    public static void allRecipeDefinitionsLoad(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        long crafting = recipes.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .map(RecipeHolder::id)
                .filter(id -> id.getNamespace().equals(DimensionalFluxMiner.MOD_ID))
                .count();
        int crushing = recipes.getAllRecipesFor(DfmRecipes.CRUSHING.get()).size();
        int ore = recipes.getAllRecipesFor(DfmRecipes.ORE_CRUSHING.get()).size();
        long alloying = recipes.getAllRecipesFor(DfmRecipes.ALLOYING.get()).stream()
                .filter(h -> h.id().getNamespace().equals(DimensionalFluxMiner.MOD_ID))
                .count();
        var mods = net.neoforged.fml.ModList.get();
        int expectedOre = 10 + (mods.isLoaded("malum") ? 5 : 0) + (mods.isLoaded("forbidden_arcanus") ? 2 : 0);
        int expectedAlloying = 2 + (mods.isLoaded("create") ? 1 : 0);
        helper.assertTrue(crafting == 14, "crafting recipes: " + crafting);
        helper.assertTrue(crushing == 3, "crushing recipes: " + crushing);
        helper.assertTrue(ore == expectedOre, "ore_crushing recipes: " + ore + ", expected " + expectedOre);
        helper.assertTrue(alloying == expectedAlloying, "alloying recipes: " + alloying + ", expected " + expectedAlloying);
        helper.succeed();
    }

    /** Farmland next to Corrupted Water stays dry; the same layout with water gets wet (control). */
    @GameTest(template = "empty_large")
    public static void corruptedWaterNeverHydratesFarmland(GameTestHelper helper) {
        helper.assertTrue(farmlandMoisture(helper, DfmBlocks.CORRUPTED_WATER.get().defaultBlockState()) == 0,
                "farmland next to Corrupted Water got wet");
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void waterStillHydratesFarmland(GameTestHelper helper) {
        helper.assertTrue(farmlandMoisture(helper, Blocks.WATER.defaultBlockState()) == FarmBlock.MAX_MOISTURE,
                "farmland next to water stayed dry");
        helper.succeed();
    }

    /** Dry farmland with a crop on it, a roof above (no rain) and the given liquid beside it, after random ticks. */
    private static int farmlandMoisture(GameTestHelper helper, BlockState liquid) {
        BlockPos farm = new BlockPos(2, 1, 2);
        helper.setBlock(farm, Blocks.FARMLAND.defaultBlockState());
        helper.setBlock(farm.above(), Blocks.WHEAT);
        helper.setBlock(farm.above(2), Blocks.STONE);
        helper.setBlock(farm.east(), liquid);
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(farm);
        for (int i = 0; i < 20; i++) {
            level.getBlockState(abs).randomTick(level, abs, level.random);
        }
        BlockState state = level.getBlockState(abs);
        return state.is(Blocks.FARMLAND) ? state.getValue(FarmBlock.MOISTURE) : -1;
    }

    /** Sugar cane needs real water beside its ground; Corrupted Water does not count. */
    @GameTest(template = "empty_large")
    public static void sugarCaneCannotGrowBesideCorruptedWater(GameTestHelper helper) {
        BlockPos ground = new BlockPos(2, 1, 2);
        helper.setBlock(ground, Blocks.SAND);
        helper.setBlock(ground.east(), DfmBlocks.CORRUPTED_WATER.get());
        BlockPos cane = helper.absolutePos(ground.above());
        helper.assertFalse(Blocks.SUGAR_CANE.defaultBlockState().canSurvive(helper.getLevel(), cane),
                "sugar cane survives beside Corrupted Water");
        helper.setBlock(ground.east(), Blocks.WATER);
        helper.assertTrue(Blocks.SUGAR_CANE.defaultBlockState().canSurvive(helper.getLevel(), cane),
                "sugar cane does not survive beside water");
        helper.succeed();
    }

    /** Every advancement of the mod loads (its criteria, including the mod's own event trigger, parse). */
    @GameTest(template = "empty_small")
    public static void allAdvancementsLoad(GameTestHelper helper) {
        List<String> ids = List.of("root", "crusher", "electric_furnace", "steel", "slag_recovered", "reinforced_eye", "stronghold",
                "miner_formed", "first_ore", "ore_filtered", "max_speed", "thousand_per_second", "efficiency_upgrade",
                "matter_injected", "trash_to_treasure", "column_drained", "scorched_earth", "purple_dive", "warden_friend",
                "world_drained");
        ServerAdvancementManager advancements = helper.getLevel().getServer().getAdvancements();
        for (String id : ids) {
            helper.assertTrue(advancements.get(DimensionalFluxMiner.id(id)) != null, "advancement missing: " + id);
        }
        long total = advancements.getAllAdvancements().stream()
                .filter(holder -> holder.id().getNamespace().equals(DimensionalFluxMiner.MOD_ID))
                .count();
        helper.assertTrue(total == ids.size(), "advancements of the mod: " + total);
        // The first Pure Steel hands out the guide (never the world start); the World Breaker an Efficiency Upgrade.
        helper.assertTrue(advancements.get(DimensionalFluxMiner.id("root")).value().rewards().loot().isEmpty(), "root gives nothing");
        assertReward(helper, advancements, "steel", "rewards/guide_book");
        assertReward(helper, advancements, "world_drained", "rewards/efficiency_upgrade");
        helper.succeed();
    }

    /** Pure Steel is not common steel: it carries no steel tag and steel of other mods never replaces it. */
    @GameTest(template = "empty_small")
    public static void pureSteelIsNotCommonSteel(GameTestHelper helper) {
        var steelTag = dimensionalfluxminer.registry.DfmTags.Items.common("ingots/steel");
        helper.assertTrue(!new ItemStack(DfmItems.INGOT_STEEL.get()).is(steelTag), "Pure Steel must not be in c:ingots/steel");
        helper.assertTrue(!new ItemStack(DfmItems.INGOT_STEEL.get()).is(dimensionalfluxminer.registry.DfmTags.Items.common("ingots")),
                "Pure Steel must not be in c:ingots");
        ItemStack eye = new ItemStack(DfmItems.ENDER_EYE_REINFORCED.get());
        net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(steelTag).ifPresent(set -> set.forEach(foreign -> {
            ItemStack result = craft(helper, List.of(
                    ItemStack.EMPTY, eye, ItemStack.EMPTY,
                    ItemStack.EMPTY, new ItemStack(foreign), ItemStack.EMPTY,
                    ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.BOOK)));
            helper.assertTrue(result.isEmpty(), "steel of another mod made the guide: " + foreign.getRegisteredName());
        }));
        helper.succeed();
    }

    private static void assertReward(GameTestHelper helper, ServerAdvancementManager advancements, String advancement, String lootTable) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, DimensionalFluxMiner.id(lootTable));
        helper.assertTrue(advancements.get(DimensionalFluxMiner.id(advancement)).value().rewards().loot().contains(key),
                advancement + " does not reward " + lootTable);
        helper.assertTrue(helper.getLevel().getServer().reloadableRegistries().getLootTable(key) != LootTable.EMPTY,
                "loot table missing: " + lootTable);
    }

    /** A book, a Steel Ingot and a Reinforced Eye of Ender, in any arrangement, make the guide; without the steel, nothing. */
    @GameTest(template = "empty_small")
    public static void guideBookIsCraftedFromABookSteelAndAReinforcedEye(GameTestHelper helper) {
        ItemStack eye = new ItemStack(DfmItems.ENDER_EYE_REINFORCED.get());
        ItemStack steel = new ItemStack(DfmItems.INGOT_STEEL.get());
        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack result = craft(helper, List.of(
                ItemStack.EMPTY, eye, ItemStack.EMPTY,
                ItemStack.EMPTY, steel, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, book));
        helper.assertTrue(result.is(DfmItems.GUIDE_BOOK.get()) && result.getCount() == 1, "guide recipe gave " + result);
        ItemStack withoutSteel = craft(helper, List.of(
                ItemStack.EMPTY, eye, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, book));
        helper.assertTrue(withoutSteel.isEmpty(), "book + eye alone gave " + withoutSteel);
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
