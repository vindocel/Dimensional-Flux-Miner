package dimensionalfluxminer.miner;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The matter the guide shows for each vanilla dimension: every slot is a block family (a block and its variants, or
 * the ores) that the guide cycles through. A GameTest checks that every item here is accepted in its dimension.
 */
public final class MatterExamples {
    private MatterExamples() {
    }

    public static final List<List<Item>> OVERWORLD = List.of(
            List.of(Items.STONE, Items.COBBLESTONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.STONE_BRICKS,
                    Items.SMOOTH_STONE, Items.POLISHED_GRANITE, Items.POLISHED_DIORITE, Items.POLISHED_ANDESITE),
            List.of(Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES),
            List.of(Items.DIRT, Items.GRASS_BLOCK),
            List.of(Items.SAND, Items.RED_SAND),
            java.util.stream.Stream.concat(java.util.stream.Stream.of(Items.TERRACOTTA), Items.DYED_TERRACOTTA.asList().stream()).toList(),
            List.of(Items.IRON_ORE, Items.COPPER_ORE, Items.COAL_ORE, Items.GOLD_ORE, Items.REDSTONE_ORE, Items.LAPIS_ORE,
                    Items.DIAMOND_ORE, Items.EMERALD_ORE));

    public static final List<List<Item>> NETHER = List.of(
            List.of(Items.NETHERRACK, Items.CRIMSON_NYLIUM, Items.WARPED_NYLIUM, Items.MAGMA_BLOCK),
            List.of(Items.BASALT, Items.POLISHED_BASALT, Items.SMOOTH_BASALT, Items.BLACKSTONE, Items.POLISHED_BLACKSTONE),
            List.of(Items.GLOWSTONE),
            List.of(Items.SOUL_SAND, Items.SOUL_SOIL),
            List.of(Items.NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS, Items.CHISELED_NETHER_BRICKS),
            List.of(Items.NETHER_QUARTZ_ORE, Items.NETHER_GOLD_ORE, Items.ANCIENT_DEBRIS));

    public static final List<List<Item>> END = List.of(
            List.of(Items.END_STONE, Items.END_STONE_BRICKS),
            List.of(Items.PURPUR_BLOCK, Items.PURPUR_PILLAR));

    public static List<List<Item>> of(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) {
            return NETHER;
        }
        return dimension == Level.END ? END : OVERWORLD;
    }
}
