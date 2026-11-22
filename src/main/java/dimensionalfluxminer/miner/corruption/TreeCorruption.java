package dimensionalfluxminer.miner.corruption;

import dimensionalfluxminer.registry.DfmTags;
import dimensionalfluxminer.registry.DfmWood;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Corruption of trees into corrupted wood (all versions).
 * <ul>
 *     <li>a tree is a group of connected logs ({@code corruptible_*logs}/{@code corruptible_*wood}) with at least one
 *     natural leaf ({@code persistent=false}) touching it, so logs and leaves placed by players never count;</li>
 *     <li>when a corrupted column passes through any log or natural leaf of a tree, the whole tree turns into corrupted
 *     wood: logs, wood and their stripped forms keep their axis, leaves keep distance and waterlogging;</li>
 *     <li>the vines of the tree ({@code corruptible_vines}: the ones clinging to its logs and leaves and the ones
 *     hanging from those) become corrupted vines on the same faces.</li>
 * </ul>
 * The tree gives no extra DM and every position is checked by the column's claim check before anything changes.
 */
public final class TreeCorruption {
    /** More connected logs than this is a building, not a tree. */
    public static final int MAX_LOGS = 1024;
    /** Leaves converted per tree at most. */
    public static final int MAX_LEAVES = 6144;
    /** Natural leaves stay within this many steps of a log (leaves decay at distance 7). */
    public static final int LEAF_REACH = 6;
    /** Vines converted per tree at most. */
    public static final int MAX_VINES = 2048;
    /** Trees tried per column. */
    private static final int MAX_SEEDS = 4;

    private TreeCorruption() {
    }

    /**
     * Plans the conversion of the first natural tree met among a column's cover blocks (top to bottom). Reads only
     * loaded positions and changes nothing. Empty when there is no tree or the tree is already corrupted.
     */
    public static List<CorruptionService.Conversion> plan(ServerLevel level, List<BlockPos> cover) {
        Set<BlockPos> tried = new HashSet<>();
        int seeds = 0;
        for (BlockPos seed : cover) {
            if (tried.contains(seed) || !level.isLoaded(seed)) {
                continue;
            }
            BlockState state = level.getBlockState(seed);
            BlockPos root;
            if (isTreeLog(state) && !isCorruptedLog(state)) {
                root = seed;
            } else if (isNaturalLeaf(state) && !state.is(DfmWood.LEAVES.get())) {
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
            List<CorruptionService.Conversion> out = convert(level, tree);
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
        queue.addAll(logs);
        queue.addAll(leaves);
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

    private static List<CorruptionService.Conversion> convert(ServerLevel level, Tree tree) {
        List<CorruptionService.Conversion> out = new ArrayList<>();
        for (BlockPos pos : tree.logs()) {
            BlockState state = level.getBlockState(pos);
            BlockState target = corruptedLog(state);
            if (target != null) {
                if (state.hasProperty(BlockStateProperties.AXIS)) {
                    target = target.setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
                }
                out.add(new CorruptionService.Conversion(pos, target));
            }
        }
        for (BlockPos pos : tree.leaves()) {
            BlockState state = level.getBlockState(pos);
            if (state.is(DfmWood.LEAVES.get())) {
                continue;
            }
            BlockState target = DfmWood.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
            if (state.hasProperty(LeavesBlock.DISTANCE)) {
                target = target.setValue(LeavesBlock.DISTANCE, state.getValue(LeavesBlock.DISTANCE));
            }
            if (state.hasProperty(LeavesBlock.WATERLOGGED)) {
                target = target.setValue(LeavesBlock.WATERLOGGED, state.getValue(LeavesBlock.WATERLOGGED));
            }
            out.add(new CorruptionService.Conversion(pos, target));
        }
        // Each vine becomes a corrupted vine clinging to the same faces.
        for (BlockPos pos : tree.vines()) {
            out.add(new CorruptionService.Conversion(pos, copyProperties(level.getBlockState(pos), DfmWood.VINE.get().defaultBlockState())));
        }
        return out;
    }

    // The properties the two states share (vine faces) copied from one to the other.
    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (to.hasProperty(property)) {
                to = copy(from, to, property);
            }
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    /** Logs, wood and their stripped forms that are already corrupted wood. */
    static boolean isCorruptedLog(BlockState state) {
        return state.is(DfmTags.Blocks.CORRUPTED_LOGS);
    }

    // Data-driven: each tag names its corrupted destination (first match: stripped wood, stripped logs, wood, logs).
    static BlockState corruptedLog(BlockState state) {
        if (isCorruptedLog(state)) {
            return null;
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_WOOD)) {
            return DfmWood.STRIPPED_WOOD.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_LOGS)) {
            return DfmWood.STRIPPED_LOG.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_WOOD)) {
            return DfmWood.WOOD.get().defaultBlockState();
        }
        if (state.is(DfmTags.Blocks.CORRUPTIBLE_LOGS)) {
            return DfmWood.LOG.get().defaultBlockState();
        }
        return null;
    }

    static boolean isTreeLog(BlockState state) {
        if (state.is(DfmTags.Blocks.CORRUPTION_BLACKLIST) || state.hasBlockEntity()) {
            return false;
        }
        return isCorruptedLog(state) || state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_WOOD)
                || state.is(DfmTags.Blocks.CORRUPTIBLE_STRIPPED_LOGS) || state.is(DfmTags.Blocks.CORRUPTIBLE_WOOD)
                || state.is(DfmTags.Blocks.CORRUPTIBLE_LOGS);
    }

    // A leaf grown by the world or a sapling: persistent=false. Leaves placed by hand are persistent and never count.
    static boolean isNaturalLeaf(BlockState state) {
        return (state.is(DfmTags.Blocks.CORRUPTIBLE_LEAVES) || state.is(DfmWood.LEAVES.get()))
                && !state.is(DfmTags.Blocks.CORRUPTION_BLACKLIST)
                && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }
}
