package dimensionalfluxminer.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.ForbiddenArcanusRecipes;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=26.1 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}
import net.minecraft.resources.RegistryOps;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
//? if >=26.1 {
import net.minecraft.world.item.crafting.RecipeMap;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeManager;
*///?}
import net.minecraft.world.item.crafting.RecipeType;
//? if >=26.1 {
import net.minecraft.world.item.crafting.SingleRecipeInput;
//?}
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Alloy recipes of other mods run by the Electric Furnace. The recipe types are configured
 * ({@link DfmConfig#externalAlloyTypes()}); each recipe is encoded with its own codec and read from that data: the
 * input list ({@code inputs} / {@code ingredients} / {@code itemInputs} / {@code input0..2}, each entry with an optional
 * {@code count}, a repeated entry counting once more), the results ({@code output} / {@code result}, or the lists
 * {@code results} / {@code outputs} / {@code itemResults}: the second becomes the byproduct), {@code experience} and the
 * time ({@code time} / {@code processing_ticks}). Recipes this does not fit (fluids, more than 3 ingredients, no item
 * result) are skipped. The Clibano alloys are always converted ({@link ForbiddenArcanusRecipes}). A converted recipe
 * that another recipe already covers is left out ({@link #withoutRedundant}). No other mod is needed to build or run.
 */
public final class ExternalAlloying {
    private static final List<String> INPUT_LISTS = List.of("inputs", "ingredients", "itemInputs");
    private static final List<String> INPUT_KEYS = List.of("input0", "input1", "input2");
    private static final List<String> SINGLE_RESULTS = List.of("output", "result");
    private static final List<String> RESULT_LISTS = List.of("results", "outputs", "itemResults");
    private static final List<String> TIME_KEYS = List.of("time", "processing_ticks", "processingTime", "duration");
    private static final Set<String> NESTED_INGREDIENT = Set.of("ingredient", "base_ingredient", "value");

    //? if >=26.1 {
    private static RecipeMap cachedManager;
    //?} else {
    /*private static RecipeManager cachedManager;
    *///?}
    private static List<RecipeHolder<AlloyingRecipe>> cached = List.of();

    private ExternalAlloying() {
    }

    /** The mod's own alloying recipes followed by the converted ones of the configured types. */
    //? if >=26.1 {
    public static synchronized List<RecipeHolder<AlloyingRecipe>> all(RecipeMap manager, HolderLookup.Provider registries) {
    //?} else {
    /*public static synchronized List<RecipeHolder<AlloyingRecipe>> all(RecipeManager manager, HolderLookup.Provider registries) {
    *///?}
        if (manager != cachedManager) {
            //? if >=26.1 {
            List<RecipeHolder<AlloyingRecipe>> list = new ArrayList<>(manager.byType(DfmRecipes.ALLOYING.get()));
            //?} else {
            /*List<RecipeHolder<AlloyingRecipe>> list = new ArrayList<>(manager.getAllRecipesFor(DfmRecipes.ALLOYING.get()));
            *///?}
            int own = list.size();
            list.addAll(convert(manager, registries));
            cached = List.copyOf(withoutRedundant(list, own));
            cachedManager = manager;
        }
        return cached;
    }

    /** The alloy recipe matching the lanes: the mod's own recipes first, then the converted ones. */
    public static Optional<AlloyingRecipe> find(AlloyRecipeInput input, Level level) {
        //? if >=26.1 {
        Optional<AlloyingRecipe> own = dimensionalfluxminer.recipe.Recipes.first(level, DfmRecipes.ALLOYING.get(), input).map(RecipeHolder::value);
        //?} else {
        /*Optional<AlloyingRecipe> own = level.getRecipeManager().getRecipeFor(DfmRecipes.ALLOYING.get(), input, level).map(RecipeHolder::value);
        *///?}
        if (own.isPresent()) {
            return own;
        }
        //? if >=26.1 {
        for (RecipeHolder<AlloyingRecipe> holder : all(dimensionalfluxminer.recipe.Recipes.map(level), level.registryAccess())) {
        //?} else {
        /*for (RecipeHolder<AlloyingRecipe> holder : all(level.getRecipeManager(), level.registryAccess())) {
        *///?}
            if (holder.value().matches(input, level)) {
                return Optional.of(holder.value());
            }
        }
        return Optional.empty();
    }

    /** Recipe types of other mods the Electric Furnace converts (configured alloys and the Clibano), when registered. */
    public static List<RecipeType<?>> convertedTypes() {
        List<RecipeType<?>> out = new ArrayList<>();
        //? if >=26.1 {
        List<Identifier> ids = new ArrayList<>(DfmConfig.externalAlloyTypes());
        //?} else {
        /*List<ResourceLocation> ids = new ArrayList<>(DfmConfig.externalAlloyTypes());
        *///?}
        ids.addAll(ForbiddenArcanusRecipes.types());
        ids.forEach(id -> BuiltInRegistries.RECIPE_TYPE.getOptional(id).ifPresent(type -> {
            if (!out.contains(type)) {
                out.add(type);
            }
        }));
        return out;
    }

    //? if >=26.1 {
    private static List<RecipeHolder<AlloyingRecipe>> convert(RecipeMap manager, HolderLookup.Provider registries) {
        List<Identifier> types = DfmConfig.externalAlloyTypes();
    //?} else {
    /*private static List<RecipeHolder<AlloyingRecipe>> convert(RecipeManager manager, HolderLookup.Provider registries) {
        List<ResourceLocation> types = DfmConfig.externalAlloyTypes();
    *///?}
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<AbstractCookingRecipe> cooking = new ArrayList<>();
        //? if >=26.1 {
        manager.byType(RecipeType.SMELTING).forEach(h -> cooking.add(h.value()));
        manager.byType(RecipeType.BLASTING).forEach(h -> cooking.add(h.value()));
        //?} else {
        /*manager.getAllRecipesFor(RecipeType.SMELTING).forEach(h -> cooking.add(h.value()));
        manager.getAllRecipesFor(RecipeType.BLASTING).forEach(h -> cooking.add(h.value()));
        *///?}
        List<RecipeHolder<AlloyingRecipe>> out = new ArrayList<>();
        //? if >=26.1 {
        for (RecipeHolder<?> holder : manager.values()) {
            Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        //?} else {
        /*for (RecipeHolder<?> holder : manager.getRecipes()) {
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        *///?}
            if (type == null) {
                continue;
            }
            Optional<AlloyingRecipe> converted;
            if (types.contains(type)) {
                converted = read(holder.value(), ops).filter(recipe -> !isFurnaceCopy(recipe, cooking, registries));
            } else if (ForbiddenArcanusRecipes.isAlloyType(type)) {
                converted = encode(holder.value(), ops).flatMap(json -> ForbiddenArcanusRecipes.alloy(json, ops, registries));
            } else {
                continue;
            }
            // Own id, so recipe viewers never mix it up with the original recipe of the other mod.
            //? if >=26.1 {
            Identifier original = holder.id().identifier();
            ResourceKey<Recipe<?>> id = ResourceKey.create(Registries.RECIPE, DimensionalFluxMiner.id("external/" + original.getNamespace() + "/" + original.getPath()));
            //?} else {
            /*ResourceLocation id = DimensionalFluxMiner.id("external/" + holder.id().getNamespace() + "/" + holder.id().getPath());
            *///?}
            converted.ifPresent(recipe -> out.add(new RecipeHolder<>(id, recipe)));
        }
        return out;
    }

    /**
     * Leaves out the converted recipes another recipe already covers: same result (at least as many), same byproduct and
     * the same items in each lane, none needed in a larger amount. Of two identical recipes the first stays. The mod's
     * own recipes (the first {@code own} entries) always stay.
     */
    public static List<RecipeHolder<AlloyingRecipe>> withoutRedundant(List<RecipeHolder<AlloyingRecipe>> recipes, int own) {
        List<List<Set<Item>>> lanes = new ArrayList<>();
        recipes.forEach(holder -> lanes.add(laneItems(holder.value())));
        List<RecipeHolder<AlloyingRecipe>> out = new ArrayList<>(recipes.subList(0, own));
        for (int i = own; i < recipes.size(); i++) {
            AlloyingRecipe recipe = recipes.get(i).value();
            boolean covered = false;
            for (int j = 0; j < recipes.size() && !covered; j++) {
                AlloyingRecipe other = recipes.get(j).value();
                covered = j != i && covers(other, lanes.get(j), recipe, lanes.get(i))
                        && (j < i || !covers(recipe, lanes.get(i), other, lanes.get(j)));
            }
            if (!covered) {
                out.add(recipes.get(i));
            }
        }
        return out;
    }

    /** True when {@code a} makes at least what {@code b} makes from the same items, never needing more of them. */
    private static boolean covers(AlloyingRecipe a, List<Set<Item>> aLanes, AlloyingRecipe b, List<Set<Item>> bLanes) {
        if (a.ingredients().size() != b.ingredients().size() || !atLeast(a.result(), b.result())
                || a.byproduct().isPresent() != b.byproduct().isPresent()) {
            return false;
        }
        if (a.byproduct().isPresent() && (a.byproduct().get().chance() < b.byproduct().get().chance()
                || !atLeast(a.byproduct().get().stack(), b.byproduct().get().stack()))) {
            return false;
        }
        return lanesCovered(a, aLanes, b, bLanes, 0, new boolean[b.ingredients().size()]);
    }

    private static boolean lanesCovered(AlloyingRecipe a, List<Set<Item>> aLanes, AlloyingRecipe b, List<Set<Item>> bLanes,
                                        int lane, boolean[] used) {
        if (lane == aLanes.size()) {
            return true;
        }
        for (int k = 0; k < bLanes.size(); k++) {
            if (!used[k] && !aLanes.get(lane).isEmpty() && aLanes.get(lane).equals(bLanes.get(k))
                    && a.ingredients().get(lane).count() <= b.ingredients().get(k).count()) {
                used[k] = true;
                if (lanesCovered(a, aLanes, b, bLanes, lane + 1, used)) {
                    return true;
                }
                used[k] = false;
            }
        }
        return false;
    }

    private static boolean atLeast(ItemStack a, ItemStack b) {
        return !a.isEmpty() && ItemStack.isSameItemSameComponents(a, b) && a.getCount() >= b.getCount();
    }

    private static List<Set<Item>> laneItems(AlloyingRecipe recipe) {
        List<Set<Item>> lanes = new ArrayList<>();
        for (SizedIngredient sized : recipe.ingredients()) {
            Set<Item> items = new LinkedHashSet<>();
            //? if >=26.1 {
            for (ItemStack option : dimensionalfluxminer.recipe.Recipes.stacks(sized.ingredient())) {
            //?} else {
            /*for (ItemStack option : sized.ingredient().getItems()) {
            *///?}
                if (!option.isEmpty()) {
                    items.add(option.getItem());
                }
            }
            lanes.add(items);
        }
        return lanes;
    }

    /**
     * Some alloy machines copy every furnace recipe into their own type. Such a copy (one ingredient, count 1, the
     * result of a furnace or blast furnace recipe of that item) is left out: the Electric Furnace already smelts it, with
     * its raw ore bonus.
     */
    private static boolean isFurnaceCopy(AlloyingRecipe recipe, List<AbstractCookingRecipe> cooking, HolderLookup.Provider registries) {
        if (recipe.ingredients().size() != 1 || recipe.ingredients().getFirst().count() != 1 || recipe.byproduct().isPresent()) {
            return false;
        }
        //? if >=26.1 {
        ItemStack[] items = dimensionalfluxminer.recipe.Recipes.stacks(recipe.ingredients().getFirst().ingredient());
        //?} else {
        /*ItemStack[] items = recipe.ingredients().getFirst().ingredient().getItems();
        *///?}
        if (items.length == 0) {
            return false;
        }
        for (AbstractCookingRecipe furnace : cooking) {
            //? if >=26.1 {
            if (furnace.input().test(items[0])
                    && ItemStack.isSameItem(furnace.assemble(new SingleRecipeInput(items[0])), recipe.result())) {
            //?} else {
            /*if (!furnace.getIngredients().isEmpty() && furnace.getIngredients().getFirst().test(items[0])
                    && ItemStack.isSameItem(furnace.getResultItem(registries), recipe.result())) {
            *///?}
                return true;
            }
        }
        return false;
    }

    /** The recipe as its own codec writes it, or empty when it cannot be written. */
    @SuppressWarnings("unchecked")
    public static Optional<JsonObject> encode(Recipe<?> recipe, RegistryOps<JsonElement> ops) {
        try {
            MapCodec<Recipe<?>> codec = (MapCodec<Recipe<?>>) recipe.getSerializer().codec();
            return codec.codec().encodeStart(ops, recipe).result()
                    .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    static Optional<AlloyingRecipe> read(Recipe<?> recipe, RegistryOps<JsonElement> ops) {
        Optional<JsonObject> encoded = encode(recipe, ops);
        if (encoded.isEmpty() || usesFluid(encoded.get())) {
            return Optional.empty();
        }
        JsonObject json = encoded.get();
        List<SizedIngredient> ingredients = ingredients(json, ops);
        List<ItemStack> results = results(json, ops);
        if (ingredients.isEmpty() || ingredients.size() > AlloyRecipeInput.LANES || results.isEmpty()) {
            return Optional.empty();
        }
        Optional<AlloyingRecipe.Byproduct> byproduct = results.size() > 1
                ? Optional.of(new AlloyingRecipe.Byproduct(results.get(1), 1.0F)) : Optional.empty();
        float experience = json.has("experience") && json.get("experience").isJsonPrimitive() ? json.get("experience").getAsFloat() : 0.0F;
        int ticks = AlloyingRecipe.DEFAULT_PROCESSING_TICKS;
        for (String key : TIME_KEYS) {
            if (json.has(key) && json.get(key).isJsonPrimitive() && json.get(key).getAsInt() > 0) {
                ticks = json.get(key).getAsInt();
                break;
            }
        }
        return Optional.of(new AlloyingRecipe(ingredients, results.getFirst(), byproduct, ticks, Math.max(0, experience)));
    }

    /** True when a fluid field holds a fluid (a non-empty list, or an entry with an amount): the furnace moves items only. */
    private static boolean usesFluid(JsonObject json) {
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getKey().toLowerCase(Locale.ROOT).contains("fluid")) {
                continue;
            }
            JsonElement value = entry.getValue();
            if (value.isJsonArray() && !value.getAsJsonArray().isEmpty()) {
                return true;
            }
            if (value.isJsonObject()) {
                JsonObject fluid = value.getAsJsonObject();
                JsonElement amount = fluid.has("amount") ? fluid.get("amount") : fluid.get("count");
                if (amount != null && amount.isJsonPrimitive() && amount.getAsJsonPrimitive().isNumber() && amount.getAsInt() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Counted ingredients; identical entries are merged. Empty when an entry is not an item ingredient. */
    private static List<SizedIngredient> ingredients(JsonObject json, RegistryOps<JsonElement> ops) {
        List<JsonElement> entries = new ArrayList<>();
        for (String key : INPUT_LISTS) {
            if (json.has(key) && json.get(key).isJsonArray()) {
                json.getAsJsonArray(key).forEach(entries::add);
                break;
            }
        }
        if (entries.isEmpty()) {
            for (String key : INPUT_KEYS) {
                if (json.has(key)) {
                    entries.add(json.get(key));
                }
            }
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Ingredient> parsed = new LinkedHashMap<>();
        for (JsonElement entry : entries) {
            int count = 1;
            JsonElement ingredientJson = entry;
            if (entry instanceof JsonObject object) {
                JsonObject copy = object.deepCopy();
                if (copy.has("count") && copy.get("count").isJsonPrimitive()) {
                    count = Math.max(1, copy.remove("count").getAsInt());
                }
                ingredientJson = copy;
                for (String nested : NESTED_INGREDIENT) {
                    if (copy.has(nested)) {
                        ingredientJson = copy.get(nested);
                        break;
                    }
                }
            }
            //? if >=26.1 {
            Optional<Ingredient> ingredient = Ingredient.CODEC.parse(ops, ingredientJson).result();
            //?} else {
            /*Optional<Ingredient> ingredient = Ingredient.CODEC_NONEMPTY.parse(ops, ingredientJson).result();
            *///?}
            if (ingredient.isEmpty()) {
                return List.of();
            }
            String key = ingredientJson.toString();
            parsed.putIfAbsent(key, ingredient.get());
            counts.merge(key, count, Integer::sum);
        }
        List<SizedIngredient> out = new ArrayList<>();
        counts.forEach((key, count) -> out.add(new SizedIngredient(parsed.get(key), count)));
        return out;
    }

    private static List<ItemStack> results(JsonObject json, RegistryOps<JsonElement> ops) {
        List<JsonElement> entries = new ArrayList<>();
        for (String key : SINGLE_RESULTS) {
            if (json.has(key) && json.get(key).isJsonObject()) {
                entries.add(json.get(key));
                break;
            }
        }
        if (entries.isEmpty()) {
            for (String key : RESULT_LISTS) {
                if (json.has(key) && json.get(key).isJsonArray()) {
                    JsonArray array = json.getAsJsonArray(key);
                    array.forEach(entries::add);
                    break;
                }
            }
        }
        List<ItemStack> out = new ArrayList<>();
        for (JsonElement entry : entries) {
            //? if >=26.1 {
            Optional<ItemStack> stack = ItemStack.CODEC.parse(ops, entry).result();
            //?} else {
            /*Optional<ItemStack> stack = ItemStack.STRICT_CODEC.parse(ops, entry).result();
            *///?}
            if (stack.isEmpty() || stack.get().isEmpty()) {
                return List.of();
            }
            out.add(stack.get());
        }
        return out;
    }
}
