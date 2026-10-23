package dimensionalfluxminer.miner.block;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.miner.MinerAssembly;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** The Miner terminal: its horizontal facing defines the front of the whole machine; it hosts the controller. */
public class MinerTerminalBlock extends MinerPartBlock implements EntityBlock {

    public MinerTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(DfmStateProperties.FORMED, false)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return dimensionalfluxminer.content.Waterlogging.placed(
                defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite()), context);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerControllerBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != DfmBlockEntities.MINER_CONTROLLER.get()) {
            return null;
        }
        return level.isClientSide()
                ? (BlockEntityTicker<T>) (BlockEntityTicker<MinerControllerBlockEntity>) MinerControllerClientTicker::tick
                : (BlockEntityTicker<T>) (BlockEntityTicker<MinerControllerBlockEntity>) MinerControllerBlockEntity::serverTick;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player && level.getBlockEntity(pos) instanceof MinerControllerBlockEntity controller) {
            controller.setOwner(player.getUUID());
            controller.tryForm();
            // The highlight of the parts still missing shows as soon as the terminal is down.
            if (!controller.formed() && player instanceof ServerPlayer serverPlayer) {
                MinerAssembly.highlight(serverPlayer, pos, MinerAssembly.missing(serverPlayer.level(), pos));
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MinerControllerBlockEntity controller) {
            if (!controller.formed()) {
                if (controller.owner() == null) {
                    controller.setOwner(player.getUUID());
                }
                Component problem = controller.tryForm();
                if (problem != null) {
                    // Assisted assembly: one missing part per click from the inventory, and a highlight of the rest; a
                    // click that places nothing switches the highlight off and on.
                    int before = MinerAssembly.missing(serverPlayer.level(), pos).size();
                    Component assembly = MinerAssembly.placeNext(serverPlayer.level(), pos, serverPlayer);
                    if (!controller.formed()) {
                        var left = MinerAssembly.missing(serverPlayer.level(), pos);
                        if (left.size() < before) {
                            MinerAssembly.highlight(serverPlayer, pos, left);
                        } else {
                            MinerAssembly.toggleHighlight(serverPlayer, pos);
                        }
                        serverPlayer.sendOverlayMessage(assembly != null ? assembly : problem);
                        return InteractionResult.CONSUME;
                    }
                }
                MinerAssembly.highlight(serverPlayer, pos, java.util.List.of());
                serverPlayer.sendOverlayMessage(Component.translatable("message.dimensionalfluxminer.structure_formed"));
                return InteractionResult.CONSUME;
            }
            serverPlayer.openMenu(controller, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Client-side ticker: the looping sound and the core particles (rendering is done by the block entity renderer). */
    public static final class MinerControllerClientTicker {
        private MinerControllerClientTicker() {
        }

        static void tick(Level level, BlockPos pos, BlockState state, MinerControllerBlockEntity be) {
            // Accelerated extra calls of the same game time add nothing: the effects keep their own pace.
            if (be.firstClientTick()) {
                dimensionalfluxminer.machine.ClientHooks.minerTick.accept(be);
            }
        }
    }
}
