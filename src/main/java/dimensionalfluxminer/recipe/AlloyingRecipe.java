package dimensionalfluxminer.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * {@code dimensionalfluxminer:alloying}: 1 to 3 counted ingredients (order independent) combined in one
 * operation into a main result and an optional byproduct.
 */
public record AlloyingRecipe(List<SizedIngredient> ingredients, ItemStack result, Optional<Byproduct> byproduct,
                             int processingTicks, float experience) implements Recipe<AlloyRecipeInput> {
    public static final int DEFAULT_PROCESSING_TICKS = 100;

    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.<AlloyingRecipe>mapCodec(i -> i.group(
            SizedIngredient.NESTED_CODEC.listOf().fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
            Byproduct.CODEC.optionalFieldOf("byproduct").forGetter(AlloyingRecipe::byproduct),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_ticks", DEFAULT_PROCESSING_TICKS).forGetter(AlloyingRecipe::processingTicks),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("experience", 0.0F).forGetter(AlloyingRecipe::experience)
    ).apply(i, AlloyingRecipe::new)).validate(r -> r.ingredients.isEmpty() || r.ingredients.size() > AlloyRecipeInput.LANES
            ? DataResult.error(() -> "alloying requires 1 to 3 ingredients")
            : DataResult.success(r));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyingRecipe::ingredients,
            ItemStack.STREAM_CODEC, AlloyingRecipe::result,
            ByteBufCodecs.optional(Byproduct.STREAM_CODEC), AlloyingRecipe::byproduct,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::processingTicks,
            ByteBufCodecs.FLOAT, AlloyingRecipe::experience,
            AlloyingRecipe::new);

    /**
     * Maps each recipe ingredient to the input lane that supplies it. Every occupied lane must be used and
     * every ingredient must be supplied by exactly one lane; returns {@code null} when the lanes do not match.
     */
    public int[] laneAssignment(AlloyRecipeInput input) {
        int occupied = 0;
        for (int lane = 0; lane < AlloyRecipeInput.LANES; lane++) {
            if (!input.getItem(lane).isEmpty()) {
                occupied++;
            }
        }
        if (occupied != ingredients.size()) {
            return null;
        }
        int[] assignment = new int[ingredients.size()];
        return assign(input, 0, new boolean[AlloyRecipeInput.LANES], assignment) ? assignment : null;
    }

    private boolean assign(AlloyRecipeInput input, int index, boolean[] used, int[] assignment) {
        if (index == ingredients.size()) {
            return true;
        }
        Ingredient ingredient = ingredients.get(index).ingredient();
        for (int lane = 0; lane < AlloyRecipeInput.LANES; lane++) {
            ItemStack stack = input.getItem(lane);
            if (!used[lane] && !stack.isEmpty() && ingredient.test(stack)) {
                used[lane] = true;
                assignment[index] = lane;
                if (assign(input, index + 1, used, assignment)) {
                    return true;
                }
                used[lane] = false;
            }
        }
        return false;
    }

    @Override
    public boolean matches(AlloyRecipeInput input, Level level) {
        return laneAssignment(input) != null;
    }

    @Override
    public ItemStack assemble(AlloyRecipeInput input, HolderLookup.Provider registries) {
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
        NonNullList<Ingredient> list = NonNullList.create();
        ingredients.forEach(s -> list.add(s.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DfmRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return DfmRecipes.ALLOYING.get();
    }

    /** Optional secondary output with an independent chance per operation. */
    public record Byproduct(ItemStack stack, float chance) {
        public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(b -> b.stack.getItemHolder()),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(b -> b.stack.getCount()),
                Codec.floatRange(0, 1).optionalFieldOf("chance", 1.0F).forGetter(Byproduct::chance)
        ).apply(i, (item, count, chance) -> new Byproduct(new ItemStack(item, count), chance)));
        public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Byproduct::stack,
                ByteBufCodecs.FLOAT, Byproduct::chance,
                Byproduct::new);
    }
}
