package dimensionalfluxminer.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
//? if >=26.1 {
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}
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
    //? if >=26.1 {
    public static void tick(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos) {
    //?} else {
    /*public static void tick(BlockState state, LevelAccessor level, BlockPos pos) {
    *///?}
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            //? if >=26.1 {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            //?} else {
            /*level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            *///?}
        }
    }
}
