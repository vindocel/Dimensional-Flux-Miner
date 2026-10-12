package dimensionalfluxminer.mixin;

import dimensionalfluxminer.registry.DfmFluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Corrupted Water counts as water for entities. NeoForge answers the vanilla "in water" / "eyes in water" checks with
 * the vanilla water fluid type only, ignoring the {@code minecraft:water} tag; with Corrupted Water added here, every
 * water behaviour applies to it as well: splash, swimming, extinguishing, underwater sounds, fog and air.
 */
@Mixin(Entity.class)
public abstract class EntityCorruptedWaterMixin {
    @Inject(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z", at = @At("RETURN"), cancellable = true)
    private void dimensionalfluxminer$touchingCorruptedWater(TagKey<Fluid> tag, double motionScale, CallbackInfoReturnable<Boolean> cir) {
        if (tag == FluidTags.WATER && !cir.getReturnValueZ()
                && ((Entity) (Object) this).isInFluidType(DfmFluids.CORRUPTED_WATER_TYPE.get())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isEyeInFluid", at = @At("RETURN"), cancellable = true)
    private void dimensionalfluxminer$eyeInCorruptedWater(TagKey<Fluid> tag, CallbackInfoReturnable<Boolean> cir) {
        if (tag == FluidTags.WATER && !cir.getReturnValueZ()
                && ((Entity) (Object) this).isEyeInFluidType(DfmFluids.CORRUPTED_WATER_TYPE.get())) {
            cir.setReturnValue(true);
        }
    }
}
