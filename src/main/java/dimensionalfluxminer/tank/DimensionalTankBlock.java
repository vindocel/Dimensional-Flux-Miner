package dimensionalfluxminer.tank;

//? if <26.3 {
import com.mojang.serialization.MapCodec;
//?}
import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <26.1 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
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
//? if >=26.1 {
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import dimensionalfluxminer.machine.transfer.Transfer;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidUtil;
*///?}

/**
 * Dimensional Tank block (single or dual). Right click with a filled bucket or fluid container empties it into a
 * chamber, with an empty one takes the liquid out (on the dual tank, the half of the block that was clicked: the right
 * side of the front, or the right face, is A; the left is B). The personal item makes the code personal, sneaking with
 * an empty hand makes it public again; anything else opens the screen. The creative tank has no code: sneaking with an
 * empty hand forgets its liquid.
 */
public class DimensionalTankBlock extends BaseEntityBlock implements net.minecraft.world.level.block.SimpleWaterloggedBlock {
    //? if =26.1.2 {
    private static final MapCodec<DimensionalTankBlock> SINGLE_CODEC = simpleCodec(p -> new DimensionalTankBlock(TankKind.SINGLE, p));
    private static final MapCodec<DimensionalTankBlock> DUAL_CODEC = simpleCodec(p -> new DimensionalTankBlock(TankKind.DUAL, p));
    private static final MapCodec<DimensionalTankBlock> CREATIVE_CODEC = simpleCodec(p -> new DimensionalTankBlock(TankKind.CREATIVE, p));

