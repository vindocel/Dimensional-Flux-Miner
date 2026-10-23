package dimensionalfluxminer.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Brushed-metal frame drawn in code around panels and windows: a 4 px band outside the panel (dark outline with
 * chamfered corners, light bevel on the top and left, shade on the bottom and right, steel body with a rivet in
 * each corner) and a soft shadow under it. Side tabs are ears of the same metal that grow out of the band, so the
 * frame and its tabs read as one piece; a tab's color is a thin light ring around its button, never the metal.
 */
final class MetalFrame {
    /** Width of the band outside the panel. */
    static final int BAND = 4;

    private static final int OUTLINE = 0xFF0E141F;
    private static final int LIGHT_TOP = 0xFFB6CCDB;
    private static final int LIGHT_SIDE = 0xFF7D90A3;
    private static final int SHADE = 0xFF2E3644;
    private static final int BODY = 0xFF4E5A6B;
    /** Line between the band and the panel: shade on the top and left, light on the bottom and right. */
    private static final int INNER = 0xFF2A323D;
    private static final int INNER_LIGHT = 0xFF6D7C8E;
    /** Panel color of the light look (the dark look takes it to the dark palette). */
    private static final int PANEL = 0xFFBEBEBE;
    private static final int RIVET_LIGHT = 0xFFC9D6E0;
    private static final int RIVET_MID = 0xFF6D7C8E;
    private static final int RIVET_DARK = 0xFF1E2631;
    private static final int SHADOW = 0x66000000;
    private static final int HAZARD_YELLOW = 0xFFF2C21B;
    private static final int HAZARD_BLACK = 0xFF16181C;
    private static final int HAZARD_DASH = 3;

    private MetalFrame() {
    }

