package dimensionalfluxminer.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dimensionalfluxminer.recipe.AlloyRecipeInput;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Clibano recipes run by the Electric Furnace, read from the data their codec writes (no compile dependency):
 * <ul>
 *   <li>melting of one item ({@code clibano_melting}; {@code clibano_combustion} with one ingredient) becomes a furnace
 *   recipe of the Electric Furnace only, making the result item of the molten material;</li>
 *   <li>alloys of up to three materials ({@code clibano_alloying}; {@code clibano_combustion} with two ingredients)
 *   become alloys. Each molten material is taken as its result item, {@value #UNITS_PER_ITEM} units per item.</li>
 * </ul>
 * Recipes needing more than three inputs, or more than a stack in one lane, are skipped.
 */
public final class ForbiddenArcanusRecipes {
    private static final String MOD = "forbidden_arcanus";
    private static final String MELTING = "clibano_melting";
    private static final String ALLOYING = "clibano_alloying";
    private static final String COMBUSTION = "clibano_combustion";
    /** Units of a molten material that make one item of its result. */
    static final int UNITS_PER_ITEM = 9;
    private static final int DEFAULT_COOKING_TICKS = 200;
    private static final int MAX_LANE_COUNT = 64;

    private ForbiddenArcanusRecipes() {
    }

    /** One furnace recipe: the input, the result of one operation, its time and experience. */
    public record Cooking(Ingredient input, ItemStack result, int ticks, float experience) {
    }

    //? if >=26.1 {
    public static List<Identifier> types() {
        return List.of(id(MOD, MELTING), id(MOD, ALLOYING), id(MOD, COMBUSTION));
    }

    public static boolean isAlloyType(Identifier type) {
        return type.getNamespace().equals(MOD) && (type.getPath().equals(ALLOYING) || type.getPath().equals(COMBUSTION));
    }

    public static boolean isCookingType(Identifier type) {
        return type.getNamespace().equals(MOD) && (type.getPath().equals(MELTING) || type.getPath().equals(COMBUSTION));
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private static Identifier parseId(String text) {
        return Identifier.tryParse(text);
    }
    //?} else {
    /*public static List<ResourceLocation> types() {
        return List.of(id(MOD, MELTING), id(MOD, ALLOYING), id(MOD, COMBUSTION));
    }

    public static boolean isAlloyType(ResourceLocation type) {
        return type.getNamespace().equals(MOD) && (type.getPath().equals(ALLOYING) || type.getPath().equals(COMBUSTION));
    }

    public static boolean isCookingType(ResourceLocation type) {
        return type.getNamespace().equals(MOD) && (type.getPath().equals(MELTING) || type.getPath().equals(COMBUSTION));
    }

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static ResourceLocation parseId(String text) {
        return ResourceLocation.tryParse(text);
    }
    *///?}

    /** The alloy of an encoded Clibano recipe, or empty when it is not an alloy the Electric Furnace can run. */
    public static Optional<AlloyingRecipe> alloy(JsonObject json, RegistryOps<JsonElement> ops, HolderLookup.Provider registries) {
        List<SizedIngredient> ingredients = new ArrayList<>();
        int ticks;
        if (json.has("required_materials") && json.get("required_materials").isJsonArray()) {
            for (JsonElement material : json.getAsJsonArray("required_materials")) {
                ItemStack stack = molten(material, registries);
                if (stack.isEmpty()) {
                    return Optional.empty();
                }
                ingredients.add(new SizedIngredient(Ingredient.of(stack.getItem()), stack.getCount()));
            }
            ticks = positiveInt(json.get("duration"), AlloyingRecipe.DEFAULT_PROCESSING_TICKS);
        } else if (json.get("ingredients") instanceof JsonObject pair && pair.has("first") && pair.has("second")) {
            for (String key : List.of("first", "second")) {
                Optional<Ingredient> ingredient = ingredient(pair.get(key), ops);
                if (ingredient.isEmpty()) {
                    return Optional.empty();
                }
                ingredients.add(new SizedIngredient(ingredient.get(), 1));
            }
            ticks = cookingTicks(json.get("cooking_time"), AlloyingRecipe.DEFAULT_PROCESSING_TICKS);
        } else {
            return Optional.empty();
        }
        ItemStack result = itemResult(json.get("result"), ops);
        if (result.isEmpty() || ingredients.isEmpty() || ingredients.size() > AlloyRecipeInput.LANES
                || ingredients.stream().anyMatch(s -> s.count() > MAX_LANE_COUNT)) {
            return Optional.empty();
        }
        return Optional.of(new AlloyingRecipe(ingredients, result, Optional.empty(), ticks, experience(json)));
    }

    /** The furnace recipe of an encoded Clibano melting of one item, or empty. */
    public static Optional<Cooking> cooking(JsonObject json, RegistryOps<JsonElement> ops, HolderLookup.Provider registries) {
        JsonElement input = json.has("ingredient") ? json.get("ingredient") : json.get("ingredients");
        if (input == null || input instanceof JsonObject inputs && inputs.has("first")) {
            return Optional.empty();
        }
        Optional<Ingredient> ingredient = ingredient(input, ops);
        JsonElement resultJson = json.get("result");
        ItemStack result = resultJson instanceof JsonObject units && units.has("amount") && !units.has("id")
                ? molten(resultJson, registries) : itemResult(resultJson, ops);
        if (ingredient.isEmpty() || result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Cooking(ingredient.get(), result, cookingTicks(json.get("cooking_time"), DEFAULT_COOKING_TICKS), experience(json)));
    }

    /** The result item of a molten material ({@code {type, amount}}), one per {@value #UNITS_PER_ITEM} units, or EMPTY. */
    private static ItemStack molten(JsonElement material, HolderLookup.Provider registries) {
        if (!(material instanceof JsonObject object) || !object.has("type") || !object.get("type").isJsonPrimitive()) {
            return ItemStack.EMPTY;
        }
        int amount = positiveInt(object.get("amount"), 0);
        var typeId = parseId(object.get("type").getAsString());
        if (typeId == null || amount <= 0 || amount % UNITS_PER_ITEM != 0) {
            return ItemStack.EMPTY;
        }
        ResourceKey<Registry<Object>> registry = ResourceKey.createRegistryKey(id(MOD, "molten_material_type"));
        Optional<? extends HolderLookup.RegistryLookup<Object>> lookup = registries.lookup(registry);
        if (lookup.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Optional<Holder.Reference<Object>> type = lookup.get().get(ResourceKey.create(registry, typeId));
        if (type.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = resultOf(type.get().value());
        return result.isEmpty() ? ItemStack.EMPTY : result.copyWithCount(result.getCount() * amount / UNITS_PER_ITEM);
    }

    private static ItemStack resultOf(Object type) {
        try {
            Method accessor = type.getClass().getMethod("result");
            accessor.setAccessible(true);
            Object result = accessor.invoke(type);
            //? if >=26.1 {
            if (result instanceof ItemStackTemplate template) {
                return template.create();
            }
            //?}
            if (result instanceof ItemStack stack) {
                return stack.copy();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static Optional<Ingredient> ingredient(JsonElement json, RegistryOps<JsonElement> ops) {
        //? if >=26.1 {
        return Ingredient.CODEC.parse(ops, json).result();
        //?} else {
        /*return Ingredient.CODEC_NONEMPTY.parse(ops, json).result();
        *///?}
    }

    private static ItemStack itemResult(JsonElement json, RegistryOps<JsonElement> ops) {
        if (!(json instanceof JsonObject)) {
            return ItemStack.EMPTY;
        }
        //? if >=26.1 {
        return ItemStack.CODEC.parse(ops, json).result().orElse(ItemStack.EMPTY);
        //?} else {
        /*return ItemStack.STRICT_CODEC.parse(ops, json).result().orElse(ItemStack.EMPTY);
        *///?}
    }

    /** The time of a plain number, or the first time of a per-fire table. */
    private static int cookingTicks(JsonElement json, int fallback) {
        if (json instanceof JsonObject table) {
            for (var entry : table.entrySet()) {
                int ticks = positiveInt(entry.getValue(), 0);
                if (ticks > 0) {
                    return ticks;
                }
            }
            return fallback;
        }
        return positiveInt(json, fallback);
    }

    private static int positiveInt(JsonElement json, int fallback) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber() && json.getAsInt() > 0) {
            return json.getAsInt();
        }
        return fallback;
    }

    private static float experience(JsonObject json) {
        JsonElement xp = json.get("experience");
        return xp != null && xp.isJsonPrimitive() && xp.getAsJsonPrimitive().isNumber() ? Math.max(0, xp.getAsFloat()) : 0.0F;
    }
}
