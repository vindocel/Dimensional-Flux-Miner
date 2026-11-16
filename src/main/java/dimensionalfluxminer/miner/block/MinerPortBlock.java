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

/**
 * Dimensional port: the seven blocks around the lower layer. Their outer faces are the Miner's nine configurable faces
 * (see {@code MinerPorts}); like the lower chassis they receive redstone and external tick acceleration.
 */
public class MinerPortBlock extends MinerPartBlock implements EntityBlock {
    //? if <26.1 {
    /*public static final com.mojang.serialization.MapCodec<MinerPortBlock> CODEC = simpleCodec(MinerPortBlock::new);
    *///?}

    public MinerPortBlock(Properties properties) {
        super(properties);
    //? if <26.1 {
    /*}

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() {
        return CODEC;
    *///?}
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerLinkBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        //? if >=26.1 {
        if (level.isClientSide() || type != DfmBlockEntities.MINER_LINK.get()) {
        //?} else {
        /*if (level.isClientSide || type != DfmBlockEntities.MINER_LINK.get()) {
        *///?}
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<MinerLinkBlockEntity>) MinerLinkBlockEntity::serverTick;
    }
}
