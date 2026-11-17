package dimensionalfluxminer.compat;

import dimensionalfluxminer.DimensionalFluxMiner;
//? if <26.1 {
/*import dimensionalfluxminer.compat.emi.EmiBridge;
*///?}
import dimensionalfluxminer.compat.jei.JeiBridge;
//? if =26.1.2 {
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

    public static final Identifier TANK_CONTAINER = DimensionalFluxMiner.id("tank_container");
//?} else if >=26.3 {
/*import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

/^*
 * Opens this mod's recipe categories in the installed recipe viewer (machine progress areas are clickable). The
 * viewer-specific bridges are only touched when that viewer is loaded. EMI and REI have no 26.x build yet, so only JEI
 * here (the 1.21.1 line picks EMI, then REI, then JEI).
 ^/
public final class RecipeViewers {
    public static final Identifier CRUSHING = DimensionalFluxMiner.id("crushing");
    public static final Identifier SMELTING = DimensionalFluxMiner.id("smelting");
    public static final Identifier ALLOYING = DimensionalFluxMiner.id("alloying");

    public static final Identifier TANK_CONTAINER = DimensionalFluxMiner.id("tank_container");
*///?} else {
/*import dimensionalfluxminer.compat.rei.ReiBridge;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/^*
 * Opens this mod's recipe categories in the installed recipe viewer (machine progress areas are clickable). The
 * viewer-specific bridges are only touched when that viewer is loaded. With more than one installed, EMI wins, then REI,
 * then JEI (each plugin steps aside for the ones before it, so recipes are never listed twice).
 ^/
public final class RecipeViewers {
    public static final ResourceLocation CRUSHING = DimensionalFluxMiner.id("crushing");
    public static final ResourceLocation SMELTING = DimensionalFluxMiner.id("smelting");
    public static final ResourceLocation ALLOYING = DimensionalFluxMiner.id("alloying");

    public static final ResourceLocation TANK_CONTAINER = DimensionalFluxMiner.id("tank_container");
*///?}

    private RecipeViewers() {
    }

    //? if <26.3 {
    public static boolean emi() {
        return ModList.get().isLoaded("emi");
    //?} else {
    /*public static boolean available() {
        return ModList.get().isLoaded("jei") && JeiBridge.ready();
    *///?}
    }

    //? if =26.1.2 {
    public static boolean rei() {
        return ModList.get().isLoaded("roughlyenoughitems");
    }

    public static boolean available() {
        return rei() || ModList.get().isLoaded("jei") && JeiBridge.ready();
    }

    /** Shows the categories (REI and JEI show them all as tabs). */
    public static void show(Identifier... categories) {
    //?} else if >=26.3 {
    /*/^* Shows the categories (JEI shows them all as tabs). ^/
    public static void show(Identifier... categories) {
    *///?} else {
    /*public static boolean rei() {
        return ModList.get().isLoaded("roughlyenoughitems");
    }

    public static boolean available() {
        return emi() || rei() || (ModList.get().isLoaded("jei") && JeiBridge.ready());
    }

    /^* Shows the categories (EMI shows the first one; REI and JEI show them all as tabs). ^/
    public static void show(ResourceLocation... categories) {
    *///?}
        if (categories.length == 0) {
            return;
        }
        //? if =26.1.2 {
        if (rei()) {
            ReiBridge.show(categories);
        } else if (ModList.get().isLoaded("jei")) {
        //?} else if >=26.3 {
        /*if (ModList.get().isLoaded("jei")) {
        *///?} else {
        /*if (emi()) {
            EmiBridge.show(categories[0]);
        } else if (rei()) {
            ReiBridge.show(categories);
        } else if (ModList.get().isLoaded("jei")) {
        *///?}
            JeiBridge.show(categories);
        }
    }
}
