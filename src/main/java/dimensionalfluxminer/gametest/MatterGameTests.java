package dimensionalfluxminer.gametest;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.miner.MatterVariants;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.registry.DfmDataMaps;
import dimensionalfluxminer.registry.DfmItems;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Dimensional Matter given by the Matter Input: upgrade bonus, reinjected ores and recipe variants. */
public final class MatterGameTests {
    private MatterGameTests() {
    }

    private static int units(Item item, ServerLevel level, boolean upgrade) {
        var value = MatterValues.matterInputUnits(new ItemStack(item), level, upgrade);
        return value.isPresent() ? value.getAsInt() : -1;
    }

    @GameTest(template = "empty_small")
    public static void upgradeAddsTheSmallerOfTheValueAndPointTwoDm(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        ServerLevel end = overworld.getServer().getLevel(Level.END);
        record Case(Item item, ServerLevel level, int plain, int upgraded) {
        }
        List<Case> cases = List.of(
                new Case(Items.DIRT, overworld, 1, 2),
                new Case(Items.COBBLESTONE, overworld, 3, 5),
                new Case(Items.SAND, overworld, 3, 5),
                new Case(Items.RED_SAND, overworld, 3, 5),
                new Case(Items.GRASS_BLOCK, overworld, 2, 4),
                new Case(Items.TERRACOTTA, overworld, 5, 7),
                new Case(Items.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.CYAN), overworld, 5, 7),
                new Case(Items.STONE, overworld, 5, 7),
                new Case(Items.OBSIDIAN, overworld, 10, 12),
                new Case(Items.OBSIDIAN, nether, 10, 12),
                new Case(Items.OBSIDIAN, end, 10, 12),
                new Case(Items.NETHER_BRICKS, nether, 15, 17),
                new Case(Items.GLOWSTONE, nether, 10, 12),
                new Case(Items.END_STONE, end, 5, 7),
                new Case(Items.DRAGON_EGG, end, 100, 102),
                // Ores the Miner makes: 2 DM at most, the cost of the attempt.
                new Case(Items.IRON_ORE, overworld, 5, 7),
                new Case(Items.DIAMOND_ORE, overworld, 10, 12),
                new Case(Items.ANCIENT_DEBRIS, nether, 20, 20));
        for (Case c : cases) {
            helper.assertTrue(units(c.item(), c.level(), false) == c.plain() && units(c.item(), c.level(), true) == c.upgraded(),
                    c.item() + " in " + c.level().dimension().identifier() + ": " + units(c.item(), c.level(), false) + " / "
                            + units(c.item(), c.level(), true) + ", expected " + c.plain() + " / " + c.upgraded());
        }
        // Still only matter of the Miner's dimension.
        helper.assertTrue(units(Items.STONE, nether, true) < 0 && units(Items.NETHERRACK, overworld, true) < 0
                && units(Items.DIAMOND_ORE, nether, true) < 0, "other dimensions still refused");
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void noOreTheMinerMakesGivesBackMoreThanItCost(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        int checked = 0;
        for (ServerLevel level : List.of(overworld, overworld.getServer().getLevel(Level.NETHER), overworld.getServer().getLevel(Level.END))) {
            for (ResourceFamily family : ResourceProfiles.get(level).families()) {
                for (ResourceFamily.Variant variant : family.variants()) {
                    int upgraded = units(variant.item(), level, true);
                    helper.assertTrue(upgraded <= MatterValues.ROLL_COST_UNITS,
                            variant.item() + " reinjected in " + level.dimension().identifier() + " gives " + upgraded + " units");
                    checked++;
                }
            }
        }
        helper.assertTrue(checked > 0, "ores checked");
        helper.succeed();
    }

    @GameTest(template = "empty_large")
    public static void reinjectingTheOutputNeverGrowsTheMatter(GameTestHelper helper) {
        MinerControllerBlockEntity c = MinerGameTests.build(helper);
        c.coreSlot().setStackInSlot(0, new ItemStack(DfmItems.STABILIZED_DIMENSIONAL_CORE.get()));
        c.upgradeSlot().setStackInSlot(0, new ItemStack(DfmItems.EFFICIENCY_UPGRADE.get()));
        MinerGameTests.powerUp(c, 10);
        c.setRunRequested(true);
        helper.runAfterDelay(1, () -> {
            long start = 2_000;
            c.setDmUnits(start);
            long attempts = 0;
            for (int cycle = 0; cycle < 20; cycle++) {
                long before = c.dmUnits();
                c.energyBuffer().setStored(1_000_000_000_000L);
                c.acceleratedCycle();
                attempts += (before - c.dmUnits()) / MatterValues.ROLL_COST_UNITS;
                // Everything made goes straight back into the Matter Input.
                for (int slot = 0; slot < MinerControllerBlockEntity.OUTPUT_SLOTS; slot++) {
                    ItemStack made = c.output().getStackInSlot(slot);
                    if (!made.isEmpty()) {
                        c.insertSubstrate(made.copy(), false);
                        c.output().setStackInSlot(slot, ItemStack.EMPTY);
                    }
                }
                helper.assertTrue(c.dmUnits() <= start, "cycle " + cycle + ": DM grew to " + c.dmUnits());
            }
            helper.assertTrue(attempts > 0, "the Miner worked");
            helper.succeed();
        });
    }

