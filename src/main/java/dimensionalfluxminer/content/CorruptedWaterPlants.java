package dimensionalfluxminer.content;

import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.SeagrassBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;

/**
 * Kelp and seagrass in Corrupted Water: the vanilla plants hold vanilla water in their own block, so the corruption
 * swaps them for these copies, identical (same models, behaviour and drops) but holding Corrupted Water. They grow
 * into Corrupted Water and give the vanilla items.
 */
public final class CorruptedWaterPlants {
    private CorruptedWaterPlants() {
    }

    private static FluidState corruptedWater() {
        return DfmFluids.CORRUPTED_WATER.get().getSource(false);
    }

    private static boolean isCorruptedWaterSource(BlockState state) {
        return state.is(DfmBlocks.CORRUPTED_WATER.get()) && state.getFluidState().isSource();
    }

    public static final class Kelp extends KelpBlock {
        public Kelp(Properties properties) {
            super(properties);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return corruptedWater();
        }

        @Override
        protected Block getBodyBlock() {
            return DfmBlocks.CORRUPTED_KELP_PLANT.get();
        }

        @Override
        protected boolean canGrowInto(BlockState state) {
            return isCorruptedWaterSource(state);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return new ItemStack(Items.KELP);
        }
    }

    public static final class KelpPlant extends KelpPlantBlock {
        public KelpPlant(Properties properties) {
            super(properties);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return corruptedWater();
        }

        @Override
        protected GrowingPlantHeadBlock getHeadBlock() {
            return DfmBlocks.CORRUPTED_KELP.get();
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return new ItemStack(Items.KELP);
        }
    }

    public static final class Seagrass extends SeagrassBlock {
        public Seagrass(Properties properties) {
            super(properties);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return corruptedWater();
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
            return isCorruptedWaterSource(level.getBlockState(pos.above()));
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
            BlockState lower = DfmBlocks.CORRUPTED_TALL_SEAGRASS.get().defaultBlockState();
            BlockState upper = lower.setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER);
            BlockPos above = pos.above();
            if (isCorruptedWaterSource(level.getBlockState(above))) {
                level.setBlock(pos, lower, Block.UPDATE_CLIENTS);
                level.setBlock(above, upper, Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return new ItemStack(Items.SEAGRASS);
        }
    }

    public static final class TallSeagrass extends TallSeagrassBlock {
        public TallSeagrass(Properties properties) {
            super(properties);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return corruptedWater();
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return new ItemStack(Items.SEAGRASS);
        }
    }
}
