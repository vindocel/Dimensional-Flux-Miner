package dimensionalfluxminer.compat;

import dimensionalfluxminer.crusher.CrusherProcessing;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmRecipes;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
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

/** Recipe data shared by the optional recipe viewer plugins (client side). */
public final class RecipeViewerData {
    private RecipeViewerData() {
    }

    /** Crusher entries; empty until a world is loaded (the dust rules depend on the loaded tags). */
    public static List<CrusherProcessing.Display> crushing() {
        Level level = Minecraft.getInstance().level;
        return level == null ? List.of() : CrusherProcessing.displays(level);
    }

    /**
     * The mod's alloys and the alloys of other mods the Electric Furnace runs, as the viewers list them (see
     * {@link #forDisplay}).
     */
    //? if >=26.1 {
    public static List<RecipeHolder<AlloyingRecipe>> alloying(RecipeMap manager) {
    //?} else {
    /*public static List<RecipeHolder<AlloyingRecipe>> alloying(RecipeManager manager) {
    *///?}
        Level level = Minecraft.getInstance().level;
        //? if >=26.1 {
        return forDisplay(level == null ? List.copyOf(manager.byType(DfmRecipes.ALLOYING.get()))
        //?} else {
        /*return forDisplay(level == null ? List.copyOf(manager.getAllRecipesFor(DfmRecipes.ALLOYING.get()))
        *///?}
                : dimensionalfluxminer.recipe.ExternalAlloying.all(manager, level.registryAccess()));
    }

    /**
     * Viewer list of the alloys: recipes with an input or a result that resolves to no item (or to the barrier
     * placeholder) are left out, and recipes with the same result, byproduct and time whose inputs differ only in the
     * items of one lane (same count) become one entry whose lane cycles through all of them. An exact repeat is dropped.
     * Only the list shown changes: the Electric Furnace still matches each original recipe.
     */
    static List<RecipeHolder<AlloyingRecipe>> forDisplay(List<RecipeHolder<AlloyingRecipe>> recipes) {
        List<RecipeHolder<AlloyingRecipe>> out = new ArrayList<>();
        for (RecipeHolder<AlloyingRecipe> holder : recipes) {
            AlloyingRecipe recipe = holder.value();
            if (!displayable(recipe)) {
                continue;
            }
            boolean joined = false;
            for (int i = 0; i < out.size() && !joined; i++) {
                AlloyingRecipe merged = join(out.get(i).value(), recipe);
                if (merged != null) {
                    out.set(i, new RecipeHolder<>(out.get(i).id(), merged));
                    joined = true;
                }
            }
            if (!joined) {
                out.add(holder);
            }
        }
        return out;
    }

    private static boolean displayable(AlloyingRecipe recipe) {
        ItemStack result = recipe.result();
        if (result.isEmpty() || result.is(net.minecraft.world.item.Items.BARRIER) || recipe.ingredients().isEmpty()) {
            return false;
        }
        for (net.neoforged.neoforge.common.crafting.SizedIngredient sized : recipe.ingredients()) {
            Set<Item> items = items(sized.ingredient());
            if (items.isEmpty() || items.contains(net.minecraft.world.item.Items.BARRIER)) {
                return false;
            }
        }
        return true;
    }

    /** {@code a} with {@code b} folded in, or null when they are not the same recipe apart from one lane's items. */
    private static AlloyingRecipe join(AlloyingRecipe a, AlloyingRecipe b) {
        List<net.neoforged.neoforge.common.crafting.SizedIngredient> left = a.ingredients();
        List<net.neoforged.neoforge.common.crafting.SizedIngredient> right = b.ingredients();
        if (left.size() != right.size() || a.processingTicks() != b.processingTicks()
                || !sameStack(a.result(), b.result()) || !sameByproduct(a, b)) {
            return null;
        }
        int differing = -1;
        Set<Item> union = null;
        for (int lane = 0; lane < left.size(); lane++) {
            if (left.get(lane).count() != right.get(lane).count()) {
                return null;
            }
            Set<Item> l = items(left.get(lane).ingredient());
            Set<Item> r = items(right.get(lane).ingredient());
            if (!l.equals(r)) {
                if (differing >= 0) {
                    return null;
                }
                differing = lane;
                union = new java.util.LinkedHashSet<>(l);
                union.addAll(r);
            }
        }
        if (differing < 0) {
            return a;
        }
        List<net.neoforged.neoforge.common.crafting.SizedIngredient> lanes = new ArrayList<>(left);
        lanes.set(differing, new net.neoforged.neoforge.common.crafting.SizedIngredient(
                Ingredient.of(union.toArray(Item[]::new)), left.get(differing).count()));
        return new AlloyingRecipe(lanes, a.result(), a.byproduct(), a.processingTicks(), Math.max(a.experience(), b.experience()));
    }

    private static boolean sameByproduct(AlloyingRecipe a, AlloyingRecipe b) {
        if (a.byproduct().isPresent() != b.byproduct().isPresent()) {
            return false;
        }
        return a.byproduct().isEmpty() || a.byproduct().get().chance() == b.byproduct().get().chance()
                && sameStack(a.byproduct().get().stack(), b.byproduct().get().stack());
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
    }

