package dimensionalfluxminer.client.config;



import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** Shared drawing primitives for the full-screen configuration interface. */
public final class ConfigStyle {
    public static final int ACCENT = 0xFF6FD9E8, TEXT = 0xFFE0E0E0, MUTED = 0xFF9A9A9A;
    public static final int MARGIN = 6, ROW = 24, CONTROL = 20, RESET = 20;

    private ConfigStyle() {}

    //? if >=26.1 {
    public static void fill(GuiGraphicsExtractor g, int x, int y, int right, int bottom, int color) {
    //?} else {
    /*public static void fill(GuiGraphics g, int x, int y, int right, int bottom, int color) {
    *///?}
        ConfigPaint.fill(g, x, y, right, bottom, color);
    }

    public static Button button(Component label, int x, int y, int width, int height, Button.OnPress action) {
        return button(label, x, y, width, height, action, () -> false);
    }

    public static Button button(Component label, int x, int y, int width, int height, Button.OnPress action, BooleanSupplier selected) {
        return new FlatButton(Button.builder(label, action).bounds(x, y, width, height), selected);
    }

    private static final class FlatButton extends Button {
        private final BooleanSupplier selected;
        FlatButton(Builder builder, BooleanSupplier selected) { super(builder); this.selected = selected; }
        @Override
        //? if >=26.1 {
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        //?} else {
        /*protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        *///?}
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            fill(g, x, y, x + w, y + h, 0xFF000000);
            fill(g, x + 1, y + 1, x + w - 1, y + h - 1,
                    !active ? 0x88303030 : selected.getAsBoolean() ? 0xE02A2A32 : isHoveredOrFocused() ? 0xD038383E : 0xB0202026);
            if (selected.getAsBoolean()) fill(g, x + 1, y + h - 1, x + w - 1, y + h, ACCENT);
            var font = Minecraft.getInstance().font;
            int textWidth = Math.min(font.width(getMessage()), w - 8);
            ConfigPaint.scrollingText(g, font, getMessage().getString(), x + (w - textWidth) / 2, y + (h - 8) / 2,
                    textWidth, active ? TEXT : MUTED);
        }
    }
}
