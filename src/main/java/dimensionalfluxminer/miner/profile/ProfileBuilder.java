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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
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
        return build(level, overrides, List.of());
    }

    /** Builds the profile, then applies the void of the first {@link ProfileDimensionRule} of the dimension. */
    public static ResourceProfile build(ServerLevel level, List<ProfileOverride> overrides, List<ProfileDimensionRule> rules) {
        ResourceProfile profile = buildFamilies(level, overrides);
        Optional<ProfileDimensionRule> rule = rules.stream().filter(r -> r.dimension().equals(level.dimension())).findFirst();
        double share = rule.map(r -> r.voidShare(profile.families().size())).orElse(0.0);
        if (ProfileWeights.emptyWeight(share) <= 0) {
            return profile;
        }
        Map<Identifier, Integer> weights = new LinkedHashMap<>();
        profile.families().forEach(f -> weights.put(f.key(), f.weight()));
        Map<Identifier, Integer> scaled = ProfileWeights.scaleForEmpty(weights, share);
        List<ResourceFamily> families = profile.families().stream()
                .map(f -> new ResourceFamily(f.key(), f.variants(), scaled.get(f.key()), f.crusherOutput())).toList();
        DimensionalFluxMiner.LOGGER.debug("Resource Profile {}: {} families, void {}", level.dimension().identifier(), families.size(), share);
        return new ResourceProfile(profile.dimension(), families, ProfileWeights.emptyWeight(share));
    }

    private static ResourceProfile buildFamilies(ServerLevel level, List<ProfileOverride> overrides) {
        Map<Identifier, Accumulator> families = new LinkedHashMap<>();
        analyzeWorldgen(level, families);
        applyOverrides(level, overrides, families);

        Map<Identifier, Double> scores = new LinkedHashMap<>();
        Map<Identifier, Double> shares = new HashMap<>();
        Map<Identifier, Item> outputs = new HashMap<>();
        families.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).forEach(e -> {
            Accumulator acc = e.getValue();
            if (acc.variants.isEmpty()) {
                return;
            }
            Item output = acc.crusherOutput != null ? acc.crusherOutput : crusherOutput(level, acc.variants.keySet().iterator().next());
            if (output == null) {
                DimensionalFluxMiner.LOGGER.warn("Resource family {} in {} has no resolvable Crusher output; add a crusher_output override. Family excluded.",
                        e.getKey(), level.dimension().identifier());
                return;
            }
            outputs.put(e.getKey(), output);
            scores.put(e.getKey(), acc.rawScore);
            if (acc.share != null) {
                shares.put(e.getKey(), acc.share);
            }
        });
        Map<Identifier, Integer> weights = ProfileWeights.applyShares(ProfileWeights.normalize(scores), shares);
        List<ResourceFamily> result = new ArrayList<>();
        for (Map.Entry<Identifier, Integer> e : weights.entrySet()) {
            Accumulator acc = families.get(e.getKey());
            List<ResourceFamily.Variant> variants = new ArrayList<>();
            acc.variants.forEach((item, w) -> variants.add(new ResourceFamily.Variant(item, Math.max(w, ProfileWeights.EPSILON))));
            result.add(new ResourceFamily(e.getKey(), List.copyOf(variants), e.getValue(), outputs.get(e.getKey())));
        }
        result.sort(Comparator.comparing(f -> f.key().toString()));
        DimensionalFluxMiner.LOGGER.debug("Resource Profile {}: {}", level.dimension().identifier(),
                result.stream().map(f -> f.key() + "=" + f.weight()).toList());
        return new ResourceProfile(level.dimension(), List.copyOf(result));
    }

    private static Item crusherOutput(ServerLevel level, Item ore) {
        return CrusherProcessing.resolve(level, new ItemStack(ore), false).map(r -> r.result().getItem()).orElse(null);
    }

    /** Family of an ore item: built-in vanilla table first, then {@code c:ores/<material>}. */
    public static Optional<Identifier> familyOf(Item item) {
        for (Map.Entry<String, List<Block>> e : VANILLA_FAMILIES.entrySet()) {
            for (Block block : e.getValue()) {
                if (block.asItem() == item) {
                    return Optional.of(Identifier.withDefaultNamespace(e.getKey()));
                }
            }
        }
        return item.builtInRegistryHolder().tags()
                .filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().startsWith("ores/"))
                .map(t -> t.location().getPath().substring("ores/".length()))
                .findFirst()
                .map(material -> VANILLA_FAMILIES.containsKey(material)
                        ? Identifier.withDefaultNamespace(material)
                        : Identifier.fromNamespaceAndPath("c", material));
    }

    private static void analyzeWorldgen(ServerLevel level, Map<Identifier, Accumulator> families) {
        analyzeBiomes(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes(), level.registryAccess(),
                terrainBlocks(level), families);
    }

    /**
     * Raw scores ({@code veinSize * placementsPerChunk * probability * biomeCoverage}) of every family generated by
     * {@code biomes}, before the weight transform. Used to study the distribution of any set of biomes.
     */
    public static Map<Identifier, Double> rawScores(Set<Holder<Biome>> biomes, net.minecraft.core.HolderLookup.Provider registries) {
        return rawScores(biomes, registries, null);
    }

    /** Same, counting only the ore blocks that can replace one of {@code terrain} (null = every ore block). */
    public static Map<Identifier, Double> rawScores(Set<Holder<Biome>> biomes, net.minecraft.core.HolderLookup.Provider registries,
                                                         Set<BlockState> terrain) {
        Map<Identifier, Accumulator> families = new LinkedHashMap<>();
        analyzeBiomes(biomes, registries, terrain, families);
        Map<Identifier, Double> scores = new LinkedHashMap<>();
        families.forEach((key, acc) -> scores.put(key, acc.rawScore));
        return scores;
    }

    /**
     * Blocks the terrain of a dimension is made of: the default block and every block its material rule places (stone
     * and deepslate in the Overworld, netherrack in the Nether, end stone in the End, the mod's own stone in a modded
     * dimension). Null when the generator is not noise based: then every ore block counts.
     */
    public static Set<BlockState> terrainBlocks(ServerLevel level) {
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator noise)) {
            return null;
        }
        return terrainBlocks(noise.generatorSettings().value(), level.registryAccess());
    }

    /** The terrain blocks of a set of noise settings (see {@link #terrainBlocks(ServerLevel)}). */
    public static Set<BlockState> terrainBlocks(NoiseGeneratorSettings settings, net.minecraft.core.RegistryAccess registries) {
        Set<BlockState> blocks = new HashSet<>();
        blocks.add(settings.defaultBlock());
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        collectRule(settings.materialRule().value(), registries, ops, blocks, new HashSet<>());
        blocks.removeIf(BlockState::isAir);
        return blocks;
    }

    private static void collectRule(MaterialRule rule, net.minecraft.core.HolderLookup.Provider registries,
                                    RegistryOps<JsonElement> ops, Set<BlockState> blocks, Set<Identifier> visited) {
        MaterialRule.DIRECT_CODEC.encodeStart(ops, rule).result()
                .ifPresent(json -> collectStates(json, registries, ops, blocks, visited));
    }

    /** Every {@code result_state} of a material rule; rules named by id (registered rules) are read too. */
    private static void collectStates(JsonElement json, net.minecraft.core.HolderLookup.Provider registries,
                                      RegistryOps<JsonElement> ops, Set<BlockState> blocks, Set<Identifier> visited) {
        if (json instanceof JsonObject obj) {
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                if (e.getKey().equals("result_state")) {
                    BlockState.CODEC.parse(ops, e.getValue()).result().ifPresent(blocks::add);
                } else {
                    collectStates(e.getValue(), registries, ops, blocks, visited);
                }
            }
        } else if (json instanceof JsonArray array) {
            array.forEach(element -> collectStates(element, registries, ops, blocks, visited));
        } else if (json instanceof JsonPrimitive primitive && primitive.isString()) {
            Identifier id = Identifier.tryParse(primitive.getAsString());
            if (id != null && visited.add(id)) {
                registries.lookup(Registries.MATERIAL_RULE)
                        .flatMap(rules -> rules.get(ResourceKey.create(Registries.MATERIAL_RULE, id)))
                        .ifPresent(holder -> collectRule(holder.value(), registries, ops, blocks, visited));
            }
        }
    }

    /**
     * Whether an ore target can replace one of the terrain blocks. Random matches ({@code random_block_match}) are
     * read by their block, not rolled.
     */
    static boolean replacesTerrain(RuleTest rule, Set<BlockState> terrain) {
        if (rule instanceof RandomBlockMatchTest || rule instanceof RandomBlockStateMatchTest) {
            JsonElement json = RuleTest.CODEC.encodeStart(JsonOps.INSTANCE, rule).result().orElse(null);
            if (!(json instanceof JsonObject obj)) {
                return true;
            }
            if (obj.has("block")) {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(obj.get("block").getAsString()));
                return terrain.stream().anyMatch(s -> s.is(block));
            }
            BlockState state = BlockState.CODEC.parse(JsonOps.INSTANCE, obj.get("block_state")).result().orElse(null);
            return state == null || terrain.contains(state);
        }
        RandomSource random = RandomSource.create(0);
        return terrain.stream().anyMatch(s -> rule.test(s, net.minecraft.core.BlockPos.ZERO, random));
    }

    /**
     * Terrain plus the rock blobs ore features add to it (granite, tuff, blackstone...): a non-ore target that replaces
     * the terrain becomes terrain too, so ores inside it still count.
     */
    private static Set<BlockState> withRockBlobs(Set<BlockState> terrain, List<OreShape> shapes) {
        Set<BlockState> blocks = new HashSet<>(terrain);
        boolean grew = true;
        for (int round = 0; grew && round < 4; round++) {
            grew = false;
            for (OreShape shape : shapes) {
                for (int i = 0; i < shape.targets().size(); i++) {
                    BlockState state = shape.targets().get(i);
                    if (!blocks.contains(state) && familyOf(state.getBlock().asItem()).isEmpty()
                            && replacesTerrain(shape.rules().get(i), blocks)) {
                        grew |= blocks.add(state);
                    }
                }
            }
        }
        return blocks;
    }

    private static void analyzeBiomes(Set<Holder<Biome>> biomes, net.minecraft.core.HolderLookup.Provider registries,
                                      Set<BlockState> terrain, Map<Identifier, Accumulator> families) {
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
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<PlacedFeature, OreShape> shapes = new LinkedHashMap<>();
        for (PlacedFeature placed : biomeCount.keySet()) {
            oreShape(placed.feature().value(), ops).ifPresent(shape -> shapes.put(placed, shape));
        }
        // One feature often lists a variant for every stone (stone, deepslate, netherrack, end stone, a mod's stone):
        // only the variants that can replace this dimension's terrain are generated here.
        Set<BlockState> hosts = terrain == null ? null : withRockBlobs(terrain, List.copyOf(shapes.values()));
        for (Map.Entry<PlacedFeature, OreShape> s : shapes.entrySet()) {
            PlacedFeature placed = s.getKey();
            OreShape ore = hosts == null ? s.getValue() : s.getValue().replacing(hosts);
            double coverage = biomeCount.get(placed) / (double) biomes.size();
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
                Optional<Identifier> family = familyOf(item);
                if (family.isEmpty()) {
                    continue;
                }
                Accumulator acc = families.computeIfAbsent(family.get(), k -> new Accumulator());
                acc.rawScore += perTarget;
                acc.variants.merge(item, perTarget, Double::sum);
            }
        }
    }

    /** Ore blocks a feature places, the block each one replaces ({@code rules}, same order) and its vein size. */
    public record OreShape(List<BlockState> targets, List<RuleTest> rules, double size) {
        /** Only the targets that can replace one of {@code terrain}. */
        public OreShape replacing(Set<BlockState> terrain) {
            List<BlockState> states = new ArrayList<>();
            List<RuleTest> kept = new ArrayList<>();
            for (int i = 0; i < targets.size(); i++) {
                if (replacesTerrain(rules.get(i), terrain)) {
                    states.add(targets.get(i));
                    kept.add(rules.get(i));
                }
            }
            return new OreShape(List.copyOf(states), List.copyOf(kept), size);
        }
    }

    /**
     * Vanilla ore features ({@link AbstractOreFeature}) are read directly. Other ore features (for example ore types
     * with their own codec) are read from their serialized form: every {@code targets[].state} entry is an ore block,
     * and a numeric {@code size} is the vein size ({@link #DEFAULT_VEIN_SIZE} when the size lives elsewhere, such as in a
     * mod config). The fields sit at the top level or, in older formats, in {@code config}.
     */
    public static Optional<OreShape> oreShape(Feature feature, RegistryOps<JsonElement> ops) {
        if (feature instanceof AbstractOreFeature ore) {
            return Optional.of(new OreShape(ore.targetStates().stream().map(BlockReplacement::state).toList(),
                    ore.targetStates().stream().map(BlockReplacement::target).toList(), ore.size()));
        }
        JsonElement json;
        try {
            json = Feature.DIRECT_CODEC.encodeStart(ops, feature).getOrThrow();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (!(json instanceof JsonObject root)) {
            return Optional.empty();
        }
        JsonObject config = root.get("config") instanceof JsonObject nested ? nested : root;
        if (!(config.get("targets") instanceof JsonArray targets)) {
            return Optional.empty();
        }
        List<BlockState> states = new ArrayList<>();
        List<RuleTest> rules = new ArrayList<>();
        for (JsonElement target : targets) {
            if (target instanceof JsonObject obj && obj.has("state")) {
                Optional<BlockState> state = BlockState.CODEC.parse(ops, obj.get("state")).result();
                if (state.isPresent()) {
                    states.add(state.get());
                    // A replaced block that cannot be read never hides the ore.
                    rules.add(obj.has("target") ? RuleTest.CODEC.parse(ops, obj.get("target")).result()
                            .orElse(AlwaysTrueTest.INSTANCE) : AlwaysTrueTest.INSTANCE);
                }
            }
        }
        if (states.isEmpty()) {
            return Optional.empty();
        }
        double size = config.get("size") instanceof JsonPrimitive p && p.isNumber() ? p.getAsDouble() : DEFAULT_VEIN_SIZE;
        return Optional.of(new OreShape(List.copyOf(states), List.copyOf(rules), size));
    }

    private static double averageCount(PlacementModifier modifier) {
        try {
            JsonElement json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, modifier).getOrThrow();
            JsonElement count = json instanceof JsonObject obj ? obj.get("count") : null;
            if (count == null) {
                return 1.0;
            }
            IntProvider provider = net.minecraft.util.valueproviders.IntProviders.CODEC.parse(JsonOps.INSTANCE, count).getOrThrow();
            return (provider.minInclusive() + provider.maxInclusive()) / 2.0;
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

    private static void applyOverrides(ServerLevel level, List<ProfileOverride> overrides, Map<Identifier, Accumulator> families) {
        for (ProfileOverride o : overrides) {
            if (!o.appliesTo(level.dimension())) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getValue(o.resource());
            if (item == Items.AIR) {
                DimensionalFluxMiner.LOGGER.warn("Dimensional miner resource override references unknown item {}", o.resource());
                continue;
            }
            if (!o.enabled()) {
                families.values().forEach(acc -> acc.variants.remove(item));
                families.values().removeIf(acc -> acc.variants.isEmpty());
                continue;
            }
            Optional<Identifier> key = o.family().or(() -> familyOf(item));
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
            o.crusherOutput().map(BuiltInRegistries.ITEM::getValue).filter(i -> i != Items.AIR).ifPresent(i -> acc.crusherOutput = i);
        }
    }
}
