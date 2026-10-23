package dimensionalfluxminer.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import dimensionalfluxminer.machine.RedstoneMode;
import dimensionalfluxminer.miner.MatterValues;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerMenu;
import dimensionalfluxminer.miner.MinerStatus;
import dimensionalfluxminer.registry.DfmItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Dimensional Flux Miner terminal: the terminal screen (status, core, limit and Dimensional Matter) with the energy bar
 * under it, the power, filter and side configuration buttons on its left, the Core slot on its right and the 27 output
 * slots (9 x 3). Filter, Side Configuration (the nine ports, with Auto Input / Auto Eject) and Details open as windows.
 * Tabs: Details (left), upgrades, Flux Rate and Redstone Control (right). Until a Core is inserted only the Core slot
 * and the Details work.
 */
public class MinerScreen extends DfmScreen<MinerMenu> {
    private static final int TERMINAL_X = 38;
    private static final int TERMINAL_Y = 18;
    private static final int TERMINAL_W = 104;
    private static final int TERMINAL_H = 55;
    private static final int ENERGY_Y = TERMINAL_Y + TERMINAL_H + 2;
    private static final int BUTTONS_X = 12;
    private static final int BUTTON_SIZE = 18;
    /** Power and Filter stacked left of the terminal. */
    private static final int BUTTON_STEP = BUTTON_SIZE + 2;
    private static final GuiIcon POWER_ON = GuiIcon.POWER_ON;
    private static final GuiIcon POWER_OFF = GuiIcon.POWER_OFF;
    private static final GuiIcon FILTER = GuiIcon.FILTER;

    private final FilterWindow filterWindow = new FilterWindow();
    private final MinerSideWindow sideWindow;
    private final MachineWindows.InfoWindow detailsWindow;
    private final MachineWindows.UpgradeWindow upgradeWindow;

