package dimensionalfluxminer.mixin.client;

import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Water particles born in Corrupted Water (bubbles, their pops, splashes and rain landing on it) become their purple
 * counterparts, and the fine underwater specks take a purple tint. Corrupted Water counts as water for entities, so
 * these particles come from the vanilla water behaviour.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCorruptedWaterMixin {
    @Shadow
    protected ClientLevel level;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void dimensionalfluxminer$corruptedWaterParticle(ParticleOptions options, double x, double y, double z, double xSpeed,
                                                             double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
        SimpleParticleType replacement = null;
        boolean surface = false;
        if (options == ParticleTypes.BUBBLE) {
            replacement = DfmParticles.CORRUPTED_BUBBLE.get();
        } else if (options == ParticleTypes.BUBBLE_POP) {
            replacement = DfmParticles.CORRUPTED_BUBBLE_POP.get();
        } else if (options == ParticleTypes.SPLASH || options == ParticleTypes.RAIN) {
            replacement = DfmParticles.CORRUPTED_SPLASH.get();
            surface = true;
        }
        if (replacement == null || !dimensionalfluxminer$inCorruptedWater(x, y, z, surface)) {
            return;
        }
        cir.setReturnValue(((ParticleEngine) (Object) this).createParticle(replacement, x, y, z, xSpeed, ySpeed, zSpeed));
    }

    @Inject(method = "createParticle", at = @At("RETURN"))
    private void dimensionalfluxminer$tintUnderwaterSpecks(ParticleOptions options, double x, double y, double z, double xSpeed,
                                                           double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
        if (cir.getReturnValue() instanceof SingleQuadParticle particle && options == ParticleTypes.UNDERWATER
                && dimensionalfluxminer$inCorruptedWater(x, y, z, false)) {
            // Purple instead of the pale blue of water.
            particle.setColor(0.55F, 0.3F, 0.75F);
        }
    }

    /** In Corrupted Water; with {@code surface}, also right above it (splashes and rain start on the surface). */
    private boolean dimensionalfluxminer$inCorruptedWater(double x, double y, double z, boolean surface) {
        if (level == null) {
            return false;
        }
        BlockPos pos = BlockPos.containing(x, y, z);
        return dimensionalfluxminer$corrupted(pos) || (surface && dimensionalfluxminer$corrupted(pos.below()));
    }

    private boolean dimensionalfluxminer$corrupted(BlockPos pos) {
        return level.getFluidState(pos).getFluidType() == DfmFluids.CORRUPTED_WATER_TYPE.get();
    }
}
