package dimensionalfluxminer.compat.rei;

import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.resources.Identifier;

/** REI side of {@link dimensionalfluxminer.compat.RecipeViewers}; loaded only when REI is installed. */
public final class ReiBridge {
    private ReiBridge() {
    }

    public static void show(Identifier... categories) {
        ViewSearchBuilder search = ViewSearchBuilder.builder();
        for (Identifier category : categories) {
            search.addCategory(CategoryIdentifier.of(category));
        }
        search.open();
    }
}