    public MinerScreen(MinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 242);
        detailsWindow = new MachineWindows.InfoWindow(this, "miner.details", Component.translatable("gui.dimensionalfluxminer.miner.details"),
                this::details);
        upgradeWindow = new MachineWindows.UpgradeWindow(this, "miner.upgrades", menu.upgradeSlot());
        sideWindow = new MinerSideWindow(this, this::data, menu.pos());
    }

    @Override
    protected int inventoryTop() {
        return MinerMenu.INVENTORY_TOP;
    }

    @Override
    protected void renderMain(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data();

        // Terminal: status, core, limit and the Dimensional Matter buffer.
        innerScreen(g, x + TERMINAL_X, y + TERMINAL_Y, TERMINAL_W, TERMINAL_H);
        screenLines(g, screenText(d), x + TERMINAL_X + 4, y + TERMINAL_Y + 5, TERMINAL_W - 8, 12);
        hotspot(x + TERMINAL_X, y + TERMINAL_Y, TERMINAL_W, TERMINAL_H, null, null, () -> metrics(d), 0);

        // Energy bar under the terminal, as wide as it.
        long cap = Math.max(1, d.getLongOr("fe_capacity", 0L));
        energyBarHorizontal(g, x + TERMINAL_X, y + ENERGY_Y, TERMINAL_W, (double) d.getLongOr("fe", 0L) / cap);
        hotspot(x + TERMINAL_X, y + ENERGY_Y, TERMINAL_W, 14, null, null, () -> MachineText.energyTooltip(d), 0);

        // Until a Core is inserted only its slot can be used: buttons, settings, upgrades and output stay locked.
        boolean locked = menu.locked();
        if (locked) {
            closeLockedWindows();
        }

        // Power (a push switch: sunk while the machine is on) and Filter, left of the terminal.
        boolean run = d.getBooleanOr("run", false);
        // The icon shows the action of the next click: the gray symbol to turn off while on, the cyan one to turn on while off.
        iconButton(g, x + BUTTONS_X, y + TERMINAL_Y, BUTTON_SIZE, run ? POWER_OFF : POWER_ON, run,
                mouseX, mouseY, locked ? () -> -1 : () -> MinerControllerBlockEntity.BUTTON_RUN, () -> locked ? lockedTooltip() : List.of(
                        Component.translatable(run ? "gui.dimensionalfluxminer.miner.power.on" : "gui.dimensionalfluxminer.miner.power.off"),
                        Component.translatable("gui.dimensionalfluxminer.miner.run.tooltip").withStyle(ChatFormatting.GRAY)));
        // Filter (sunk while some families are selected).
        int selected = selectedCount(d);
        iconButton(g, x + BUTTONS_X, y + TERMINAL_Y + BUTTON_STEP, BUTTON_SIZE, FILTER, selected > 0, mouseX, mouseY, () -> {
            if (!locked) {
                openWindow(filterWindow);
            }
            return -1;
        }, () -> locked ? lockedTooltip() : List.of(Component.translatable(selected == 0 ? "gui.dimensionalfluxminer.miner.filter.off"
                : "gui.dimensionalfluxminer.miner.filter.on", selected),
                Component.translatable("gui.dimensionalfluxminer.miner.filter.tooltip").withStyle(ChatFormatting.GRAY)));

        // Core slot right of the terminal.
        slot(g, SlotKind.EXTRA, x + MinerMenu.CORE_X - 1, y + MinerMenu.CORE_Y - 1);
        if (!menu.getSlot(0).hasItem()) {
            ghost(g, new ItemStack(DfmItems.BASIC_DIMENSIONAL_CORE.get()), x + MinerMenu.CORE_X, y + MinerMenu.CORE_Y);
            hotspot(x + MinerMenu.CORE_X - 1, y + MinerMenu.CORE_Y - 1, 18, 18, null, null,
                    () -> List.of(Component.translatable("gui.dimensionalfluxminer.miner.core_slot")), 0);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, SlotKind.NORMAL, x + MinerMenu.OUTPUT_X + col * 18 - 1, y + MinerMenu.OUTPUT_Y + row * 18 - 1);
            }
        }
        if (locked) {
            // The output grid is shaded and does nothing until a Core is inserted.
            g.pose().pushMatrix();
            g.pose().translate(0, 0);
            g.fill(x + MinerMenu.OUTPUT_X - 1, y + MinerMenu.OUTPUT_Y - 1, x + MinerMenu.OUTPUT_X + 9 * 18 - 1, y + MinerMenu.OUTPUT_Y + 3 * 18 - 1,
                    0xA0101418);
            g.pose().popMatrix();
            hotspot(x + MinerMenu.OUTPUT_X - 1, y + MinerMenu.OUTPUT_Y - 1, 9 * 18, 3 * 18, null, null, this::lockedTooltip, 0);
        }

        // Left tabs like the other machines: Side Configuration on top (locked without a Core), Details under it.
        MachineTabs.details(this, g, x, y, true, MachineTabs.ROW_2, detailsWindow, mouseX, mouseY);
        // The look of the screen can be changed even without a Core.
        themeTab(g, x, y, MachineTabs.ROW_3, mouseX, mouseY);
        if (locked) {
            return;
        }
        MachineTabs.sideConfig(this, g, x, y, sideWindow, mouseX, mouseY);
        MachineTabs.upgrades(this, g, x, y, upgradeWindow, mouseX, mouseY);
        MachineTabs.rate(this, g, x, y, d, MinerControllerBlockEntity.BUTTON_RATE_CYCLE, MinerControllerBlockEntity.BUTTON_RATE_CYCLE_BACK,
                List.of(Component.translatable("gui.dimensionalfluxminer.rate.consumption", fe(d.getLongOr("power", 0L))),
                        Component.translatable("gui.dimensionalfluxminer.miner.max_throughput", number(d.getDoubleOr("nominal", 0.0)))),
                mouseX, mouseY);
        // Redstone Control of the miner has two modes: Ignored and Normal (signal on the lower layer required).
        MinerStatus status = parseStatus(d.getStringOr("status", ""));
        RedstoneMode redstone = d.getBooleanOr("redstone", false) ? RedstoneMode.HIGH : RedstoneMode.IGNORED;
        MachineTabs.redstoneTab(this, g, x, y, redstone, status == MinerStatus.REDSTONE_HALT, MinerControllerBlockEntity.BUTTON_REDSTONE,
                MinerControllerBlockEntity.BUTTON_REDSTONE, mouseX, mouseY);
        List<Component> problems = new ArrayList<>();
        switch (status) {
            case REDSTONE_HALT -> problems.add(MachineTabs.redstoneProblem(redstone));
            case OUTPUT_BLOCKED -> problems.add(Component.translatable("gui.dimensionalfluxminer.problem.output_blocked"));
            case WAITING_FOR_ENERGY -> problems.add(Component.translatable("gui.dimensionalfluxminer.problem.no_energy"));
            default -> {
            }
        }
        MachineTabs.warnings(this, g, x, y, problems);
    }

    private List<Component> lockedTooltip() {
        return List.of(Component.translatable("gui.dimensionalfluxminer.miner.locked").withStyle(ChatFormatting.YELLOW));
    }

    /** The windows a locked machine cannot use are closed when the Core is taken out. */
    private void closeLockedWindows() {
        for (Window window : List.of(filterWindow, sideWindow, upgradeWindow)) {
            if (isWindowOpen(window)) {
                closeWindow(window);
            }
        }
    }

    private List<Component> screenText(CompoundTag d) {
        Component matter = Component.translatable("gui.dimensionalfluxminer.miner.dm.terminal",
                dmValue(d.getLongOr("dm", 0L)), dmValue(MatterValues.BUFFER_UNITS));
        String core = d.getStringOr("core", "");
        if (core.isEmpty() || "NONE".equals(core)) {
            return List.of(
                    Component.translatable("gui.dimensionalfluxminer.miner.system_offline").withColor(SCREEN_ERROR & 0xFFFFFF),
                    Component.translatable("gui.dimensionalfluxminer.miner.core_required").withColor(SCREEN_WARN & 0xFFFFFF),
                    matter);
        }
        MinerStatus status = parseStatus(d.getStringOr("status", ""));
        String limit = d.getStringOr("limit", "").isEmpty() ? "NONE" : d.getStringOr("limit", "");
        return List.of(
                Component.translatable(status.key()).withColor((status == MinerStatus.RUNNING ? screenText() : SCREEN_WARN) & 0xFFFFFF),
                Component.translatable("gui.dimensionalfluxminer.miner.core." + core.toLowerCase(Locale.ROOT)),
                limitLine(limit),
                matter);
    }

    /** "LIMIT: ..." line of the terminal: what held the production back in the last tick (warning color unless none). */
    private static Component limitLine(String limit) {
        MinerStatus.LimitingFactor factor;
        try {
            factor = MinerStatus.LimitingFactor.valueOf(limit);
        } catch (IllegalArgumentException e) {
            factor = MinerStatus.LimitingFactor.NONE;
        }
        Component line = Component.translatable("gui.dimensionalfluxminer.miner.limiting", Component.translatable(factor.key()));
        return factor == MinerStatus.LimitingFactor.NONE ? line : line.copy().withColor(SCREEN_WARN & 0xFFFFFF);
    }

    /**
     * FILTER window: family icon grid of the dimension's Resource Profile; empty selection = FILTER OFF. A search box
     * narrows the grid by ore name (in the player's language) or family key; the marks stay, and closing the window or
     * clearing the box shows every family again.
     */
    private final class FilterWindow extends Window {
        private static final int COLUMNS = 7;
        private static final int GRID_TOP = 50;
        private static final int MAX_QUERY = 32;
        private String query = "";
        private boolean focused;
        private int boxX;
        private int boxY;

        FilterWindow() {
            super(MinerScreen.this, "miner.filter", 156);
        }

        @Override
        public Component title() {
            return Component.translatable("gui.dimensionalfluxminer.miner.filter.title");
        }

        @Override
        public int height() {
            int shown = visible(data().getListOrEmpty("families")).size();
            int rows = Math.max(1, (shown + COLUMNS - 1) / COLUMNS);
            return GRID_TOP + rows * 20 + 26;
        }

        @Override
        protected void onClose() {
            query = "";
            focused = false;
        }

        @Override
        protected void mousePressed(double mouseX, double mouseY, boolean onThis) {
            focused = onThis && mouseX >= boxX && mouseY >= boxY && mouseX < boxX + width - 12 && mouseY < boxY + 12;
        }

        @Override
        protected boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!focused) {
                return false;
            }
            if (keyCode == InputConstants.KEY_BACKSPACE && !query.isEmpty()) {
                query = net.minecraft.client.Minecraft.getInstance().hasControlDown() ? "" : query.substring(0, query.length() - 1);
            } else if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                focused = false;
            }
            // Every other key is swallowed while typing (the inventory key must not close the screen).
            return true;
        }

        @Override
        protected boolean charTyped(char c, int modifiers) {
            if (!focused || !net.minecraft.util.StringUtil.isAllowedChatCharacter(c)) {
                return false;
            }
            if (query.length() < MAX_QUERY) {
                query += c;
            }
            return true;
        }

        /** Indexes (in the synced list) of the families matching the search. */
        private List<Integer> visible(ListTag families) {
            String q = fold(query.trim());
            List<Integer> shown = new ArrayList<>();
            for (int i = 0; i < families.size(); i++) {
                CompoundTag f = families.getCompoundOrEmpty(i);
                if (q.isEmpty() || fold(f.getStringOr("key", "")).contains(q) || fold(name(f).getString()).contains(q)) {
                    shown.add(i);
                }
            }
            return shown;
        }

        @Override
        protected void render(GuiGraphicsExtractor g, int px, int py, int mouseX, int mouseY) {
            CompoundTag d = data();
            ListTag families = d.getListOrEmpty("families");
            innerScreen(g, px + 6, py + 18, width - 12, 12);
            fitted(g, Component.translatable(selectedCount(d) == 0 ? "gui.dimensionalfluxminer.miner.filter.state_off"
                    : "gui.dimensionalfluxminer.miner.filter.state_on"), px + 9, py + 21, width - 18, screenText(), 0.8F, false);
            // Search box.
            boxX = px + 6;
            boxY = py + 34;
            innerScreen(g, boxX, boxY, width - 12, 12);
            if (query.isEmpty() && !focused) {
                fitted(g, Component.translatable("gui.dimensionalfluxminer.miner.filter.search"), boxX + 3, boxY + 3, width - 18,
                        GuiTheme.subheading(), 0.8F, false);
            } else {
                boolean caret = focused && (net.minecraft.util.Util.getMillis() / 500) % 2 == 0;
                fitted(g, Component.literal(query + (caret ? "_" : "")), boxX + 3, boxY + 3, width - 18, screenText(), 0.8F, false);
            }
            hotspot(boxX, boxY, width - 12, 12, () -> -1, () -> {
                query = "";
                return -1;
            }, () -> List.of(Component.translatable("gui.dimensionalfluxminer.miner.filter.search_tooltip")
                    .withStyle(ChatFormatting.GRAY)), 0);
            List<Integer> shown = visible(families);
            int gridX = px + (width - COLUMNS * 20) / 2;
            for (int n = 0; n < shown.size(); n++) {
                int i = shown.get(n);
                CompoundTag f = families.getCompoundOrEmpty(i);
                int fx = gridX + (n % COLUMNS) * 20;
                int fy = py + GRID_TOP + (n / COLUMNS) * 20;
                boolean selected = f.getBooleanOr("selected", false);
                slot(g, selected ? SlotKind.OUTPUT : SlotKind.NORMAL, fx, fy);
                g.item(icon(f), fx + 1, fy + 1);
                if (selected) {
                    g.pose().pushMatrix();
                    g.pose().translate(0, 0);
                    g.blitSprite(RenderPipelines.GUI_TEXTURED, CHECK, fx, fy, 18, 18);
                    g.pose().popMatrix();
                }
                if (hovered(fx + 1, fy + 1, 16, 16, mouseX, mouseY)) {
                    g.pose().pushMatrix();
                    g.pose().translate(0, 0);
                    g.fill(fx + 1, fy + 1, fx + 17, fy + 17, 0x80FFFFFF);
                    g.pose().popMatrix();
                }
                int index = i;
                Component name = name(f);
                String key = f.getStringOr("key", "");
                int weight = f.getIntOr("weight", 0);
                hotspot(fx, fy, 18, 18, () -> MinerControllerBlockEntity.BUTTON_FILTER_TOGGLE + index, null,
                        () -> List.of(name, Component.literal(key).withStyle(ChatFormatting.GRAY),
                                Component.translatable("gui.dimensionalfluxminer.miner.filter.weight", weight),
                                Component.translatable(selected ? "gui.dimensionalfluxminer.miner.filter.kept"
                                        : "gui.dimensionalfluxminer.miner.filter.not_selected")), 0);
            }
            if (families.isEmpty()) {
                fitted(g, Component.translatable("gui.dimensionalfluxminer.miner.filter.empty_profile"), px + 6, py + GRID_TOP + 5,
                        width - 12, GuiTheme.subheading(), 1.0F, true);
            } else if (shown.isEmpty()) {
                fitted(g, Component.translatable("gui.dimensionalfluxminer.miner.filter.no_match"), px + 6, py + GRID_TOP + 5,
                        width - 12, GuiTheme.subheading(), 1.0F, true);
            }
            button(g, px + 6, py + height() - 22, width - 12, 16, Component.translatable("gui.dimensionalfluxminer.miner.filter.clear"),
                    selectedCount(d) > 0, mouseX, mouseY, () -> MinerControllerBlockEntity.BUTTON_FILTER_CLEAR, null, null);
        }
    }

    private static ItemStack icon(CompoundTag family) {
        Identifier icon = Identifier.tryParse(family.getStringOr("icon", ""));
        return icon == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(icon));
    }

    /** Ore name in the player's language (what the ore's mod translates), or the family key without one. */
    private static Component name(CompoundTag family) {
        ItemStack icon = icon(family);
        return icon.isEmpty() ? Component.literal(family.getStringOr("key", "")) : icon.getHoverName();
    }

    /** Lower case without accents, so "minerio" finds "Minério". */
    private static String fold(String text) {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private List<Component> details() {
        CompoundTag d = data();
        List<Component> lines = new ArrayList<>();
        String core = d.getStringOr("core", "");
        if ("BASIC".equals(core)) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.drain"));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.coverage", percent(d.getDoubleOr("survey_coverage", 0.0))));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.known", d.getIntOr("known_drainable", 0)));
            lines.add(Component.translatable(d.getBooleanOr("survey_ready", false) ? "gui.dimensionalfluxminer.miner.details.ready"
                    : "gui.dimensionalfluxminer.miner.details.surveying"));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.blight", percent(d.getDoubleOr("blight_threat", 0.0))));
        } else if ("STABILIZED".equals(core)) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.injection"));
            String substrate = d.getStringOr("substrate", "");
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.substrate",
                    substrate.isEmpty() ? Component.literal("-") : Component.translatable(substrate)));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.item_dm", dm(d.getLongOr("substrate_units", 0L))));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.dm", dm(d.getLongOr("dm", 0L)), dm(MatterValues.BUFFER_UNITS)));
        } else {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.core_required"));
        }
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.dimension", d.getStringOr("dimension", "")));
        if (d.getDoubleOr("empty_share", 0.0) > 0) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.void", percent(d.getDoubleOr("empty_share", 0.0))));
        }
        lines.add(Component.translatable("gui.dimensionalfluxminer.rate.tooltip", d.getIntOr("rate", 0)));
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.potential", number(d.getDoubleOr("potential", 0.0))));
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.actual", number(d.getDoubleOr("actual", 0.0))));
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.output_used", d.getIntOr("output_used", 0), MinerControllerBlockEntity.OUTPUT_SLOTS));
        return lines;
    }

    private List<Component> metrics(CompoundTag d) {
        return List.of(
                Component.translatable("gui.dimensionalfluxminer.miner.dimension", d.getStringOr("dimension", "")),
                Component.translatable("gui.dimensionalfluxminer.miner.potential", number(d.getDoubleOr("potential", 0.0))),
                Component.translatable("gui.dimensionalfluxminer.miner.actual", number(d.getDoubleOr("actual", 0.0))),
                Component.translatable("gui.dimensionalfluxminer.miner.output_used", d.getIntOr("output_used", 0), MinerControllerBlockEntity.OUTPUT_SLOTS));
    }

    private static int selectedCount(CompoundTag d) {
        ListTag families = d.getListOrEmpty("families");
        int n = 0;
        for (int i = 0; i < families.size(); i++) {
            if (families.getCompoundOrEmpty(i).getBooleanOr("selected", false)) {
                n++;
            }
        }
        return n;
    }

    private static MinerStatus parseStatus(String name) {
        try {
            return MinerStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return MinerStatus.MULTIBLOCK_INVALID;
        }
    }

    private static String dm(long units) {
        return dmValue(units) + " DM";
    }

    private static String dmValue(long units) {
        return String.format(Locale.ROOT, "%.1f", units / (double) MatterValues.UNITS_PER_DM);
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.1f%%", fraction * 100.0);
    }
}
