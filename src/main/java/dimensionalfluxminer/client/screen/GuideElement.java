package dimensionalfluxminer.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * One block of a guide chapter, laid out top to bottom in the scrolling page. Coordinates are absolute; the
 * mouse position is outside the page ({@code -1}) whenever the pointer is not over its visible area.
 */
interface GuideElement {
    /** Height for the given page width. */
    int height(GuideScreen screen, int width);

    void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY);

    /** A click on the element (x, y: its top-left corner); true when handled. */
    default boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
        return false;
    }

    /** A drag that started on this element; true when handled. */
    default boolean drag(GuideScreen screen, double dragX, double dragY) {
        return false;
    }
}
