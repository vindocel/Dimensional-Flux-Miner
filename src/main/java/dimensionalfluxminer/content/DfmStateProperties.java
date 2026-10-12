package dimensionalfluxminer.content;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class DfmStateProperties {
    public static final EnumProperty<MachineState> MACHINE_STATE = EnumProperty.create("machine_state", MachineState.class);
    /** True while the block participates in a formed Dimensional Flux Miner. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private DfmStateProperties() {
    }
}
