package dimensionalfluxminer.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Every button of the machine screens, drawn with plain fills: dark outline (cyan when hovered, purple with purple
 * icons, see {@link IconColor#hoverOutline}), light border and a slate inside. A disabled button and a switched-on
 * one (toggle on, selected tab) are both gray and sunk: gray border and dark inside; the switched-on one still reacts
 * to the mouse. In the dark look the neutral parts follow {@link GuiTheme#chromeColor}; colored parts keep their color.
 */
final class ButtonArt {
    private static final int OUTLINE = 0xFF1A242B;
    private static final int BORDER = 0xFFE8EDEB;
    private static final int BORDER_DISABLED = 0xFF879399;
    private static final int FILL = 0xFF69767E;
    private static final int FILL_DISABLED = 0xFF506066;

    private ButtonArt() {
    }

    /** Button of {@code w} x {@code h} at (x, y). */
    static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered, boolean on) {
        border(g, x, y, w, h, enabled, hovered, !enabled || on);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GuiTheme.chromeColor(!enabled || on ? FILL_DISABLED : FILL));
    }

    /**
     * Button marked with {@code rgb} (a route color, for example): its light border takes the color while the inside
     * stays slate, a subtle mark instead of a fully painted button.
     */
    static void drawMarked(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered, int rgb) {
        int outline = hovered && enabled ? GuiTheme.icons().hoverOutline() : GuiTheme.chromeColor(OUTLINE);
        frame(g, x, y, w, h, outline);
        frame(g, x + 1, y + 1, w - 2, h - 2, 0xFF000000 | rgb);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, GuiTheme.chromeColor(enabled ? FILL : FILL_DISABLED));
    }

    private static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hovered, boolean sunk) {
        int outline = hovered && enabled ? GuiTheme.icons().hoverOutline() : GuiTheme.chromeColor(OUTLINE);
        frame(g, x, y, w, h, outline);
        frame(g, x + 1, y + 1, w - 2, h - 2, GuiTheme.chromeColor(sunk ? BORDER_DISABLED : BORDER));
    }

    /** 1 px ring. */
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
