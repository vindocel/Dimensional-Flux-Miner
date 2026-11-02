package dimensionalfluxminer.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dimensionalfluxminer.client.screen.GuiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Textures drawn as machine-screen chrome use the mod's {@code dark_gui} pipeline instead of the plain textured one
 * (see {@link GuiTheme#chrome}); every other texture is drawn as usual.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsThemeMixin {
    @ModifyVariable(method = {
            "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
            "innerTiledBlit"}, at = @At("HEAD"), argsOnly = true)
    private RenderPipeline dimensionalfluxminer$themePipeline(RenderPipeline pipeline) {
        return GuiTheme.pipeline(pipeline);
    }
}
