package dimensionalfluxminer.client.screen;

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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Dimensional Flux Miner terminal: the terminal screen (status, core, limit and Dimensional Matter) with the energy bar
 * under it, the power, filter and settings buttons on its left, the Core slot on its right and the 27 output slots
 * (9 x 3). Filter, Settings (a small terminal with the Auto Eject / Auto Input switches) and Details open as windows.
 * Tabs: Details (left),
 * upgrades, Flux Rate and Redstone Control (right).
 */
public class MinerScreen extends DfmScreen<MinerMenu> {
    private static final int TERMINAL_X = 38;
    private static final int TERMINAL_Y = 18;
    private static final int TERMINAL_W = 104;
    private static final int TERMINAL_H = 55;
    private static final int ENERGY_Y = TERMINAL_Y + TERMINAL_H + 2;
    private static final int BUTTONS_X = 12;
    private static final int BUTTON_SIZE = 18;
    /** Power, Filter and Settings stacked left of the terminal. */
    private static final int BUTTON_STEP = BUTTON_SIZE + 2;
    private static final ResourceLocation POWER_ON = sprite("power_controls/power_on");
    private static final ResourceLocation POWER_OFF = sprite("power_controls/power_off");
    private static final ResourceLocation SETTINGS = sprite("power_controls/settings");
    private static final ResourceLocation FILTER = sprite("power_controls/filter");

    private final FilterWindow filterWindow = new FilterWindow();
    private final SettingsWindow settingsWindow = new SettingsWindow();
    private final MachineWindows.InfoWindow detailsWindow;
    private final MachineWindows.UpgradeWindow upgradeWindow;

    public MinerScreen(MinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 242;
        detailsWindow = new MachineWindows.InfoWindow(this, "miner.details", Component.translatable("gui.dimensionalfluxminer.miner.details"),
                this::details);
        upgradeWindow = new MachineWindows.UpgradeWindow(this, "miner.upgrades", menu.upgradeSlot());
    }

    @Override
    protected int inventoryTop() {
        return MinerMenu.INVENTORY_TOP;
    }

