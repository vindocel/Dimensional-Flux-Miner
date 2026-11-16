package dimensionalfluxminer.client.screen;

import java.util.List;
import net.minecraft.ChatFormatting;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.network.chat.Component;

/** Local appearance preferences and the volume of this machine. */
final class ThemeWindow extends DfmScreen.Window {
    private static final String THEME = "gui.dimensionalfluxminer.theme.";
    private static final int INSET = 10;
    private static final int GAP = 4;
    private static final int CHOICE_HEIGHT = 22;
    private static final int SCREEN_Y = 32;
    private static final int ICONS_Y = 70;
    private static final int APPLY_Y = 129;
    private static final int VOLUME_LABEL_Y = 151;
    private static final int VOLUME_Y = 163;
    private static final int STEPS = 10;
    private static final int SEGMENT_W = 8;
    private static final int SEGMENT_H = 8;
    private static final int SEGMENT_GAP = 1;
    private static final int VOLUME_OFF = 0xFF1E2631;
    private final CustomColorWindow colorPicker;

    ThemeWindow(DfmScreen<?> screen) {
        super(screen, "theme", 200);
        colorPicker = new CustomColorWindow(screen);
    }

    @Override
    public Component title() {
        return Component.translatable("gui.dimensionalfluxminer.theme");
    }

    /** Machines that make sound have the volume; the Dimensional Tanks are silent. */
    private boolean hasSound() {
        return !(screen instanceof TankScreen);
    }

    @Override
    public int height() {
        return hasSound() ? VOLUME_Y + DfmScreen.SMALL_BUTTON + 8 : APPLY_Y + ButtonArt.SWITCH_HEIGHT + 8;
    }

    @Override
    //? if >=26.1 {
    protected void render(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
    //?} else {
    /*protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
    *///?}
        var pos = screen.machinePos();
        int left = x + INSET;
        int contentWidth = width - 2 * INSET;
        screen.text(g, Component.translatable(THEME + "color"), left, y + SCREEN_Y - 12, GuiTheme.heading());
        int themeWidth = (contentWidth - GAP) / 2;
        boolean dark = GuiThemes.dark(pos);
        choice(g, left, y + SCREEN_Y, themeWidth, GuiTheme.LIGHT_PANEL, !dark, "light", mouseX, mouseY,
                () -> GuiThemes.choose(pos, false));
        choice(g, left + themeWidth + GAP, y + SCREEN_Y, themeWidth, GuiTheme.DARK_PANEL, dark, "dark", mouseX, mouseY,
                () -> GuiThemes.choose(pos, true));

        screen.text(g, Component.translatable(THEME + "icons"), left, y + ICONS_Y - 12, GuiTheme.heading());
        IconColor icons = GuiThemes.icons(pos);
        IconColor[] colors = IconColor.values();
        int iconWidth = (contentWidth - GAP) / 2;
        for (IconColor color : colors) {
            int offset = color.ordinal() % 2 * (iconWidth + GAP);
            choice(g, left + offset, y + ICONS_Y + color.ordinal() / 2 * (CHOICE_HEIGHT + GAP), iconWidth,
                    color.preview(), color == icons, color.id(), mouseX, mouseY, () -> {
                        if (color == IconColor.CUSTOM) {
                            colorPicker.open();
                        } else {
                            GuiThemes.chooseAccent(pos, color);
                        }
                    });
        }

        boolean all = GuiThemes.applyToAll();
        int switchX = left + contentWidth - ButtonArt.SWITCH_WIDTH;
        int switchY = y + APPLY_Y;
        screen.fitted(g, Component.translatable(THEME + "apply_all.label"), left, switchY + 3,
                switchX - left - GAP, GuiTheme.heading(), 1.0F, false);
        ButtonArt.drawSwitch(g, switchX, switchY, all,
                screen.hovered(switchX, switchY, ButtonArt.SWITCH_WIDTH, ButtonArt.SWITCH_HEIGHT, mouseX, mouseY));
        screen.buttonHotspot(switchX, switchY, ButtonArt.SWITCH_WIDTH, ButtonArt.SWITCH_HEIGHT, () -> {
            GuiThemes.setApplyToAll(pos, !all);
            return -1;
        }, null, () -> List.of(
                Component.translatable(THEME + "apply_all", MachineText.onOff(all)),
                Component.translatable(THEME + (all ? "apply_all.on" : "apply_all.off")).withStyle(ChatFormatting.GRAY)));

        if (hasSound()) {
            volume(g, x, y, mouseX, mouseY);
        }
    }

