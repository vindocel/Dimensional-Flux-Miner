package dimensionalfluxminer.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
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

/**
 * {@code dimensionalfluxminer:ore_crushing}: an ore block becomes {@code base_count} (or
 * {@code efficiency_upgrade_count}) canonical processing products. The product is either a fixed item or the
 * canonical member of an item tag such as {@code c:raw_materials/<material>}.
 */
public record OreCrushingRecipe(Ingredient ingredient, Optional<ItemStackTemplate> template, Optional<TagKey<Item>> resultTag,
                                int baseCount, int efficiencyUpgradeCount) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<OreCrushingRecipe> CODEC = RecordCodecBuilder.<OreCrushingRecipe>mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(OreCrushingRecipe::ingredient),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(OreCrushingRecipe::template),
            TagKey.codec(Registries.ITEM).optionalFieldOf("result_tag").forGetter(OreCrushingRecipe::resultTag),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("base_count", 2).forGetter(OreCrushingRecipe::baseCount),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("efficiency_upgrade_count", 3).forGetter(OreCrushingRecipe::efficiencyUpgradeCount)
    ).apply(i, OreCrushingRecipe::new)).validate(r -> r.template.isPresent() == r.resultTag.isPresent()
            ? DataResult.error(() -> "ore_crushing requires exactly one of 'result' or 'result_tag'")
            : DataResult.success(r));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreCrushingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, OreCrushingRecipe::ingredient,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), OreCrushingRecipe::template,
            ByteBufCodecs.optional(ByteBufCodecs.fromCodec(TagKey.codec(Registries.ITEM))), OreCrushingRecipe::resultTag,
            ByteBufCodecs.VAR_INT, OreCrushingRecipe::baseCount,
            ByteBufCodecs.VAR_INT, OreCrushingRecipe::efficiencyUpgradeCount,
            OreCrushingRecipe::new);

    /** The fixed product stack, when the recipe names one instead of a tag. */
    public Optional<ItemStack> result() {
        return template.map(ItemStackTemplate::create);
    }

    /** The single processing product (count 1), or empty when the tag currently has no members. */
    public ItemStack product() {
        if (template.isPresent()) {
            return template.get().create().copyWithCount(1);
        }
        return CanonicalItems.first(resultTag.orElseThrow()).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    public int yield(boolean efficiencyUpgrade) {
        return efficiencyUpgrade ? efficiencyUpgradeCount : baseCount;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && !product().isEmpty();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return product().copyWithCount(baseCount);
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
    public RecipeSerializer<OreCrushingRecipe> getSerializer() {
        return DfmRecipes.ORE_CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<OreCrushingRecipe> getType() {
        return DfmRecipes.ORE_CRUSHING.get();
    }
}
