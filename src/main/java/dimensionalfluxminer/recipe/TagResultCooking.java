package dimensionalfluxminer.recipe;

//? if >=26.1 {
import com.mojang.serialization.Codec;
//?}
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.registry.DfmRecipes;
//? if >=26.1 {
import java.util.List;
//?} else {
/*import net.minecraft.core.HolderLookup;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
//?}
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=26.1 {
import net.minecraft.world.item.crafting.Recipe;
//?}
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
//? if >=26.1 {
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
//?}
import net.minecraft.world.level.Level;

/**
 * Furnace and blast furnace recipes whose result is the first item of an item tag (for example {@code c:ingots/tin}),
 * resolved when the recipe is used: a DFM dust smelts into whatever ingot the installed mods provide. The recipes
 * are loaded only when the tag has an item (a {@code neoforge:tag_empty} condition in the JSON).
 */
public final class TagResultCooking {
    //? if >=26.1 {
    /** Placeholder result of the vanilla recipe; the real one comes from the tag. */
    private static final ItemStackTemplate PLACEHOLDER = new ItemStackTemplate(Items.BARRIER);

    //?}
    private TagResultCooking() {
    }

    /** The result for the current tags, or EMPTY when no item has the tag. */
    public static ItemStack resolve(TagKey<Item> tag, int count) {
        //? if >=26.1 {
        return BuiltInRegistries.ITEM.get(tag)
        //?} else {
        /*return BuiltInRegistries.ITEM.getTag(tag)
        *///?}
                .flatMap(set -> set.stream().findFirst())
                .map(item -> new ItemStack(item, count))
                .orElse(ItemStack.EMPTY);
    }

