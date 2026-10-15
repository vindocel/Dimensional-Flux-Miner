package dimensionalfluxminer;

import dimensionalfluxminer.machine.math.LevelTable;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server configuration. By default each machine has a single energy source of truth, a fixed FE cost per operation: the
 * FE/t of every Rate level is that cost multiplied by the operations the level performs per tick, and the speed of the
 * levels is a geometric curve between two ends. Modpacks may replace either with an optional table of all ten levels
 * (speed per level, FE/t per level, or both); the cost of a level is then its FE/t divided by its operations per tick.
 */
public final class DfmConfig {
    /** Upper bound of every per-operation cost; keeps every FE/t, buffer and acceleration headroom well inside a long. */
    public static final long MAX_ENERGY_PER_OPERATION = 1_000_000_000L;
    /** Range of each entry of a speed table: operations per tick (64 is every machine's per-tick ceiling). */
    public static final double MIN_LEVEL_SPEED = 0.0001;
    public static final double MAX_LEVEL_SPEED = 64.0;
    /** Upper bound of each entry of a power table: 64 operations per tick at the highest cost per operation. */
    public static final long MAX_LEVEL_POWER = 64L * MAX_ENERGY_PER_OPERATION;
    /** Number of entries a level table must have. */
    public static final int LEVELS = 10;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue MINER_ENERGY_PER_ATTEMPT = BUILDER
            .comment("Dimensional Flux Miner: FE spent by every mining attempt, at every Flux Rate (FE/t = attempts per tick x this).",
                    "Attempts rejected by the filter or lost to a full output also cost it. Replaces the old minimumFEPerTick/maximumFEPerTick.")
            .defineInRange("miner.energyPerAttempt", 200_000L, 1L, MAX_ENERGY_PER_OPERATION);
    public static final ModConfigSpec.DoubleValue MINER_MIN_RATE = BUILDER
            .comment("Dimensional Flux Miner: mining attempts per logical tick at Flux Rate 1 (minimumProductionRate).")
            .defineInRange("miner.minimumProductionRate", 0.0125, 0.0001, 64.0);
    public static final ModConfigSpec.DoubleValue MINER_MAX_RATE = BUILDER
            .comment("Dimensional Flux Miner: mining attempts per normal logical cycle at Flux Rate 10 (maximumProductionRate). Can be lowered, never raised above 64.")
            .defineInRange("miner.maximumProductionRate", 64.0, 0.0001, 64.0);

    public static final ModConfigSpec.LongValue CRUSHER_ENERGY_PER_OPERATION = BUILDER
            .comment("Dimensional Crusher: FE spent by every input item crushed, at every Rate (the Efficiency Upgrade halves it).",
                    "Rate 1 = 1 operation / 200 ticks, Rate 10 = 64 operations per tick. Replaces the old minimumFEPerTick/maximumFEPerTick.")
            .defineInRange("crusher.energyPerOperation", 20_000L, 1L, MAX_ENERGY_PER_OPERATION);

    public static final ModConfigSpec.LongValue SMELTER_ALLOY_ENERGY = BUILDER
            .comment("Alloy Smelter: FE spent by one standard alloy execution (one unit of each ingredient), at every Rate.",
                    "A recipe asking for N units of an ingredient counts as N units of work. The Efficiency Upgrade halves it.")
            .defineInRange("smelter.alloyEnergyPerOperation", 30_000L, 1L, MAX_ENERGY_PER_OPERATION);
    public static final ModConfigSpec.LongValue SMELTER_FURNACE_ENERGY = BUILDER
            .comment("Alloy Smelter: FE spent by three items smelted in the furnace lanes (one item costs a third of it), at every Rate.",
                    "The Efficiency Upgrade halves it. Replaces the old smelter.minimumFEPerTick/maximumFEPerTick.")
            .defineInRange("smelter.furnaceEnergyPerThreeItems", 50_000L, 1L, MAX_ENERGY_PER_OPERATION);

