package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import dimensionalfluxminer.machine.transfer.Transfer;

/**
 * Dimensional Tank block (single or dual). Right click with a filled bucket or fluid container empties it into a
 * chamber, with an empty one takes the liquid out (on the dual tank, the half of the block that was clicked: the right
 * side of the front, or the right face, is A; the left is B). The personal item makes the code personal, sneaking with
 * an empty hand makes it public again; anything else opens the screen.
 */
public class DimensionalTankBlock extends BaseEntityBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
    private final TankKind kind;

    public DimensionalTankBlock(TankKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return dimensionalfluxminer.content.Waterlogging.fluid(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, net.minecraft.util.RandomSource random) {
        dimensionalfluxminer.content.Waterlogging.tick(state, level, ticks, pos);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    public TankKind kind() {
        return kind;
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return dimensionalfluxminer.content.Waterlogging.placed(
                defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()), context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    /** Glowing liquids (lava) keep lighting the area from inside the tank. */
    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank ? tank.lightLevel() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalTankBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != DfmBlockEntities.DIMENSIONAL_TANK.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<DimensionalTankBlockEntity>) DimensionalTankBlockEntity::serverTick;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // The personal item (a diamond by default) on a public tank gives it a channel of its own for this player.
        if (stack.is(dimensionalfluxminer.DfmConfig.tankPersonalItem()) && !tank.code().personal()) {
            if (!level.isClientSide()) {
                tank.makePersonal(player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() || ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int chamber = chamberAt(state.getValue(BlockStateProperties.HORIZONTAL_FACING), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
        return FluidUtil.interactWithFluidHandler(player, hand, pos, Transfer.exposeFluids(tank.chamberHandler(chamber), tank.journal()), null)
                ? InteractionResult.CONSUME : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Chamber under a point of the block (0..1 coordinates): on the dual tank A is the half with the lower model X. */
    public int chamberAt(Direction facing, Vec3 local) {
        if (kind.chambers() < 2) {
            return 0;
        }
        // Model X of the point: the inverse of the facing rotation around the block center (native front north).
        double modelX = switch (facing) {
            case EAST -> local.z;
            case SOUTH -> 1 - local.x;
            case WEST -> 1 - local.z;
            default -> local.x;
        };
        return modelX < 0.5 ? 0 : 1;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank) {
            // Sneaking with an empty hand makes a personal tank public again (the personal item comes back).
            if (!player.isShiftKeyDown() || !tank.makePublic(player)) {
                serverPlayer.openMenu(tank, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

}
