package dimensionalfluxminer.smelter;

import dimensionalfluxminer.recipe.AlloyRecipeInput;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.recipe.ExternalAlloying;
import dimensionalfluxminer.registry.DfmTags;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Recipe lookups of one Alloy Smelter, with their caches: the furnace recipe of each lane's item and of every item
 * offered, the alloy of the three lanes, and the items that are alloy ingredients. Everything is rebuilt when the
 * recipes reload (a new {@link RecipeManager}).
 */
final class SmelterRecipes {
    private static final int COOKING_CACHE_LIMIT = 256;

    private RecipeManager cachedManager;
    private final ItemStack[] laneKey;
    private final Optional<AbstractCookingRecipe>[] laneRecipe;
    private ItemStack[] alloyKey;
    private Optional<AlloyingRecipe> alloyRecipe = Optional.empty();
    private Set<Item> alloyIngredientItems = Set.of();
    /** Cooking recipe of each item kind seen (item + components). */
    private final Map<ItemStack, Optional<AbstractCookingRecipe>> cookingCache =
            new it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);

    @SuppressWarnings("unchecked")
    SmelterRecipes(int lanes) {
        laneKey = new ItemStack[lanes];
        laneRecipe = new Optional[lanes];
        alloyKey = new ItemStack[lanes];
        clear();
    }

    private void clear() {
        Arrays.fill(laneKey, ItemStack.EMPTY);
        Arrays.fill(laneRecipe, Optional.empty());
        Arrays.fill(alloyKey, ItemStack.EMPTY);
        alloyRecipe = Optional.empty();
        cookingCache.clear();
    }

    private void refresh(Level level) {
        RecipeManager manager = level.getRecipeManager();
        if (manager == cachedManager) {
            return;
        }
        cachedManager = manager;
        clear();
        Set<Item> items = new HashSet<>();
        for (RecipeHolder<AlloyingRecipe> holder : ExternalAlloying.all(manager, level.registryAccess())) {
            for (SizedIngredient sized : holder.value().ingredients()) {
                for (ItemStack option : sized.ingredient().getItems()) {
                    items.add(option.getItem());
                }
            }
        }
        alloyIngredientItems = items;
    }

    /** Furnace recipe of the item in {@code lane} (remembered while the lane keeps the same item). */
    Optional<AbstractCookingRecipe> cooking(Level level, int lane, ItemStack stack) {
        refresh(level);
        if (!ItemStack.isSameItemSameComponents(laneKey[lane], stack)) {
            laneKey[lane] = stack.copyWithCount(1);
            laneRecipe[lane] = stack.isEmpty() ? Optional.empty() : cooking(level, stack);
        }
        return laneRecipe[lane];
    }

    /**
     * Blast furnace recipe of the item, otherwise its furnace recipe. Remembered per item and components: insertion
     * checks every offered stack, and a full recipe search per stack was the costliest part of the automation.
     */
    Optional<AbstractCookingRecipe> cooking(Level level, ItemStack stack) {
        refresh(level);
        Optional<AbstractCookingRecipe> known = cookingCache.get(stack);
        if (known == null) {
            if (cookingCache.size() >= COOKING_CACHE_LIMIT) {
                cookingCache.clear();
            }
            known = find(level, stack);
            cookingCache.put(stack.copyWithCount(1), known);
        }
        return known;
    }

    private static Optional<AbstractCookingRecipe> find(Level level, ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        RecipeManager manager = level.getRecipeManager();
        Optional<AbstractCookingRecipe> blasting = manager.getRecipeFor(RecipeType.BLASTING, input, level)
                .map(h -> (AbstractCookingRecipe) h.value());
        return blasting.isPresent() ? blasting : manager.getRecipeFor(RecipeType.SMELTING, input, level)
                .map(h -> (AbstractCookingRecipe) h.value());
    }

    /** The alloy the three lanes hold (the mod's own alloys first, then the alloys of other mods). */
    Optional<AlloyingRecipe> alloy(Level level, AlloyRecipeInput input) {
        refresh(level);
        boolean same = true;
        for (int i = 0; i < alloyKey.length; i++) {
            same &= ItemStack.isSameItemSameComponents(alloyKey[i], input.getItem(i));
        }
        if (!same) {
            alloyKey = new ItemStack[] {input.lane1().copy(), input.lane2().copy(), input.lane3().copy()};
            alloyRecipe = ExternalAlloying.find(input, level);
        }
        return alloyRecipe;
    }

    boolean isAlloyIngredient(Level level, ItemStack stack) {
        refresh(level);
        return alloyIngredientItems.contains(stack.getItem());
    }

    /** True when one alloy recipe uses both items as different ingredients. */
    static boolean alloyPartners(Level level, ItemStack a, ItemStack b) {
        for (RecipeHolder<AlloyingRecipe> holder : ExternalAlloying.all(level.getRecipeManager(), level.registryAccess())) {
            List<SizedIngredient> ingredients = holder.value().ingredients();
            for (int i = 0; i < ingredients.size(); i++) {
                for (int j = 0; j < ingredients.size(); j++) {
                    if (i != j && ingredients.get(i).ingredient().test(a) && ingredients.get(j).ingredient().test(b)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Raw -> Ingot of the same material with a single result, the only case eligible for the bulk yield. */
    static boolean isRawToIngot(ItemStack input, ItemStack result) {
        if (result.getCount() != 1) {
            return false;
        }
        return input.getTags()
                .filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().startsWith("raw_materials/"))
                .map(t -> t.location().getPath().substring("raw_materials/".length()))
                .anyMatch(material -> result.is(DfmTags.Items.common("ingots/" + material)));
    }
}