    // Optional tables of the ten speed levels. Empty (the default) keeps the curves above; a table must list all ten
    // levels, otherwise it is ignored with a warning. A speed table replaces the geometric speed curve, a power table
    // replaces the fixed cost per operation (the cost of a level is then its FE/t divided by its operations per tick).
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> MINER_SPEED_BY_LEVEL = BUILDER
            .comment("Optional: mining attempts per logical tick of each Flux Rate, all ten levels (e.g. [0.0125, 0.03, ..., 64]).",
                    "Empty = the curve of minimumProductionRate / maximumProductionRate. Each value from 0.0001 to 64.")
            .defineListAllowEmpty("miner.speedByLevel", List.of(), () -> 1.0, o -> inRange(o, MIN_LEVEL_SPEED, MAX_LEVEL_SPEED));
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> MINER_POWER_BY_LEVEL = BUILDER
            .comment("Optional: FE/t of each Flux Rate at full speed, all ten levels. Empty = attempts per tick x energyPerAttempt.",
                    "The FE of one attempt at a level is its FE/t divided by its attempts per tick. Each value from 1 to " + MAX_LEVEL_POWER + ".")
            .defineListAllowEmpty("miner.powerByLevel", List.of(), () -> 1L, o -> inRange(o, 1, MAX_LEVEL_POWER));
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> CRUSHER_SPEED_BY_LEVEL = BUILDER
            .comment("Optional: operations (input items) per tick of each Rate, all ten levels.",
                    "Empty = 1 operation / 200 ticks at Rate 1 up to 64 per tick at Rate 10. Each value from 0.0001 to 64.")
            .defineListAllowEmpty("crusher.speedByLevel", List.of(), () -> 1.0, o -> inRange(o, MIN_LEVEL_SPEED, MAX_LEVEL_SPEED));
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> CRUSHER_POWER_BY_LEVEL = BUILDER
            .comment("Optional: FE/t of each Rate at full speed without the Efficiency Upgrade (which halves it), all ten levels.",
                    "Empty = operations per tick x energyPerOperation. Each value from 1 to " + MAX_LEVEL_POWER + ".")
            .defineListAllowEmpty("crusher.powerByLevel", List.of(), () -> 1L, o -> inRange(o, 1, MAX_LEVEL_POWER));
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> SMELTER_SPEED_BY_LEVEL = BUILDER
            .comment("Optional: operations per lane per tick of each Rate for a standard 10 s (200 tick) recipe, all ten levels.",
                    "Other durations keep the proportion of the default curve. Empty = 1/200 at Rate 1 up to 64 at Rate 10. Each value from 0.0001 to 64.")
            .defineListAllowEmpty("smelter.speedByLevel", List.of(), () -> 1.0, o -> inRange(o, MIN_LEVEL_SPEED, MAX_LEVEL_SPEED));
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> SMELTER_FURNACE_POWER_BY_LEVEL = BUILDER
            .comment("Optional: FE/t of each Rate with the three lanes smelting standard recipes, without the upgrade, all ten levels.",
                    "Empty = items per tick x furnaceEnergyPerThreeItems / 3. Each value from 1 to " + MAX_LEVEL_POWER + ".")
            .defineListAllowEmpty("smelter.furnacePowerByLevel", List.of(), () -> 1L, o -> inRange(o, 1, MAX_LEVEL_POWER));
    public static final ModConfigSpec.ConfigValue<List<? extends Number>> SMELTER_ALLOY_POWER_BY_LEVEL = BUILDER
            .comment("Optional: FE/t of each Rate running a standard alloy (one unit of each ingredient, 200 ticks), without the upgrade, all ten levels.",
                    "Empty = executions per tick x alloyEnergyPerOperation. Each value from 1 to " + MAX_LEVEL_POWER + ".")
            .defineListAllowEmpty("smelter.alloyPowerByLevel", List.of(), () -> 1L, o -> inRange(o, 1, MAX_LEVEL_POWER));

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SMELTER_EXTERNAL_ALLOY_TYPES = BUILDER
            .comment("Alloy Smelter: recipe types of other mods also run as alloys when their recipes have 1 to 3 counted item",
                    "ingredients and item results (a second result becomes the byproduct). Types of mods that are not installed are ignored.")
            .defineListAllowEmpty("smelter.externalAlloyRecipeTypes", List.of("enderio:alloy_smelting", "oritech:foundry"),
                    () -> "", o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec.BooleanValue CORRUPTION_ENABLED = BUILDER
            .comment("Dimensional Death: whether the Basic Core may corrupt terrain at all. When false the Basic Core finds",
                    "no matter to drain (it never gets DM without corrupting); the Stabilized Core is not affected.")
            .define("corruption.enabled", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CORRUPTION_BLOCKED_DIMENSIONS = BUILDER
            .comment("Dimensional Death: dimensions where nothing is ever corrupted (and Corrupted Water does not spread).")
            .defineListAllowEmpty("corruption.blockedDimensions", List.of(), () -> "", o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
    public static final ModConfigSpec.BooleanValue CORRUPTION_SPAWN_PROTECTION = BUILDER
            .comment("Dimensional Death: never corrupt inside the server's spawn protection (spawn-protection of server.properties,",
                    "a square around the world spawn in the Overworld).")
            .define("corruption.respectSpawnProtection", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CORRUPTION_PROTECTED_REGIONS = BUILDER
            .comment("Dimensional Death: administrative regions that are never corrupted, one per entry as",
                    "\"<dimension>,<x1>,<z1>,<x2>,<z2>\" (block coordinates, both corners included), e.g. \"minecraft:overworld,-100,-100,100,100\".")
            .defineListAllowEmpty("corruption.protectedRegions", List.of(), () -> "", o -> o instanceof String s && Region.parse(s) != null);
    public static final ModConfigSpec.ConfigValue<String> CLAIM_FAILURE_POLICY = BUILDER
            .comment("Dimensional Death: what to do when an installed claim system (FTB Chunks, Open Parties and Claims) cannot be",
                    "queried. \"deny\": no corruption anywhere until it works again (safe default). \"ignore\": treat it as not installed.")
            .define("corruption.claimFailurePolicy", "deny", o -> "deny".equals(o) || "ignore".equals(o));

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** An administrative rectangle of one dimension (block coordinates, inclusive). */
    public record Region(ResourceLocation dimension, int minX, int minZ, int maxX, int maxZ) {
        public static Region parse(String text) {
            String[] parts = text.split(",");
            if (parts.length != 5) {
                return null;
            }
            ResourceLocation dimension = ResourceLocation.tryParse(parts[0].trim());
            if (dimension == null) {
                return null;
            }
            try {
                int x1 = Integer.parseInt(parts[1].trim());
                int z1 = Integer.parseInt(parts[2].trim());
                int x2 = Integer.parseInt(parts[3].trim());
                int z2 = Integer.parseInt(parts[4].trim());
                return new Region(dimension, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public boolean contains(ResourceLocation dim, int x, int z) {
            return dimension.equals(dim) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    private static <T> T value(ModConfigSpec.ConfigValue<T> config) {
        return SPEC.isLoaded() ? config.get() : config.getDefault();
    }

    /** The corruption rules of the configuration, parsed once (they are read for every corrupted block and water flow). */
    public record CorruptionRules(boolean enabled, java.util.Set<ResourceLocation> blockedDimensions, boolean spawnProtection,
                                  List<Region> regions, boolean claimFailureDenies) {
    }

    private static volatile CorruptionRules corruptionRules;

    public static CorruptionRules corruptionRules() {
        CorruptionRules rules = corruptionRules;
        if (rules == null) {
            rules = new CorruptionRules(value(CORRUPTION_ENABLED),
                    java.util.Set.copyOf(value(CORRUPTION_BLOCKED_DIMENSIONS).stream().map(ResourceLocation::tryParse)
                            .filter(java.util.Objects::nonNull).toList()),
                    value(CORRUPTION_SPAWN_PROTECTION),
                    value(CORRUPTION_PROTECTED_REGIONS).stream().map(Region::parse).filter(java.util.Objects::nonNull).toList(),
                    !"ignore".equals(value(CLAIM_FAILURE_POLICY)));
            if (SPEC.isLoaded()) {
                corruptionRules = rules;
            }
        }
        return rules;
    }

    /** Drops the parsed rules and level tables whenever the server configuration is loaded or edited. */
    public static void onConfigChanged(net.neoforged.fml.event.config.ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            invalidateCaches();
        }
    }

    /** Drops every value parsed from the configuration (also used by tests that change it in memory). */
    public static void invalidateCaches() {
        corruptionRules = null;
        LEVEL_TABLES.clear();
    }

    private static boolean inRange(Object o, double min, double max) {
        return o instanceof Number n && n.doubleValue() >= min && n.doubleValue() <= max && Double.isFinite(n.doubleValue());
    }

    private static final java.util.Map<ModConfigSpec.ConfigValue<List<? extends Number>>, java.util.Optional<LevelTable>> LEVEL_TABLES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The configured table of the ten levels, or null when it is not set (the machine then uses its default curve). A
     * table that does not list exactly ten levels is ignored, with a warning, so a partial table never mixes with the
     * default curve. Parsed once per configuration load.
     */
    public static LevelTable levelTable(ModConfigSpec.ConfigValue<List<? extends Number>> config) {
        if (!SPEC.isLoaded()) {
            return null;
        }
        return LEVEL_TABLES.computeIfAbsent(config, c -> {
            List<? extends Number> values = c.get();
            if (values.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (values.size() != LEVELS) {
                DimensionalFluxMiner.LOGGER.warn("Config {} lists {} levels instead of {}; it is ignored and the default curve is used",
                        String.join(".", c.getPath()), values.size(), LEVELS);
                return java.util.Optional.empty();
            }
            double[] table = new double[LEVELS];
            for (int i = 0; i < LEVELS; i++) {
                table[i] = values.get(i).doubleValue();
            }
            return java.util.Optional.of(new LevelTable(table));
        }).orElse(null);
    }

    public static boolean corruptionEnabled() {
        return corruptionRules().enabled();
    }

    public static java.util.Set<ResourceLocation> corruptionBlockedDimensions() {
        return corruptionRules().blockedDimensions();
    }

    public static boolean corruptionRespectsSpawnProtection() {
        return corruptionRules().spawnProtection();
    }

    public static List<Region> corruptionProtectedRegions() {
        return corruptionRules().regions();
    }

    public static boolean claimFailureDenies() {
        return corruptionRules().claimFailureDenies();
    }

    private DfmConfig() {
    }

    /** Recipe types of other mods run as alloys by the Alloy Smelter. */
    public static List<ResourceLocation> externalAlloyTypes() {
        List<? extends String> ids = SPEC.isLoaded() ? SMELTER_EXTERNAL_ALLOY_TYPES.get() : SMELTER_EXTERNAL_ALLOY_TYPES.getDefault();
        return ids.stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull).toList();
    }

    /** FE cost of one operation; the default while the server configuration is not loaded. */
    public static long energy(ModConfigSpec.LongValue cost) {
        long value = SPEC.isLoaded() ? cost.get() : cost.getDefault();
        return Math.max(1L, Math.min(MAX_ENERGY_PER_OPERATION, value));
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
