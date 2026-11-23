package dimensionalfluxminer.client.screen;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * Every button of the machine screens, drawn with plain fills as in the Steelframe screens of Dimensional Flux: dark
 * outline (a light shade of the theme color when hovered, see {@link ThemeAccent#hover}), light border and a slate
 * inside. A switched-on one (toggle on, selected tab) takes a deep shade of the theme color inside
 * ({@link ThemeAccent#fill}), dark enough for the white icons, and still reacts to the mouse; a disabled one is sunk and
 * gray. In the dark look the neutral parts follow {@link GuiTheme#chromeColor}; colored parts keep their color.
 */
final class ButtonArt {
    static final int SWITCH_WIDTH = 31;
    static final int SWITCH_HEIGHT = 14;

    private static final int OUTLINE = 0xFF1A242B;
    private static final int BORDER = 0xFFE8EDEB;
    private static final int BORDER_DISABLED = 0xFF879399;
    private static final int FILL = 0xFF69767E;
    private static final int FILL_DISABLED = 0xFF506066;

    private ButtonArt() {
    }

    /** Button of {@code w} x {@code h} at (x, y). */
    //? if >=26.1 {
    static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered, boolean on) {
    //?} else {
    /*static void draw(GuiGraphics g, int x, int y, int w, int h, boolean enabled, boolean hovered, boolean on) {
    *///?}
        border(g, x, y, w, h, enabled, hovered, !enabled);
        int fill = !enabled ? FILL_DISABLED : on ? ThemeAccent.fill() : FILL;
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GuiTheme.chromeColor(fill));
    }

    /** A recessed track with the handle on the right when enabled. */
    //? if >=26.1 {
    static void drawSwitch(GuiGraphicsExtractor g, int x, int y, boolean on, boolean hovered) {
    //?} else {
    /*static void drawSwitch(GuiGraphics g, int x, int y, boolean on, boolean hovered) {
    *///?}
        border(g, x, y, SWITCH_WIDTH, SWITCH_HEIGHT, true, hovered, false);
        g.fill(x + 2, y + 2, x + SWITCH_WIDTH - 2, y + SWITCH_HEIGHT - 2, GuiTheme.chromeColor(0xFF182229));
        g.fill(x + 3, y + 3, x + SWITCH_WIDTH - 3, y + SWITCH_HEIGHT - 3, GuiTheme.chromeColor(0xFF29363D));
        if (on) {
            g.fill(x + 12, y + 3, x + 20, y + 4, GuiTheme.chromeColor(ThemeAccent.top()));
            g.fill(x + 12, y + 4, x + 20, y + 10, GuiTheme.chromeColor(ThemeAccent.fill()));
            g.fill(x + 12, y + 10, x + 20, y + 11, GuiTheme.chromeColor(ThemeAccent.bottom()));
        }
        int knob = x + (on ? 20 : 3);
        g.fill(knob, y + 3, knob + 8, y + 11, GuiTheme.chromeColor(0xFF3E4A50));
        g.fill(knob + 1, y + 3, knob + 8, y + 10, 0xFFB2BFC5);
        g.fill(knob + 1, y + 3, knob + 7, y + 4, 0xFFF2F5F4);
        g.fill(knob + 1, y + 4, knob + 2, y + 9, 0xFFD3DDE0);
        g.fill(knob + 3, y + 5, knob + 6, y + 6, 0xFFD3DDE0);
    }

    /**
     * Button marked with {@code rgb} (a route color, for example): its light border takes the color while the inside
     * stays slate, a subtle mark instead of a fully painted button.
     */
    //? if >=26.1 {
    static void drawMarked(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered, int rgb) {
    //?} else {
    /*static void drawMarked(GuiGraphics g, int x, int y, int w, int h, boolean enabled, boolean hovered, int rgb) {
    *///?}
        int outline = hovered && enabled ? ThemeAccent.hover() : GuiTheme.chromeColor(OUTLINE);
        frame(g, x, y, w, h, outline);
        frame(g, x + 1, y + 1, w - 2, h - 2, 0xFF000000 | rgb);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GuiTheme.chromeColor(enabled ? FILL : FILL_DISABLED));
    }

    //? if >=26.1 {
    private static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered, boolean sunk) {
    //?} else {
    /*private static void border(GuiGraphics g, int x, int y, int w, int h, boolean enabled, boolean hovered, boolean sunk) {
    *///?}
        int outline = hovered && enabled ? ThemeAccent.hover() : GuiTheme.chromeColor(OUTLINE);
        frame(g, x, y, w, h, outline);
        frame(g, x + 1, y + 1, w - 2, h - 2, GuiTheme.chromeColor(sunk ? BORDER_DISABLED : BORDER));
    }

    /** 1 px ring. */
    //? if >=26.1 {
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
    //?} else {
    /*private static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
    *///?}
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
