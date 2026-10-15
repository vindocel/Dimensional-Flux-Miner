package dimensionalfluxminer.miner;

import dimensionalfluxminer.registry.DfmDataMaps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;

/**
 * Variants of the substrates of the {@code matter_value} data map: blocks made from them in a stonecutter, a furnace
 * or a crafting grid (polished, smooth, bricks, cracked, mossy, stairs, slabs, walls...), followed through chains.
 * <ul>
 *     <li>A variant is accepted in a dimension when it can be made from the substrates accepted there.</li>
 *     <li>It is worth the substrates spent divided by the items made (a slab is half a block); other ingredients
 *     (vines, honeycomb, a hammer) add nothing, and at least half of a crafting recipe must be substrate.</li>
 *     <li>The cheapest way to make it sets its value, so no recipe ever gives more matter than it costs.</li>
 *     <li>Only plain blocks qualify: no block entities (furnaces, chests), nothing made from ores (ingots).</li>
 * </ul>
 * An item with its own data map entry keeps that entry. Rebuilt whenever the recipes are reloaded.
 */
public final class MatterVariants {
    /** A recipe that may turn substrates into a block: the options of each ingredient, the block and how many. */
    public record Making(List<Item[]> ingredients, Item output, int count) {
    }

    /** Variants of one dimension, valued with the plain substrate values or with their Efficiency Upgrade values. */
    private record Key(ResourceKey<Level> dimension, boolean upgrade) {
    }

    private record Cache(RecipeManager recipes, List<Making> makings, Map<Key, Map<Item, Integer>> byDimension) {
    }

    private static volatile Cache cache;

    private MatterVariants() {
    }

    /** Value in units of a variant accepted in {@code dimension}; empty for any other item. */
    public static OptionalInt value(RecipeManager recipes, HolderLookup.Provider registries, ResourceKey<Level> dimension, Item item) {
        return value(recipes, registries, dimension, item, false);
    }

    /**
     * Value in units of a variant accepted in {@code dimension}. With {@code upgrade}, the same recipes are followed from
     * the substrates' Efficiency Upgrade values: cutting one block into many pieces never multiplies the upgrade bonus,
     * because each piece is worth its share of the upgraded block, not a bonus of its own.
     */
    public static OptionalInt value(RecipeManager recipes, HolderLookup.Provider registries, ResourceKey<Level> dimension, Item item,
                                    boolean upgrade) {
        Cache current = cache;
        if (current == null || current.recipes() != recipes) {
            current = new Cache(recipes, makings(recipes, registries), new ConcurrentHashMap<>());
            cache = current;
        }
        List<Making> makings = current.makings();
        Map<Key, Map<Item, Integer>> maps = current.byDimension();
        // Accepted only when the plain values accept it; the upgrade never turns a rejected item into matter.
        if (!maps.computeIfAbsent(new Key(dimension, false), k -> variants(makings, k)).containsKey(item)) {
            return OptionalInt.empty();
        }
        Integer units = maps.computeIfAbsent(new Key(dimension, upgrade), k -> variants(makings, k)).get(item);
        return units == null ? OptionalInt.empty() : OptionalInt.of(units);
    }

    private static List<Making> makings(RecipeManager recipes, HolderLookup.Provider registries) {
        List<Making> list = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (!(recipe instanceof StonecutterRecipe || recipe instanceof AbstractCookingRecipe || recipe instanceof CraftingRecipe)) {
                continue;
            }
            ItemStack result = recipe.getResultItem(registries);
            if (result.isEmpty() || !plainBlock(result)) {
                continue;
            }
            List<Item[]> ingredients = new ArrayList<>();
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (!ingredient.isEmpty()) {
                    ingredients.add(java.util.Arrays.stream(ingredient.getItems()).map(ItemStack::getItem).toArray(Item[]::new));
                }
            }
            if (!ingredients.isEmpty()) {
                list.add(new Making(List.copyOf(ingredients), result.getItem(), result.getCount()));
            }
        }
        return List.copyOf(list);
    }

    private static boolean plainBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && !(block.getBlock() instanceof EntityBlock)
                && !MatterValues.isOre(stack) && stack.getItemHolder().getData(DfmDataMaps.MATTER_VALUE) == null;
    }

    /** Every variant accepted in {@code dimension} and its value, by repeating the recipes until nothing gets cheaper. */
    private static Map<Item, Integer> variants(List<Making> makings, Key key) {
        Map<Item, Integer> substrates = new HashMap<>();
        BuiltInRegistries.ITEM.getDataMap(DfmDataMaps.MATTER_VALUE).forEach((id, value) -> {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (value.acceptedIn(key.dimension()) && !MatterValues.isOre(new ItemStack(item))) {
                substrates.put(item, MatterValues.matterInputUnits(value.units(), key.upgrade()));
            }
        });
        return variants(makings, substrates);
    }

    /**
     * Every variant of {@code substrates} and its value, by repeating the recipes until nothing gets cheaper. Exposed
     * for tests with made-up recipes.
     */
    public static Map<Item, Integer> variants(List<Making> makings, Map<Item, Integer> substrates) {
        Map<Item, Integer> variants = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Making making : makings) {
                int spent = 0;
                int substrateIngredients = 0;
                for (Item[] options : making.ingredients()) {
                    int cheapest = Integer.MAX_VALUE;
                    for (Item option : options) {
                        Integer units = substrates.containsKey(option) ? substrates.get(option) : variants.get(option);
                        if (units != null) {
                            cheapest = Math.min(cheapest, units);
                        }
                    }
                    if (cheapest != Integer.MAX_VALUE) {
                        spent += cheapest;
                        substrateIngredients++;
                    }
                }
                if (substrateIngredients == 0 || substrateIngredients * 2 < making.ingredients().size()) {
                    continue;
                }
                // Rounded down, and a piece worth less than one unit is no matter at all: rounding it up to one unit
                // would let a recipe that makes many pieces (2 dirt + 2 gravel -> 4 coarse dirt) give more than it costs.
                int units = spent / making.count();
                if (units <= 0) {
                    continue;
                }
                Integer known = variants.get(making.output());
                if (known == null || units < known) {
                    variants.put(making.output(), units);
                    changed = true;
                }
            }
        }
        return Map.copyOf(variants);
    }
}
