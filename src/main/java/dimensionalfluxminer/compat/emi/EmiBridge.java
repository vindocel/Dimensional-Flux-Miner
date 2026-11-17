package dimensionalfluxminer.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/** EMI side of {@link dimensionalfluxminer.compat.RecipeViewers}; loaded only when EMI is installed. */
public final class EmiBridge {
    private EmiBridge() {
    }

    //? if >=26.1 {
    public static void show(Identifier category) {
    //?} else {
    /*public static void show(ResourceLocation category) {
    *///?}
        for (EmiRecipeCategory candidate : EmiApi.getRecipeManager().getCategories()) {
            if (candidate.getId().equals(category)) {
                EmiApi.displayRecipeCategory(candidate);
                return;
            }
        }
    }
}
