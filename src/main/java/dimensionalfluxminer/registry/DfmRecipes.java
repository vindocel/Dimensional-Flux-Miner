package dimensionalfluxminer.registry;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.recipe.CrushingRecipe;
import dimensionalfluxminer.recipe.OreCrushingRecipe;
import dimensionalfluxminer.recipe.TagResultCooking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, DimensionalFluxMiner.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING = type("crushing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<OreCrushingRecipe>> ORE_CRUSHING = type("ore_crushing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING = type("alloying");

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
            serializer("crushing", CrushingRecipe.CODEC, CrushingRecipe.STREAM_CODEC);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<OreCrushingRecipe>> ORE_CRUSHING_SERIALIZER =
            serializer("ore_crushing", OreCrushingRecipe.CODEC, OreCrushingRecipe.STREAM_CODEC);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER =
            serializer("alloying", AlloyingRecipe.CODEC, AlloyingRecipe.STREAM_CODEC);
    /** Furnace / blast furnace recipes whose result comes from an item tag (DFM dusts into the installed ingots). */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TagResultCooking.Smelting>> TAG_SMELTING_SERIALIZER =
            serializer("tag_smelting", TagResultCooking.Smelting.CODEC, TagResultCooking.Smelting.STREAM_CODEC);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TagResultCooking.Blasting>> TAG_BLASTING_SERIALIZER =
            serializer("tag_blasting", TagResultCooking.Blasting.CODEC, TagResultCooking.Blasting.STREAM_CODEC);

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> type(String name) {
        return TYPES.register(name, () -> RecipeType.simple(DimensionalFluxMiner.id(name)));
    }

    private static <T extends Recipe<?>> DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> serializer(
            String name, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        return SERIALIZERS.register(name, () -> new RecipeSerializer<>(codec, streamCodec));
    }

    private DfmRecipes() {
    }
}
