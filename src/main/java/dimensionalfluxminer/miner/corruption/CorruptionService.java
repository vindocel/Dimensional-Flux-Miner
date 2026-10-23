package dimensionalfluxminer.miner.corruption;

import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmTags;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Dimensional Drain: plans and applies the corruption of one X/Z column following its own topography.
 * <ul>
 *     <li>cover over the ground (trees, buildings, machines) is looked through, so covered ground corrupts too;</li>
 *     <li>decorative vegetation above the terrain is handled separately and never counts toward depth;</li>
 *     <li>up to 3 contiguous water layers from the exposed surface, never following deep water further;</li>
 *     <li>then exactly 3 full terrain blocks;</li>
 *     <li>blacklisted blocks and block entities are never converted.</li>
 * </ul>
 * One successfully corrupted column is one logical corruption unit = 64 DM, whatever the number of blocks.
 */
public final class CorruptionService {
    public static final int SOLID_DEPTH = 3;
    public static final int WATER_DEPTH = 3;
    /** Cover blocks (trees, buildings, machines) the corruption looks through to reach the ground. */
    public static final int MAX_COVER = 32;
    private static final int REMOVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private CorruptionService() {
    }

    /** {@code to} is the terrain block to place, or null for a water layer ({@link WaterCorruption#corruptWaterAt}). */
    public record Conversion(BlockPos pos, BlockState to) {
    }

    /** Vegetation action: replace {@code base} (and remove {@code upper} if present). */
    public record Vegetation(BlockPos base, BlockPos upper, BlockState to) {
    }

    public record ColumnPlan(List<Conversion> conversions, List<Vegetation> vegetation) {
        public boolean drainable() {
            return !conversions.isEmpty();
        }
    }

