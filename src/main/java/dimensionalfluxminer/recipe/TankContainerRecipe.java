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
//? if <26.1 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
//? if <26.1 {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
*///?}
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?}
import net.minecraft.world.item.crafting.Ingredient;
//? if >=26.1 {
import net.minecraft.world.item.crafting.PlacementInfo;
//?}
import net.minecraft.world.item.crafting.Recipe;
//? if >=26.1 {
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
//?}
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.tags.TagKey;
//?}
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
//? if >=26.1 {
import net.neoforged.neoforge.fluids.FluidStackTemplate;
//?}

/** One container converted in place while filling from or emptying into a tank. */
//? if >=26.1 {
public record TankContainerRecipe(Ingredient ingredient, ItemStackTemplate template, FluidStackTemplate fluidTemplate,
//?} else {
/*public record TankContainerRecipe(Ingredient ingredient, ItemStack template, FluidStack fluidTemplate,
*///?}
                                  boolean filling, Optional<TagKey<Fluid>> fluidTag) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<TankContainerRecipe> CODEC = RecordCodecBuilder.<TankContainerRecipe>mapCodec(i -> i.group(
            //? if >=26.1 {
            Ingredient.CODEC.fieldOf("ingredient").forGetter(TankContainerRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(TankContainerRecipe::template),
            FluidStackTemplate.CODEC.fieldOf("fluid").forGetter(TankContainerRecipe::fluidTemplate),
            //?} else {
            /*Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(TankContainerRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TankContainerRecipe::template),
            FluidStack.CODEC.fieldOf("fluid").forGetter(TankContainerRecipe::fluidTemplate),
            *///?}
            Codec.BOOL.fieldOf("filling").forGetter(TankContainerRecipe::filling),
            TagKey.codec(Registries.FLUID).optionalFieldOf("fluid_tag").forGetter(TankContainerRecipe::fluidTag)
    //? if >=26.1 {
    ).apply(i, TankContainerRecipe::new)).validate(recipe -> recipe.template().count() == 1
    //?} else {
    /*).apply(i, TankContainerRecipe::new)).validate(recipe -> recipe.template().getCount() == 1
    *///?}
            ? DataResult.success(recipe) : DataResult.error(() -> "A tank container recipe must produce one item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TankContainerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, TankContainerRecipe::ingredient,
            //? if >=26.1 {
            ItemStackTemplate.STREAM_CODEC, TankContainerRecipe::template,
            FluidStackTemplate.STREAM_CODEC, TankContainerRecipe::fluidTemplate,
            //?} else {
            /*ItemStack.STREAM_CODEC, TankContainerRecipe::template,
            FluidStack.STREAM_CODEC, TankContainerRecipe::fluidTemplate,
            *///?}
            ByteBufCodecs.BOOL, TankContainerRecipe::filling,
            //? if >=26.1 {
            ByteBufCodecs.optional(net.minecraft.resources.Identifier.STREAM_CODEC.map(
            //?} else {
            /*ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC.map(
            *///?}
                    id -> TagKey.create(Registries.FLUID, id), TagKey::location)), TankContainerRecipe::fluidTag,
            TankContainerRecipe::new);

    public ItemStack result() {
        //? if >=26.1 {
        return template.create();
        //?} else {
        /*return template.copy();
        *///?}
    }

    public FluidStack fluid() {
        //? if >=26.1 {
        return fluidTemplate.create();
        //?} else {
        /*return fluidTemplate.copy();
        *///?}
    }

    /** Use matching fluid already stored in the tank; an empty tank receives the canonical fluid. */
    public FluidStack fluidFor(FluidStack stored) {
        //? if >=26.1 {
        return !stored.isEmpty() && fluidTag.isPresent() && stored.is(fluidTag.get())
                ? stored.copyWithAmount(fluidTemplate.amount()) : fluid();
        //?} else {
        /*return !stored.isEmpty() && fluidTag.isPresent() && stored.is(fluidTag.get())
                ? stored.copyWithAmount(fluidTemplate.getAmount()) : fluid();
        *///?}
    }

    public List<FluidStack> fluids() {
        List<FluidStack> choices = new ArrayList<>();
        choices.add(fluid());
        fluidTag.ifPresent(tag -> BuiltInRegistries.FLUID.getTagOrEmpty(tag).forEach(holder -> {
            //? if >=26.1 {
            if (!holder.is(fluidTemplate.fluid())) {
                choices.add(new FluidStack(holder, fluidTemplate.amount()));
            //?} else {
            /*if (holder.value() != fluidTemplate.getFluid()) {
                choices.add(new FluidStack(holder, fluidTemplate.getAmount()));
            *///?}
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
    //? if >=26.1 {
    public ItemStack assemble(SingleRecipeInput input) {
    //?} else {
    /*public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
    *///?}
        return result();
    //? if <26.1 {
    /*}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return template;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    *///?}
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
    //? if >=26.1 {
    public String group() {
    //?} else {
    /*public String getGroup() {
    *///?}
        return "";
    //? if >=26.1 {
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    //?}
    }

    @Override
    public RecipeSerializer<TankContainerRecipe> getSerializer() {
        return DfmRecipes.TANK_CONTAINER_SERIALIZER.get();
    }

    @Override
    public RecipeType<TankContainerRecipe> getType() {
        return DfmRecipes.TANK_CONTAINER.get();
    }
//? if >=26.1 {

//?}
}
