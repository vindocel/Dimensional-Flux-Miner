package dimensionalfluxminer.miner.corruption;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
//? if >=26.1 {
import dimensionalfluxminer.registry.DfmTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
//?}

/**
 * Pale corruption of trees (Minecraft 26.1.2 and 26.3 only; on 1.21.1 trees are left as they are).
 * <ul>
 *     <li>a tree is a group of connected logs ({@code corruptible_*logs}/{@code corruptible_*wood}) with at least one
 *     natural leaf ({@code persistent=false}) touching it, so logs and leaves placed by players never count;</li>
 *     <li>when a corrupted column passes through any log or natural leaf of a tree, the whole tree turns into pale
 *     oak: logs, wood and their stripped forms keep their axis, leaves keep distance and waterlogging;</li>
 *     <li>{@link #HEART_CHANCE} of the converted trees (3%) get a natural Creaking Heart in a vertical trunk log;</li>
 *     <li>a tree with vines ({@code corruptible_vines}) loses them and gets pale hanging moss under some of its leaves
 *     instead (moss only hangs down, it cannot cling to sides like vines).</li>
 * </ul>
 * The tree gives no extra DM and every position is checked by the column's claim check before anything changes.
 */
public final class PaleTreeCorruption {
    /** More connected logs than this is a building, not a tree. */
    public static final int MAX_LOGS = 1024;
    /** Leaves converted per tree at most. */
    public static final int MAX_LEAVES = 6144;
    /** Natural leaves stay within this many steps of a log (leaves decay at distance 7). */
    public static final int LEAF_REACH = 6;
    /** Chance of a converted tree getting a Creaking Heart (3%). */
    public static final float HEART_CHANCE = 0.03F;
    /** Vines removed per tree at most. */
    public static final int MAX_VINES = 2048;
    /** On a tree that had vines, one leaf with air below in this many gets a pale hanging moss strand. */
    public static final int MOSS_CHANCE = 4;
    /** Pale hanging moss strands are 1 to this many blocks long. */
    public static final int MOSS_MAX_LENGTH = 3;
    /** Trees tried per column. */
    private static final int MAX_SEEDS = 4;

    private PaleTreeCorruption() {
    }

    /**
     * Plans the pale conversion of the first natural tree met among a column's cover blocks (top to bottom). Reads
     * only loaded positions and changes nothing. Empty when there is no tree, the tree is already pale, or on 1.21.1.
     */
    public static List<CorruptionService.Conversion> plan(ServerLevel level, List<BlockPos> cover) {
        //? if >=26.1 {
        return planTree(level, cover, level.getRandom());
        //?} else {
        /*return List.of();
        *///?}
    }

    //? if >=26.1 {
    // Same as plan(level, cover), with the random source of the Creaking Heart roll (GameTests).
    public static List<CorruptionService.Conversion> planTree(ServerLevel level, List<BlockPos> cover, RandomSource random) {
        Set<BlockPos> tried = new HashSet<>();
        int seeds = 0;
        for (BlockPos seed : cover) {
            if (tried.contains(seed) || !level.isLoaded(seed)) {
                continue;
            }
            BlockState state = level.getBlockState(seed);
            BlockPos root;
            if (isTreeLog(state) && !state.is(BlockTags.PALE_OAK_LOGS)) {
                root = seed;
            } else if (isNaturalLeaf(state) && !state.is(Blocks.PALE_OAK_LEAVES)) {
                root = nearestLog(level, seed);
            } else {
                continue;
            }
            if (++seeds > MAX_SEEDS) {
                break;
            }
            if (root == null) {
                continue;
            }
            Tree tree = collect(level, root);
            if (tree == null) {
                tried.add(root);
                continue;
            }
            tried.addAll(tree.logs());
            tried.addAll(tree.leaves());
            List<CorruptionService.Conversion> out = convert(level, tree, random);
            if (!out.isEmpty()) {
                return out;
            }
        }
        return List.of();
    }

    private record Tree(Set<BlockPos> logs, Set<BlockPos> leaves, Set<BlockPos> vines) {
    }

