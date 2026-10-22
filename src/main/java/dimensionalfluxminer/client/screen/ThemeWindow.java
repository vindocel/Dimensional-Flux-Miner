package dimensionalfluxminer.client.screen;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Settings window of a machine screen, only on this player's computer:
 * <ul>
 *     <li>the color of the screen, light or dark, and the color of the icons, blue or purple, each picked like a
 *     family of the Miner's filter; the "reuse" switch beside them (one switch shared by every machine) applies both
 *     colors to all machines;</li>
 *     <li>the volume of this machine's sound, 0 to 100 % in steps of 10 (100 % until changed; 0 is mute), always this
 *     machine's own; not on the Dimensional Tanks, which make no sound.</li>
 * </ul>
 */
final class ThemeWindow extends DfmScreen.Window {
    private static final int CELL = 18;
    private static final int CELL_GAP = 16;
    private static final int COLOR_LABEL_Y = 20;
    private static final int COLOR_CELLS_Y = 32;
    private static final int ICON_LABEL_Y = 64;
    private static final int ICON_CELLS_Y = 76;
    private static final int VOLUME_LABEL_Y = 108;
    private static final int VOLUME_Y = 120;
    private static final int STEPS = 10;
    private static final int SEGMENT_W = 8;
    private static final int SEGMENT_H = 8;
    private static final int SEGMENT_GAP = 1;
    private static final int VOLUME_OFF = 0xFF1E2631;

    ThemeWindow(DfmScreen<?> screen) {
        super(screen, "theme", 156);
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
        return hasSound() ? VOLUME_Y + DfmScreen.SMALL_BUTTON + 8 : ICON_CELLS_Y + CELL + 16;
    }

    @Override
    protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        var pos = screen.machinePos();
        int rowWidth = 3 * CELL + 2 * CELL_GAP;
        int cellsX = x + (width - rowWidth) / 2;
        int secondX = cellsX + CELL + CELL_GAP;

        // Row titles centered over their two choices (the reuse switch sits in the third column, between the rows).
        int pairX = cellsX - CELL_GAP / 2;
        int pairWidth = 2 * CELL + 2 * CELL_GAP;

        // Screen color: Light and Dark.
        screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.theme.color"), pairX, y + COLOR_LABEL_Y, pairWidth,
                GuiTheme.subheading(), 1.0F, true);
        boolean dark = GuiThemes.dark(pos);
        cell(g, cellsX, y + COLOR_CELLS_Y, GuiTheme.LIGHT_PANEL, !dark, "gui.dimensionalfluxminer.theme.light", mouseX, mouseY,
                () -> GuiThemes.choose(pos, false));
        cell(g, secondX, y + COLOR_CELLS_Y, GuiTheme.DARK_PANEL, dark, "gui.dimensionalfluxminer.theme.dark", mouseX, mouseY,
                () -> GuiThemes.choose(pos, true));

        // Icon color: Blue and Purple.
        screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.theme.icons"), pairX, y + ICON_LABEL_Y, pairWidth,
                GuiTheme.subheading(), 1.0F, true);
        boolean purple = GuiThemes.purple(pos);
        cell(g, cellsX, y + ICON_CELLS_Y, GuiTheme.BLUE_ICONS, !purple, "gui.dimensionalfluxminer.theme.blue", mouseX, mouseY,
                () -> GuiThemes.chooseAccent(pos, false));
        cell(g, secondX, y + ICON_CELLS_Y, GuiTheme.PURPLE_ICONS, purple, "gui.dimensionalfluxminer.theme.purple", mouseX, mouseY,
                () -> GuiThemes.chooseAccent(pos, true));

        // The switch that applies both colors to every machine, between the two rows.
        boolean all = GuiThemes.applyToAll();
        int reuseX = cellsX + 2 * (CELL + CELL_GAP);
        int reuseY = y + (COLOR_CELLS_Y + ICON_CELLS_Y) / 2;
        screen.imageButton(g, reuseX, reuseY, CELL, GuiIcon.REUSE, true, all, mouseX, mouseY, () -> {
            GuiThemes.setApplyToAll(pos, !all);
            return -1;
        }, null, () -> List.of(
                Component.translatable("gui.dimensionalfluxminer.theme.apply_all", MachineText.onOff(all)),
                Component.translatable(all ? "gui.dimensionalfluxminer.theme.apply_all.on"
                        : "gui.dimensionalfluxminer.theme.apply_all.off").withStyle(ChatFormatting.GRAY)));
        screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.theme.apply_all.short"), reuseX - CELL_GAP / 2,
                reuseY + CELL + 3, CELL + CELL_GAP, all ? GuiTheme.heading() : GuiTheme.subheading(), 0.8F, true);

        if (hasSound()) {
            volume(g, x, y, mouseX, mouseY);
        }
    }

    private void label(GuiGraphics g, int x, int y, Component text) {
        screen.fitted(g, text, x + 6, y, width - 12, GuiTheme.subheading(), 1.0F, true);
    }

    /** Volume of this machine's sound: - [10 segments] + */
    private void volume(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
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
        screen.button(g, left, y + VOLUME_Y, DfmScreen.SMALL_BUTTON, DfmScreen.SMALL_BUTTON, Component.literal("-"), volume > 0,
                mouseX, mouseY, () -> {
                    GuiThemes.setVolume(pos, volume - 100 / STEPS);
                    return -1;
                }, null, () -> help);
        int lit = GuiTheme.purple() ? GuiTheme.PURPLE_ICONS : DfmScreen.SCREEN_TEXT;
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
        screen.button(g, barX + barWidth + 4, y + VOLUME_Y, DfmScreen.SMALL_BUTTON, DfmScreen.SMALL_BUTTON, Component.literal("+"),
                volume < 100, mouseX, mouseY, () -> {
                    GuiThemes.setVolume(pos, volume + 100 / STEPS);
                    return -1;
                }, null, () -> help);
    }

    /** One choice, drawn like a filter family: a slot holding its color, checked when selected. */
    private void cell(GuiGraphics g, int cx, int cy, int color, boolean selected, String nameKey, int mouseX, int mouseY, Runnable choose) {
        screen.slot(g, selected ? DfmScreen.SlotKind.OUTPUT : DfmScreen.SlotKind.NORMAL, cx, cy);
        g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, color);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        if (selected) {
            g.blitSprite(DfmScreen.CHECK, cx, cy, CELL, CELL);
        }
        if (screen.hovered(cx + 1, cy + 1, 16, 16, mouseX, mouseY)) {
            g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x80FFFFFF);
        }
        g.pose().popPose();
        Component name = Component.translatable(nameKey);
        screen.fitted(g, name, cx - CELL_GAP / 2, cy + CELL + 3, CELL + CELL_GAP, selected ? GuiTheme.heading() : GuiTheme.subheading(),
                0.8F, true);
        screen.hotspot(cx, cy, CELL, CELL, () -> {
            choose.run();
            return -1;
        }, null, () -> List.of(name), 0);
    }
}
