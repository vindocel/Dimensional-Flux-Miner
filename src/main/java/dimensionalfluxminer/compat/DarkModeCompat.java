package dimensionalfluxminer.compat;

import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Dark Mode Everywhere darkens every screen with a shader and recolors dark text. The mod's screens have their own
 * light and dark looks, so they are put on its list of exceptions (every render call made from this mod's classes,
 * text included) through the message it reads from other mods. Nothing happens without it.
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
