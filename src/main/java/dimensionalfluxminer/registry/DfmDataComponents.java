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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TANK_CHANNEL_LOCKED =
            COMPONENTS.registerComponentType("tank_channel_locked", builder -> builder.persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_LOCAL_A =
            COMPONENTS.registerComponentType("tank_local_a", builder -> builder.persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_LOCAL_B =
            COMPONENTS.registerComponentType("tank_local_b", builder -> builder.persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    /** Read-only channel snapshots for item rendering; never used as fluid storage. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_DISPLAY_A =
            COMPONENTS.registerComponentType("tank_display_a", builder -> builder.persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_DISPLAY_B =
            COMPONENTS.registerComponentType("tank_display_b", builder -> builder.persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

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
