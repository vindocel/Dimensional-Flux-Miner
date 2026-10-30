package dimensionalfluxminer.compat;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.jei.JeiBridge;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

/**
 * Opens this mod's recipe categories in the installed recipe viewer (machine progress areas are clickable). The
 * viewer-specific bridges are only touched when that viewer is loaded. EMI and REI have no 26.x build yet, so only JEI
 * here (the 1.21.1 line picks EMI, then REI, then JEI).
 */
public final class RecipeViewers {
    public static final Identifier CRUSHING = DimensionalFluxMiner.id("crushing");
    public static final Identifier SMELTING = DimensionalFluxMiner.id("smelting");
    public static final Identifier ALLOYING = DimensionalFluxMiner.id("alloying");

    private RecipeViewers() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("jei") && JeiBridge.ready();
    }

    /** Shows the categories (JEI shows them all as tabs). */
    public static void show(Identifier... categories) {
        if (categories.length == 0) {
            return;
        }
        if (ModList.get().isLoaded("jei")) {
            JeiBridge.show(categories);
        }
    }
}
