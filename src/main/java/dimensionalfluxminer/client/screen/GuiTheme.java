package dimensionalfluxminer.client.screen;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Light or dark look of the machine screens while one is drawn. The dark look only touches the gray parts of the
 * screen (panel, slots, buttons, machine artwork, energy frame): those are drawn inside {@link #chrome}, where the
 * textures go through the {@code dark_gui} shader, which takes gray pixels to the dark palette and keeps colored
 * ones. Icons, items, liquids and the navy screens are never drawn inside it, so they keep their colors.
 */
public final class GuiTheme {
    /** Brightness kept by gray pixels in the dark look ({@code DARKNESS} of {@code dark_gui.fsh}). */
    static final float DARKNESS = 0.32F;
    /** Textured GUI pipelines of the chrome: dark, and light (no change, but still the mod's shader). */
    public static final RenderPipeline DARK_PIPELINE = pipeline("dark_gui", DARKNESS);
    public static final RenderPipeline LIGHT_PIPELINE = pipeline("light_gui", 1.0F);
    /** Panel color of each look, for the previews of the theme window. */
    static final int LIGHT_PANEL = 0xFFBEBEBE;
    static final int DARK_PANEL = 0xFF3D3D3D;

    private static boolean dark;
    /** Icon color of the screen being drawn. */
    private static IconColor icons = IconColor.WHITE;
    private static int chromeDepth;

    private GuiTheme() {
    }

    private static RenderPipeline pipeline(String name, float darkness) {
        return RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(DimensionalFluxMiner.id("pipeline/" + name))
                .withFragmentShader(DimensionalFluxMiner.id("core/dark_gui"))
                .withShaderDefine("DARKNESS", darkness)
                .build();
    }

    static void setDark(boolean value) {
        dark = value;
    }

    public static boolean dark() {
        return dark;
    }

    /**
     * Draws {@code drawing} with the light or dark look for a page another mod shows (a recipe viewer with its own dark
     * theme), then puts the previous look back; the player's choices of the machine screens are never touched.
     */
    public static void withDark(boolean value, Runnable drawing) {
        boolean before = dark;
        dark = value;
        try {
            drawing.run();
        } finally {
            dark = before;
        }
    }

    static void setIcons(IconColor value) {
        icons = value;
    }

    static IconColor icons() {
        return icons;
    }

    /** Draws {@code drawing} as screen chrome: darkened in the dark look, unchanged in the light one. */
    static void chrome(Runnable drawing) {
        chromeDepth++;
        try {
            drawing.run();
        } finally {
            chromeDepth--;
        }
    }

    /**
     * Pipeline of a texture drawn now. Inside {@link #chrome} the plain textured one becomes the mod's own (dark, or with
     * no change in the light look), so screen-wide dark shaders of other mods never touch these screens twice.
     */
    public static RenderPipeline pipeline(RenderPipeline normal) {
        if (chromeDepth == 0 || normal != RenderPipelines.GUI_TEXTURED) {
            return normal;
        }
        return dark ? DARK_PIPELINE : LIGHT_PIPELINE;
    }

    /**
     * Draws a widget another mod puts on a machine screen (the Polymorph recipe button) with that screen's look, as
     * chrome; on any other screen it is drawn as usual.
     */
    public static void drawOnMachineScreen(net.minecraft.client.gui.screens.Screen screen, Runnable drawing) {
        if (!(screen instanceof DfmScreen<?> machine)) {
            drawing.run();
            return;
        }
        boolean before = dark;
        IconColor beforeIcons = icons;
        dark = GuiThemes.dark(machine.machinePos());
        icons = GuiThemes.icons(machine.machinePos());
        try {
            chrome(drawing);
        } finally {
            dark = before;
            icons = beforeIcons;
        }
    }

    /**
     * A color drawn in code for a control (buttons): in the dark look neutral colors darken like the gray textures,
     * while saturated ones keep their color (the more saturated, the less they darken).
     */
    static int chromeColor(int argb) {
        if (!dark) {
            return argb;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float chroma = (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b))) / 255.0F;
        float t = Math.max(0, Math.min(1, (chroma - 0.12F) / (0.35F - 0.12F)));
        float factor = DARKNESS + (1 - DARKNESS) * t * t * (3 - 2 * t);
        return (argb & 0xFF000000) | Math.round(r * factor) << 16 | Math.round(g * factor) << 8 | Math.round(b * factor);
    }

    /** A gray drawn in code (bevels, reservoir insides), taken to the dark palette like the textures. */
    static int gray(int argb) {
        if (!dark) {
            return argb;
        }
        int r = Math.round(((argb >> 16) & 0xFF) * DARKNESS);
        int g = Math.round(((argb >> 8) & 0xFF) * DARKNESS);
        int b = Math.round((argb & 0xFF) * DARKNESS);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Panel title and labels. */
    static int title() {
        return dark ? 0xFFD0D0D0 : DfmScreen.TITLE;
    }

    /** Emphasized label. */
    static int heading() {
        return dark ? 0xFFFFFFFF : DfmScreen.HEADING;
    }

    /** Secondary label. */
    static int subheading() {
        return dark ? 0xFF9A9A9A : DfmScreen.SUBHEADING;
    }
}
