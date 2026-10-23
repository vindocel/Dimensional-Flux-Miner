package dimensionalfluxminer.content;

import dimensionalfluxminer.miner.corruption.ProtectionService;
import dimensionalfluxminer.miner.corruption.WaterCorruption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Corrupted Water: flows like water and also turns the water directly below it into Corrupted Water, one layer per
 * fluid tick (and on random ticks), so a corrupted surface sinks through the whole water column. It never spreads the
 * corruption sideways; only the Dimensional Drain does.
 * <p>
 * These later ticks happen without the Miner that started them, so each one is checked again where it acts: the water
 * never flows or corrupts across a claim border, into a protected region, or where the corruption is off
 * ({@link ProtectionService#waterMayCross}).
 */
public final class CorruptedWaterFluid {
    private CorruptedWaterFluid() {
    }

    static void corruptBelow(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && ProtectionService.waterMayCross(server, pos, pos.below())) {
            WaterCorruption.corruptWaterAt(level, pos.below());
        }
    }

    /** Flowing into {@code pos} from the neighbour on the other side of {@code direction}, only inside the same claim. */
    static boolean mayFlow(LevelAccessor level, BlockPos pos, Direction direction) {
        return !(level instanceof ServerLevel server) || ProtectionService.waterMayCross(server, pos.relative(direction.getOpposite()), pos);
    }

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState state) {
            super.tick(level, pos, blockState, state);
            corruptBelow(level, pos);
        }

        @Override
        protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
            if (mayFlow(level, pos, direction)) {
                super.spreadTo(level, pos, blockState, direction, fluidState);
            }
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
            corruptBelow(level, pos);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState state) {
            super.tick(level, pos, blockState, state);
            corruptBelow(level, pos);
        }

        @Override
        protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
            if (mayFlow(level, pos, direction)) {
                super.spreadTo(level, pos, blockState, direction, fluidState);
            }
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
            corruptBelow(level, pos);
        }
    }
}