    //? if >=26.1 {
    private void label(GuiGraphicsExtractor g, int x, int y, Component text) {
    //?} else {
    /*private void label(GuiGraphics g, int x, int y, Component text) {
    *///?}
        screen.fitted(g, text, x + 6, y, width - 12, GuiTheme.subheading(), 1.0F, true);
    }

    /** Volume of this machine's sound: - [10 segments] + */
    //? if >=26.1 {
    private void volume(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
    //?} else {
    /*private void volume(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
    *///?}
        var pos = screen.machinePos();
        int volume = GuiThemes.volume(pos);
        Component level = volume == 0 ? Component.translatable("gui.dimensionalfluxminer.theme.volume.mute")
                : Component.literal(volume + "%");
        label(g, x, y + VOLUME_LABEL_Y, Component.translatable("gui.dimensionalfluxminer.theme.volume", level));
        int barWidth = STEPS * SEGMENT_W + (STEPS - 1) * SEGMENT_GAP;
        int total = DfmScreen.SMALL_BUTTON + 4 + barWidth + 4 + DfmScreen.SMALL_BUTTON;
        int left = x + (width - total) / 2;
        int barX = left + DfmScreen.SMALL_BUTTON + 4;
        int barY = y + VOLUME_Y + (DfmScreen.SMALL_BUTTON - SEGMENT_H) / 2;
        List<Component> help = List.of(Component.translatable("gui.dimensionalfluxminer.theme.volume", level),
                Component.translatable("gui.dimensionalfluxminer.theme.volume.help").withStyle(ChatFormatting.GRAY));
        screen.imageButton(g, left, y + VOLUME_Y, DfmScreen.SMALL_BUTTON, GuiIcon.MINUS, volume > 0, false,
                mouseX, mouseY, () -> {
                    GuiThemes.setVolume(pos, volume - 100 / STEPS);
                    return -1;
                }, null, () -> help);
        int lit = GuiTheme.icons().volumeLit();
        for (int i = 0; i < STEPS; i++) {
            int sx = barX + i * (SEGMENT_W + SEGMENT_GAP);
            int step = (i + 1) * 100 / STEPS;
            g.fill(sx, barY, sx + SEGMENT_W, barY + SEGMENT_H, step <= volume ? lit : VOLUME_OFF);
            // Clicking a segment sets that volume; clicking the lit last one again mutes.
            screen.hotspot(sx, barY, SEGMENT_W, SEGMENT_H, () -> {
                GuiThemes.setVolume(pos, step == volume && step == 100 / STEPS ? 0 : step);
                return -1;
            }, null, () -> help, 0);
        }
        screen.imageButton(g, barX + barWidth + 4, y + VOLUME_Y, DfmScreen.SMALL_BUTTON, GuiIcon.PLUS,
                volume < 100, false, mouseX, mouseY, () -> {
                    GuiThemes.setVolume(pos, volume + 100 / STEPS);
                    return -1;
                }, null, () -> help);
    }

    /** A framed button with a color preview and its name in the same click target. */
    //? if >=26.1 {
    private void choice(GuiGraphicsExtractor g, int x, int y, int w, int color, boolean selected, String nameKey,
    //?} else {
    /*private void choice(GuiGraphics g, int x, int y, int w, int color, boolean selected, String nameKey,
    *///?}
                        int mouseX, int mouseY, Runnable choose) {
        ButtonArt.draw(g, x, y, w, CHOICE_HEIGHT, true, screen.hovered(x, y, w, CHOICE_HEIGHT, mouseX, mouseY), selected);
        g.fill(x + 3, y + 3, x + 19, y + 19, 0xFF1A1A1A);
        g.fill(x + 4, y + 4, x + 18, y + 18, color);
        if (selected) {
            //? if >=26.1 {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, DfmScreen.CHECK, x + 4, y + 4, 14, 14);
            //?} else {
            /*g.blitSprite(DfmScreen.CHECK, x + 4, y + 4, 14, 14);
            *///?}
        }
        Component name = Component.translatable(THEME + nameKey);
        screen.fitted(g, name, x + 22, y + 7, w - 25, 0xFFFFFFFF, 1.0F, false);
        screen.buttonHotspot(x, y, w, CHOICE_HEIGHT, () -> {
            choose.run();
            return -1;
        }, null, () -> List.of(name));
    }
}
