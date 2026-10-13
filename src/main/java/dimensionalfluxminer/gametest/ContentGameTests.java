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

    /** 14 crafting + 3 crushing + 10 ore_crushing + 2 alloying = 29 definitions. */
    @GameTest(template = "empty_small")
    public static void allRecipeDefinitionsLoad(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        long crafting = recipes.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .map(RecipeHolder::id)
                .filter(id -> id.getNamespace().equals(DimensionalFluxMiner.MOD_ID))
                .count();
        int crushing = recipes.getAllRecipesFor(DfmRecipes.CRUSHING.get()).size();
        int ore = recipes.getAllRecipesFor(DfmRecipes.ORE_CRUSHING.get()).size();
        int alloying = recipes.getAllRecipesFor(DfmRecipes.ALLOYING.get()).size();
        helper.assertTrue(crafting == 14, "crafting recipes: " + crafting);
        helper.assertTrue(crushing == 3, "crushing recipes: " + crushing);
        helper.assertTrue(ore == 10, "ore_crushing recipes: " + ore);
        helper.assertTrue(alloying == 2, "alloying recipes: " + alloying);
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
        List<String> ids = List.of("root", "crusher", "smelter", "steel", "slag_recovered", "reinforced_eye", "stronghold",
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
        // The root hands out the guide; the World Breaker an Efficiency Upgrade.
        assertReward(helper, advancements, "root", "rewards/guide_book");
        assertReward(helper, advancements, "world_drained", "rewards/efficiency_upgrade");
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
