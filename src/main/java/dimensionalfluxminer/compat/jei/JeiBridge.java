package dimensionalfluxminer.compat.jei;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

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

    public static void show(ResourceLocation... categories) {
        if (runtime == null) {
            return;
        }
        List<RecipeType<?>> types = new ArrayList<>();
        for (ResourceLocation id : categories) {
            runtime.getRecipeManager().getRecipeType(id).ifPresent(types::add);
        }
        if (!types.isEmpty()) {
            runtime.getRecipesGui().showTypes(types);
        }
    }
}