    /** The panel at (x, y, w, h) with the band around it. */
    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int x0 = x - BAND;
        int y0 = y - BAND;
        int x1 = x + w + BAND;
        int y1 = y + h + BAND;
        // Shadow under the bottom edge only, tapered like the chamfered corners so it never shows at the sides.
        g.fill(x0 + 2, y1, x1 - 2, y1 + 1, SHADOW);
        g.fill(x0 + 3, y1 + 1, x1 - 3, y1 + 2, SHADOW);
        g.fill(x0 + 2, y0, x1 - 2, y0 + 1, OUTLINE);
        g.fill(x0 + 2, y1 - 1, x1 - 2, y1, OUTLINE);
        g.fill(x0, y0 + 2, x0 + 1, y1 - 2, OUTLINE);
        g.fill(x1 - 1, y0 + 2, x1, y1 - 2, OUTLINE);
        pixel(g, x0 + 1, y0 + 1, OUTLINE);
        pixel(g, x1 - 2, y0 + 1, OUTLINE);
        pixel(g, x0 + 1, y1 - 2, OUTLINE);
        pixel(g, x1 - 2, y1 - 2, OUTLINE);
        g.fill(x0 + 2, y0 + 1, x1 - 2, y0 + 2, LIGHT_TOP);
        g.fill(x0 + 1, y0 + 2, x0 + 2, y1 - 2, LIGHT_SIDE);
        g.fill(x0 + 2, y1 - 2, x1 - 2, y1 - 1, SHADE);
        g.fill(x1 - 2, y0 + 2, x1 - 1, y1 - 2, SHADE);
        g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, BODY);
        rivet(g, x0 + 2, y0 + 2);
        rivet(g, x1 - 4, y0 + 2);
        rivet(g, x0 + 2, y1 - 4);
        rivet(g, x1 - 4, y1 - 4);
        // Flat panel (no vanilla bevel), set into the band by one line: shade on the top and left, light on the
        // bottom and right.
        g.fill(x, y, x + w, y + h, GuiTheme.gray(PANEL));
        g.fill(x, y, x + w, y + 1, INNER);
        g.fill(x, y, x + 1, y + h, INNER);
        g.fill(x, y + h - 1, x + w, y + h, INNER_LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, INNER_LIGHT);
    }

    /**
     * Tab ear of {@code size} wide and {@code height} tall at (x, y): on the left side it ends on the panel's left
     * edge, on the right side it starts on the panel's right edge; the side next to the panel opens into the band. Tabs
     * stacked next to each other share one tall ear.
     */
    static void ear(GuiGraphicsExtractor g, int x, int y, int size, int height, boolean leftSide) {
        int light = LIGHT_TOP;
        int side = leftSide ? LIGHT_SIDE : SHADE;
        int shade = SHADE;
        int body = BODY;
        int last = height - 1;
        // Columns counted from the outer side; the band itself takes the last BAND columns.
        int open = size - BAND;
        column(g, x, size, leftSide, 0, y + 2, y + last - 1, OUTLINE);
        row(g, x, size, leftSide, 2, open, y, OUTLINE);
        row(g, x, size, leftSide, 2, open, y + last, OUTLINE);
        column(g, x, size, leftSide, 1, y + 1, y + 2, OUTLINE);
        column(g, x, size, leftSide, 1, y + last - 1, y + last, OUTLINE);
        column(g, x, size, leftSide, 1, y + 2, y + last - 1, side);
        row(g, x, size, leftSide, 2, open + 1, y + 1, light);
        row(g, x, size, leftSide, 2, open + 1, y + last - 1, shade);
        for (int r = y + 2; r < y + last - 1; r++) {
            row(g, x, size, leftSide, 2, open + 1, r, body);
        }
    }

    /**
     * One pixel light ring set into the metal around a button at (x, y, size): the tab's color, or yellow and black
     * hazard dashes when {@code hazard} is set.
     */
    static void ring(GuiGraphicsExtractor g, int x, int y, int size, int color, boolean hazard) {
        int x0 = x - 1;
        int y0 = y - 1;
        int span = size + 2;
        if (!hazard) {
            g.fill(x0, y0, x0 + span, y0 + 1, color);
            g.fill(x0, y0 + span - 1, x0 + span, y0 + span, color);
            g.fill(x0, y0, x0 + 1, y0 + span, color);
            g.fill(x0 + span - 1, y0, x0 + span, y0 + span, color);
            return;
        }
        // Dashes of HAZARD_DASH pixels running around the ring.
        int perimeter = 4 * (span - 1);
        for (int i = 0; i < perimeter; i++) {
            int px;
            int py;
            if (i < span - 1) {
                px = x0 + i;
                py = y0;
            } else if (i < 2 * (span - 1)) {
                px = x0 + span - 1;
                py = y0 + i - (span - 1);
            } else if (i < 3 * (span - 1)) {
                px = x0 + span - 1 - (i - 2 * (span - 1));
                py = y0 + span - 1;
            } else {
                px = x0;
                py = y0 + span - 1 - (i - 3 * (span - 1));
            }
            pixel(g, px, py, (i / HAZARD_DASH) % 2 == 0 ? HAZARD_YELLOW : HAZARD_BLACK);
        }
    }

    private static void row(GuiGraphicsExtractor g, int x, int size, boolean leftSide, int from, int to, int y, int color) {
        int a = leftSide ? x + from : x + size - 1 - to;
        int b = leftSide ? x + to : x + size - 1 - from;
        g.fill(a, y, b + 1, y + 1, color);
    }

    private static void column(GuiGraphicsExtractor g, int x, int size, boolean leftSide, int col, int fromY, int toY, int color) {
        int c = leftSide ? x + col : x + size - 1 - col;
        g.fill(c, fromY, c + 1, toY, color);
    }

    private static void rivet(GuiGraphicsExtractor g, int x, int y) {
        pixel(g, x, y, RIVET_LIGHT);
        pixel(g, x + 1, y, RIVET_MID);
        pixel(g, x, y + 1, RIVET_MID);
        pixel(g, x + 1, y + 1, RIVET_DARK);
    }

    private static void pixel(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
    }

}
