package dimensionalfluxminer.compat;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.compat.jei.JeiBridge;
import dimensionalfluxminer.compat.rei.ReiBridge;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

/**
 * Opens this mod's recipe categories in the installed recipe viewer (machine progress areas are clickable). The
 * viewer-specific bridges are only touched when that viewer is loaded. With more than one installed, EMI wins, then REI,
 * then JEI (each plugin steps aside for the ones before it, so recipes are never listed twice). EMI has no 26.1.2 build
 * yet: its plugin stays out of the build, and with EMI installed this mod's recipes would simply not be listed.
 */
public final class RecipeViewers {
    public static final Identifier CRUSHING = DimensionalFluxMiner.id("crushing");
    public static final Identifier SMELTING = DimensionalFluxMiner.id("smelting");
    public static final Identifier ALLOYING = DimensionalFluxMiner.id("alloying");

    private RecipeViewers() {
    }

    public static boolean emi() {
        return ModList.get().isLoaded("emi");
    }

    public static boolean rei() {
        return ModList.get().isLoaded("roughlyenoughitems");
    }

    public static boolean available() {
        return rei() || ModList.get().isLoaded("jei") && JeiBridge.ready();
    }

    /** Shows the categories (REI and JEI show them all as tabs). */
    public static void show(Identifier... categories) {
        if (categories.length == 0) {
            return;
        }
        if (rei()) {
            ReiBridge.show(categories);
        } else if (ModList.get().isLoaded("jei")) {
            JeiBridge.show(categories);
        }
    }
}
