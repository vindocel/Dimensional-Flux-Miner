package dimensionalfluxminer.mixin.client;

import dimensionalfluxminer.client.screen.GuiTheme;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Textures drawn as machine-screen chrome in the dark look use the mod's {@code dark_gui} shader instead of the plain
 * textured one (see {@link GuiTheme#chrome}); every other texture is drawn as usual.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsThemeMixin {
    @ModifyArg(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"))
    private Supplier<ShaderInstance> dimensionalfluxminer$themeShader(Supplier<ShaderInstance> shader) {
        return GuiTheme.shader(shader);
    }
}
