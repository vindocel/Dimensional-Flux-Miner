package dimensionalfluxminer.client.screen;

/**
 * Colors of the screen frame and controls in the theme color of the screen being drawn, as the Steelframe screens of
 * Dimensional Flux do with the network color: selected buttons and lit switches take a deep shade of the color (dark
 * enough for the white icons), the outline under the mouse a light shade, and the metal band and tab ears a dark, muted
 * shade of it. The blue theme (the default) keeps the plain steel and the teal controls. Gray, black and white custom
 * colors keep the steel a little darker.
 */
final class ThemeAccent {
    // Blue theme: the plain teal of the controls.
    private static final int BLUE_FILL = 0xFF26858F;
    private static final int BLUE_TOP = 0xFF4D9BA3;
    private static final int BLUE_BOTTOM = 0xFF1E6069;
    private static final int BLUE_HOVER = 0xFF4AABB9;

    private static int cachedRgb = -1;
    private static int fill = BLUE_FILL;
    private static int top = BLUE_TOP;
    private static int bottom = BLUE_BOTTOM;
    private static int hover = BLUE_HOVER;
    /** Theme color of the band, or -1 for the plain steel. */
    private static int bandColor = -1;

    private ThemeAccent() {
    }

    /** Fill of a selected button and of the lit block of a switch. */
    static int fill() {
        update();
        return fill;
    }

    /** Top line of the lit block of a switch. */
    static int top() {
        update();
        return top;
    }

    /** Bottom line of the lit block of a switch. */
    static int bottom() {
        update();
        return bottom;
    }

    /** Outline of the button under the mouse. */
    static int hover() {
        update();
        return hover;
    }

    /** {@code argb}, a color of the steel band, in the shade of the theme color. */
    static int band(int argb) {
        update();
        if (bandColor < 0) {
            return argb;
        }
        float[] steel = hsl(argb);
        float[] theme = hsl(bandColor);
        if (theme[1] < 0.15F) {
            return (argb & 0xFF000000) | (hslToRgb(steel[0], steel[1], steel[2] * 0.9F) & 0xFFFFFF);
        }
        float saturation = Math.min(0.42F, theme[1] * 0.55F);
        return (argb & 0xFF000000) | (hslToRgb(theme[0], saturation, steel[2] * 0.82F) & 0xFFFFFF);
    }

    /** Recomputes the shades when the screen being drawn has another theme color. */
    private static void update() {
        IconColor theme = GuiTheme.icons();
        int rgb = theme == IconColor.BLUE ? -2 : theme.iconTint() & 0xFFFFFF;
        if (rgb == cachedRgb) {
            return;
        }
        cachedRgb = rgb;
        if (rgb < 0) {
            fill = BLUE_FILL;
            top = BLUE_TOP;
            bottom = BLUE_BOTTOM;
            hover = BLUE_HOVER;
            bandColor = -1;
            return;
        }
        fill = withLightness(rgb, 0.36F);
        top = withLightness(rgb, 0.50F);
        bottom = withLightness(rgb, 0.24F);
        hover = withLightness(rgb, 0.58F);
        bandColor = rgb;
    }

    /**
     * Tint (0xRRGGBB) of the neutral terminal texture for the theme color {@code rgb}: its hue, saturation at most 0.8
     * and lightness kept between 0.45 and 0.60, the range of the blue terminal. The brightest pixels of the texture
     * take this color, so a light theme color would turn pale; a dark one would make the screen unreadable.
     */
    static int terminal(int rgb) {
        float[] c = hsl(rgb);
        return hslToRgb(c[0], Math.min(0.8F, c[1]), Math.max(0.45F, Math.min(0.60F, c[2]))) & 0xFFFFFF;
    }

    /** {@code rgb} with its hue and saturation (at most 0.75, never neon) at the HSL lightness {@code lightness}. */
    private static int withLightness(int rgb, float lightness) {
        float[] c = hsl(rgb);
        return hslToRgb(c[0], Math.min(0.75F, c[1]), lightness);
    }

    /** {hue 0..1, saturation, lightness} of a color. */
    private static float[] hsl(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255F, g = (rgb >> 8 & 0xFF) / 255F, b = (rgb & 0xFF) / 255F;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float l = (max + min) / 2;
        float s = delta == 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));
        float h = 0;
        if (delta != 0) {
            if (max == r) {
                h = ((g - b) / delta) % 6;
            } else if (max == g) {
                h = (b - r) / delta + 2;
            } else {
                h = (r - g) / delta + 4;
            }
            h /= 6;
            if (h < 0) {
                h += 1;
            }
        }
        return new float[]{h, s, l};
    }

    private static int hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float hh = h * 6, x = c * (1 - Math.abs(hh % 2 - 1));
        float rr, gg, bb;
        if (hh < 1) { rr = c; gg = x; bb = 0; }
        else if (hh < 2) { rr = x; gg = c; bb = 0; }
        else if (hh < 3) { rr = 0; gg = c; bb = x; }
        else if (hh < 4) { rr = 0; gg = x; bb = c; }
        else if (hh < 5) { rr = x; gg = 0; bb = c; }
        else { rr = c; gg = 0; bb = x; }
        float m = l - c / 2;
        return 0xFF000000 | channel(rr + m) << 16 | channel(gg + m) << 8 | channel(bb + m);
    }

    private static int channel(float v) {
        return Math.round(Math.max(0, Math.min(1, v)) * 255);
    }
}
