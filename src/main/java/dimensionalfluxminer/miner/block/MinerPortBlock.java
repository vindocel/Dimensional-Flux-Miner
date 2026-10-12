package dimensionalfluxminer.miner.block;

import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Generic dimensional port; its role (energy, matter, output) is assigned only when the assembly forms. */
public class MinerPortBlock extends MinerPartBlock implements EntityBlock {
    public static final com.mojang.serialization.MapCodec<MinerPortBlock> CODEC = simpleCodec(MinerPortBlock::new);

    public MinerPortBlock(Properties properties) {
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
}
