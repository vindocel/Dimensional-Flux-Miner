package dimensionalfluxminer.miner.block;

import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Steel chassis: structural part; the lower one is a redstone and external tick-acceleration receiver. */
public class MinerChassisBlock extends MinerPartBlock implements EntityBlock {

    public MinerChassisBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerLinkBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != DfmBlockEntities.MINER_LINK.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MinerLinkBlockEntity>) MinerLinkBlockEntity::serverTick;
    }
}
