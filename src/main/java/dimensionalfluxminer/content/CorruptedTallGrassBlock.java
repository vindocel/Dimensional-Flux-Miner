package dimensionalfluxminer.content;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.registry.DfmTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Two-block corrupted vegetation placed by the Blight in place of tall grass and large ferns. */
public class CorruptedTallGrassBlock extends DoublePlantBlock {
    //? if <26.1 {
    /*public static final MapCodec<CorruptedTallGrassBlock> CODEC = simpleCodec(CorruptedTallGrassBlock::new);
    *///?}

    public CorruptedTallGrassBlock(Properties properties) {
        super(properties);
    //? if <26.1 {
    /*}

    @Override
    public MapCodec<? extends DoublePlantBlock> codec() {
        return CODEC;
    *///?}
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(DfmTags.Blocks.CORRUPTED_TERRAIN) || super.mayPlaceOn(state, level, pos);
    }
}
