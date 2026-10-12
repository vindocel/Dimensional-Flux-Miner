package dimensionalfluxminer.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.registry.DfmRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** {@code dimensionalfluxminer:crushing}: one input item becomes a fixed output stack in the Crusher. */
public record CrushingRecipe(Ingredient ingredient, ItemStack result) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrushingRecipe::result)
    ).apply(i, CrushingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
            ItemStack.STREAM_CODEC, CrushingRecipe::result,
            CrushingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DfmRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return DfmRecipes.CRUSHING.get();
    }
}
