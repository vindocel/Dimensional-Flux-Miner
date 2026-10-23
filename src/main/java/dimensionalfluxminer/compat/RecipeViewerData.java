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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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

    /** The mod's alloys and the alloys of other mods the Electric Furnace runs. */
    public static List<RecipeHolder<AlloyingRecipe>> alloying(RecipeMap manager) {
        Level level = Minecraft.getInstance().level;
        return level == null ? List.copyOf(manager.byType(DfmRecipes.ALLOYING.get()))
                : dimensionalfluxminer.recipe.ExternalAlloying.all(manager, level.registryAccess());
    }

    /**
     * One Electric Furnace furnace-mode operation: the blast furnace recipe of an item, or its furnace recipe when it
     * has none. Raw -> Ingot shows the bulk yield (2 -> 3; 1 -> 2 with the Efficiency Upgrade).
     */
    public record FurnaceDisplay(Identifier id, Ingredient input, int inputCount, ItemStack output, boolean bulk, int cookTicks) {
    }

    public static List<FurnaceDisplay> electricFurnaceSmelting(RecipeMap manager, RegistryAccess registries) {
        List<FurnaceDisplay> result = new ArrayList<>();
        Set<Item> blastable = new HashSet<>();
        for (RecipeHolder<?> holder : manager.byType(RecipeType.BLASTING)) {
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
            for (ItemStack option : dimensionalfluxminer.recipe.Recipes.stacks(recipe.input())) {
                blastable.add(option.getItem());
            }
            add(result, holder.id().identifier(), recipe, registries);
        }
        for (RecipeHolder<?> holder : manager.byType(RecipeType.SMELTING)) {
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
            ItemStack[] options = dimensionalfluxminer.recipe.Recipes.stacks(recipe.input());
            boolean allBlastable = options.length > 0;
            for (ItemStack option : options) {
                allBlastable &= blastable.contains(option.getItem());
            }
            if (!allBlastable) {
                add(result, holder.id().identifier(), recipe, registries);
            }
        }
        return result;
    }

    private static void add(List<FurnaceDisplay> out, Identifier id, AbstractCookingRecipe recipe, RegistryAccess registries) {
        Ingredient input = recipe.input();
        ItemStack[] inputs = dimensionalfluxminer.recipe.Recipes.stacks(input);
        if (inputs.length == 0) {
            return;
        }
        ItemStack output = recipe.assemble(new SingleRecipeInput(inputs[0]));
        if (output.isEmpty()) {
            return;
        }
        boolean bulk = false;
        for (ItemStack option : inputs) {
            bulk |= ElectricFurnaceBlockEntity.isRawToIngot(option, output);
        }
        out.add(new FurnaceDisplay(id, input, bulk ? 2 : 1, bulk ? output.copyWithCount(3) : output.copy(), bulk, recipe.cookingTime()));
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