    @Override
    protected void renderMain(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data();

        // Terminal: status, core, limit and the Dimensional Matter buffer.
        innerScreen(g, x + TERMINAL_X, y + TERMINAL_Y, TERMINAL_W, TERMINAL_H);
        screenLines(g, screenText(d), x + TERMINAL_X + 4, y + TERMINAL_Y + 5, TERMINAL_W - 8, 12);
        hotspot(x + TERMINAL_X, y + TERMINAL_Y, TERMINAL_W, TERMINAL_H, null, null, () -> metrics(d), 0);

        // Energy bar under the terminal, as wide as it.
        long cap = Math.max(1, d.getLong("fe_capacity"));
        energyBarHorizontal(g, x + TERMINAL_X, y + ENERGY_Y, TERMINAL_W, (double) d.getLong("fe") / cap);
        hotspot(x + TERMINAL_X, y + ENERGY_Y, TERMINAL_W, 14, null, null, () -> MachineText.energyTooltip(d), 0);

        // Power (a push switch: sunk while the machine is on) and Settings, left of the terminal.
        boolean run = d.getBoolean("run");
        // The icon shows the action of the next click: the gray symbol to turn off while on, the cyan one to turn on while off.
        iconButton(g, x + BUTTONS_X, y + TERMINAL_Y, BUTTON_SIZE, run ? POWER_OFF : POWER_ON, 18, 18, run,
                mouseX, mouseY, () -> MinerControllerBlockEntity.BUTTON_RUN, () -> List.of(
                        Component.translatable(run ? "gui.dimensionalfluxminer.miner.power.on" : "gui.dimensionalfluxminer.miner.power.off"),
                        Component.translatable("gui.dimensionalfluxminer.miner.run.tooltip").withStyle(ChatFormatting.GRAY)));
        // Filter (sunk while some families are selected), then Settings.
        int selected = selectedCount(d);
        iconButton(g, x + BUTTONS_X, y + TERMINAL_Y + BUTTON_STEP, BUTTON_SIZE, FILTER, 11, 10, selected > 0, mouseX, mouseY, () -> {
            openWindow(filterWindow);
            return -1;
        }, () -> List.of(Component.translatable(selected == 0 ? "gui.dimensionalfluxminer.miner.filter.off"
                : "gui.dimensionalfluxminer.miner.filter.on", selected),
                Component.translatable("gui.dimensionalfluxminer.miner.filter.tooltip").withStyle(ChatFormatting.GRAY)));
        iconButton(g, x + BUTTONS_X, y + TERMINAL_Y + 2 * BUTTON_STEP, BUTTON_SIZE, SETTINGS, 18, 18, false, mouseX, mouseY, () -> {
            openWindow(settingsWindow);
            return -1;
        }, () -> List.of(Component.translatable("gui.dimensionalfluxminer.miner.settings")));

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

        MachineTabs.details(this, g, x, y, true, MachineTabs.TOP_Y, detailsWindow, mouseX, mouseY);
        MachineTabs.upgrades(this, g, x, y, upgradeWindow, mouseX, mouseY);
        MachineTabs.rate(this, g, x, y, d, MinerControllerBlockEntity.BUTTON_RATE_CYCLE, MinerControllerBlockEntity.BUTTON_RATE_CYCLE_BACK,
                List.of(Component.translatable("gui.dimensionalfluxminer.rate.consumption", fe(d.getLong("power"))),
                        Component.translatable("gui.dimensionalfluxminer.miner.max_throughput", number(d.getDouble("nominal")))),
                mouseX, mouseY);
        // Redstone Control of the miner has two modes: Ignored and Normal (signal on the lower layer required).
        MinerStatus status = parseStatus(d.getString("status"));
        RedstoneMode redstone = d.getBoolean("redstone") ? RedstoneMode.HIGH : RedstoneMode.IGNORED;
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

    private List<Component> screenText(CompoundTag d) {
        Component matter = Component.translatable("gui.dimensionalfluxminer.miner.dm.terminal",
                dmValue(d.getLong("dm")), dmValue(MatterValues.BUFFER_UNITS));
        String core = d.getString("core");
        if (core.isEmpty() || "NONE".equals(core)) {
            return List.of(
                    Component.translatable("gui.dimensionalfluxminer.miner.system_offline").withColor(SCREEN_ERROR & 0xFFFFFF),
                    Component.translatable("gui.dimensionalfluxminer.miner.core_required").withColor(SCREEN_WARN & 0xFFFFFF),
                    matter);
        }
        MinerStatus status = parseStatus(d.getString("status"));
        String limit = d.getString("limit").isEmpty() ? "NONE" : d.getString("limit");
        return List.of(
                Component.translatable(status.key()).withColor((status == MinerStatus.RUNNING ? SCREEN_TEXT : SCREEN_WARN) & 0xFFFFFF),
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

    /** Settings window: a small terminal showing the automation state, with its Auto Eject / Auto Input switches inside. */
    private final class SettingsWindow extends Window {
        private static final int WIDTH = 100;
        /** Two text lines, then the switches (31 px tall), all inside the terminal. */
        private static final int SCREEN_H = 58;
        private static final int SWITCHES_TOP = 25;
        private static final int SWITCH_W = 15;
        private static final int SWITCH_GAP = 8;

        SettingsWindow() {
            super(MinerScreen.this, "miner.settings", WIDTH);
        }

        @Override
        public Component title() {
            return Component.translatable("gui.dimensionalfluxminer.miner.settings");
        }

        @Override
        public int height() {
            return 18 + SCREEN_H + 6;
        }

        @Override
        protected void render(GuiGraphics g, int px, int py, int mouseX, int mouseY) {
            CompoundTag d = data();
            boolean eject = d.getBoolean("auto_eject");
            boolean pull = d.getBoolean("auto_input");
            innerScreen(g, px + 6, py + 18, WIDTH - 12, SCREEN_H);
            screenLines(g, List.of(Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(eject)),
                    Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(pull))),
                    px + 10, py + 21, WIDTH - 20, 10);
            int switchesX = px + (WIDTH - 2 * SWITCH_W - SWITCH_GAP) / 2;
            int switchesY = py + 18 + SWITCHES_TOP;
            digitalSwitch(g, switchesX, switchesY, sprite("switch/eject"), eject, mouseX, mouseY,
                    () -> MinerControllerBlockEntity.BUTTON_AUTO_EJECT, () -> List.of(
                            Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(eject)),
                            Component.translatable("gui.dimensionalfluxminer.miner.auto_eject.note").withStyle(ChatFormatting.GRAY)));
            digitalSwitch(g, switchesX + SWITCH_W + SWITCH_GAP, switchesY, sprite("switch/input"), pull, mouseX, mouseY,
                    () -> MinerControllerBlockEntity.BUTTON_AUTO_INPUT, () -> List.of(
                            Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(pull)),
                            Component.translatable("gui.dimensionalfluxminer.miner.auto_input.note").withStyle(ChatFormatting.GRAY)));
        }
    }

    /** FILTER window: family icon grid of the dimension's Resource Profile; empty selection = FILTER OFF. */
    private final class FilterWindow extends Window {
        private static final int COLUMNS = 7;

        FilterWindow() {
            super(MinerScreen.this, "miner.filter", 156);
        }

        @Override
        public Component title() {
            return Component.translatable("gui.dimensionalfluxminer.miner.filter.title");
        }

        @Override
        public int height() {
            int families = data().getList("families", Tag.TAG_COMPOUND).size();
            int rows = Math.max(1, (families + COLUMNS - 1) / COLUMNS);
            return 34 + rows * 20 + 26;
        }

        @Override
        protected void render(GuiGraphics g, int px, int py, int mouseX, int mouseY) {
            CompoundTag d = data();
            ListTag families = d.getList("families", Tag.TAG_COMPOUND);
            innerScreen(g, px + 6, py + 18, width - 12, 12);
            fitted(g, Component.translatable(selectedCount(d) == 0 ? "gui.dimensionalfluxminer.miner.filter.state_off"
                    : "gui.dimensionalfluxminer.miner.filter.state_on"), px + 9, py + 21, width - 18, SCREEN_TEXT, 0.8F, false);
            int gridX = px + (width - COLUMNS * 20) / 2;
            for (int i = 0; i < families.size(); i++) {
                CompoundTag f = families.getCompound(i);
                int fx = gridX + (i % COLUMNS) * 20;
                int fy = py + 34 + (i / COLUMNS) * 20;
                boolean selected = f.getBoolean("selected");
                slot(g, selected ? SlotKind.OUTPUT : SlotKind.NORMAL, fx, fy);
                ResourceLocation icon = ResourceLocation.tryParse(f.getString("icon"));
                if (icon != null) {
                    g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(icon)), fx + 1, fy + 1);
                }
                if (selected) {
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.blitSprite(CHECK, fx, fy, 18, 18);
                    g.pose().popPose();
                }
                if (hovered(fx + 1, fy + 1, 16, 16, mouseX, mouseY)) {
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.fill(fx + 1, fy + 1, fx + 17, fy + 17, 0x80FFFFFF);
                    g.pose().popPose();
                }
                int index = i;
                String key = f.getString("key");
                int weight = f.getInt("weight");
                hotspot(fx, fy, 18, 18, () -> MinerControllerBlockEntity.BUTTON_FILTER_TOGGLE + index, null,
                        () -> List.of(Component.literal(key), Component.translatable("gui.dimensionalfluxminer.miner.filter.weight", weight),
                                Component.translatable(selected ? "gui.dimensionalfluxminer.miner.filter.kept"
                                        : "gui.dimensionalfluxminer.miner.filter.not_selected")), 0);
            }
            if (families.isEmpty()) {
                fitted(g, Component.translatable("gui.dimensionalfluxminer.miner.filter.empty_profile"), px + 6, py + 39, width - 12,
                        SUBHEADING, 1.0F, true);
            }
            button(g, px + 6, py + height() - 22, width - 12, 16, Component.translatable("gui.dimensionalfluxminer.miner.filter.clear"),
                    selectedCount(d) > 0, mouseX, mouseY, () -> MinerControllerBlockEntity.BUTTON_FILTER_CLEAR, null, null);
        }
    }

    private List<Component> details() {
        CompoundTag d = data();
        List<Component> lines = new ArrayList<>();
        String core = d.getString("core");
        if ("BASIC".equals(core)) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.drain"));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.coverage", percent(d.getDouble("survey_coverage"))));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.known", d.getInt("known_drainable")));
            lines.add(Component.translatable(d.getBoolean("survey_ready") ? "gui.dimensionalfluxminer.miner.details.ready"
                    : "gui.dimensionalfluxminer.miner.details.surveying"));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.blight", percent(d.getDouble("blight_threat"))));
        } else if ("STABILIZED".equals(core)) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.injection"));
            String substrate = d.getString("substrate");
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.substrate",
                    substrate.isEmpty() ? Component.literal("-") : Component.translatable(substrate)));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.item_dm", dm(d.getLong("substrate_units"))));
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.dm", dm(d.getLong("dm")), dm(MatterValues.BUFFER_UNITS)));
        } else {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.core_required"));
        }
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.dimension", d.getString("dimension")));
        if (d.getDouble("empty_share") > 0) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.details.void", percent(d.getDouble("empty_share"))));
        }
        lines.add(Component.translatable("gui.dimensionalfluxminer.rate.tooltip", d.getInt("rate")));
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.potential", number(d.getDouble("potential"))));
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.actual", number(d.getDouble("actual"))));
        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.output_used", d.getInt("output_used"), MinerControllerBlockEntity.OUTPUT_SLOTS));
        return lines;
    }

    private List<Component> metrics(CompoundTag d) {
        return List.of(
                Component.translatable("gui.dimensionalfluxminer.miner.dimension", d.getString("dimension")),
                Component.translatable("gui.dimensionalfluxminer.miner.potential", number(d.getDouble("potential"))),
                Component.translatable("gui.dimensionalfluxminer.miner.actual", number(d.getDouble("actual"))),
                Component.translatable("gui.dimensionalfluxminer.miner.output_used", d.getInt("output_used"), MinerControllerBlockEntity.OUTPUT_SLOTS));
    }

    private static int selectedCount(CompoundTag d) {
        ListTag families = d.getList("families", Tag.TAG_COMPOUND);
        int n = 0;
        for (int i = 0; i < families.size(); i++) {
            if (families.getCompound(i).getBoolean("selected")) {
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
