package dimensionalfluxminer.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Water around the mod's blocks: machines, tanks and Miner parts are waterloggable, so under water the water fills the
 * block's own space and surrounds the model (as around a slab or a chest) instead of stopping at the block's edge
 * like a wall. Shared by every such block.
 */
public final class Waterlogging {
    private Waterlogging() {
    }

    /** The placed state, waterlogged when it goes into water. */
    public static BlockState placed(BlockState state, BlockPlaceContext context) {
        if (state == null) {
            return null;
        }
        return state.setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    public static FluidState fluid(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    /** Lets the water held by the block flow on (call from {@code updateShape}). */
    public static void tick(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }
}