    /** The items an ingredient accepts, in its own order. */
    private static Set<Item> items(Ingredient ingredient) {
        Set<Item> items = new java.util.LinkedHashSet<>();
        //? if >=26.1 {
        for (ItemStack stack : dimensionalfluxminer.recipe.Recipes.stacks(ingredient)) {
        //?} else {
        /*for (ItemStack stack : ingredient.getItems()) {
        *///?}
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        return items;
    }

    /**
     * One Electric Furnace furnace-mode operation: the blast furnace recipe of an item, or its furnace recipe when it
     * has none. Raw -> Ingot shows the bulk yield (2 -> 3; 1 -> 2 with the Efficiency Upgrade).
     */
    //? if >=26.1 {
    public record FurnaceDisplay(Identifier id, Ingredient input, int inputCount, ItemStack output, boolean bulk, int cookTicks) {
    //?} else {
    /*public record FurnaceDisplay(ResourceLocation id, Ingredient input, int inputCount, ItemStack output, boolean bulk, int cookTicks) {
    *///?}
    }

    //? if >=26.1 {
    public static List<FurnaceDisplay> electricFurnaceSmelting(RecipeMap manager, RegistryAccess registries) {
    //?} else {
    /*public static List<FurnaceDisplay> electricFurnaceSmelting(RecipeManager manager, RegistryAccess registries) {
    *///?}
        List<FurnaceDisplay> result = new ArrayList<>();
        Set<Item> blastable = new HashSet<>();
        Set<Item> smeltable = new HashSet<>();
        //? if >=26.1 {
        for (RecipeHolder<?> holder : manager.byType(RecipeType.BLASTING)) {
        //?} else {
        /*for (RecipeHolder<?> holder : manager.getAllRecipesFor(RecipeType.BLASTING)) {
        *///?}
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
            //? if >=26.1 {
            for (ItemStack option : dimensionalfluxminer.recipe.Recipes.stacks(recipe.input())) {
            //?} else {
            /*for (ItemStack option : recipe.getIngredients().getFirst().getItems()) {
            *///?}
                blastable.add(option.getItem());
            }
            //? if >=26.1 {
            add(result, holder.id().identifier(), recipe, registries);
        }
        for (RecipeHolder<?> holder : manager.byType(RecipeType.SMELTING)) {
            //?} else {
            /*add(result, holder.id(), recipe, registries);
        }
        for (RecipeHolder<?> holder : manager.getAllRecipesFor(RecipeType.SMELTING)) {
            *///?}
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
            //? if >=26.1 {
            ItemStack[] options = dimensionalfluxminer.recipe.Recipes.stacks(recipe.input());
            //?} else {
            /*ItemStack[] options = recipe.getIngredients().getFirst().getItems();
            *///?}
            boolean allBlastable = options.length > 0;
            for (ItemStack option : options) {
                allBlastable &= blastable.contains(option.getItem());
                smeltable.add(option.getItem());
            }
            if (!allBlastable) {
                //? if >=26.1 {
                add(result, holder.id().identifier(), recipe, registries);
                //?} else {
                /*add(result, holder.id(), recipe, registries);
                *///?}
            }
        }
        // Furnace recipes of other mods' furnaces, for the items no blast furnace or furnace recipe takes.
        for (RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe> holder : dimensionalfluxminer.recipe.ExternalCooking.all(manager, registries)) {
            //? if >=26.1 {
            ItemStack[] options = dimensionalfluxminer.recipe.Recipes.stacks(holder.value().input());
            //?} else {
            /*ItemStack[] options = holder.value().getIngredients().getFirst().getItems();
            *///?}
            boolean covered = true;
            for (ItemStack option : options) {
                covered &= blastable.contains(option.getItem()) || smeltable.contains(option.getItem());
            }
            if (!covered) {
                //? if >=26.1 {
                add(result, holder.id().identifier(), holder.value(), registries);
                //?} else {
                /*add(result, holder.id(), holder.value(), registries);
                *///?}
            }
        }
        return result;
    }

    //? if >=26.1 {
    private static void add(List<FurnaceDisplay> out, Identifier id, AbstractCookingRecipe recipe, RegistryAccess registries) {
        Ingredient input = recipe.input();
        ItemStack[] inputs = dimensionalfluxminer.recipe.Recipes.stacks(input);
        if (inputs.length == 0) {
            return;
        }
        ItemStack output = recipe.assemble(new SingleRecipeInput(inputs[0]));
        if (output.isEmpty()) {
    //?} else {
    /*private static void add(List<FurnaceDisplay> out, ResourceLocation id, AbstractCookingRecipe recipe, RegistryAccess registries) {
        ItemStack output = recipe.getResultItem(registries);
        Ingredient input = recipe.getIngredients().getFirst();
        if (output.isEmpty() || input.isEmpty()) {
    *///?}
            return;
        }
        boolean bulk = false;
        //? if >=26.1 {
        for (ItemStack option : inputs) {
        //?} else {
        /*for (ItemStack option : input.getItems()) {
        *///?}
            bulk |= ElectricFurnaceBlockEntity.isRawToIngot(option, output);
        }
        //? if >=26.1 {
        out.add(new FurnaceDisplay(id, input, bulk ? 2 : 1, bulk ? output.copyWithCount(3) : output.copy(), bulk, recipe.cookingTime()));
        //?} else {
        /*out.add(new FurnaceDisplay(id, input, bulk ? 2 : 1, bulk ? output.copyWithCount(3) : output.copy(), bulk, recipe.getCookingTime()));
        *///?}
    }

    /** Extra line for crusher outputs that change with the Efficiency Upgrade. */
    public static Component upgradeLine(CrusherProcessing.Display display) {
        return Component.translatable("recipe_viewer.dimensionalfluxminer.crushing.upgrade", display.upgradedOutput().getCount());
    }

    public static Component bulkLine() {
        return Component.translatable("recipe_viewer.dimensionalfluxminer.smelting.bulk");
    }

    public static Component chanceLine(float chance) {
        return Component.translatable("recipe_viewer.dimensionalfluxminer.chance", Math.round(chance * 100));
    }
}
