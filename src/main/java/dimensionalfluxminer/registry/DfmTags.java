package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;

public final class DfmTags {
    private DfmTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> CORRUPTION_BLACKLIST = tag("corruption_blacklist");
        /** Stone-family sources converted into corrupted stone; extensible by datapacks. */
        public static final TagKey<Block> CORRUPTIBLE_STONE = tag("corruptible_stone");
        public static final TagKey<Block> CORRUPTIBLE_TERRACOTTA = tag("corruptible_terracotta");
        /** Blocks converted into corrupted dirt; extensible by datapacks. */
        public static final TagKey<Block> CORRUPTIBLE_DIRT = tag("corruptible_dirt");
        /** Blocks converted into corrupted grass; extensible by datapacks. */
        public static final TagKey<Block> CORRUPTIBLE_GRASS = tag("corruptible_grass");
        /** Plants on a corrupted column that become wither roses; extensible by datapacks. */
        public static final TagKey<Block> CORRUPTIBLE_FLOWERS = tag("corruptible_flowers");
        /** Corrupted solid terrain used by the Blight systems. */
        public static final TagKey<Block> CORRUPTED_TERRAIN = tag("corrupted_terrain");
        /** Loose gravity terrain (sands, gravels) converted into corrupted sediment; extensible by datapacks. */
        public static final TagKey<Block> CORRUPTIBLE_SEDIMENT = tag("corruptible_sediment");
        /** Weeds (grass, ferns, dead bushes) turned into corrupted grass; extensible by datapacks. */
        public static final TagKey<Block> CORRUPTIBLE_WEEDS = tag("corruptible_weeds");
        /**
         * Natural ground: the corruption of a column stops at the first ground block and looks through anything else
         * (trees, buildings, machines) above it.
         */
        public static final TagKey<Block> CORRUPTION_GROUND = tag("corruption_ground");

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, DimensionalFluxMiner.id(path));
        }

        private Blocks() {
        }
    }

    public static final class Items {
        public static TagKey<Item> common(String path) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
        }

        private Items() {
        }
    }

    public static final class DimensionTypes {
        /** Dimension types where Dimensional Death never acts: the Basic Core finds nothing to drain there. */
        public static final TagKey<DimensionType> CORRUPTION_BLOCKED = TagKey.create(Registries.DIMENSION_TYPE,
                DimensionalFluxMiner.id("corruption_blocked"));

        private DimensionTypes() {
        }
    }

    public static final class Entities {
        public static final TagKey<EntityType<?>> BLIGHT_LIGHT_BYPASS_ELIGIBLE = tag("blight_light_bypass_eligible");
        public static final TagKey<EntityType<?>> BLIGHT_SPAWN_BLACKLIST = tag("blight_spawn_blacklist");
        public static final TagKey<EntityType<?>> CORRUPTED_WATER_HEAL_BLACKLIST = tag("corrupted_water_heal_blacklist");
        /** Healed one heart at the player damage pace instead of the normal monster heal. */
        public static final TagKey<EntityType<?>> CORRUPTED_WATER_HEART_HEAL = tag("corrupted_water_heart_heal");

        private static TagKey<EntityType<?>> tag(String path) {
            return TagKey.create(Registries.ENTITY_TYPE, DimensionalFluxMiner.id(path));
        }

        private Entities() {
        }
    }
}
