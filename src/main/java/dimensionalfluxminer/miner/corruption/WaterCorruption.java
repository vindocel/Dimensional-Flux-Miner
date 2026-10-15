package dimensionalfluxminer.miner.corruption;

import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmFluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;

/**
 * Water under Dimensional Death: which water the corruption takes (water blocks, kelp, seagrass) and how it becomes
 * Corrupted Water. Used by a corrupted column's water layers and, later, by Corrupted Water sinking through the water
 * below it (each time checked again by {@link ProtectionService#waterMayCross}).
 */
public final class WaterCorruption {
    private WaterCorruption() {
    }
    /** Vanilla water (source or flowing). */
    static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER) && (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER));
    }

    /** The Corrupted Water copy of a vanilla water plant (kelp, seagrass), or null. */
    private static Block corruptedPlant(BlockState state) {
        if (state.is(Blocks.KELP)) {
            return DfmBlocks.CORRUPTED_KELP.get();
        }
        if (state.is(Blocks.KELP_PLANT)) {
            return DfmBlocks.CORRUPTED_KELP_PLANT.get();
        }
        if (state.is(Blocks.SEAGRASS)) {
            return DfmBlocks.CORRUPTED_SEAGRASS.get();
        }
        if (state.is(Blocks.TALL_SEAGRASS)) {
            return DfmBlocks.CORRUPTED_TALL_SEAGRASS.get();
        }
        return null;
    }

    /** Vanilla water the corruption turns into Corrupted Water: water blocks and the water plants. */
    public static boolean isCorruptibleWater(BlockState state) {
        return isWater(state) || corruptedPlant(state) != null;
    }

    /** Corrupted Water (source or flowing, or a plant holding it). */
    public static boolean isCorruptedWater(BlockState state) {
        return state.getFluidState().getType().isSame(DfmFluids.CORRUPTED_WATER.get());
    }

    private static boolean isKelp(BlockState state) {
        return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    /**
     * Turns the vanilla water at {@code pos} into Corrupted Water: a water block keeps its level; a kelp or seagrass
     * stays, swapped for its copy holding Corrupted Water (a whole kelp column or both halves of a tall seagrass at
     * once, so the plant never breaks). Also used by Corrupted Water to turn the water below it.
     * @return true when something was converted
     */
    public static boolean corruptWaterAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (isWater(state)) {
            level.setBlock(pos, DfmBlocks.CORRUPTED_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL)),
                    Block.UPDATE_ALL);
            return true;
        }
        if (corruptedPlant(state) == null) {
            return false;
        }
        List<BlockPos> cells = new ArrayList<>();
        if (isKelp(state)) {
            BlockPos bottom = pos;
            while (isKelp(level.getBlockState(bottom.below()))) {
                bottom = bottom.below();
            }
            for (BlockPos cell = bottom; isKelp(level.getBlockState(cell)); cell = cell.above()) {
                cells.add(cell);
            }
        } else if (state.is(Blocks.TALL_SEAGRASS)) {
            BlockPos lower = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            cells.add(lower);
            cells.add(lower.above());
        } else {
            cells.add(pos);
        }
        for (BlockPos cell : cells) {
            BlockState current = level.getBlockState(cell);
            Block target = corruptedPlant(current);
            if (target != null) {
                // No shape updates while swapping: the plant is never seen half vanilla, half corrupted.
                level.setBlock(cell, target.withPropertiesOf(current), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        // Keeps the corruption sinking below the plant.
        BlockPos bottom = cells.getFirst();
        level.scheduleTick(bottom, DfmFluids.CORRUPTED_WATER.get(), DfmFluids.CORRUPTED_WATER.get().getTickDelay(level));
        return true;
    }
}
