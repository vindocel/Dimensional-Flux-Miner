package dimensionalfluxminer.client.screen;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.renderer.RenderPipelines;

/** Runtime colors for the neutral pixels of the shared icon artwork. */
public final class IconPalette {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(DimensionalFluxMiner.id("pipeline/icon_palette"))
            .withFragmentShader(DimensionalFluxMiner.id("core/icon_palette"))
            .build();
    /** Fixed monochrome artwork, including the silver power-off symbol. */
    public static final RenderPipeline MONOCHROME_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(DimensionalFluxMiner.id("pipeline/icon_monochrome"))
            .withFragmentShader(DimensionalFluxMiner.id("core/icon_palette"))
            .withShaderDefine("MONOCHROME_ICON")
            .build();

    private IconPalette() {
    }

    static RenderPipeline pipeline(IconColor color) {
        return PIPELINE;
    }

    /** Furnace display details stay cyan with white icons. */
    static RenderPipeline accent(IconColor color) {
        return PIPELINE;
    }

    static int accentColor(IconColor color) {
        return color == IconColor.WHITE ? 0xFF45E5E9 : color.iconTint();
    }

    /** Dark foreground over the selected button's lighter accent. */
    static int selectedColor(int argb) {
        int rgb = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            rgb |= Math.round((argb >> shift & 255) * 0.42F) << shift;
        }
        return 0xFF000000 | rgb;
    }
}
