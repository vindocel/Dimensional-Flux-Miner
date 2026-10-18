package dimensionalfluxminer.furnace;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.HorizontalMachineBlock;
import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlock extends HorizontalMachineBlock {
    public static final MapCodec<ElectricFurnaceBlock> CODEC = simpleCodec(ElectricFurnaceBlock::new);

    public ElectricFurnaceBlock(Properties properties) {
        super(properties);
    }

    /** Real block light derived from the machine state (no separate LIT property): ACTIVE emits 14, OFF/IDLE emit 0. */
    public static int lightFromState(BlockState state) {
        return state.getValue(DfmStateProperties.MACHINE_STATE) == MachineState.ACTIVE ? 14 : 0;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> blockEntityType() {
        return DfmBlockEntities.ELECTRIC_FURNACE.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }
}
