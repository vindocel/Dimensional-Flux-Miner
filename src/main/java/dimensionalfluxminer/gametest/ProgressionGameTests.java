package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.profile.ProfileBuilder;
import dimensionalfluxminer.miner.profile.ProfileDimensionRule;
import dimensionalfluxminer.miner.profile.ProfileOverride;
import dimensionalfluxminer.miner.profile.ProfileWeights;
import dimensionalfluxminer.miner.profile.VoidCurve;
import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmRecipes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Recipe progression, tags, datapack overrides and the ore distribution of the Resource Profiles. */
public final class ProgressionGameTests {
    private ProgressionGameTests() {
    }

    private static List<Ingredient> ingredients(Recipe<?> recipe) {
        if (recipe instanceof AlloyingRecipe alloy) {
            return alloy.ingredients().stream().map(SizedIngredient::ingredient).toList();
        }
        return dimensionalfluxminer.recipe.Recipes.ingredients(recipe);
    }

    @GameTest(template = "empty_small")
    public static void everyRecipeOfTheModHasRealIngredientsAndResults(GameTestHelper helper) {
        var level = helper.getLevel();
        int checked = 0;
        for (RecipeHolder<?> holder : dimensionalfluxminer.recipe.Recipes.all(level)) {
            if (!holder.id().identifier().getNamespace().equals(DimensionalFluxMiner.MOD_ID)) {
                continue;
            }
            Recipe<?> recipe = holder.value();
            List<Ingredient> list = ingredients(recipe);
            helper.assertTrue(!list.isEmpty(), holder.id() + " has ingredients");
            for (Ingredient ingredient : list) {
                helper.assertTrue(dimensionalfluxminer.recipe.Recipes.stacks(ingredient).length > 0, holder.id() + ": an ingredient matches no item");
            }
            helper.assertTrue(!dimensionalfluxminer.recipe.Recipes.result(level.registryAccess(), recipe).isEmpty()
                    || recipe.getType() == DfmRecipes.ORE_CRUSHING.get(), holder.id() + " has a result");
            checked++;
        }
        helper.assertTrue(checked > 20, "recipes checked: " + checked);
        helper.succeed();
    }

    /**
     * Which items can be made from vanilla items, following the mod's recipes in order: crafting at once, Crusher
     * recipes once a Crusher exists, Electric Furnace alloys once an Electric Furnace exists. Returns the step each item becomes available.
     * A step only uses what earlier steps made, so the result never depends on the order the recipes are listed in.
     */
    private static Map<Item, Integer> reachable(ServerLevel level, boolean withCrushing, boolean withAlloying) {
        Map<Item, Integer> step = new HashMap<>();
        Item crusher = DfmItems.MECHANICAL_CRUSHER.get();
        Item furnace = DfmItems.ELECTRIC_FURNACE.get();
        List<RecipeHolder<?>> recipes = new ArrayList<>(dimensionalfluxminer.recipe.Recipes.all(level));
        boolean changed = true;
        int round = 0;
        while (changed) {
            changed = false;
            round++;
            // Items made before this step; what this step makes is only usable from the next one.
            Set<Item> known = new HashSet<>(step.keySet());
            for (RecipeHolder<?> holder : recipes) {
                Recipe<?> recipe = holder.value();
                ItemStack result = dimensionalfluxminer.recipe.Recipes.result(level.registryAccess(), recipe);
                if (result.isEmpty() || !BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(DimensionalFluxMiner.MOD_ID)
                        || step.containsKey(result.getItem())) {
                    continue;
                }
                boolean crushing = recipe.getType() == DfmRecipes.CRUSHING.get() || recipe.getType() == DfmRecipes.ORE_CRUSHING.get();
                boolean alloying = recipe.getType() == DfmRecipes.ALLOYING.get();
                if ((crushing && (!withCrushing || !known.contains(crusher))) || (alloying && (!withAlloying || !known.contains(furnace)))) {
                    continue;
                }
                boolean all = true;
                for (Ingredient ingredient : ingredients(recipe)) {
                    boolean any = false;
                    for (ItemStack option : dimensionalfluxminer.recipe.Recipes.stacks(ingredient)) {
                        String ns = BuiltInRegistries.ITEM.getKey(option.getItem()).getNamespace();
                        if (ns.equals("minecraft") || known.contains(option.getItem())) {
                            any = true;
                            break;
                        }
                    }
                    all &= any;
                }
                if (all) {
                    step.put(result.getItem(), round);
                    changed = true;
                }
            }
        }
        return step;
    }

