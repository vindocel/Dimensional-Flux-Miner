package dimensionalfluxminer.registry;

import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.CoreFluxOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Purple counterparts of the water particles, shown in and on Corrupted Water, and the quantum motes the Miner's core
 * pulls in while it works (one type per core color).
 */
public final class DfmParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTED_BUBBLE = register("corrupted_bubble");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTED_BUBBLE_POP = register("corrupted_bubble_pop");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTED_SPLASH = register("corrupted_splash");
    public static final DeferredHolder<ParticleType<?>, ParticleType<CoreFluxOptions>> CORE_FLUX_BASIC = registerFlux("core_flux_basic");
    public static final DeferredHolder<ParticleType<?>, ParticleType<CoreFluxOptions>> CORE_FLUX_STABILIZED = registerFlux("core_flux_stabilized");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false));
    }

    private static DeferredHolder<ParticleType<?>, ParticleType<CoreFluxOptions>> registerFlux(String name) {
        return PARTICLES.register(name, () -> new ParticleType<CoreFluxOptions>(false) {
            @Override
            public MapCodec<CoreFluxOptions> codec() {
                return CoreFluxOptions.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, CoreFluxOptions> streamCodec() {
                return CoreFluxOptions.streamCodec(this);
            }
        });
    }

    private DfmParticles() {
    }
}
