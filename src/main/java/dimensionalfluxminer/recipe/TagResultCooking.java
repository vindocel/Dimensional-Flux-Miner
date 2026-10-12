package dimensionalfluxminer.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.registry.DfmRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;

/**
 * Furnace and blast furnace recipes whose result is the first item of an item tag (for example {@code c:ingots/tin}),
 * resolved when the recipe is used: a DFM dust smelts into whatever ingot the installed mods provide. The recipes
 * are loaded only when the tag has an item (a {@code neoforge:tag_empty} condition in the JSON).
 */
public final class TagResultCooking {
    private TagResultCooking() {
    }

    /** The result for the current tags, or EMPTY when no item has the tag. */
    public static ItemStack resolve(TagKey<Item> tag, int count) {
        return BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(set -> set.stream().findFirst())
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    public static final class Smelting extends SmeltingRecipe {
        public static final MapCodec<Smelting> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                TagKey.codec(Registries.ITEM).fieldOf("result_tag").forGetter(r -> r.resultTag),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(Smelting::getExperience),
                com.mojang.serialization.Codec.INT.optionalFieldOf("cookingtime", 200).forGetter(Smelting::getCookingTime)
        ).apply(i, Smelting::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Smelting> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ResourceLocation.STREAM_CODEC, r -> r.resultTag.location(),
                ByteBufCodecs.FLOAT, Smelting::getExperience,
                ByteBufCodecs.VAR_INT, Smelting::getCookingTime,
                (ingredient, tag, xp, time) -> new Smelting(ingredient, TagKey.create(Registries.ITEM, tag), xp, time));

        private final TagKey<Item> resultTag;

        public Smelting(Ingredient ingredient, TagKey<Item> resultTag, float experience, int cookingTime) {
            super("", CookingBookCategory.MISC, ingredient, ItemStack.EMPTY, experience, cookingTime);
            this.resultTag = resultTag;
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return !resolve(resultTag, 1).isEmpty() && super.matches(input, level);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return resolve(resultTag, 1);
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return resolve(resultTag, 1);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return DfmRecipes.TAG_SMELTING_SERIALIZER.get();
        }
    }

    public static final class Blasting extends BlastingRecipe {
        public static final MapCodec<Blasting> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                TagKey.codec(Registries.ITEM).fieldOf("result_tag").forGetter(r -> r.resultTag),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(Blasting::getExperience),
                com.mojang.serialization.Codec.INT.optionalFieldOf("cookingtime", 100).forGetter(Blasting::getCookingTime)
        ).apply(i, Blasting::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Blasting> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ResourceLocation.STREAM_CODEC, r -> r.resultTag.location(),
                ByteBufCodecs.FLOAT, Blasting::getExperience,
                ByteBufCodecs.VAR_INT, Blasting::getCookingTime,
                (ingredient, tag, xp, time) -> new Blasting(ingredient, TagKey.create(Registries.ITEM, tag), xp, time));

        private final TagKey<Item> resultTag;

        public Blasting(Ingredient ingredient, TagKey<Item> resultTag, float experience, int cookingTime) {
            super("", CookingBookCategory.MISC, ingredient, ItemStack.EMPTY, experience, cookingTime);
            this.resultTag = resultTag;
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return !resolve(resultTag, 1).isEmpty() && super.matches(input, level);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return resolve(resultTag, 1);
        }

        @Override
        public ItemStack getResultItem(HolderLookup.Provider registries) {
            return resolve(resultTag, 1);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return DfmRecipes.TAG_BLASTING_SERIALIZER.get();
        }
    }
}
