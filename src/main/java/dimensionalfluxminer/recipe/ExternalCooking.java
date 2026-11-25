package dimensionalfluxminer.recipe;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.ForbiddenArcanusRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=26.1 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
//? if >=26.1 {
import net.minecraft.world.item.crafting.RecipeMap;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeManager;
*///?}
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/**
 * Furnace recipes of other mods' furnaces that only the Electric Furnace runs (today the Clibano melting,
 * {@link ForbiddenArcanusRecipes}). They are kept out of the recipe manager, so vanilla furnaces never use them, and are
 * looked up only for items without a blast furnace or furnace recipe.
 */
public final class ExternalCooking {
    //? if >=26.1 {
    private static RecipeMap cachedManager;
    //?} else {
    /*private static RecipeManager cachedManager;
    *///?}
    private static List<RecipeHolder<SmeltingRecipe>> cached = List.of();

    private ExternalCooking() {
    }

    //? if >=26.1 {
    public static synchronized List<RecipeHolder<SmeltingRecipe>> all(RecipeMap manager, HolderLookup.Provider registries) {
    //?} else {
    /*public static synchronized List<RecipeHolder<SmeltingRecipe>> all(RecipeManager manager, HolderLookup.Provider registries) {
    *///?}
        if (manager != cachedManager) {
            cached = List.copyOf(convert(manager, registries));
            cachedManager = manager;
        }
        return cached;
    }

    /** The converted furnace recipe of the item, if any. */
    public static Optional<RecipeHolder<SmeltingRecipe>> find(Level level, SingleRecipeInput input) {
        //? if >=26.1 {
        for (RecipeHolder<SmeltingRecipe> holder : all(dimensionalfluxminer.recipe.Recipes.map(level), level.registryAccess())) {
        //?} else {
        /*for (RecipeHolder<SmeltingRecipe> holder : all(level.getRecipeManager(), level.registryAccess())) {
        *///?}
            if (holder.value().matches(input, level)) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    //? if >=26.1 {
    private static List<RecipeHolder<SmeltingRecipe>> convert(RecipeMap manager, HolderLookup.Provider registries) {
    //?} else {
    /*private static List<RecipeHolder<SmeltingRecipe>> convert(RecipeManager manager, HolderLookup.Provider registries) {
    *///?}
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        List<RecipeHolder<SmeltingRecipe>> out = new ArrayList<>();
        //? if >=26.1 {
        for (RecipeHolder<?> holder : manager.values()) {
            Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        //?} else {
        /*for (RecipeHolder<?> holder : manager.getRecipes()) {
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        *///?}
            if (type == null || !ForbiddenArcanusRecipes.isCookingType(type)) {
                continue;
            }
            Optional<ForbiddenArcanusRecipes.Cooking> cooking = ExternalAlloying.encode(holder.value(), ops)
                    .flatMap(json -> ForbiddenArcanusRecipes.cooking(json, ops, registries));
            if (cooking.isEmpty()) {
                continue;
            }
            ForbiddenArcanusRecipes.Cooking c = cooking.get();
            //? if >=26.1 {
            Identifier original = holder.id().identifier();
            ResourceKey<Recipe<?>> id = ResourceKey.create(Registries.RECIPE, DimensionalFluxMiner.id("external/" + original.getNamespace() + "/" + original.getPath()));
            SmeltingRecipe recipe = new SmeltingRecipe(new Recipe.CommonInfo(false),
                    new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), c.input(),
                    ItemStackTemplate.fromNonEmptyStack(c.result()), c.experience(), c.ticks());
            //?} else {
            /*ResourceLocation id = DimensionalFluxMiner.id("external/" + holder.id().getNamespace() + "/" + holder.id().getPath());
            SmeltingRecipe recipe = new SmeltingRecipe("", CookingBookCategory.MISC, c.input(), c.result(), c.experience(), c.ticks());
            *///?}
            out.add(new RecipeHolder<>(id, recipe));
        }
        return out;
    }
}
