package dimensionalfluxminer.mixin;

import dimensionalfluxminer.registry.DfmTags;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hostiles that normally burn in daylight do not receive sun burning while standing on eligible corrupted
 * terrain. Only the daylight check is affected: fire, lava and every other damage source keep working.
 */
@Mixin(Mob.class)
public abstract class MobSunlightMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void dimensionalfluxminer$corruptedTerrainShade(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (self.onGround() && self.level().getBlockState(self.getOnPos()).is(DfmTags.Blocks.CORRUPTED_TERRAIN)) {
            cir.setReturnValue(false);
        }
    }
}
