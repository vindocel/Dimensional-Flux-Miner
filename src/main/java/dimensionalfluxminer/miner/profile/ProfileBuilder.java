package dimensionalfluxminer.miner.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.CrusherProcessing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * Builds a dimension's Resource Profile from the ore features that really generate in that dimension
 * (vein size, placements per chunk, rarity and biome coverage), then applies datapack overrides. Unknown custom
 * world generation is never guessed: it needs an override.
 */
public final class ProfileBuilder {
    /** Built-in vanilla families: family -> ore blocks. */
    static final Map<String, List<Block>> VANILLA_FAMILIES = new LinkedHashMap<>();

    static {
        VANILLA_FAMILIES.put("coal", List.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE));
        VANILLA_FAMILIES.put("iron", List.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE));
        VANILLA_FAMILIES.put("copper", List.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE));
        VANILLA_FAMILIES.put("gold", List.of(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE));
        VANILLA_FAMILIES.put("redstone", List.of(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE));
        VANILLA_FAMILIES.put("lapis", List.of(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE));
        VANILLA_FAMILIES.put("diamond", List.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE));
        VANILLA_FAMILIES.put("emerald", List.of(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE));
        VANILLA_FAMILIES.put("quartz", List.of(Blocks.NETHER_QUARTZ_ORE));
        VANILLA_FAMILIES.put("nether_gold", List.of(Blocks.NETHER_GOLD_ORE));
        VANILLA_FAMILIES.put("ancient_debris", List.of(Blocks.ANCIENT_DEBRIS));
    }

    /** Vein size assumed for ore features whose size is not part of their serialized configuration. */
    static final double DEFAULT_VEIN_SIZE = 8.0;

    private ProfileBuilder() {
    }

    private static final class Accumulator {
        double rawScore;
        final Map<Item, Double> variants = new LinkedHashMap<>();
        Item crusherOutput;
        Double share;
    }

    public static ResourceProfile build(ServerLevel level, List<ProfileOverride> overrides) {
        Map<ResourceLocation, Accumulator> families = new LinkedHashMap<>();
        analyzeWorldgen(level, families);
        applyOverrides(level, overrides, families);

        Map<ResourceLocation, Double> scores = new LinkedHashMap<>();
        Map<ResourceLocation, Double> shares = new HashMap<>();
        Map<ResourceLocation, Item> outputs = new HashMap<>();
        families.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).forEach(e -> {
            Accumulator acc = e.getValue();
            if (acc.variants.isEmpty()) {
                return;
            }
            Item output = acc.crusherOutput != null ? acc.crusherOutput : crusherOutput(level, acc.variants.keySet().iterator().next());
            if (output == null) {
                DimensionalFluxMiner.LOGGER.warn("Resource family {} in {} has no resolvable Crusher output; add a crusher_output override. Family excluded.",
                        e.getKey(), level.dimension().location());
                return;
            }
            outputs.put(e.getKey(), output);
            scores.put(e.getKey(), acc.rawScore);
            if (acc.share != null) {
                shares.put(e.getKey(), acc.share);
            }
        });
        Map<ResourceLocation, Integer> weights = ProfileWeights.applyShares(ProfileWeights.normalize(scores), shares);
        List<ResourceFamily> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> e : weights.entrySet()) {
            Accumulator acc = families.get(e.getKey());
            List<ResourceFamily.Variant> variants = new ArrayList<>();
            acc.variants.forEach((item, w) -> variants.add(new ResourceFamily.Variant(item, Math.max(w, ProfileWeights.EPSILON))));
            result.add(new ResourceFamily(e.getKey(), List.copyOf(variants), e.getValue(), outputs.get(e.getKey())));
        }
        result.sort(Comparator.comparing(f -> f.key().toString()));
        DimensionalFluxMiner.LOGGER.debug("Resource Profile {}: {}", level.dimension().location(),
                result.stream().map(f -> f.key() + "=" + f.weight()).toList());
        return new ResourceProfile(level.dimension(), List.copyOf(result));
    }

    private static Item crusherOutput(ServerLevel level, Item ore) {
        return CrusherProcessing.resolve(level, new ItemStack(ore), false).map(r -> r.result().getItem()).orElse(null);
    }

    /** Family of an ore item: built-in vanilla table first, then {@code c:ores/<material>}. */
    public static Optional<ResourceLocation> familyOf(Item item) {
        for (Map.Entry<String, List<Block>> e : VANILLA_FAMILIES.entrySet()) {
            for (Block block : e.getValue()) {
                if (block.asItem() == item) {
                    return Optional.of(ResourceLocation.withDefaultNamespace(e.getKey()));
                }
            }
        }
        return new ItemStack(item).getTags()
                .filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().startsWith("ores/"))
                .map(t -> t.location().getPath().substring("ores/".length()))
                .findFirst()
                .map(material -> VANILLA_FAMILIES.containsKey(material)
                        ? ResourceLocation.withDefaultNamespace(material)
                        : ResourceLocation.fromNamespaceAndPath("c", material));
    }

    private static void analyzeWorldgen(ServerLevel level, Map<ResourceLocation, Accumulator> families) {
        Set<Holder<Biome>> biomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
        if (biomes.isEmpty()) {
            return;
        }
        int step = GenerationStep.Decoration.UNDERGROUND_ORES.ordinal();
        Map<PlacedFeature, Integer> biomeCount = new LinkedHashMap<>();
        for (Holder<Biome> biome : biomes) {
            List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
            for (int s = 0; s < steps.size(); s++) {
                if (s != step && s != GenerationStep.Decoration.UNDERGROUND_DECORATION.ordinal()) {
                    continue;
                }
                for (Holder<PlacedFeature> feature : steps.get(s)) {
                    biomeCount.merge(feature.value(), 1, Integer::sum);
                }
            }
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        for (Map.Entry<PlacedFeature, Integer> e : biomeCount.entrySet()) {
            PlacedFeature placed = e.getKey();
            Optional<OreShape> shape = oreShape(placed.feature().value(), ops);
            if (shape.isEmpty()) {
                continue;
            }
            OreShape ore = shape.get();
            double coverage = e.getValue() / (double) biomes.size();
            double placements = 1.0;
            double probability = 1.0;
            for (PlacementModifier modifier : placed.placement()) {
                if (modifier instanceof CountPlacement) {
                    placements *= averageCount(modifier);
                } else if (modifier instanceof RarityFilter) {
                    probability *= rarity(modifier);
                }
            }
            double contribution = ore.size() * placements * probability * coverage;
            if (contribution <= 0 || ore.targets().isEmpty()) {
                continue;
            }
            double perTarget = contribution / ore.targets().size();
            for (BlockState target : ore.targets()) {
                Item item = target.getBlock().asItem();
                if (item == Items.AIR) {
                    continue;
                }
                Optional<ResourceLocation> family = familyOf(item);
                if (family.isEmpty()) {
                    continue;
                }
                Accumulator acc = families.computeIfAbsent(family.get(), k -> new Accumulator());
                acc.rawScore += perTarget;
                acc.variants.merge(item, perTarget, Double::sum);
            }
        }
    }

    /** Ore blocks a feature places and its vein size. */
    public record OreShape(List<BlockState> targets, double size) {
    }

    /**
     * Vanilla {@link OreConfiguration}s are read directly. Other ore features (for example ore types with their own
     * configuration codec) are read from their serialized form: every {@code targets[].state} entry is an ore
     * block, and a numeric {@code size} is the vein size ({@link #DEFAULT_VEIN_SIZE} when the size lives elsewhere,
     * such as in a mod config).
     */
    public static Optional<OreShape> oreShape(ConfiguredFeature<?, ?> feature, RegistryOps<JsonElement> ops) {
        if (feature.config() instanceof OreConfiguration ore) {
            return Optional.of(new OreShape(ore.targetStates.stream().map(t -> t.state).toList(), ore.size));
        }
        JsonElement json;
        try {
            json = ConfiguredFeature.DIRECT_CODEC.encodeStart(ops, feature).getOrThrow();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (!(json instanceof JsonObject root) || !(root.get("config") instanceof JsonObject config)
                || !(config.get("targets") instanceof JsonArray targets)) {
            return Optional.empty();
        }
        List<BlockState> states = new ArrayList<>();
        for (JsonElement target : targets) {
            if (target instanceof JsonObject obj && obj.has("state")) {
                BlockState.CODEC.parse(ops, obj.get("state")).result().ifPresent(states::add);
            }
        }
        if (states.isEmpty()) {
            return Optional.empty();
        }
        double size = config.get("size") instanceof JsonPrimitive p && p.isNumber() ? p.getAsDouble() : DEFAULT_VEIN_SIZE;
        return Optional.of(new OreShape(states, size));
    }

    private static double averageCount(PlacementModifier modifier) {
        try {
            JsonElement json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier).getOrThrow();
            JsonElement count = json instanceof JsonObject obj ? obj.get("count") : null;
            if (count == null) {
                return 1.0;
            }
            IntProvider provider = IntProvider.CODEC.parse(JsonOps.INSTANCE, count).getOrThrow();
            return (provider.getMinValue() + provider.getMaxValue()) / 2.0;
        } catch (RuntimeException e) {
            return 1.0;
        }
    }

    private static double rarity(PlacementModifier modifier) {
        try {
            JsonElement json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier).getOrThrow();
            int chance = json instanceof JsonObject obj && obj.has("chance") ? obj.get("chance").getAsInt() : 1;
            return 1.0 / Math.max(1, chance);
        } catch (RuntimeException e) {
            return 1.0;
        }
    }

    private static void applyOverrides(ServerLevel level, List<ProfileOverride> overrides, Map<ResourceLocation, Accumulator> families) {
        for (ProfileOverride o : overrides) {
            if (!o.appliesTo(level.dimension())) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(o.resource());
            if (item == Items.AIR) {
                DimensionalFluxMiner.LOGGER.warn("Dimensional miner resource override references unknown item {}", o.resource());
                continue;
            }
            if (!o.enabled()) {
                families.values().forEach(acc -> acc.variants.remove(item));
                families.values().removeIf(acc -> acc.variants.isEmpty());
                continue;
            }
            Optional<ResourceLocation> key = o.family().or(() -> familyOf(item));
            if (key.isEmpty()) {
                key = Optional.of(BuiltInRegistries.ITEM.getKey(item));
            }
            if (o.dimension().isEmpty() && !families.containsKey(key.get())) {
                DimensionalFluxMiner.LOGGER.warn("Override for {} adds a resource without 'dimension'; ignored.", o.resource());
                continue;
            }
            Accumulator acc = families.computeIfAbsent(key.get(), k -> new Accumulator());
            acc.variants.merge(item, 1.0, (a, b) -> a);
            o.weight().ifPresent(w -> acc.share = w);
            o.crusherOutput().map(BuiltInRegistries.ITEM::get).filter(i -> i != Items.AIR).ifPresent(i -> acc.crusherOutput = i);
        }
    }
}
