package dimensionalfluxminer.compat;

import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Dark Mode Everywhere darkens every screen with a shader and recolors dark text. The mod's screens have their own
 * light and dark looks, so their draw calls are added to its shader exceptions. ScreenPaletteGuard also suspends
 * external text and fill overrides while the mod's screens are extracted.
 */
public final class DarkModeCompat {
    private static final String MOD_ID = "darkmodeeverywhere";

    private DarkModeCompat() {
    }

    public static void onInterModEnqueue(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded(MOD_ID)) {
            InterModComms.sendTo(MOD_ID, "dme-shaderblacklist", () -> "dimensionalfluxminer.");
        }
    }
}