    //? if =26.1.2 {
    private static <T extends AbstractCookingRecipe> MapCodec<T> codec(int defaultTime, Factory<T> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(AbstractCookingRecipe::input),
                TagKey.codec(Registries.ITEM).fieldOf("result_tag").forGetter(r -> ((Tagged) r).resultTag()),
                Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(AbstractCookingRecipe::experience),
                Codec.INT.optionalFieldOf("cookingtime", defaultTime).forGetter(AbstractCookingRecipe::cookingTime)
        ).apply(i, factory::create));
    }

    private static <T extends AbstractCookingRecipe> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(Factory<T> factory) {
        return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, AbstractCookingRecipe::input,
                Identifier.STREAM_CODEC, r -> ((Tagged) r).resultTag().location(),
                ByteBufCodecs.FLOAT, AbstractCookingRecipe::experience,
                ByteBufCodecs.VAR_INT, AbstractCookingRecipe::cookingTime,
                (ingredient, tag, xp, time) -> factory.create(ingredient, TagKey.create(Registries.ITEM, tag), xp, time));
    }

    private interface Factory<T> {
        T create(Ingredient ingredient, TagKey<Item> resultTag, float experience, int cookingTime);
    }

    /** A cooking recipe whose result comes from an item tag. */
    interface Tagged {
        TagKey<Item> resultTag();
    }

    private static List<RecipeDisplay> display(AbstractCookingRecipe recipe, TagKey<Item> tag, Item station) {
        SlotDisplay result = BuiltInRegistries.ITEM.get(tag).isPresent() ? new SlotDisplay.TagSlotDisplay(tag) : SlotDisplay.Empty.INSTANCE;
        return List.of(new FurnaceRecipeDisplay(recipe.input().display(), SlotDisplay.AnyFuel.INSTANCE, result,
                new SlotDisplay.ItemSlotDisplay(station), recipe.cookingTime(), recipe.experience()));
    }

    public static final class Smelting extends SmeltingRecipe implements Tagged {
        public static final MapCodec<Smelting> CODEC = codec(200, Smelting::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, Smelting> STREAM_CODEC = streamCodec(Smelting::new);
    //?} else if >=26.3 {
    /*private static <T extends AbstractCookingRecipe> MapCodec<T> codec(int defaultTime, Factory<T> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(AbstractCookingRecipe::input),
                TagKey.codec(Registries.ITEM).fieldOf("result_tag").forGetter(r -> ((Tagged) r).resultTag()),
                Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(AbstractCookingRecipe::experience),
                Codec.INT.optionalFieldOf("cookingtime", defaultTime).forGetter(AbstractCookingRecipe::cookingTime)
        ).apply(i, factory::create));
    }

    private static <T extends AbstractCookingRecipe> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(Factory<T> factory) {
        return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, AbstractCookingRecipe::input,
                Identifier.STREAM_CODEC, r -> ((Tagged) r).resultTag().location(),
                ByteBufCodecs.FLOAT, AbstractCookingRecipe::experience,
                ByteBufCodecs.VAR_INT, AbstractCookingRecipe::cookingTime,
                (ingredient, tag, xp, time) -> factory.create(ingredient, TagKey.create(Registries.ITEM, tag), xp, time));
    }

    private interface Factory<T> {
        T create(Ingredient ingredient, TagKey<Item> resultTag, float experience, int cookingTime);
    }

    /^* A cooking recipe whose result comes from an item tag. ^/
    interface Tagged {
        TagKey<Item> resultTag();
    }

    private static List<RecipeDisplay> display(AbstractCookingRecipe recipe, TagKey<Item> tag, Item station) {
        SlotDisplay result = BuiltInRegistries.ITEM.get(tag).<SlotDisplay>map(SlotDisplay.TagSlotDisplay::new).orElse(SlotDisplay.Empty.INSTANCE);
        return List.of(new FurnaceRecipeDisplay(recipe.input().display(), SlotDisplay.AnyFuel.INSTANCE, result,
                new SlotDisplay.ItemSlotDisplay(station), recipe.cookingTime(), recipe.experience()));
    }

    public static final class Smelting extends SmeltingRecipe implements Tagged {
        public static final MapCodec<Smelting> CODEC = codec(200, Smelting::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, Smelting> STREAM_CODEC = streamCodec(Smelting::new);
    *///?} else {
    /*public static final class Smelting extends SmeltingRecipe {
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
    *///?}

        private final TagKey<Item> resultTag;

        public Smelting(Ingredient ingredient, TagKey<Item> resultTag, float experience, int cookingTime) {
            //? if >=26.1 {
            super(new Recipe.CommonInfo(true), new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), ingredient,
                    PLACEHOLDER, experience, cookingTime);
            //?} else {
            /*super("", CookingBookCategory.MISC, ingredient, ItemStack.EMPTY, experience, cookingTime);
            *///?}
            this.resultTag = resultTag;
        //? if >=26.1 {
        }

        @Override
        public TagKey<Item> resultTag() {
            return resultTag;
        //?}
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return !resolve(resultTag, 1).isEmpty() && super.matches(input, level);
        }

        @Override
        //? if >=26.1 {
        public ItemStack assemble(SingleRecipeInput input) {
        //?} else {
        /*public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        *///?}
            return resolve(resultTag, 1);
        }

        @Override
        //? if >=26.1 {
        public List<RecipeDisplay> display() {
            return TagResultCooking.display(this, resultTag, Items.FURNACE);
        //?} else {
        /*public ItemStack getResultItem(HolderLookup.Provider registries) {
            return resolve(resultTag, 1);
        *///?}
        }

        @Override
        //? if >=26.1 {
        public RecipeSerializer<SmeltingRecipe> getSerializer() {
            return (RecipeSerializer<SmeltingRecipe>) (RecipeSerializer<?>) DfmRecipes.TAG_SMELTING_SERIALIZER.get();
        //?} else {
        /*public RecipeSerializer<?> getSerializer() {
            return DfmRecipes.TAG_SMELTING_SERIALIZER.get();
        *///?}
        }
    }

    //? if >=26.1 {
    public static final class Blasting extends BlastingRecipe implements Tagged {
        public static final MapCodec<Blasting> CODEC = codec(100, Blasting::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, Blasting> STREAM_CODEC = streamCodec(Blasting::new);
    //?} else {
    /*public static final class Blasting extends BlastingRecipe {
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
    *///?}

        private final TagKey<Item> resultTag;

        public Blasting(Ingredient ingredient, TagKey<Item> resultTag, float experience, int cookingTime) {
            //? if >=26.1 {
            super(new Recipe.CommonInfo(true), new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, ""), ingredient,
                    PLACEHOLDER, experience, cookingTime);
            //?} else {
            /*super("", CookingBookCategory.MISC, ingredient, ItemStack.EMPTY, experience, cookingTime);
            *///?}
            this.resultTag = resultTag;
        //? if >=26.1 {
        }

        @Override
        public TagKey<Item> resultTag() {
            return resultTag;
        //?}
        }

        @Override
        public boolean matches(SingleRecipeInput input, Level level) {
            return !resolve(resultTag, 1).isEmpty() && super.matches(input, level);
        }

        @Override
        //? if >=26.1 {
        public ItemStack assemble(SingleRecipeInput input) {
        //?} else {
        /*public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        *///?}
            return resolve(resultTag, 1);
        }

        @Override
        //? if >=26.1 {
        public List<RecipeDisplay> display() {
            return TagResultCooking.display(this, resultTag, Items.BLAST_FURNACE);
        //?} else {
        /*public ItemStack getResultItem(HolderLookup.Provider registries) {
            return resolve(resultTag, 1);
        *///?}
        }

        @Override
        //? if >=26.1 {
        public RecipeSerializer<BlastingRecipe> getSerializer() {
            return (RecipeSerializer<BlastingRecipe>) (RecipeSerializer<?>) DfmRecipes.TAG_BLASTING_SERIALIZER.get();
        //?} else {
        /*public RecipeSerializer<?> getSerializer() {
            return DfmRecipes.TAG_BLASTING_SERIALIZER.get();
        *///?}
        }
    }
}
