package dimensionalfluxminer.miner.block;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.content.Waterlogging;
import dimensionalfluxminer.miner.MinerStructureEvents;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A structural block of the Dimensional Flux Miner. While the assembly is formed the controller draws the
 * whole machine once, so every participant hides its own render but keeps its collision.
 */
public class MinerPartBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<MinerPartBlock> CODEC = simpleCodec(MinerPartBlock::new);

    public MinerPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DfmStateProperties.FORMED, false).setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DfmStateProperties.FORMED, BlockStateProperties.WATERLOGGED);
    }

    /** Waterloggable: under water the formed machine (or a loose glass part) is surrounded by the water, not walled. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Waterlogging.placed(defaultBlockState(), context);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Waterlogging.fluid(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        Waterlogging.tick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(DfmStateProperties.FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** Loose parts of the same kind side by side hide the faces between them (seen through the glass), like vanilla glass. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) && !state.getValue(DfmStateProperties.FORMED) && !adjacent.getValue(DfmStateProperties.FORMED)
                || super.skipRendering(state, adjacent, direction);
    }

    /** A hidden participant must not cull neighbor faces, or the world behind the formed model shows through. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(DfmStateProperties.FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    /** Light passes through the formed assembly so the single formed model is lit like the surroundings. */
    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(DfmStateProperties.FORMED) ? 0 : super.getLightBlock(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(DfmStateProperties.FORMED) || super.propagatesSkylightDown(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(DfmStateProperties.FORMED) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(DfmStateProperties.FORMED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()) && level instanceof ServerLevel serverLevel) {
            // Only formation turns an existing part formed: a new block placed already formed is a copy (Building
            // Gadgets, structure tools), which would stay invisible without its machine. It comes back loose.
            if (state.getValue(DfmStateProperties.FORMED)) {
                level.setBlock(pos, state.setValue(DfmStateProperties.FORMED, false), Block.UPDATE_ALL);
            }
            MinerStructureEvents.onPartPlaced(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            MinerStructureEvents.onPartRemoved(serverLevel, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