    @GameTest(template = "empty_small")
    public static void progressionIsReachableInOrderAndPureSteelNeedsBothMachines(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<Item, Integer> step = reachable(level, true, true);
        List<Item> chain = List.of(DfmItems.MECHANICAL_CRUSHER.get(), DfmItems.ELECTRIC_FURNACE.get(), DfmItems.INGOT_STEEL.get(),
                DfmItems.ENDER_EYE_REINFORCED.get(), DfmItems.REINFORCED_STRUCTURAL_GLASS.get(), DfmItems.STEEL_CHASSIS.get(),
                DfmItems.VENTILATION_UNIT.get(), DfmItems.DIMENSIONAL_PORT.get(), DfmItems.MINER_TERMINAL.get(), DfmItems.QUANTUM_CORE.get(),
                DfmItems.BASIC_DIMENSIONAL_CORE.get(), DfmItems.STABILIZED_DIMENSIONAL_CORE.get(), DfmItems.EFFICIENCY_UPGRADE.get(),
                DfmItems.DIMENSIONAL_TANK.get(), DfmItems.DIMENSIONAL_DUAL_TANK.get());
        StringBuilder map = new StringBuilder();
        for (Item item : chain) {
            helper.assertTrue(step.containsKey(item), BuiltInRegistries.ITEM.getKey(item) + " is reachable from vanilla items");
            map.append(BuiltInRegistries.ITEM.getKey(item).getPath()).append('=').append(step.get(item)).append(' ');
        }
        DimensionalFluxMiner.LOGGER.info("Progression steps: {}", map.toString().trim());
        helper.assertTrue(step.get(DfmItems.INGOT_STEEL.get()) > step.get(DfmItems.ELECTRIC_FURNACE.get()), "steel after the Electric Furnace");
        helper.assertTrue(step.get(DfmItems.STEEL_CHASSIS.get()) > step.get(DfmItems.INGOT_STEEL.get()), "chassis after steel");
        helper.assertTrue(step.get(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()) > step.get(DfmItems.BASIC_DIMENSIONAL_CORE.get()), "stabilized after basic");
        // Pure Steel cannot skip either machine: no Electric Furnace, no steel; no Crusher, no calcite grains.
        helper.assertFalse(reachable(level, true, false).containsKey(DfmItems.INGOT_STEEL.get()), "no steel without the Electric Furnace");
        helper.assertFalse(reachable(level, false, true).containsKey(DfmItems.INGOT_STEEL.get()), "no steel without the Crusher");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void pureSteelKeepsItsIdentity(GameTestHelper helper) {
        ItemStack steel = new ItemStack(DfmItems.INGOT_STEEL.get());
        for (String tag : List.of("ingots", "ingots/steel")) {
            helper.assertFalse(steel.is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", tag))), "Pure Steel is not in c:" + tag);
        }
        // Every ingredient of the mod that takes Pure Steel takes only Pure Steel, never the steel of another mod.
        Set<Item> otherSteels = new HashSet<>();
        BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/steel")))
                .ifPresent(set -> set.forEach(h -> otherSteels.add(h.value())));
        int checked = 0;
        for (RecipeHolder<?> holder : dimensionalfluxminer.recipe.Recipes.all(helper.getLevel())) {
            if (!holder.id().identifier().getNamespace().equals(DimensionalFluxMiner.MOD_ID)) {
                continue;
            }
            for (Ingredient ingredient : ingredients(holder.value())) {
                if (ingredient.test(steel)) {
                    checked++;
                    for (Item other : otherSteels) {
                        helper.assertFalse(ingredient.test(new ItemStack(other)), holder.id() + " accepts " + other);
                    }
                }
            }
        }
        helper.assertTrue(checked > 0, "steel ingredients checked");
        DimensionalFluxMiner.LOGGER.info("Steel of other mods installed (c:ingots/steel): {}", otherSteels.stream().map(BuiltInRegistries.ITEM::getKey).toList());
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void datapackOverridesDisableReweightAndAddResources(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceKey<Level> here = level.dimension();
        Identifier diamond = Identifier.withDefaultNamespace("diamond");
        List<ProfileOverride> overrides = List.of(
                // Remove both coal ores, the whole family goes.
                new ProfileOverride(Identifier.parse("minecraft:coal_ore"), Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.empty()),
                new ProfileOverride(Identifier.parse("minecraft:deepslate_coal_ore"), Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.empty()),
                // Diamonds take 25 % of the profile.
                new ProfileOverride(Identifier.parse("minecraft:diamond_ore"), Optional.of(here), Optional.empty(), Optional.of(0.25), true, Optional.empty()),
                // A resource this dimension does not generate, with its own family and Crusher output.
                new ProfileOverride(Identifier.parse("minecraft:nether_gold_ore"), Optional.of(here), Optional.of(Identifier.parse("test:custom")),
                        Optional.of(0.05), true, Optional.of(Identifier.parse("minecraft:gold_nugget"))),
                // Without a dimension a new resource is refused.
                new ProfileOverride(Identifier.parse("minecraft:ancient_debris"), Optional.empty(), Optional.of(Identifier.parse("test:nowhere")),
                        Optional.empty(), true, Optional.empty()));
        ResourceProfile profile = ProfileBuilder.build(level, overrides);
        helper.assertTrue(profile.family(Identifier.withDefaultNamespace("coal")).isEmpty(), "coal disabled");
        int total = profile.totalWeight();
        int diamonds = profile.family(diamond).map(ResourceFamily::weight).orElse(0);
        helper.assertTrue(Math.abs(diamonds - 2_500) <= 1 && Math.abs(total - 10_000) <= profile.families().size(), "diamond share 25 %: " + diamonds + " / " + total);
        var custom = profile.family(Identifier.parse("test:custom"));
        helper.assertTrue(custom.isPresent() && custom.get().crusherOutput() == Items.GOLD_NUGGET && Math.abs(custom.get().weight() - 500) <= 1,
                "custom family added with its output and 5 %: " + custom);
        helper.assertTrue(profile.family(Identifier.parse("test:nowhere")).isEmpty(), "no dimension, no new family");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void voidAppliesOnlyBelowTheMinimumNumberOfFamilies(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceProfile plain = ProfileBuilder.build(level, List.of());
        int families = plain.families().size();
        helper.assertTrue(families > 1, "the test dimension has several families: " + families);
        // Enough families: the plain worldgen distribution, no void.
        ResourceProfile enough = ProfileBuilder.build(level, List.of(), List.of(new ProfileDimensionRule(level.dimension(), 0.97, families, VoidCurve.GEOMETRIC)));
        helper.assertTrue(enough.emptyWeight() == 0 && enough.equals(plain), "with " + families + " families there is no void");
        // Fewer families than the minimum (step curve): 97 % void, the families keep their proportions in the 3 % left.
        ResourceProfile sparse = ProfileBuilder.build(level, List.of(), List.of(new ProfileDimensionRule(level.dimension(), 0.97, families + 1, VoidCurve.STEP)));
        // Geometric curve: one family short of the minimum gives the curve's last step, not the full void.
        ResourceProfile gradual = ProfileBuilder.build(level, List.of(), List.of(new ProfileDimensionRule(level.dimension(), 0.97, families + 1, VoidCurve.GEOMETRIC)));
        double expectedGradual = VoidCurve.GEOMETRIC.voidShare(0.97, families + 1, families);
        helper.assertTrue(expectedGradual > 0 && expectedGradual < 0.97 && Math.abs(gradual.emptyShare() - expectedGradual) < 0.002,
                "geometric void " + gradual.emptyShare() + ", expected " + expectedGradual);
        helper.assertTrue(Math.abs(sparse.emptyShare() - 0.97) < 0.002, "void share " + sparse.emptyShare());
        helper.assertTrue(sparse.families().size() == families, "no family removed");
        ResourceFamily topPlain = plain.families().stream().max(java.util.Comparator.comparingInt(ResourceFamily::weight)).orElseThrow();
        ResourceFamily topSparse = sparse.family(topPlain.key()).orElseThrow();
        helper.assertTrue(Math.abs(topSparse.weight() - topPlain.weight() * 0.03) <= 1, "proportions kept: " + topSparse.weight());
        // A rule of another dimension changes nothing here.
        helper.assertTrue(ProfileBuilder.build(level, List.of(), List.of(new ProfileDimensionRule(Level.END, 0.97, 1000, VoidCurve.STEP))).equals(plain),
                "rules only apply to their dimension");
        // The roll: about 97 % of the attempts find nothing.
        var random = net.minecraft.util.RandomSource.create(7);
        int empty = 0;
        int rolls = 200_000;
        for (int i = 0; i < rolls; i++) {
            if (sparse.roll(random).isEmpty()) {
                empty++;
            }
        }
        helper.assertTrue(Math.abs(empty / (double) rolls - 0.97) < 0.003, "void rolls " + empty + " / " + rolls);
        int none = 0;
        for (int i = 0; i < 10_000; i++) {
            if (plain.roll(random).isEmpty()) {
                none++;
            }
        }
        helper.assertTrue(none == 0, "without a void every attempt finds a family");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void theEndGetsItsDefaultVoid(GameTestHelper helper) {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(dimensionalfluxminer.miner.profile.ResourceProfiles.rules().stream()
                .anyMatch(r -> r.dimension().equals(Level.END) && r.emptyShare() == 0.97 && r.minFamilies() == 5 && r.curve() == VoidCurve.GEOMETRIC),
                "default End rule loaded");
        ResourceProfile profile = dimensionalfluxminer.miner.profile.ResourceProfiles.get(end);
        int families = profile.families().size();
        DimensionalFluxMiner.LOGGER.info("End profile: {} families, void {}", families, profile.emptyShare());
        if (families == 0) {
            helper.assertTrue(profile.emptyShare() == 0, "vanilla End: no resources, no void");
        } else if (families < 5) {
            double expected = VoidCurve.GEOMETRIC.voidShare(0.97, 5, families);
            helper.assertTrue(Math.abs(profile.emptyShare() - expected) < 0.002, "End with " + families + " families: void " + expected);
        } else {
            helper.assertTrue(profile.emptyShare() == 0, "End with " + families + " families: plain distribution");
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void onlyOreVariantsOfTheDimensionStoneCount(GameTestHelper helper) {
        // The terrain comes from the noise settings of each dimension (read from the registry: the test world is flat).
        var registries = helper.getLevel().registryAccess();
        var noise = registries.lookupOrThrow(Registries.NOISE_SETTINGS);
        Set<net.minecraft.world.level.block.state.BlockState> netherTerrain = ProfileBuilder.terrainBlocks(
                noise.getValueOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.NETHER), registries);
        Set<net.minecraft.world.level.block.state.BlockState> endTerrain = ProfileBuilder.terrainBlocks(
                noise.getValueOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.END), registries);
        helper.assertTrue(netherTerrain != null && netherTerrain.contains(net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState())
                && !netherTerrain.contains(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()), "Nether terrain is netherrack");
        helper.assertTrue(endTerrain != null && endTerrain.contains(net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState())
                && !endTerrain.contains(net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState()), "End terrain is end stone");
        // A vein listing a variant per stone keeps only the variant of the stone that is there.
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var gold = helper.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE)
                .getValue(Identifier.withDefaultNamespace("ore_gold"));
        var shape = ProfileBuilder.oreShape(gold, ops).orElseThrow();
        var onStone = shape.replacing(Set.of(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())).targets();
        helper.assertTrue(onStone.size() == 1 && onStone.get(0).is(net.minecraft.world.level.block.Blocks.GOLD_ORE), "stone: gold ore only");
        helper.assertTrue(shape.replacing(endTerrain).targets().isEmpty(), "end stone: no gold variant");
        // Overworld biomes on netherrack: no Overworld ore family is generated.
        Set<Holder<Biome>> overworld = biomes(helper.getLevel(), BiomeTags.IS_OVERWORLD);
        var stone = ProfileBuilder.rawScores(overworld, helper.getLevel().registryAccess(),
                Set.of(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
        var rack = ProfileBuilder.rawScores(overworld, helper.getLevel().registryAccess(), netherTerrain);
        Identifier iron = Identifier.withDefaultNamespace("iron");
        helper.assertTrue(stone.containsKey(iron) && !rack.containsKey(iron), "iron only where its stone exists");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void conditionalResourceFilesFollowTheirMods(GameTestHelper helper) {
        // compat/allthemodium_unobtainium.json only loads with Allthemodium installed.
        Identifier unobtainium = Identifier.fromNamespaceAndPath("allthemodium", "unobtainium_ore");
        boolean loaded = dimensionalfluxminer.miner.profile.ResourceProfiles.overrides().stream()
                .anyMatch(o -> o.resource().equals(unobtainium));
        helper.assertTrue(loaded == net.neoforged.fml.ModList.get().isLoaded("allthemodium"), "Unobtainium entry follows Allthemodium");
        helper.succeed();
    }

    // ---------------------------------------------------------------- distribution

    private static Set<Holder<Biome>> biomes(ServerLevel level, TagKey<Biome> tag) {
        Set<Holder<Biome>> set = new HashSet<>();
        level.registryAccess().lookupOrThrow(Registries.BIOME).getTagOrEmpty(tag).forEach(set::add);
        return set;
    }

    /** Linear normalization (no exponent), for comparison. */
    private static Map<Identifier, Integer> linear(Map<Identifier, Double> raw) {
        double sum = raw.values().stream().mapToDouble(v -> Math.max(v, ProfileWeights.EPSILON)).sum();
        Map<Identifier, Integer> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> out.put(k, Math.max(1, (int) Math.round(Math.max(v, ProfileWeights.EPSILON) / sum * ProfileWeights.TARGET_TOTAL))));
        return out;
    }

    private static Map<Identifier, Long> roll(Map<Identifier, Integer> weights, long rolls, long seed) {
        java.util.Random random = new java.util.Random(seed);
        List<Identifier> keys = new ArrayList<>(weights.keySet());
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        Map<Identifier, Long> counts = new LinkedHashMap<>();
        keys.forEach(k -> counts.put(k, 0L));
        for (long i = 0; i < rolls; i++) {
            int r = random.nextInt(total);
            for (Identifier k : keys) {
                r -= weights.get(k);
                if (r < 0) {
                    counts.merge(k, 1L, Long::sum);
                    break;
                }
            }
        }
        return counts;
    }

    @GameTest(template = "empty_small", timeoutTicks = 400)
    public static void oreDistributionReport(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long rolls = 1_000_000;
        Map<String, TagKey<Biome>> dimensions = new LinkedHashMap<>();
        dimensions.put("overworld", BiomeTags.IS_OVERWORLD);
        dimensions.put("nether", BiomeTags.IS_NETHER);
        dimensions.put("end", BiomeTags.IS_END);
        for (Map.Entry<String, TagKey<Biome>> dim : dimensions.entrySet()) {
            Set<Holder<Biome>> set = biomes(level, dim.getValue());
            Map<Identifier, Double> raw = ProfileBuilder.rawScores(set, level.registryAccess());
            for (boolean vanillaOnly : new boolean[] {true, false}) {
                Map<Identifier, Double> scores = new LinkedHashMap<>();
                raw.forEach((k, v) -> {
                    if (!vanillaOnly || k.getNamespace().equals("minecraft")) {
                        scores.put(k, v);
                    }
                });
                if (scores.isEmpty()) {
                    DimensionalFluxMiner.LOGGER.info("Distribution {} ({}): no ore families", dim.getKey(), vanillaOnly ? "vanilla" : "all mods");
                    continue;
                }
                Map<Identifier, Integer> weighted = ProfileWeights.normalize(scores);
                Map<Identifier, Integer> lin = linear(scores);
                Map<Identifier, Long> simulated = roll(weighted, rolls, 42);
                int weightedTotal = weighted.values().stream().mapToInt(Integer::intValue).sum();
                int linTotal = lin.values().stream().mapToInt(Integer::intValue).sum();
                StringBuilder line = new StringBuilder();
                List<Identifier> keys = new ArrayList<>(scores.keySet());
                keys.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
                for (Identifier k : keys) {
                    line.append(String.format(java.util.Locale.ROOT, "%n  %-24s raw=%10.4f linear=%6.2f%% profile=%6.2f%% simulated=%7d (%6.2f%%)",
                            k, scores.get(k), 100.0 * lin.get(k) / linTotal, 100.0 * weighted.get(k) / weightedTotal, simulated.get(k),
                            100.0 * simulated.get(k) / rolls));
                }
                DimensionalFluxMiner.LOGGER.info("Distribution {} ({}, {} biomes, {} rolls):{}", dim.getKey(), vanillaOnly ? "vanilla" : "all mods",
                        set.size(), rolls, line);
                helper.assertTrue(Math.abs(weightedTotal - ProfileWeights.TARGET_TOTAL) <= scores.size(), "weights normalized");
                for (Identifier k : keys) {
                    double expected = weighted.get(k) / (double) weightedTotal;
                    double got = simulated.get(k) / (double) rolls;
                    // Within 5 standard deviations of the weighted roll.
                    double sigma = Math.sqrt(expected * (1 - expected) / rolls);
                    helper.assertTrue(Math.abs(got - expected) <= 5 * sigma + 1e-9, k + " rolls follow the weights");
                }
            }
        }
        helper.succeed();
    }
}
