package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.tank.TankCode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            DimensionalFluxMiner.MOD_ID);

    /** Code of a Dimensional Tank item: the broken tank keeps it, so placing it again reconnects to the same channel. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TankCode>> TANK_CODE = COMPONENTS.registerComponentType("tank_code",
            builder -> builder.persistent(TankCode.CODEC).networkSynchronized(TankCode.STREAM_CODEC));

    private DfmDataComponents() {
    }
}
