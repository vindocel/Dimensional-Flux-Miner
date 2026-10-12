package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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

    /** 13 crafting + 3 crushing + 10 ore_crushing + 2 alloying = 28 definitions. */
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
        helper.assertTrue(crafting == 13, "crafting recipes: " + crafting);
        helper.assertTrue(crushing == 3, "crushing recipes: " + crushing);
        helper.assertTrue(ore == 10, "ore_crushing recipes: " + ore);
        helper.assertTrue(alloying == 2, "alloying recipes: " + alloying);
        helper.succeed();
    }
}
