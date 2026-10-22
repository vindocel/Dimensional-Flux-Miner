package dimensionalfluxminer.client.screen;

import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Light or dark look of the machine screens while one is drawn. The dark look only touches the gray parts of the
 * screen (panel, slots, buttons, machine artwork, energy frame): those are drawn inside {@link #chrome}, where the
 * textures go through the {@code dark_gui} shader, which takes gray pixels to the dark palette and keeps colored
 * ones. Icons, items, liquids and the navy screens are never drawn inside it, so they keep their colors.
 */
public final class GuiTheme {
    /** Brightness kept by gray pixels in the dark look (same value as {@code Darkness} in {@code dark_gui.json}). */
    static final float DARKNESS = 0.32F;
    /** Panel color of each look, for the previews of the theme window. */
    static final int LIGHT_PANEL = 0xFFBEBEBE;
    static final int DARK_PANEL = 0xFF3D3D3D;
    /** Icon colors, for the previews of the settings window. */
    static final int BLUE_ICONS = 0xFF20C8D1;
    static final int PURPLE_ICONS = 0xFF9366DE;

    private static ShaderInstance shader;
    private static boolean dark;
    /** Icons of the screen being drawn: purple, otherwise blue. */
    private static boolean purple;
    private static int chromeDepth;

    private GuiTheme() {
    }

    /** Set when the shaders load (and reload with the resource packs). */
    public static void setShader(ShaderInstance instance) {
        shader = instance;
    }

    static void setDark(boolean value) {
        dark = value;
    }

    public static boolean dark() {
        return dark;
    }

    static void setPurple(boolean value) {
        purple = value;
    }

    static boolean purple() {
        return purple;
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
     * Shader used by a texture drawn now. Inside {@link #chrome} it is always the mod's own shader (dark, or with no
     * change in the light look), so screen-wide dark shaders of other mods never touch these screens twice.
     */
    public static Supplier<ShaderInstance> shader(Supplier<ShaderInstance> normal) {
        if (chromeDepth == 0 || shader == null) {
            return normal;
        }
        float darkness = dark ? DARKNESS : 1.0F;
        ShaderInstance instance = shader;
        return () -> {
            instance.safeGetUniform("Darkness").set(darkness);
            return instance;
        };
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
        boolean beforePurple = purple;
        dark = GuiThemes.dark(machine.machinePos());
        purple = GuiThemes.purple(machine.machinePos());
        try {
            chrome(drawing);
        } finally {
            dark = before;
            purple = beforePurple;
        }
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
