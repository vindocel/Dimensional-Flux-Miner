package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Purple counterparts of the water particles, shown in and on Corrupted Water. */
public final class DfmParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTED_BUBBLE = register("corrupted_bubble");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTED_BUBBLE_POP = register("corrupted_bubble_pop");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CORRUPTED_SPLASH = register("corrupted_splash");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false));
    }

    private DfmParticles() {
    }
}
