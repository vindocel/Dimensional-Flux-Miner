package dimensionalfluxminer.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The read-out windows of the screens (status of the Miner, contents of the Dimensional Tanks, automation lines of the
 * side windows...), drawn in code as a glass sunk into the panel: a 1 px inset edge (shade on top and left, light on
 * the bottom and right, rounded corners), a dark rim, and a glass lighter towards the bottom with a soft shadow under
 * the top edge and a faint diagonal reflection. The glass follows the icon color ({@link IconColor#glassTop()}):
 * navy with white and blue icons, dark purple with purple icons. The edge is darkened with the dark look like the
 * panel; the glass is already dark and is never darkened. {@link #led} draws the small status lamp of a window.
 */
final class GlassViewport {
    /** Inset edge on the panel (neutral grays, darkened with the dark look). */
    private static final int EDGE_SHADE = 0xFF6E767F;
    private static final int EDGE_LIGHT = 0xFFF4F6F7;
    private static final int EDGE_CORNER = 0xFF9EA4AA;
    /** Outline of {@link #button}, the same as the buttons'. */
    private static final int BUTTON_OUTLINE = 0xFF1A242B;
    /** Light of the tab metal on the bottom and right of {@link #button}'s sunk bevel. */
    private static final int BUTTON_LIGHT = 0xFFB6CCDB;
    /** Reflection: added to the glass on its band (0xRRGGBB per step of the band's 2 rows). */
    private static final int[] SHINE = {0x07090B, 0x05070A};
    /** Height of the glass from which the reflection is drawn (thin windows stay plain). */
    private static final int SHINE_FROM = 10;
    /** The glass starts this far inside the window. */
    static final int INSET = 2;
    /** Side of the status lamp. */
    static final int LED = 5;

    private GlassViewport() {
    }

    /** Window of {@code w} x {@code h} at (x, y); the glass starts {@value #INSET} px inside. */
    static void draw(GuiGraphics g, int x, int y, int w, int h) {
        int shade = GuiTheme.chromeColor(EDGE_SHADE);
        int light = GuiTheme.chromeColor(EDGE_LIGHT);
        int corner = GuiTheme.chromeColor(EDGE_CORNER);
        // Inset edge: the shade falls on the top and left, the light catches the bottom and right.
        g.fill(x + 1, y, x + w - 1, y + 1, shade);
        g.fill(x, y + 1, x + 1, y + h - 1, shade);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, light);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, light);
        pixel(g, x + w - 1, y, corner);
        pixel(g, x, y + h - 1, corner);
        IconColor icons = GuiTheme.icons();
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000 | icons.glassRim());
        glass(g, x + 2, y + 2, w - 4, h - 4, false);
    }

    /**
     * Square glass window shaped like a button of {@code size} x {@code size} (for the Problems tab), set deep into the
     * tab: the square dark outline of the buttons ({@link ButtonArt}), a sunk bevel (very dark on the top and left, the
     * light of the tab metal on the bottom and right, meeting in a middle tone at the two other corners so the ring
     * reads as one piece) and a plain glass with a deep shadow under its top and left edges.
     */
    static void button(GuiGraphics g, int x, int y, int size) {
        int outline = GuiTheme.chromeColor(BUTTON_OUTLINE);
        g.fill(x, y, x + size, y + size, outline);
        int deep = mix(GuiTheme.icons().glassRim(), 0x000000, 0.55F);
        int light = GuiTheme.chromeColor(BUTTON_LIGHT) & 0xFFFFFF;
        g.fill(x + 1, y + 1, x + size - 2, y + 2, 0xFF000000 | deep);
        g.fill(x + 1, y + 2, x + 2, y + size - 2, 0xFF000000 | deep);
        g.fill(x + 2, y + size - 2, x + size - 1, y + size - 1, 0xFF000000 | light);
        g.fill(x + size - 2, y + 2, x + size - 1, y + size - 2, 0xFF000000 | light);
        int middle = 0xFF000000 | mix(deep, light, 0.5F);
        pixel(g, x + size - 2, y + 1, middle);
        pixel(g, x + 1, y + size - 2, middle);
        glass(g, x + 2, y + 2, size - 4, size - 4, true);
    }

    /**
     * The glass of {@code w} x {@code h} at (x, y). Its top edge casts a soft shadow on it, or with {@code deep} a deep
     * one, two rows under the top and one column along the left.
     */
    private static void glass(GuiGraphics g, int gx, int gy, int gw, int gh, boolean deep) {
        IconColor icons = GuiTheme.icons();
        if (gw <= 0 || gh <= 0) {
            return;
        }
        for (int row = 0; row < gh; row++) {
            int color = glass(icons, row, gh);
            if (row == 0) {
                color = mix(icons.glassRim(), color, deep ? 0.25F : 0.7F);
            } else if (row == 1 && deep) {
                color = mix(icons.glassRim(), color, 0.6F);
            }
            g.fill(gx, gy + row, gx + gw, gy + row + 1, 0xFF000000 | color);
        }
        if (deep) {
            for (int row = 1; row < gh; row++) {
                pixel(g, gx, gy + row, 0xFF000000 | mix(icons.glassRim(), glass(icons, row, gh), 0.45F));
            }
        }
        if (gh >= SHINE_FROM && !deep) {
            // The reflection rises 1 row every 2 columns, starting a little below the middle on the left.
            int start = gh * 3 / 5;
            for (int col = 0; col < gw; col++) {
                int top = start - col / 2;
                for (int step = 0; step < SHINE.length; step++) {
                    int row = top + step;
                    if (row >= 1 && row < gh) {
                        pixel(g, gx + col, gy + row, 0xFF000000 | add(glass(icons, row, gh), SHINE[step]));
                    }
                }
            }
        }
        if (deep) {
            return;
        }
        // Rounded inner corners: the glass corners take the rim's tone halfway.
        int top = mix(icons.glassRim(), glass(icons, 0, gh), 0.55F);
        int bottom = mix(icons.glassRim(), glass(icons, gh - 1, gh), 0.7F);
        pixel(g, gx, gy, 0xFF000000 | top);
        pixel(g, gx + gw - 1, gy, 0xFF000000 | top);
        pixel(g, gx, gy + gh - 1, 0xFF000000 | bottom);
        pixel(g, gx + gw - 1, gy + gh - 1, 0xFF000000 | bottom);
    }

    /**
     * Status lamp of {@value #LED} x {@value #LED} at (x, y), meant for the glass (see {@link #lamp}). With
     * {@code blinkMs} above 0 it blinks: lit, then off for that long each.
     */
    static void led(GuiGraphics g, int x, int y, int rgb, long blinkMs) {
        boolean lit = blinkMs <= 0 || (Util.getMillis() / blinkMs) % 2 == 0;
        lamp(g, x, y, LED, lit ? rgb : -1);
    }

    /**
     * Round lamp of {@code size} x {@code size} at (x, y) on the glass: lit in {@code rgb} with a darker ring and a
     * light glint, or, with {@code rgb} -1, off: a faint lighter patch of the glass itself, like a light waiting to
     * come on (never a black hole).
     */
    static void lamp(GuiGraphics g, int x, int y, int size, int rgb) {
        IconColor icons = GuiTheme.icons();
        int ring;
        int inside;
        int glint;
        if (rgb == -1) {
            ring = add(icons.glassBottom(), 0x101418);
            inside = add(icons.glassBottom(), 0x080A0C);
            glint = add(icons.glassBottom(), 0x14181C);
        } else {
            inside = rgb & 0xFFFFFF;
            ring = mix(inside, icons.glassRim(), 0.45F);
            glint = mix(inside, 0xFFFFFF, 0.5F);
        }
        g.fill(x + 1, y, x + size - 1, y + size, 0xFF000000 | ring);
        g.fill(x, y + 1, x + size, y + size - 1, 0xFF000000 | ring);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF000000 | inside);
        pixel(g, x + 1, y + 1, 0xFF000000 | glint);
    }

    private static void pixel(GuiGraphics g, int x, int y, int argb) {
        g.fill(x, y, x + 1, y + 1, argb);
    }

    private static int glass(IconColor icons, int row, int gh) {
        return mix(icons.glassTop(), icons.glassBottom(), gh <= 1 ? 0 : row / (float) (gh - 1));
    }

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int gr = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (gr << 8) | bl;
    }

    private static int add(int a, int b) {
        int r = Math.min(255, ((a >> 16) & 0xFF) + ((b >> 16) & 0xFF));
        int gr = Math.min(255, ((a >> 8) & 0xFF) + ((b >> 8) & 0xFF));
        int bl = Math.min(255, (a & 0xFF) + (b & 0xFF));
        return (r << 16) | (gr << 8) | bl;
    }
}