    // Logs (26 neighbours, so diagonal branches of acacia and dark oak count) and the natural leaves reached from them
    // within LEAF_REACH steps. Null when the logs are too many or no natural leaf touches them.
    private static Tree collect(ServerLevel level, BlockPos root) {
        Set<BlockPos> logs = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        logs.add(root);
        queue.add(root);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (logs.contains(next) || !level.isLoaded(next) || !isTreeLog(level.getBlockState(next))) {
                            continue;
                        }
                        if (logs.size() >= MAX_LOGS) {
                            return null;
                        }
                        logs.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        Set<BlockPos> leaves = new LinkedHashSet<>();
        Map<BlockPos, Integer> depth = new HashMap<>();
        for (BlockPos log : logs) {
            depth.put(log, 0);
            queue.add(log);
        }
        boolean touching = false;
        while (!queue.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos pos = queue.poll();
            int d = depth.get(pos);
            if (d >= LEAF_REACH) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (depth.containsKey(next) || !level.isLoaded(next) || !isNaturalLeaf(level.getBlockState(next))) {
                    continue;
                }
                depth.put(next, d + 1);
                leaves.add(next);
                queue.add(next);
                touching |= d == 0;
                if (leaves.size() >= MAX_LEAVES) {
                    break;
                }
            }
        }
        if (!touching) {
            return null;
        }
        // Vines clinging to the tree, and the vines hanging from those.
        Set<BlockPos> vines = new LinkedHashSet<>();
        queue.clear();
        List<Set<BlockPos>> parts = List.of(logs, leaves);
        for (Set<BlockPos> part : parts) {
            for (BlockPos pos : part) {
                queue.add(pos);
            }
        }
        while (!queue.isEmpty() && vines.size() < MAX_VINES) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (vines.contains(next) || logs.contains(next) || leaves.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (!state.is(DfmTags.Blocks.CORRUPTIBLE_VINES) || state.is(DfmTags.Blocks.CORRUPTION_BLACKLIST)) {
                    continue;
                }
                vines.add(next);
                queue.add(next);
                if (vines.size() >= MAX_VINES) {
                    break;
                }
            }
        }
        return new Tree(logs, leaves, vines);
    }

    // The first tree log reached through natural leaves from a leaf, or null.
    private static BlockPos nearestLog(ServerLevel level, BlockPos leaf) {
        Map<BlockPos, Integer> depth = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        depth.put(leaf, 0);
        queue.add(leaf);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int d = depth.get(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (depth.containsKey(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (isTreeLog(state)) {
                    return next;
                }
                if (d + 1 < LEAF_REACH && isNaturalLeaf(state)) {
                    depth.put(next, d + 1);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    private static List<CorruptionService.Conversion> convert(ServerLevel level, Tree tree, RandomSource random) {
        List<CorruptionService.Conversion> out = new ArrayList<>();
        Map<BlockPos, Integer> logIndex = new HashMap<>();
        boolean logConverted = false;
        for (BlockPos pos : tree.logs()) {
            BlockState state = level.getBlockState(pos);
            BlockState target = paleLog(state);
            if (target != null) {
                if (state.hasProperty(BlockStateProperties.AXIS)) {
                    target = target.setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
                }
                logIndex.put(pos, out.size());
                out.add(new CorruptionService.Conversion(pos, target));
                logConverted = true;
            }
        }
        for (BlockPos pos : tree.leaves()) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.PALE_OAK_LEAVES)) {
                continue;
            }
            BlockState target = Blocks.PALE_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
            if (state.hasProperty(LeavesBlock.DISTANCE)) {
                target = target.setValue(LeavesBlock.DISTANCE, state.getValue(LeavesBlock.DISTANCE));
            }
            if (state.hasProperty(LeavesBlock.WATERLOGGED)) {
                target = target.setValue(LeavesBlock.WATERLOGGED, state.getValue(LeavesBlock.WATERLOGGED));
            }
            out.add(new CorruptionService.Conversion(pos, target));
        }
        if (!tree.vines().isEmpty()) {
            for (BlockPos pos : tree.vines()) {
                out.add(new CorruptionService.Conversion(pos, Blocks.AIR.defaultBlockState()));
            }
            addMoss(level, tree, random, out);
        }
        if (logConverted && random.nextFloat() < HEART_CHANCE) {
            BlockPos heart = heartPosition(level, tree, logIndex, random);
            if (heart != null) {
                out.set(logIndex.get(heart), new CorruptionService.Conversion(heart, Blocks.CREAKING_HEART.defaultBlockState()
                        .setValue(CreakingHeartBlock.AXIS, Direction.Axis.Y)
                        .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
                        .setValue(CreakingHeartBlock.NATURAL, true)));
            }
        }
        return out;
    }

    // Pale hanging moss under random leaves of a tree that had vines: strands of 1 to MOSS_MAX_LENGTH blocks into air or
    // into the vines being removed, the last block being the tip. Applied after the vines are removed.
    private static void addMoss(ServerLevel level, Tree tree, RandomSource random, List<CorruptionService.Conversion> out) {
        Set<BlockPos> taken = new HashSet<>();
        for (BlockPos leaf : tree.leaves()) {
            if (random.nextInt(MOSS_CHANCE) != 0) {
                continue;
            }
            int length = 1 + random.nextInt(MOSS_MAX_LENGTH);
            List<BlockPos> strand = new ArrayList<>();
            BlockPos pos = leaf.below();
            while (strand.size() < length && free(level, tree, taken, pos)) {
                strand.add(pos);
                pos = pos.below();
            }
            for (int i = 0; i < strand.size(); i++) {
                taken.add(strand.get(i));
                out.add(new CorruptionService.Conversion(strand.get(i),
                        Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, i == strand.size() - 1)));
            }
        }
    }

    private static boolean free(ServerLevel level, Tree tree, Set<BlockPos> taken, BlockPos pos) {
        if (taken.contains(pos) || tree.logs().contains(pos) || tree.leaves().contains(pos) || !level.isLoaded(pos)) {
            return false;
        }
        return tree.vines().contains(pos) || level.getBlockState(pos).isAir();
    }

    // A converted vertical log with vertical tree logs above and below (what the heart needs to work). Logs enclosed by
    // tree logs on all six sides are preferred, like the pale garden's own hearts.
    private static BlockPos heartPosition(ServerLevel level, Tree tree, Map<BlockPos, Integer> logIndex, RandomSource random) {
        List<BlockPos> enclosed = new ArrayList<>();
        List<BlockPos> trunk = new ArrayList<>();
        for (BlockPos pos : logIndex.keySet()) {
            if (!vertical(level, pos) || !tree.logs().contains(pos.above()) || !tree.logs().contains(pos.below())
                    || !vertical(level, pos.above()) || !vertical(level, pos.below())) {
                continue;
            }
            trunk.add(pos);
            boolean all = true;
            for (Direction direction : Direction.values()) {
                all &= tree.logs().contains(pos.relative(direction));
            }
            if (all) {
                enclosed.add(pos);
            }
        }
        List<BlockPos> pool = enclosed.isEmpty() ? trunk : enclosed;
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    private static boolean vertical(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.hasProperty(BlockStateProperties.AXIS) || state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }

    // Data-driven: each tag names its pale destination (first match: stripped wood, stripped logs, wood, logs).
    static BlockState paleLog(BlockState state) {
        if (state.is(BlockTags.PALE_OAK_LOGS)) {
            return null;
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_WOOD)) {
            return Blocks.STRIPPED_PALE_OAK_WOOD.defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_LOGS)) {
            return Blocks.STRIPPED_PALE_OAK_LOG.defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_WOOD)) {
            return Blocks.PALE_OAK_WOOD.defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_LOGS)) {
            return Blocks.PALE_OAK_LOG.defaultBlockState();
        }
        return null;
    }

    static boolean isTreeLog(BlockState state) {
        if (state.is(DfmTags.Blocks.CORRUPTION_BLACKLIST) || state.hasBlockEntity()) {
            return false;
        }
        return state.is(BlockTags.PALE_OAK_LOGS) || state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_WOOD)
                || state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_LOGS) || state.is(DfmTags.Blocks.CORRUPTIBLE_WOOD)
                || state.is(DfmTags.Blocks.CORRUPTIBLE_LOGS);
    }

    // A leaf grown by the world or a sapling: persistent=false. Leaves placed by hand are persistent and never count.
    static boolean isNaturalLeaf(BlockState state) {
        return (state.is(DfmTags.Blocks.CORRUPTIBLE_LEAVES) || state.is(Blocks.PALE_OAK_LEAVES))
                && !state.is(DfmTags.Blocks.CORRUPTION_BLACKLIST)
                && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }
    //?}
}
