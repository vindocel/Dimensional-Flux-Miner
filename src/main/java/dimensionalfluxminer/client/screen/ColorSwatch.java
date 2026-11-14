package dimensionalfluxminer.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A color sample inside the same sunken bevel as the tank code fields. */
final class ColorSwatch {
    private ColorSwatch() {
    }

    static void draw(GuiGraphicsExtractor g, int x, int y, int size, int rgb) {
        int dark = GuiTheme.gray(0xFF373737);
        int light = GuiTheme.gray(0xFFFFFFFF);
        int corner = GuiTheme.gray(0xFF8B8B8B);
        g.fill(x, y, x + size - 1, y + 1, dark);
        g.fill(x, y, x + 1, y + size - 1, dark);
        g.fill(x + 1, y + size - 1, x + size, y + size, light);
        g.fill(x + size - 1, y + 1, x + size, y + size, light);
        g.fill(x + size - 1, y, x + size, y + 1, corner);
        g.fill(x, y + size - 1, x + 1, y + size, corner);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF000000 | rgb);
    }
}