    //?}
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
    //? if =26.1.2 {
    protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, net.minecraft.util.RandomSource random) {
        dimensionalfluxminer.content.Waterlogging.tick(state, level, ticks, pos);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return switch (kind) {
            case DUAL -> DUAL_CODEC;
            case CREATIVE -> CREATIVE_CODEC;
            default -> SINGLE_CODEC;
        };
    //?} else if >=26.3 {
    /*protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, net.minecraft.util.RandomSource random) {
        dimensionalfluxminer.content.Waterlogging.tick(state, level, ticks, pos);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    *///?} else {
    /*protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        dimensionalfluxminer.content.Waterlogging.tick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    public TankKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return switch (kind) {
            case DUAL -> DUAL_CODEC;
            case CREATIVE -> CREATIVE_CODEC;
            default -> SINGLE_CODEC;
        };
    *///?}
    }

    //? if >=26.1 {
    public TankKind kind() {
        return kind;
    }

    //?} else {
    /*private static final MapCodec<DimensionalTankBlock> SINGLE_CODEC = simpleCodec(p -> new DimensionalTankBlock(TankKind.SINGLE, p));
    private static final MapCodec<DimensionalTankBlock> DUAL_CODEC = simpleCodec(p -> new DimensionalTankBlock(TankKind.DUAL, p));
    private static final MapCodec<DimensionalTankBlock> CREATIVE_CODEC = simpleCodec(p -> new DimensionalTankBlock(TankKind.CREATIVE, p));
    *///?}

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
        //? if >=26.1 {
        if (level.isClientSide() || type != DfmBlockEntities.DIMENSIONAL_TANK.get()) {
        //?} else {
        /*if (level.isClientSide || type != DfmBlockEntities.DIMENSIONAL_TANK.get()) {
        *///?}
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<DimensionalTankBlockEntity>) DimensionalTankBlockEntity::serverTick;
    }

    @Override
    //? if >=26.1 {
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
    //?} else {
    /*protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
    *///?}
                                              BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank)) {
            //? if >=26.1 {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
            //?} else {
            /*return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            *///?}
        }
        // The personal item (a diamond by default) on a public tank gives it a channel of its own for this player.
        if (!kind.creative() && stack.is(dimensionalfluxminer.DfmConfig.tankPersonalItem()) && !tank.code().personal()) {
            //? if >=26.1 {
            if (!level.isClientSide()) {
            //?} else {
            /*if (!level.isClientSide) {
            *///?}
                tank.makePersonal(player, stack);
            }
            //? if >=26.1 {
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() || ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
            //?} else {
            /*return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (FluidUtil.getFluidHandler(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
            *///?}
        }
        int chamber = chamberAt(state.getValue(BlockStateProperties.HORIZONTAL_FACING), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
        if (stack.getItem() instanceof DimensionalTankItem item) {
            //? if >=26.1 {
            return tankItemOn(stack, item, tank, chamber, player, hand);
        }
        return FluidUtil.interactWithFluidHandler(player, hand, pos, Transfer.exposeFluids(tank.chamberHandler(chamber), tank.journal()), null)
                ? InteractionResult.CONSUME : InteractionResult.TRY_WITH_EMPTY_HAND;
            //?} else {
            /*return tankItemOn(stack, item, tank, chamber);
        }
        return FluidUtil.interactWithFluidHandler(player, hand, tank.chamberHandler(chamber))
                ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            *///?}
    }

    /**
     * A single or creative tank item on a tank (any mode): it empties into the chamber as much as fits; only when nothing goes in does
     * it fill from the chamber. A single tank on a tank of its own channel does nothing.
     */
    //? if >=26.1 {
    private InteractionResult tankItemOn(ItemStack stack, DimensionalTankItem item, DimensionalTankBlockEntity tank, int chamber, Player player,
                                         InteractionHand hand) {
        var itemHandler = DimensionalTankItem.fluidHandler(ItemAccess.forPlayerInteraction(player, hand));
    //?} else {
    /*private ItemInteractionResult tankItemOn(ItemStack stack, DimensionalTankItem item, DimensionalTankBlockEntity tank, int chamber) {
        net.neoforged.neoforge.fluids.capability.IFluidHandlerItem itemHandler = DimensionalTankItem.fluidHandler(stack);
    *///?}
        if (itemHandler == null) {
            //? if >=26.1 {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
            //?} else {
            /*return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            *///?}
        }
        if (item.kind() == kind && !kind.creative() && !tank.channelLocked()
                && !DimensionalTankItem.channelLocked(stack)
                && stack.getOrDefault(dimensionalfluxminer.registry.DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT).sameChannel(tank.code())) {
            //? if >=26.1 {
            return InteractionResult.CONSUME;
        }
        var chamberHandler = Transfer.exposeFluids(tank.chamberHandler(chamber), tank.journal());
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            if (net.neoforged.neoforge.transfer.ResourceHandlerUtil.moveFirst(itemHandler, chamberHandler, resource -> true, Integer.MAX_VALUE,
                    transaction) == null) {
                net.neoforged.neoforge.transfer.ResourceHandlerUtil.moveFirst(chamberHandler, itemHandler, resource -> true, Integer.MAX_VALUE,
                        transaction);
            }
            transaction.commit();
        }
        return InteractionResult.CONSUME;
            //?} else {
            /*return ItemInteractionResult.CONSUME;
        }
        net.neoforged.neoforge.fluids.capability.IFluidHandler chamberHandler = tank.chamberHandler(chamber);
        if (FluidUtil.tryFluidTransfer(chamberHandler, itemHandler, Integer.MAX_VALUE, true).isEmpty()) {
            FluidUtil.tryFluidTransfer(itemHandler, chamberHandler, Integer.MAX_VALUE, true);
        }
        return ItemInteractionResult.CONSUME;
            *///?}
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
        //? if >=26.1 {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank) {
        //?} else {
        /*if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank) {
        *///?}
            // Sneaking with an empty hand makes a personal tank public again (the personal item comes back), and empties
            // the creative tank.
            if (player.isShiftKeyDown() && kind.creative()) {
                tank.setCreativeFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
            } else if (!player.isShiftKeyDown() || !tank.makePublic(player)) {
                serverPlayer.openMenu(tank, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeEnum(tank.kind());
                });
            }
        }
        //? if >=26.1 {
        return InteractionResult.SUCCESS;
        //?} else {
        /*return InteractionResult.sidedSuccess(level.isClientSide);
        *///?}
    }

    //? if <26.1 {
    /*/^* Anarchy mode: the drop is public (see the block entity's components), so the personal item drops apart. ^/
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity containers
                && containers.kind() == TankKind.SINGLE && !level.isClientSide) {
            for (int slot = 0; slot < containers.containers().getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), containers.containers().getStackInSlot(slot));
                containers.containers().setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (!state.is(newState.getBlock()) && dimensionalfluxminer.DfmConfig.tankAnarchyMode()
                && level.getBlockEntity(pos) instanceof DimensionalTankBlockEntity tank && tank.code().personal()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