    @GameTest(template = "empty_small")
    public static void variantsNeverMultiplyTheUpgradeOrRoundUp(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        record Case(Item item, ServerLevel level, int plain, int upgraded) {
        }
        // Plain values are unchanged; with the upgrade a piece is its share of the upgraded block.
        List<Case> cases = List.of(
                new Case(Items.POLISHED_ANDESITE, overworld, 5, 7),
                new Case(Items.STONE_BRICKS, overworld, 5, 7),
                new Case(Items.STONE_SLAB, overworld, 2, 3),
                new Case(Items.SMOOTH_STONE_SLAB, overworld, 2, 3),
                new Case(Items.COBBLESTONE_STAIRS, overworld, 3, 5),
                new Case(Items.CHISELED_STONE_BRICKS, overworld, 4, 6),
                new Case(Items.MOSSY_COBBLESTONE, overworld, 3, 5),
                new Case(Items.NETHER_BRICK_SLAB, nether, 7, 8));
        for (Case c : cases) {
            helper.assertTrue(units(c.item(), c.level(), false) == c.plain() && units(c.item(), c.level(), true) == c.upgraded(),
                    c.item() + ": " + units(c.item(), c.level(), false) + " / " + units(c.item(), c.level(), true)
                            + ", expected " + c.plain() + " / " + c.upgraded());
        }
        helper.assertTrue(2 * units(Items.STONE_SLAB, overworld, true) <= units(Items.STONE, overworld, true), "two slabs never beat a stone");
        // 2 dirt + 2 gravel -> 4 coarse dirt is worth a quarter of a unit each: no matter (it used to be rounded up to 1).
        helper.assertTrue(units(Items.COARSE_DIRT, overworld, false) < 0 && units(Items.COARSE_DIRT, overworld, true) < 0, "coarse dirt refused");
        // Every variant of every dimension: the upgrade adds at most min(value, 2).
        for (ServerLevel level : List.of(overworld, nether, overworld.getServer().getLevel(Level.END))) {
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (MatterValues.isOre(stack) || stack.typeHolder().getData(DfmDataMaps.MATTER_VALUE) != null) {
                    continue;
                }
                int plain = units(item, level, false);
                if (plain < 0) {
                    continue;
                }
                int upgraded = units(item, level, true);
                helper.assertTrue(plain > 0 && upgraded >= plain && upgraded <= plain + Math.min(plain, 2),
                        item + " in " + level.dimension().identifier() + ": " + plain + " -> " + upgraded);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void madeUpRecipesNeverGiveMoreThanTheyCost(GameTestHelper helper) {
        // Substrates A = 5 and B = 1; X, Y, Z, W are blocks made from them by imaginary recipes of other mods.
        Item a = Items.STONE;
        Item b = Items.DIRT;
        Item unknown = Items.GRAVEL;
        Item x = Items.STONE_BRICKS;
        Item y = Items.POLISHED_ANDESITE;
        Item z = Items.POLISHED_GRANITE;
        Item w = Items.POLISHED_DIORITE;
        Item v = Items.SMOOTH_STONE;
        Map<Item, Integer> substrates = new HashMap<>(Map.of(a, 5, b, 1));
        List<MatterVariants.Making> makings = List.of(
                // 1 A -> 9 X: under one unit each, refused.
                new MatterVariants.Making(List.<Item[]>of(new Item[] {a}), x, 9),
                // A + unrecognized -> 2 Y: the unrecognized item adds nothing (2 each, 4 < 5).
                new MatterVariants.Making(List.<Item[]>of(new Item[] {a}, new Item[] {unknown}), y, 2),
                // A loop Y -> Z -> Y and a merge 2 Z -> W: the cheapest way wins, nothing grows.
                new MatterVariants.Making(List.<Item[]>of(new Item[] {y}), z, 1),
                new MatterVariants.Making(List.<Item[]>of(new Item[] {z}), y, 1),
                new MatterVariants.Making(List.<Item[]>of(new Item[] {z}, new Item[] {z}), w, 1),
                // Mostly unrecognized: 1 B + 3 unknown -> V is not matter.
                new MatterVariants.Making(List.<Item[]>of(new Item[] {b}, new Item[] {unknown}, new Item[] {unknown}, new Item[] {unknown}), v, 1));
        Map<Item, Integer> values = MatterVariants.variants(makings, substrates);
        Map<Item, Integer> expected = new LinkedHashMap<>();
        expected.put(y, 2);
        expected.put(z, 2);
        expected.put(w, 4);
        helper.assertTrue(values.equals(expected), "variants " + values + ", expected " + expected);
        // No recipe gives more value out than the recognized value that goes in.
        for (MatterVariants.Making making : makings) {
            Integer out = values.get(making.output());
            if (out == null) {
                continue;
            }
            int in = 0;
            for (Item[] options : making.ingredients()) {
                int cheapest = Integer.MAX_VALUE;
                for (Item option : options) {
                    Integer value = substrates.containsKey(option) ? substrates.get(option) : values.get(option);
                    if (value != null) {
                        cheapest = Math.min(cheapest, value);
                    }
                }
                in += cheapest == Integer.MAX_VALUE ? 0 : cheapest;
            }
            helper.assertTrue(out * making.count() <= in, making.output() + ": " + out + " x " + making.count() + " > " + in);
        }
        helper.succeed();
    }

    @GameTest(template = "empty_small")
    public static void modMaterialsFollowTheSameRules(GameTestHelper helper) {
        // Allthemodium's ancient stone and dirt (when installed) are worth 0.5 and 0.1 DM in The Other.
        if (!net.neoforged.fml.ModList.get().isLoaded("allthemodium")) {
            helper.succeed();
            return;
        }
        var other = helper.getLevel().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse("allthemodium:the_other")));
        if (other == null) {
            helper.succeed();
            return;
        }
        Item stone = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("allthemodium:ancient_stone"));
        Item dirt = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("allthemodium:ancient_dirt"));
        helper.assertTrue(units(stone, other, false) == 5 && units(stone, other, true) == 7, "ancient stone 0.5 -> 0.7 DM");
        helper.assertTrue(units(dirt, other, false) == 1 && units(dirt, other, true) == 2, "ancient dirt 0.1 -> 0.2 DM");
        helper.assertTrue(units(stone, helper.getLevel(), true) < 0, "only in The Other");
        helper.succeed();
    }

    /**
     * Every block the guide shows as matter of a vanilla dimension (with its variants) is accepted there. The ores
     * depend on the worldgen (the GameTest world is flat, without emerald), so they are left out here.
     */
    @GameTest(template = "empty_small")
    public static void guideMatterExamplesAreAccepted(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (var dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            ServerLevel level = server.getLevel(dimension);
            for (List<Item> slot : dimensionalfluxminer.miner.MatterExamples.of(dimension)) {
                for (Item item : slot) {
                    if (MatterValues.isOre(new ItemStack(item))) {
                        continue;
                    }
                    helper.assertTrue(units(item, level, false) > 0, item + " is not matter in " + dimension.identifier());
                }
            }
        }
        helper.succeed();
    }

    /**
     * Ad Astra planets (when installed): each planet's own stone, and the blocks of other dimensions it generates, are
     * matter only there, and Dimensional Death never acts on a planet or in orbit.
     */
    @GameTest(template = "empty_small")
    public static void adAstraPlanetsTakeTheirOwnMatterOnly(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        ServerLevel moon = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse("ad_astra:moon")));
        ServerLevel mercury = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse("ad_astra:mercury")));
        if (!net.neoforged.fml.ModList.get().isLoaded("ad_astra") || moon == null || mercury == null) {
            helper.succeed();
            return;
        }
        Item moonStone = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ad_astra:moon_stone"));
        Item moonCobblestone = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ad_astra:moon_cobblestone"));
        Item moonSand = BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("ad_astra:moon_sand"));
        helper.assertTrue(units(moonStone, moon, false) == 5 && units(moonCobblestone, moon, false) == 3
                && units(moonSand, moon, false) == 3, "moon stone 0.5, cobblestone and sand 0.3 DM");
        helper.assertTrue(units(moonStone, mercury, false) < 0 && units(moonStone, helper.getLevel(), false) < 0, "moon stone only on the Moon");
        helper.assertTrue(units(Items.BASALT, mercury, false) == 5 && units(Items.SOUL_SOIL, moon, false) == 10,
                "blocks of other dimensions that generate on a planet keep their value there");
        helper.assertTrue(units(Items.BASALT, moon, false) < 0 && units(Items.STONE, moon, false) < 0, "and nowhere else");
        helper.assertTrue(units(Items.OBSIDIAN, moon, false) == 10, "obsidian everywhere");
        helper.assertTrue(!dimensionalfluxminer.miner.corruption.ProtectionService.corruptionAllowed(moon)
                && !dimensionalfluxminer.miner.corruption.ProtectionService.corruptionAllowed(mercury)
                && dimensionalfluxminer.miner.corruption.ProtectionService.corruptionAllowed(helper.getLevel()),
                "no Dimensional Death on the planets");
        helper.succeed();
    }
}
