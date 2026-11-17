package dimensionalfluxminer.client.screen;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.network.chat.Component;

/** A local color draft, committed only by Apply. */
final class CustomColorWindow extends DfmScreen.Window {
    private static final String KEY = "gui.dimensionalfluxminer.theme.";
    private static final int TRACK_X = 12;
    private static final int TRACK_WIDTH = 176;
    private static final int TRACK_HEIGHT = 9;
    private final float[] hsv = new float[3];
    private int draft;
    private int draggedSlider = -1;
    private String hex = "EF8B32";
    private boolean focused;
    private boolean replaceText;

    CustomColorWindow(DfmScreen<?> screen) {
        super(screen, "custom_color", 200);
    }

    void open() {
        readColor(GuiThemes.customRgb(screen.machinePos()));
        focused = false;
        draggedSlider = -1;
        screen.openWindow(this);
    }

    @Override
    public Component title() {
        return Component.translatable(KEY + "custom.title");
    }

    @Override
    public int height() {
        return 166;
    }

    private void readColor(int rgb) {
        draft = rgb & 0xFFFFFF;
        float hue = hsv[0];
        Color.RGBtoHSB(draft >> 16 & 255, draft >> 8 & 255, draft & 255, hsv);
        if (hsv[1] == 0) {
            hsv[0] = hue;
        }
        hex = String.format(Locale.ROOT, "%06X", draft);
    }

    private static int trackY(int slider) {
        return 61 + slider * 26;
    }

    private int gradient(int slider, float value) {
        return switch (slider) {
            case 0 -> Color.HSBtoRGB(value, 1, 1);
            case 1 -> Color.HSBtoRGB(hsv[0], value, hsv[2]);
            default -> Color.HSBtoRGB(hsv[0], hsv[1], value);
        };
    }

    @Override
    //? if >=26.1 {
    protected void render(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
    //?} else {
    /*protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
    *///?}
        IconColor before = GuiTheme.icons();
        int beforeRgb = GuiTheme.customRgb();
        GuiTheme.setIcons(IconColor.CUSTOM);
        GuiTheme.setCustomRgb(draft);
        try {
            GlassViewport.draw(g, x + 12, y + 25, 146, 18);
            screen.fitted(g, Component.literal("#" + hex + (focused ? "_" : "")), x + 16, y + 30, 138,
                    0xFFFFFFFF, 1.0F, false);
            ColorSwatch.draw(g, x + 170, y + 25, 18, draft);
            String[] labels = {"hue", "saturation", "brightness"};
            for (int slider = 0; slider < hsv.length; slider++) {
                int top = y + trackY(slider);
                int value = Math.round(hsv[slider] * (slider == 0 ? 360 : 100));
                screen.fitted(g, Component.translatable(KEY + "custom." + labels[slider], value), x + TRACK_X, top - 11,
                        TRACK_WIDTH, GuiTheme.heading(), 1.0F, false);
                g.fill(x + TRACK_X, top, x + TRACK_X + TRACK_WIDTH, top + TRACK_HEIGHT, 0xFF1A242B);
                for (int column = 0; column < TRACK_WIDTH - 2; column++) {
                    g.fill(x + TRACK_X + 1 + column, top + 1, x + TRACK_X + 2 + column, top + TRACK_HEIGHT - 1,
                            gradient(slider, column / (float) (TRACK_WIDTH - 3)));
                }
                int knob = x + TRACK_X + 1 + Math.round(hsv[slider] * (TRACK_WIDTH - 3));
                knob = Math.max(x + TRACK_X + 2, Math.min(x + TRACK_X + TRACK_WIDTH - 3, knob));
                g.fill(knob - 2, top - 2, knob + 3, top + TRACK_HEIGHT + 2, 0xFF161A1D);
                g.fill(knob - 1, top - 1, knob + 2, top + TRACK_HEIGHT + 1, 0xFFF2F5F7);
            }
            screen.button(g, x + 12, y + 140, 82, 16, Component.translatable(KEY + "custom.cancel"), true, mouseX, mouseY,
                    () -> { screen.closeWindow(this); return -1; }, null, List::of);
            screen.button(g, x + 106, y + 140, 82, 16, Component.translatable(KEY + "custom.apply"), hex.length() == 6,
                    mouseX, mouseY, () -> {
                        GuiThemes.chooseCustom(screen.machinePos(), draft);
                        screen.closeWindow(this);
                        return -1;
                    }, null, List::of);
        } finally {
            GuiTheme.setIcons(before);
            GuiTheme.setCustomRgb(beforeRgb);
        }
    }

