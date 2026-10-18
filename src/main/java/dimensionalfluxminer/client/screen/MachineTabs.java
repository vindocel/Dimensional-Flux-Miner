package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.RedstoneMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Standard tabs of the single-block machines and small drawing helpers shared by their screens. */
final class MachineTabs {
    /** Tab rows, 28 px apart from the top of the panel; the bottom row follows the panel height. */
    static final int TOP_Y = 6;
    static final int ROW_2 = 34;
    static final int ROW_3 = 62;
    static final int ROW_4 = 90;

    private MachineTabs() {
    }

    /** Bottom tab row: 137 on a 166 px panel. */
    static int bottomY(DfmScreen<?> s) {
        return s.panelHeight() - 29;
    }

    static void details(DfmScreen<?> s, GuiGraphics g, int x, int y, boolean leftSide, int row, MachineWindows.InfoWindow window,
                        int mouseX, int mouseY) {
        s.windowTab(g, leftSide ? x - DfmScreen.TAB_SIZE : x + s.panelWidth(), y + row, leftSide, DfmScreen.TAB_DEFAULT,
                DfmScreen.ICON_STATS, window, mouseX, mouseY, () -> List.of(Component.translatable("gui.dimensionalfluxminer.details")));
    }

    /** Rate selector tab (right column, second row). */
    static void rate(DfmScreen<?> s, GuiGraphics g, int x, int y, CompoundTag d, int nextId, int previousId, List<Component> details,
                     int mouseX, int mouseY) {
        s.rateTab(g, x + s.panelWidth(), y + ROW_2, false, d.getInt("rate"), nextId, previousId, mouseX, mouseY, () -> details);
    }

    static void sideConfig(DfmScreen<?> s, GuiGraphics g, int x, int y, DfmScreen.Window window, int mouseX, int mouseY) {
        s.windowTab(g, x - DfmScreen.TAB_SIZE, y + TOP_Y, true, DfmScreen.TAB_DEFAULT, DfmScreen.ICON_SIDE_CONFIG, window,
                mouseX, mouseY, () -> List.of(Component.translatable("gui.dimensionalfluxminer.side_config")));
    }

    static void upgrades(DfmScreen<?> s, GuiGraphics g, int x, int y, MachineWindows.UpgradeWindow window, int mouseX, int mouseY) {
        s.windowTab(g, x + s.panelWidth(), y + TOP_Y, false, DfmScreen.TAB_DEFAULT, DfmScreen.ICON_UPGRADE, window,
                mouseX, mouseY, () -> List.of(Component.translatable("gui.dimensionalfluxminer.upgrades")));
    }

    /**
     * Redstone Control tab of Crusher and Electric Furnace: the whole block listens to redstone; left click selects the next
     * mode (Ignored, Normal, Inverted), right click the previous one. Striped while redstone is halting the machine.
     */
    static void redstone(DfmScreen<?> s, GuiGraphics g, int x, int y, CompoundTag d, int mouseX, int mouseY) {
        RedstoneMode mode = RedstoneMode.byName(d.getString("redstone_mode"));
        redstoneTab(s, g, x, y, mode, d.getBoolean("redstone_halt"), AbstractMachineBlockEntity.BUTTON_REDSTONE_NEXT,
                AbstractMachineBlockEntity.BUTTON_REDSTONE_PREVIOUS, mouseX, mouseY);
    }

    static void redstoneTab(DfmScreen<?> s, GuiGraphics g, int x, int y, RedstoneMode mode, boolean halted, int nextId, int previousId,
                            int mouseX, int mouseY) {
        ResourceLocation icon = switch (mode) {
            case IGNORED -> DfmScreen.ICON_REDSTONE_OFF;
            case HIGH -> DfmScreen.ICON_REDSTONE_ON;
            case LOW -> DfmScreen.ICON_REDSTONE_LOW;
        };
        s.iconTab(g, x + s.panelWidth(), y + bottomY(s), false, DfmScreen.TAB_REDSTONE, icon, DfmScreen.TAB_INNER, true, false, halted,
                mouseX, mouseY, () -> nextId, () -> previousId, () -> List.of(
                        Component.translatable("gui.dimensionalfluxminer.redstone_control", Component.translatable(mode.translationKey()))));
    }

    /** Problems tab (bottom left) when something keeps a Crusher or Electric Furnace from running. */
    static void problems(DfmScreen<?> s, GuiGraphics g, int x, int y, CompoundTag d) {
        List<Component> problems = new ArrayList<>();
        if (d.getBoolean("redstone_halt")) {
            problems.add(redstoneProblem(RedstoneMode.byName(d.getString("redstone_mode"))));
        }
        switch (d.getString("status")) {
            case "output_blocked" -> problems.add(Component.translatable("gui.dimensionalfluxminer.problem.output_blocked"));
            case "no_energy" -> problems.add(Component.translatable("gui.dimensionalfluxminer.problem.no_energy"));
            default -> {
            }
        }
        warnings(s, g, x, y, problems);
    }

    static Component redstoneProblem(RedstoneMode mode) {
        return Component.translatable(mode == RedstoneMode.LOW ? "gui.dimensionalfluxminer.problem.redstone_present"
                : "gui.dimensionalfluxminer.problem.redstone_absent");
    }

    static void warnings(DfmScreen<?> s, GuiGraphics g, int x, int y, List<Component> problems) {
        if (!problems.isEmpty()) {
            s.warningTab(g, x - DfmScreen.TAB_SIZE, y + bottomY(s), problems);
        }
    }

    /** One pixel outline (configuration-only slot highlight). */
    static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
