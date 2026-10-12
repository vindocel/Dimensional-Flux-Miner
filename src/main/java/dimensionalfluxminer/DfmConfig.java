package dimensionalfluxminer;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server configuration. Only the extremes are configurable; every intermediate Rate level is recalculated
 * from them, so none of the intermediate tables are hard-coded.
 */
public final class DfmConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue MINER_MIN_FE = BUILDER
            .comment("Dimensional Flux Miner: FE/t power limit at Flux Rate 1 (minimumFEPerTick).")
            .defineInRange("miner.minimumFEPerTick", 500.0, 1.0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue MINER_MAX_FE = BUILDER
            .comment("Dimensional Flux Miner: FE/t power limit at Flux Rate 10 (maximumFEPerTick). Must be greater than the minimum.")
            .defineInRange("miner.maximumFEPerTick", 1_500_000.0, 2.0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue MINER_MIN_RATE = BUILDER
            .comment("Dimensional Flux Miner: resources per logical tick at the minimum power (minimumProductionRate).")
            .defineInRange("miner.minimumProductionRate", 0.0125, 0.0001, 64.0);
    public static final ModConfigSpec.DoubleValue MINER_MAX_RATE = BUILDER
            .comment("Dimensional Flux Miner: resources per normal logical cycle at the maximum power (maximumProductionRate). Can be lowered, never raised above 64.")
            .defineInRange("miner.maximumProductionRate", 64.0, 0.0001, 64.0);

    public static final ModConfigSpec.DoubleValue CRUSHER_MIN_FE = BUILDER
            .comment("Dimensional Crusher: FE/t at Rate 1.")
            .defineInRange("crusher.minimumFEPerTick", 250.0, 1.0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue CRUSHER_MAX_FE = BUILDER
            .comment("Dimensional Crusher: FE/t at Rate 10. Must be greater than the minimum.")
            .defineInRange("crusher.maximumFEPerTick", 500_000.0, 2.0, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue SMELTER_MIN_FE = BUILDER
            .comment("Alloy Smelter: FE/t at Rate 1.")
            .defineInRange("smelter.minimumFEPerTick", 600.0, 1.0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue SMELTER_MAX_FE = BUILDER
            .comment("Alloy Smelter: FE/t at Rate 10. Must be greater than the minimum.")
            .defineInRange("smelter.maximumFEPerTick", 600_000.0, 2.0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SMELTER_EXTERNAL_ALLOY_TYPES = BUILDER
            .comment("Alloy Smelter: recipe types of other mods also run as alloys when their recipes have 1 to 3 counted item",
                    "ingredients and item results (a second result becomes the byproduct). Types of mods that are not installed are ignored.")
            .defineListAllowEmpty("smelter.externalAlloyRecipeTypes", List.of("enderio:alloy_smelting", "oritech:foundry"),
                    () -> "", o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DfmConfig() {
    }

    /** Recipe types of other mods run as alloys by the Alloy Smelter. */
    public static List<ResourceLocation> externalAlloyTypes() {
        List<? extends String> ids = SPEC.isLoaded() ? SMELTER_EXTERNAL_ALLOY_TYPES.get() : SMELTER_EXTERNAL_ALLOY_TYPES.getDefault();
        return ids.stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull).toList();
    }

    /** Returns a valid {min, max} pair; an invalid configuration falls back to the defaults. */
    public static double[] range(ModConfigSpec.DoubleValue min, ModConfigSpec.DoubleValue max) {
        double lo = SPEC.isLoaded() ? min.get() : min.getDefault();
        double hi = SPEC.isLoaded() ? max.get() : max.getDefault();
        if (!(lo > 0) || !(hi > lo)) {
            return new double[] {min.getDefault(), max.getDefault()};
        }
        return new double[] {lo, hi};
    }
}
