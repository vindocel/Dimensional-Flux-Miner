package dimensionalfluxminer.compat;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.emi.EmiBridge;
import dimensionalfluxminer.compat.jei.JeiBridge;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Opens this mod's recipe categories in the installed recipe viewer (machine progress areas are clickable). The
 * viewer-specific bridges are only touched when that viewer is loaded.
 */
public final class RecipeViewers {
    public static final ResourceLocation CRUSHING = DimensionalFluxMiner.id("crushing");
    public static final ResourceLocation SMELTING = DimensionalFluxMiner.id("smelting");
    public static final ResourceLocation ALLOYING = DimensionalFluxMiner.id("alloying");

    private RecipeViewers() {
    }

    private static boolean emi() {
        return ModList.get().isLoaded("emi");
    }

    public static boolean available() {
        return emi() || (ModList.get().isLoaded("jei") && JeiBridge.ready());
    }

    /** Shows the categories (EMI shows the first one; JEI shows them all as tabs). */
    public static void show(ResourceLocation... categories) {
        if (categories.length == 0) {
            return;
        }
        if (emi()) {
            EmiBridge.show(categories[0]);
        } else if (ModList.get().isLoaded("jei")) {
            JeiBridge.show(categories);
        }
    }
}
