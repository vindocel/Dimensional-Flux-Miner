package dimensionalfluxminer.compat.jei;

import java.util.ArrayList;
import java.util.List;
//? if >=26.1 {
import mezz.jei.api.recipe.types.IRecipeType;
//?} else {
/*import mezz.jei.api.recipe.RecipeType;
*///?}
import mezz.jei.api.runtime.IJeiRuntime;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/** JEI side of {@link dimensionalfluxminer.compat.RecipeViewers}; loaded only when JEI is installed. */
public final class JeiBridge {
    private static IJeiRuntime runtime;

    private JeiBridge() {
    }

    static void setRuntime(IJeiRuntime value) {
        runtime = value;
    }

    public static boolean ready() {
        return runtime != null;
    }

    //? if >=26.1 {
    public static void show(Identifier... categories) {
    //?} else {
    /*public static void show(ResourceLocation... categories) {
    *///?}
        if (runtime == null) {
            return;
        }
        //? if >=26.1 {
        List<IRecipeType<?>> types = new ArrayList<>();
        for (Identifier id : categories) {
        //?} else {
        /*List<RecipeType<?>> types = new ArrayList<>();
        for (ResourceLocation id : categories) {
        *///?}
            runtime.getRecipeManager().getRecipeType(id).ifPresent(types::add);
        }
        if (!types.isEmpty()) {
            runtime.getRecipesGui().showTypes(types);
        }
    }
}
