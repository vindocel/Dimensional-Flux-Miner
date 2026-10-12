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

/** Steel chassis: structural part; the lower eight are redstone and external tick-acceleration receivers. */
public class MinerChassisBlock extends MinerPartBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<MinerChassisBlock> CODEC = simpleCodec(MinerChassisBlock::new);

    public MinerChassisBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerLinkBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != DfmBlockEntities.MINER_LINK.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MinerLinkBlockEntity>) MinerLinkBlockEntity::serverTick;
    }
}