    /** Plans a column without touching the world. The chunk must be loaded. */
    public static ColumnPlan plan(ServerLevel level, int x, int z) {
        List<Conversion> conversions = new ArrayList<>();
        List<Vegetation> vegetation = new ArrayList<>();
        int min = level.getMinY();
        List<BlockPos> decorative = new ArrayList<>();
        BlockPos ground = findGround(level, x, z, decorative);
        if (ground == null) {
            return new ColumnPlan(List.of(), List.of());
        }
        BlockPos.MutableBlockPos pos = ground.mutable();
        int waterLayers = 0;
        while (pos.getY() >= min && waterLayers < WATER_DEPTH) {
            if (!WaterCorruption.isCorruptibleWater(level.getBlockState(pos))) {
                break;
            }
            conversions.add(new Conversion(pos.immutable(), null));
            waterLayers++;
            pos.move(0, -1, 0);
        }
        // Deeper water is never followed here: Corrupted Water itself turns the water below it, layer by layer.
        BlockPos surface = pos.immutable();
        for (int layer = 0; layer < SOLID_DEPTH && pos.getY() >= min; layer++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isCollisionShapeFullBlock(level, pos)) {
                break;
            }
            BlockState target = corruptedTerrain(level, pos, state);
            if (target != null) {
                conversions.add(new Conversion(pos.immutable(), target));
            }
            pos.move(0, -1, 0);
        }
        if (!conversions.isEmpty() && waterLayers == 0) {
            planVegetation(level, decorative, surface, vegetation);
        }
        return new ColumnPlan(List.copyOf(conversions), List.copyOf(vegetation));
    }

    /**
     * Same answer as {@code plan(level, x, z).drainable()} without building the plan: the survey asks it for every
     * column it inspects.
     */
    public static boolean drainable(ServerLevel level, int x, int z) {
        BlockPos ground = findGround(level, x, z, null);
        if (ground == null) {
            return false;
        }
        BlockState state = level.getBlockState(ground);
        if (WaterCorruption.isCorruptibleWater(state)) {
            return true;
        }
        int min = level.getMinY();
        BlockPos.MutableBlockPos pos = ground.mutable();
        for (int layer = 0; layer < SOLID_DEPTH && pos.getY() >= min; layer++) {
            state = level.getBlockState(pos);
            if (!state.isCollisionShapeFullBlock(level, pos)) {
                return false;
            }
            if (corruptedTerrain(level, pos, state) != null) {
                return true;
            }
            pos.move(0, -1, 0);
        }
        return false;
    }

    /**
     * The column's ground (or first water layer), found from the top through air, decorative vegetation and cover:
     * trees, buildings, machines or anything else that is not natural ground ({@code corruption_ground}). Null when
     * more than {@link #MAX_COVER} cover blocks stand above it.
     */
    public static BlockPos ground(ServerLevel level, int x, int z) {
        return findGround(level, x, z, new ArrayList<>());
    }

    private static BlockPos findGround(ServerLevel level, int x, int z, List<BlockPos> decorative) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int min = level.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        int cover = 0;
        while (pos.getY() >= min) {
            BlockState state = level.getBlockState(pos);
            if (WaterCorruption.isCorruptibleWater(state) || WaterCorruption.isCorruptedWater(state) || state.is(DfmTags.Blocks.CORRUPTION_GROUND)) {
                return pos.immutable();
            }
            if (!state.isAir()) {
                if (isDecorative(level, pos, state)) {
                    if (decorative != null) {
                        decorative.add(pos.immutable());
                    }
                } else if (++cover > MAX_COVER) {
                    return null;
                }
            }
            pos.move(0, -1, 0);
        }
        return null;
    }

    private static void planVegetation(ServerLevel level, List<BlockPos> decorative, BlockPos surface, List<Vegetation> out) {
        BlockPos above = surface.above();
        BlockState state = level.getBlockState(above);
        if (!decorative.contains(above)) {
            return;
        }
        boolean doublePlant = state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
        BlockPos upper = doublePlant ? above.above() : null;
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_WEEDS)) {
            // Weeds (two-cell ones atomically) become one corrupted grass plant at their base.
            out.add(new Vegetation(above, upper, DfmBlocks.CORRUPTED_GRASS_PLANT.get().defaultBlockState()));
        } else if (state.is(DfmTags.Blocks.CORRUPTIBLE_FLOWERS) && !state.is(Blocks.WITHER_ROSE)) {
            out.add(new Vegetation(above, upper, Blocks.WITHER_ROSE.defaultBlockState()));
        }
    }

    /**
     * Revalidates a column right before corrupting it and applies the plan.
     * @return true when the column was corrupted (worth 64 DM)
     */
    public static boolean corrupt(ServerLevel level, int x, int z, UUID owner) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return false;
        }
        if (!ProtectionService.corruptionAllowed(level)) {
            return false;
        }
        ColumnPlan plan = plan(level, x, z);
        if (!plan.drainable()) {
            return false;
        }
        // The whole column is authorized or denied before anything changes: terrain, water, the plants on it and both
        // halves of tall plants. A denied column changes nothing and is worth nothing.
        if (!ProtectionService.allowsAll(level, positions(plan), owner)) {
            return false;
        }
        for (Vegetation v : plan.vegetation()) {
            if (v.upper() != null) {
                level.setBlock(v.upper(), Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
            }
            level.setBlock(v.base(), Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
        }
        for (Conversion c : plan.conversions()) {
            if (c.to() == null) {
                WaterCorruption.corruptWaterAt(level, c.pos());
            } else {
                level.setBlock(c.pos(), c.to(), Block.UPDATE_ALL);
            }
        }
        for (Vegetation v : plan.vegetation()) {
            if (v.to().canSurvive(level, v.base())) {
                level.setBlock(v.base(), v.to(), Block.UPDATE_ALL);
            }
        }
        BlightData.get(level).markCorrupted(x, z);
        return true;
    }

    /** Every position a plan changes. */
    public static List<BlockPos> positions(ColumnPlan plan) {
        List<BlockPos> out = new ArrayList<>();
        for (Conversion c : plan.conversions()) {
            out.add(c.pos());
        }
        for (Vegetation v : plan.vegetation()) {
            out.add(v.base());
            if (v.upper() != null) {
                out.add(v.upper());
            }
        }
        return out;
    }

    private static boolean isDecorative(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Data-driven terrain mapping: each {@code corruptible_*} tag names the corrupted block that replaces its members
     * (first match in this order: sediment, dirt, grass, stone, terracotta). Blacklisted blocks, block entities and
     * corrupted terrain are never converted.
     */
    static BlockState corruptedTerrain(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(DfmTags.Blocks.CORRUPTION_BLACKLIST) || state.hasBlockEntity() || state.is(DfmTags.Blocks.CORRUPTED_TERRAIN)) {
            return null;
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_SEDIMENT)) {
            return DfmBlocks.CORRUPTED_SEDIMENT.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_DIRT)) {
            return DfmBlocks.CORRUPTED_DIRT.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_GRASS)) {
            return DfmBlocks.CORRUPTED_GRASS.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_STONE)) {
            return DfmBlocks.CORRUPTED_STONE.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_TERRACOTTA)) {
            return DfmBlocks.CORRUPTED_TERRACOTTA.get().defaultBlockState();
        }
        return null;
    }
}
