package dimensionalfluxminer.compat.rei;

import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/** REI side of {@link dimensionalfluxminer.compat.RecipeViewers}; loaded only when REI is installed. */
public final class ReiBridge {
    private ReiBridge() {
    }

    //? if >=26.1 {
    public static void show(Identifier... categories) {
    //?} else {
    /*public static void show(ResourceLocation... categories) {
    *///?}
        ViewSearchBuilder search = ViewSearchBuilder.builder();
        //? if >=26.1 {
        for (Identifier category : categories) {
        //?} else {
        /*for (ResourceLocation category : categories) {
        *///?}
            search.addCategory(CategoryIdentifier.of(category));
        }
        search.open();
    }
}
