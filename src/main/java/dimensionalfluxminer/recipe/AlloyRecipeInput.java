package dimensionalfluxminer.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The three Alloy Smelter input lanes, each represented by one stack of its current item. */
public record AlloyRecipeInput(ItemStack lane1, ItemStack lane2, ItemStack lane3) implements RecipeInput {
    public static final int LANES = 3;

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> lane1;
            case 1 -> lane2;
            case 2 -> lane3;
            default -> throw new IllegalArgumentException("No alloy lane " + index);
        };
    }

    @Override
    public int size() {
        return LANES;
    }
}
