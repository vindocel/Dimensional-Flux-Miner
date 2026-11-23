package dimensionalfluxminer.client.screen;

//? if =26.1.2 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.renderer.RenderPipelines;

/** Runtime colors for the neutral pixels of the shared icon artwork. */
//?} else if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.renderer.RenderPipelines;

/^* Runtime colors for the neutral pixels of the shared icon artwork. ^/
*///?} else {
/*import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;

/^*
 * Runtime colors for the neutral pixels of the shared icon artwork. While an icon is drawn, the textured blits of
 * {@link GuiGraphics} use the {@code icon_palette} shader (see {@link GuiTheme#shader}); the tint travels in the
 * shader color ({@code ColorModulator}).
 ^/
*///?}
public final class IconPalette {
    //? if >=26.1 {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(DimensionalFluxMiner.id("pipeline/icon_palette"))
            .withFragmentShader(DimensionalFluxMiner.id("core/icon_palette"))
            .build();
    //?} else {
    /*private static ShaderInstance shader;
    /^* Mode of the icon being drawn: -1 none, 0 neutral pixels take the tint, 1 the whole artwork as monochrome. ^/
    private static int mode = -1;
    *///?}

    private IconPalette() {
    }

    //? if >=26.1 {
    static RenderPipeline pipeline(IconColor color) {
        return PIPELINE;
    //?} else {
    /*/^* Set when the shaders load (and reload with the resource packs). ^/
    public static void setShader(ShaderInstance instance) {
        shader = instance;
    }

    /^* Shader for a texture drawn now: the palette shader while an icon is drawn, otherwise null. ^/
    static Supplier<ShaderInstance> shader() {
        if (mode < 0 || shader == null) {
            return null;
        }
        float monochrome = mode;
        ShaderInstance instance = shader;
        return () -> {
            instance.safeGetUniform("Monochrome").set(monochrome);
            return instance;
        };
    }

    /^* Draws icon artwork with {@code argb} on its neutral pixels (or on all of it, as monochrome). ^/
    static void draw(GuiGraphics g, int argb, boolean monochrome, Runnable drawing) {
        int before = mode;
        mode = monochrome ? 1 : 0;
        g.setColor(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
        try {
            drawing.run();
        } finally {
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            mode = before;
        }
    *///?}
    }

    /** Furnace mode icon: its small terminal and divider take the theme color. */
    //? if >=26.1 {
    static RenderPipeline accent(IconColor color) {
        return PIPELINE;
    }

    //?}
    static int accentColor(IconColor color) {
        return color.iconTint();
    }
}