    private void slide(double mouseX) {
        hsv[draggedSlider] = Math.max(0, Math.min(1, (float) ((mouseX - x() - TRACK_X - 1) / (TRACK_WIDTH - 3))));
        draft = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0xFFFFFF;
        hex = String.format(Locale.ROOT, "%06X", draft);
    }

    @Override
    protected void mousePressed(double mouseX, double mouseY, boolean onThis) {
        draggedSlider = -1;
        focused = onThis && mouseX >= x() + 12 && mouseX < x() + 158 && mouseY >= y() + 25 && mouseY < y() + 43;
        replaceText = focused;
        if (!onThis || mouseX < x() + TRACK_X || mouseX >= x() + TRACK_X + TRACK_WIDTH) {
            return;
        }
        for (int slider = 0; slider < hsv.length; slider++) {
            if (mouseY >= y() + trackY(slider) - 2 && mouseY < y() + trackY(slider) + TRACK_HEIGHT + 2) {
                draggedSlider = slider;
                focused = false;
                slide(mouseX);
                return;
            }
        }
    }

    @Override
    protected boolean mouseDragged(double mouseX, double mouseY) {
        if (draggedSlider < 0) {
            return false;
        }
        slide(mouseX);
        return true;
    }

    @Override
    protected boolean mouseReleased() {
        boolean consumed = draggedSlider >= 0;
        draggedSlider = -1;
        return consumed;
    }

    @Override
    protected void onClose() {
        draggedSlider = -1;
        focused = false;
    }

    private void editHex(String value) {
        hex = value.toUpperCase(Locale.ROOT);
        if (hex.length() == 6) {
            readColor(Integer.parseInt(hex, 16));
        }
    }

    @Override
    protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) {
            return false;
        }
        if (keyCode == 259) {
            //? if >=26.1 {
            editHex(replaceText || Minecraft.getInstance().hasControlDown() ? "" : hex.substring(0, Math.max(0, hex.length() - 1)));
            //?} else {
            /*editHex(replaceText || net.minecraft.client.gui.screens.Screen.hasControlDown() ? "" : hex.substring(0, Math.max(0, hex.length() - 1)));
            *///?}
            replaceText = false;
        //? if >=26.1 {
        } else if (keyCode == 65 && Minecraft.getInstance().hasControlDown()) {
        //?} else {
        /*} else if (keyCode == 65 && net.minecraft.client.gui.screens.Screen.hasControlDown()) {
        *///?}
            replaceText = true;
        //? if >=26.1 {
        } else if (keyCode == 86 && Minecraft.getInstance().hasControlDown()) {
        //?} else {
        /*} else if (keyCode == 86 && net.minecraft.client.gui.screens.Screen.hasControlDown()) {
        *///?}
            String pasted = Minecraft.getInstance().keyboardHandler.getClipboard().replace("#", "").trim();
            if (pasted.matches("[0-9a-fA-F]{6}")) {
                editHex(pasted);
                replaceText = true;
            }
        } else if (keyCode == 257 || keyCode == 335) {
            focused = false;
        }
        return true;
    }

    @Override
    protected boolean charTyped(char c, int modifiers) {
        if (!focused || Character.digit(c, 16) < 0) {
            return focused;
        }
        if (replaceText) {
            hex = "";
            replaceText = false;
        }
        if (hex.length() < 6) {
            editHex(hex + c);
        }
        return true;
    }
}
