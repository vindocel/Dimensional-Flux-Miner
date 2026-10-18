package dimensionalfluxminer.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Alloy recipes of other mods run by the Electric Furnace. The recipe types are configured
 * ({@link DfmConfig#externalAlloyTypes()}); each recipe is encoded with its own codec and read from that data: the
 * input list ({@code inputs} / {@code ingredients} / {@code input0..2}, each entry with an optional {@code count}),
 * the results ({@code output} / {@code result}, or the lists {@code results} / {@code outputs}: the second becomes the
 * byproduct), {@code experience} and the time ({@code time} / {@code processing_ticks}). Recipes this does not fit
 * (fluids, more than 3 ingredients, no item result) are skipped. No other mod is needed to build or run.
 */
public final class ExternalAlloying {
    private static final List<String> INPUT_LISTS = List.of("inputs", "ingredients");
    private static final List<String> INPUT_KEYS = List.of("input0", "input1", "input2");
    private static final List<String> SINGLE_RESULTS = List.of("output", "result");
    private static final List<String> RESULT_LISTS = List.of("results", "outputs");
    private static final List<String> TIME_KEYS = List.of("time", "processing_ticks", "processingTime", "duration");
    private static final Set<String> NESTED_INGREDIENT = Set.of("ingredient", "base_ingredient", "value");

    private static RecipeManager cachedManager;
    private static List<RecipeHolder<AlloyingRecipe>> cached = List.of();

    private ExternalAlloying() {
    }

    /** The mod's own alloying recipes followed by the converted ones of the configured types. */
    public static synchronized List<RecipeHolder<AlloyingRecipe>> all(RecipeManager manager, HolderLookup.Provider registries) {
        if (manager != cachedManager) {
            List<RecipeHolder<AlloyingRecipe>> list = new ArrayList<>(manager.getAllRecipesFor(DfmRecipes.ALLOYING.get()));
            list.addAll(convert(manager, registries));
            cached = List.copyOf(list);
            cachedManager = manager;
        }
        return cached;
    }

    /** The alloy recipe matching the lanes: the mod's own recipes first, then the converted ones. */
    public static Optional<AlloyingRecipe> find(AlloyRecipeInput input, Level level) {
        Optional<AlloyingRecipe> own = level.getRecipeManager().getRecipeFor(DfmRecipes.ALLOYING.get(), input, level).map(RecipeHolder::value);
        if (own.isPresent()) {
            return own;
        }
        for (RecipeHolder<AlloyingRecipe> holder : all(level.getRecipeManager(), level.registryAccess())) {
            if (holder.value().matches(input, level)) {
                return Optional.of(holder.value());
            }
        }
        return Optional.empty();
    }

    private static List<RecipeHolder<AlloyingRecipe>> convert(RecipeManager manager, HolderLookup.Provider registries) {
        List<ResourceLocation> types = DfmConfig.externalAlloyTypes();
        if (types.isEmpty()) {
            return List.of();
        }
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<AbstractCookingRecipe> cooking = new ArrayList<>();
        manager.getAllRecipesFor(RecipeType.SMELTING).forEach(h -> cooking.add(h.value()));
        manager.getAllRecipesFor(RecipeType.BLASTING).forEach(h -> cooking.add(h.value()));
        List<RecipeHolder<AlloyingRecipe>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            if (type != null && types.contains(type)) {
                // Own id, so recipe viewers never mix it up with the original recipe of the other mod.
                ResourceLocation id = DimensionalFluxMiner.id("external/" + holder.id().getNamespace() + "/" + holder.id().getPath());
                read(holder.value(), ops).filter(recipe -> !isFurnaceCopy(recipe, cooking, registries))
                        .ifPresent(recipe -> out.add(new RecipeHolder<>(id, recipe)));
            }
        }
        return out;
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
        ItemStack[] items = recipe.ingredients().getFirst().ingredient().getItems();
        if (items.length == 0) {
            return false;
        }
        for (AbstractCookingRecipe furnace : cooking) {
            if (!furnace.getIngredients().isEmpty() && furnace.getIngredients().getFirst().test(items[0])
                    && ItemStack.isSameItem(furnace.getResultItem(registries), recipe.result())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    static Optional<AlloyingRecipe> read(Recipe<?> recipe, RegistryOps<JsonElement> ops) {
        JsonElement encoded;
        try {
            MapCodec<Recipe<?>> codec = (MapCodec<Recipe<?>>) recipe.getSerializer().codec();
            encoded = codec.codec().encodeStart(ops, recipe).result().orElse(null);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (!(encoded instanceof JsonObject json)) {
            return Optional.empty();
        }
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
            Optional<Ingredient> ingredient = Ingredient.CODEC_NONEMPTY.parse(ops, ingredientJson).result();
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
            Optional<ItemStack> stack = ItemStack.STRICT_CODEC.parse(ops, entry).result();
            if (stack.isEmpty() || stack.get().isEmpty()) {
                return List.of();
            }
            out.add(stack.get());
        }
        return out;
    }
}
