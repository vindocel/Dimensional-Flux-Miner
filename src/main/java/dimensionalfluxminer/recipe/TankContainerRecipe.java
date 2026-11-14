package dimensionalfluxminer.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

/** One container converted in place while filling from or emptying into a tank. */
public record TankContainerRecipe(Ingredient ingredient, ItemStackTemplate template, FluidStackTemplate fluidTemplate,
                                  boolean filling, Optional<TagKey<Fluid>> fluidTag) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<TankContainerRecipe> CODEC = RecordCodecBuilder.<TankContainerRecipe>mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(TankContainerRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(TankContainerRecipe::template),
            FluidStackTemplate.CODEC.fieldOf("fluid").forGetter(TankContainerRecipe::fluidTemplate),
            Codec.BOOL.fieldOf("filling").forGetter(TankContainerRecipe::filling),
            TagKey.codec(Registries.FLUID).optionalFieldOf("fluid_tag").forGetter(TankContainerRecipe::fluidTag)
    ).apply(i, TankContainerRecipe::new)).validate(recipe -> recipe.template().count() == 1
            ? DataResult.success(recipe) : DataResult.error(() -> "A tank container recipe must produce one item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TankContainerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, TankContainerRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, TankContainerRecipe::template,
            FluidStackTemplate.STREAM_CODEC, TankContainerRecipe::fluidTemplate,
            ByteBufCodecs.BOOL, TankContainerRecipe::filling,
            ByteBufCodecs.optional(net.minecraft.resources.Identifier.STREAM_CODEC.map(
                    id -> TagKey.create(Registries.FLUID, id), TagKey::location)), TankContainerRecipe::fluidTag,
            TankContainerRecipe::new);

    public ItemStack result() {
        return template.create();
    }

    public FluidStack fluid() {
        return fluidTemplate.create();
    }

    /** Use matching fluid already stored in the tank; an empty tank receives the canonical fluid. */
    public FluidStack fluidFor(FluidStack stored) {
        return !stored.isEmpty() && fluidTag.isPresent() && stored.is(fluidTag.get())
                ? stored.copyWithAmount(fluidTemplate.amount()) : fluid();
    }

    public List<FluidStack> fluids() {
        List<FluidStack> choices = new ArrayList<>();
        choices.add(fluid());
        fluidTag.ifPresent(tag -> BuiltInRegistries.FLUID.getTagOrEmpty(tag).forEach(holder -> {
            if (!holder.is(fluidTemplate.fluid())) {
                choices.add(new FluidStack(holder, fluidTemplate.amount()));
            }
        }));
        return choices.stream().sorted(Comparator.comparing(stack ->
                BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString())).toList();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return result();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<TankContainerRecipe> getSerializer() {
        return DfmRecipes.TANK_CONTAINER_SERIALIZER.get();
    }

    @Override
    public RecipeType<TankContainerRecipe> getType() {
        return DfmRecipes.TANK_CONTAINER.get();
    }

}
