package dimensionalfluxminer.crusher;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.HorizontalMachineBlock;
import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MechanicalCrusherBlock extends HorizontalMachineBlock {
    public static final MapCodec<MechanicalCrusherBlock> CODEC = simpleCodec(MechanicalCrusherBlock::new);

    public MechanicalCrusherBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> blockEntityType() {
        return DfmBlockEntities.MECHANICAL_CRUSHER.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MechanicalCrusherBlockEntity(pos, state);
    }
}
