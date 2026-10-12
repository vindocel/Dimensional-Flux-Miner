package dimensionalfluxminer.content;

import dimensionalfluxminer.miner.corruption.CorruptionService;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Corrupted Water: flows like water and also turns the water directly below it into Corrupted Water, one layer per
 * fluid tick (and on random ticks), so a corrupted surface sinks through the whole water column. It never spreads the
 * corruption sideways; only the Dimensional Drain does.
 */
public final class CorruptedWaterFluid {
    private CorruptedWaterFluid() {
    }

    static void corruptBelow(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            CorruptionService.corruptWaterAt(level, pos.below());
        }
    }

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            corruptBelow(level, pos);
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            corruptBelow(level, pos);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(Level level, BlockPos pos, FluidState state) {
            super.tick(level, pos, state);
            corruptBelow(level, pos);
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
            corruptBelow(level, pos);
        }
    }
}
