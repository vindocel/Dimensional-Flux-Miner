package dimensionalfluxminer.compat.rei;

import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.resources.ResourceLocation;

/** REI side of {@link dimensionalfluxminer.compat.RecipeViewers}; loaded only when REI is installed. */
public final class ReiBridge {
    private ReiBridge() {
    }

    public static void show(ResourceLocation... categories) {
        ViewSearchBuilder search = ViewSearchBuilder.builder();
        for (ResourceLocation category : categories) {
            search.addCategory(CategoryIdentifier.of(category));
        }
        search.open();
    }
}
