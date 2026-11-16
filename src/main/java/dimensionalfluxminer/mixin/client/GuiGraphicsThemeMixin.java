package dimensionalfluxminer.mixin.client;

//? if =26.1.2 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?}
import dimensionalfluxminer.client.screen.GuiTheme;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.1 {
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Textures drawn as machine-screen chrome use the mod's {@code dark_gui} pipeline instead of the plain textured one
 * (see {@link GuiTheme#chrome}); every other texture is drawn as usual.
 */
@Mixin(GuiGraphicsExtractor.class)
//?} else {
/*import org.spongepowered.asm.mixin.injection.ModifyArg;

/^*
 * Textures drawn as machine-screen chrome in the dark look use the mod's {@code dark_gui} shader instead of the plain
 * textured one (see {@link GuiTheme#chrome}); every other texture is drawn as usual.
 ^/
@Mixin(GuiGraphics.class)
*///?}
public abstract class GuiGraphicsThemeMixin {
    //? if =26.1.2 {
    @ModifyVariable(method = {
            "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
            "innerTiledBlit"}, at = @At("HEAD"), argsOnly = true)
    private RenderPipeline dimensionalfluxminer$themePipeline(RenderPipeline pipeline) {
        return GuiTheme.pipeline(pipeline);
    //?} else if >=26.3 {
    /*@ModifyVariable(method = {
            "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V",
            "innerTiledBlit"}, at = @At("HEAD"), argsOnly = true)
    private RenderPipeline dimensionalfluxminer$themePipeline(RenderPipeline pipeline) {
        return GuiTheme.pipeline(pipeline);
    *///?} else {
    /*@ModifyArg(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"))
    private Supplier<ShaderInstance> dimensionalfluxminer$themeShader(Supplier<ShaderInstance> shader) {
        return GuiTheme.shader(shader);
    *///?}
    }
}
