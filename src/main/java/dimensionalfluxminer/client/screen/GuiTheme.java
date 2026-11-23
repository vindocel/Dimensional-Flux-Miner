package dimensionalfluxminer.client.screen;

//? if =26.1.2 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.renderer.RenderPipelines;
*///?} else {
/*import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;
*///?}

/**
 * Light or dark look of the machine screens while one is drawn. The dark look only touches the gray parts of the
 * screen (panel, slots, buttons, machine artwork, energy frame): those are drawn inside {@link #chrome}, where the
 * textures go through the {@code dark_gui} shader, which takes gray pixels to the dark palette and keeps colored
 * ones. Icons, items, liquids and the navy screens are never drawn inside it, so they keep their colors.
 */
public final class GuiTheme {
    //? if >=26.1 {
    /** Brightness kept by gray pixels in the dark look ({@code DARKNESS} of {@code dark_gui.fsh}). */
    static final float DARKNESS = 0.32F;
    /** Textured GUI pipelines of the chrome: dark, and light (no change, but still the mod's shader). */
    public static final RenderPipeline DARK_PIPELINE = pipeline("dark_gui", DARKNESS);
    public static final RenderPipeline LIGHT_PIPELINE = pipeline("light_gui", 1.0F);
    //?} else {
    /*/^* Brightness kept by gray pixels in the dark look (same value as {@code Darkness} in {@code dark_gui.json}). ^/
    static final float DARKNESS = 0.32F;
    *///?}
    /** Panel color of each look, for the previews of the theme window. */
    static final int LIGHT_PANEL = 0xFFBEBEBE;
    static final int DARK_PANEL = 0xFF3D3D3D;

    //? if <26.1 {
    /*private static ShaderInstance shader;
    *///?}
    private static boolean dark;
    /** Theme color of the screen being drawn. */
    private static IconColor icons = IconColor.BLUE;
    private static int customRgb = -1;
    private static int customText;
    private static int customTop;
    private static int customBottom;
    private static int customRim;
    static {
        setCustomRgb(GuiThemes.DEFAULT_CUSTOM_COLOR);
    }
    private static int chromeDepth;

    private GuiTheme() {
    }

    //? if >=26.1 {
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

    static void setCustomRgb(int rgb) {
        rgb &= 0xFFFFFF;
        if (customRgb == rgb) {
            return;
        }
        customRgb = rgb;
        float[] chosen = java.awt.Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
        customText = 0xFF000000 | customTone(0x52D9DB, chosen);
        customTop = customTone(0x14243A, chosen);
        customBottom = customTone(0x1F3852, chosen);
        customRim = customTone(0x0F1A2B, chosen);
    }

    /** Retain the display's contrast while following the chosen hue and saturation. */
    private static int customTone(int base, float[] chosen) {
        float[] source = java.awt.Color.RGBtoHSB(base >> 16 & 255, base >> 8 & 255, base & 255, null);
        return java.awt.Color.HSBtoRGB(chosen[0], source[1] * chosen[1], source[2]) & 0xFFFFFF;
    }

    static int customText() { return customText; }
    static int customTop() { return customTop; }
    static int customBottom() { return customBottom; }
    static int customRim() { return customRim; }

    static int customRgb() {
        return customRgb;
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
    //?} else {
    /*/^* Set when the shaders load (and reload with the resource packs). ^/
    public static void setShader(ShaderInstance instance) {
        shader = instance;
    }

    static void setDark(boolean value) {
        dark = value;
    }

    public static boolean dark() {
        return dark;
    }

    /^*
     * Draws {@code drawing} with the light or dark look for a page another mod shows (a recipe viewer with its own dark
     * theme), then puts the previous look back; the player's choices of the machine screens are never touched.
     ^/
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

    static void setCustomRgb(int rgb) {
        rgb &= 0xFFFFFF;
        if (customRgb == rgb) {
            return;
        }
        customRgb = rgb;
        float[] chosen = java.awt.Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
        customText = 0xFF000000 | customTone(0x52D9DB, chosen);
        customTop = customTone(0x14243A, chosen);
        customBottom = customTone(0x1F3852, chosen);
        customRim = customTone(0x0F1A2B, chosen);
    }

    /^* Retain the display's contrast while following the chosen hue and saturation. ^/
    private static int customTone(int base, float[] chosen) {
        float[] source = java.awt.Color.RGBtoHSB(base >> 16 & 255, base >> 8 & 255, base & 255, null);
        return java.awt.Color.HSBtoRGB(chosen[0], source[1] * chosen[1], source[2]) & 0xFFFFFF;
    }

    static int customText() { return customText; }
    static int customTop() { return customTop; }
    static int customBottom() { return customBottom; }
    static int customRim() { return customRim; }

    static int customRgb() {
        return customRgb;
    }

    /^* Draws {@code drawing} as screen chrome: darkened in the dark look, unchanged in the light one. ^/
    static void chrome(Runnable drawing) {
        chromeDepth++;
        try {
            drawing.run();
        } finally {
            chromeDepth--;
        }
    }

    /^*
     * Shader used by a texture drawn now. Inside {@link #chrome} it is always the mod's own shader (dark, or with no
     * change in the light look), so screen-wide dark shaders of other mods never touch these screens twice.
     ^/
    public static Supplier<ShaderInstance> shader(Supplier<ShaderInstance> normal) {
        Supplier<ShaderInstance> icon = IconPalette.shader();
        if (icon != null) {
            return icon;
        }
        if (chromeDepth == 0 || shader == null) {
    *///?}
            return normal;
        }
        //? if >=26.1 {
        return dark ? DARK_PIPELINE : LIGHT_PIPELINE;
        //?} else {
        /*float darkness = dark ? DARKNESS : 1.0F;
        ShaderInstance instance = shader;
        return () -> {
            instance.safeGetUniform("Darkness").set(darkness);
            return instance;
        };
        *///?}
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
        int beforeRgb = customRgb;
        dark = GuiThemes.dark(machine.machinePos());
        icons = GuiThemes.icons(machine.machinePos());
        setCustomRgb(GuiThemes.customRgb(machine.machinePos()));
        try {
            chrome(drawing);
        } finally {
            dark = before;
            icons = beforeIcons;
            setCustomRgb(beforeRgb);
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
