package dimensionalfluxminer.compat;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Cognition's Experience Obelisk takes only the experience fluids named in its own {@code allowedFluids} option (not the
 * {@code c:experience} tag). Liquid Experience is added to that list in memory (the option's file is never written), so
 * the Electric Furnace can fill an obelisk out of the box; both use 20 mB per experience point. Read by reflection: no
 * dependency, nothing happens without the mod.
 */
public final class CognitionCompat {
    private static final String MOD_ID = "cognition";
    private static final String FLUID = DimensionalFluxMiner.MOD_ID + ":liquid_experience";

    private CognitionCompat() {
    }

    /** Called once the configs are loaded (common setup) and again when a server starts, after any config reload. */
    @SuppressWarnings("unchecked")
    public static void allowLiquidExperience() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> config = Class.forName("com.cyanogen.cognition.config.Config");
            Object common = config.getField("COMMON").get(null);
            ModConfigSpec.ConfigValue<List<String>> allowed =
                    (ModConfigSpec.ConfigValue<List<String>>) common.getClass().getField("allowedFluids").get(common);
            List<String> current = allowed.get();
            if (!current.contains(FLUID)) {
                List<String> extended = new ArrayList<>(current);
                extended.add(FLUID);
                allowed.set(extended);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            DimensionalFluxMiner.LOGGER.warn("Could not let Cognition's Experience Obelisk accept Liquid Experience", e);
        }
    }
}
