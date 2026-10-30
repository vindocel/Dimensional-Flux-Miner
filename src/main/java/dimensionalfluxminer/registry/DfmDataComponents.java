package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.tank.TankCode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE,
            DimensionalFluxMiner.MOD_ID);

    /** Code of a Dimensional Tank item: the broken tank keeps it, so placing it again reconnects to the same channel. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TankCode>> TANK_CODE = COMPONENTS.registerComponentType("tank_code",
            builder -> builder.persistent(TankCode.CODEC).networkSynchronized(TankCode.STREAM_CODEC));
    /** Liquid of a Creative Dimensional Tank item: placing it gives the tank that endless liquid. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> CREATIVE_TANK_FLUID =
            COMPONENTS.registerComponentType("creative_tank_fluid",
                    builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));
    /** Single and creative tank items: bucket mode (pours out and picks up liquid instead of being placed). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TANK_BUCKET_MODE =
            COMPONENTS.registerComponentType("tank_bucket_mode",
                    builder -> builder.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));

    private DfmDataComponents() {
    }
}
