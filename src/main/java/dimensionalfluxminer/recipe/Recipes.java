package dimensionalfluxminer.recipe;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Recipe lookups of a level. The server has every recipe; a client has the recipes the server sends to it
 * ({@link #setClientRecipes}, filled from NeoForge's recipe sync), which the guide and the recipe viewers read.
 */
public final class Recipes {
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    private Recipes() {
    }

    public static void setClientRecipes(RecipeMap recipes) {
        clientRecipes = recipes;
    }

    public static RecipeMap map(Level level) {
        MinecraftServer server = level.getServer();
        if (!level.isClientSide() && server != null) {
            return server.getRecipeManager().recipeMap();
        }
        return clientRecipes;
    }

    public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(Level level, RecipeType<T> type) {
        return map(level).byType(type);
    }

    /** Every recipe of {@code type} matching {@code input}, in priority order. */
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> matching(Level level, RecipeType<T> type, I input) {
        return map(level).getRecipesFor(type, input, level).toList();
    }

    /** The recipe of {@code type} the game would use for {@code input}. */
    public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> first(Level level, RecipeType<T> type, I input) {
        MinecraftServer server = level.getServer();
        if (!level.isClientSide() && server != null) {
            return server.getRecipeManager().getRecipeFor(type, input, level);
        }
        return map(level).getRecipesFor(type, input, level).findFirst();
    }

    public static Optional<RecipeHolder<?>> byKey(Level level, ResourceKey<Recipe<?>> key) {
        return Optional.ofNullable(map(level).byKey(key));
    }

    public static Collection<RecipeHolder<?>> all(Level level) {
        return map(level).values();
    }

    /** One stack of every item the ingredient accepts. */
    public static ItemStack[] stacks(Ingredient ingredient) {
        return ingredient.items().map(ItemStack::new).toArray(ItemStack[]::new);
    }

    /** The result of one craft of the recipe (the mod's recipes directly, others as their display shows it), or EMPTY. */
    public static ItemStack result(RegistryAccess registries, Recipe<?> recipe) {
        switch (recipe) {
            case CrushingRecipe crushing -> {
                return crushing.result().copy();
            }
            case OreCrushingRecipe ore -> {
                return ore.product().copyWithCount(ore.yield(false));
            }
            case AlloyingRecipe alloy -> {
                return alloy.result().copy();
            }
            default -> {
            }
        }
        ContextMap context = ContextMap.builder().set(SlotDisplayContext.REGISTRIES, registries).buildAndValidate(SlotDisplayContext.CONTEXT);
        for (RecipeDisplay display : recipe.display()) {
            ItemStack result = display.result().resolveForFirstStack(context);
            if (!result.isEmpty()) {
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Every ingredient of the recipe, without empty grid cells. */
    public static List<Ingredient> ingredients(Recipe<?> recipe) {
        return switch (recipe) {
            case CrushingRecipe crushing -> List.of(crushing.ingredient());
            case OreCrushingRecipe ore -> List.of(ore.ingredient());
            case AlloyingRecipe alloy -> alloy.ingredients().stream().map(SizedIngredient::ingredient).toList();
            case ShapedRecipe shaped -> shaped.getIngredients().stream().flatMap(Optional::stream).toList();
            case SingleItemRecipe single -> List.of(single.input());
            default -> recipe.placementInfo().ingredients();
        };
    }

    /** The crafting grid of the recipe, row by row ({@code width} cells per row for shaped recipes); empty cells are empty. */
    public static List<Optional<Ingredient>> grid(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return shaped.getIngredients();
        }
        return ingredients(recipe).stream().map(Optional::of).toList();
    }
}
