package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideConfiguration;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Side Configuration window (156 x 135): one tab per transmission on its left edge, each always in its own place (the
 * current one drawn gray and sunk, like the tab of an open window), the six faces as 22 px buttons
 * (top / left-front-right / back-bottom) tinted with the route color, the automation button of the current tab and
 * the batch button (every side to the next/previous route, Shift clears every transmission). Left click selects the
 * next route of a face, right click the previous one.
 * <p>
 * Every material has its own automation, two small switches in the top bar: Auto Input and Auto Eject, each on and off
 * by itself; a fluid tab that only outputs has no Auto Input.
 */
final class SideConfigWindow extends DfmScreen.Window {
    private static final int SIDE = 22;

    /** The automation a tab controls. */
    enum Automation {
        /** Auto Input and Auto Eject of items. */
        ITEMS,
        /** Auto Eject of an output-only fluid (no Auto Input). */
        FLUID_OUTPUT,
        /** Auto Input and Auto Eject of liquids (Dimensional Tanks). */
        FLUID
    }

    /** One tab's automation and the lines explaining it on this machine. */
    record AutomationTab(Automation automation, List<Component> help) {
    }

    private final SideRules rules;
    private final List<Transmission> transmissions;
    private final Supplier<CompoundTag> data;
    private final BlockPos pos;
    private final Map<Transmission, AutomationTab> automation;
    private Transmission current;
    private RelativeFace focused;
    private ItemStack[] neighbors;

    /**
     * @param automation the automation of each tab that has one (the button is disabled on the other tabs)
     */
    SideConfigWindow(DfmScreen<?> screen, String key, SideRules rules, List<Transmission> transmissions, Supplier<CompoundTag> data,
                     BlockPos pos, Map<Transmission, AutomationTab> automation) {
        super(screen, key, 156);
        this.rules = rules;
        this.transmissions = transmissions;
        this.data = data;
        this.pos = pos;
        this.automation = automation;
        this.current = transmissions.getFirst();
    }

    @Override
    public Component title() {
        return Component.translatable("gui.dimensionalfluxminer.side_config.type", Component.translatable(current.translationKey()));
    }

    @Override
    public int height() {
        return 135;
    }

    /** Auto Input and Auto Eject switches in the top bar, side by side, 2 px apart. */
    static final int INPUT_BUTTON_X = 128;
    static final int EJECT_BUTTON_X = INPUT_BUTTON_X + DfmScreen.SMALL_BUTTON + 2;

    @Override
    protected int titlePadEnd() {
        // Clears the two automation switches with a 2 px gap.
        return 156 - INPUT_BUTTON_X + 2;
    }

    /** Tooltip of an automation switch: its state, the tab's help and how it is used. */
    static List<Component> automationTooltip(AutomationTab tab, boolean usable, Component state) {
        List<Component> lines = new ArrayList<>();
        if (tab == null) {
            lines.add(Component.translatable("gui.dimensionalfluxminer.side_config.cant_eject"));
            return lines;
        }
        if (!usable) {
            // Only an output-only fluid has a tab where one of the switches does not apply: it never comes in.
            lines.add(Component.translatable("gui.dimensionalfluxminer.side_config.cant_input"));
            return lines;
        }
        lines.add(state);
        for (Component help : tab.help()) {
            lines.add(help.copy().withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("gui.dimensionalfluxminer.side_config.auto.toggle").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    Transmission current() {
        return current;
    }

    /** Route of the face under the mouse in the current tab (slot highlighting), NONE otherwise. */
    SideRoute focusedRoute() {
        if (focused == null) {
            return SideRoute.NONE;
        }
        return config().get(focused, current, data.get().getIntOr("profile", 0));
    }

    @Override
    protected void onClose() {
        focused = null;
        neighbors = null;
    }

    private SideConfiguration config() {
        SideConfiguration config = new SideConfiguration(rules);
        config.load(data.get().getCompoundOrEmpty("sides"));
        int[] locks = data.get().getIntArray("node_sides").orElse(new int[0]);
        for (Transmission t : Transmission.VALUES) {
            config.setLocks(t, t.ordinal() < locks.length ? locks[t.ordinal()] : 0);
        }
        return config;
    }

    static GuiIcon icon(Transmission t) {
        return switch (t) {
            case ITEM -> DfmScreen.ICON_ITEMS;
            case ENERGY -> DfmScreen.ICON_ENERGY;
            case REDSTONE -> DfmScreen.ICON_REDSTONE_ON;
            case EXPERIENCE, FLUID -> DfmScreen.ICON_FLUIDS;
        };
    }

    @Override
    protected void render(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data.get();
        SideConfiguration config = config();
        int profile = d.getIntOr("profile", 0);

        for (int i = 0; i < transmissions.size(); i++) {
            Transmission t = transmissions.get(i);
            boolean left = i < 4;
            screen.materialTab(g, left ? x - DfmScreen.MATERIAL_TAB_SIZE : x + width, y + 2 + DfmScreen.MATERIAL_TAB_PITCH * (i % 4), left,
                    icon(t), t != current, mouseX, mouseY, () -> {
                        current = t;
                        return -1;
                    }, () -> List.of(Component.translatable(t.translationKey())));
        }

        AutomationTab tabAutomation = automation.get(current);
        boolean ejectable = tabAutomation != null;
        boolean items = ejectable && tabAutomation.automation() == Automation.ITEMS;
        boolean liquids = ejectable && tabAutomation.automation() == Automation.FLUID;
        boolean hasInput = items || liquids;
        boolean input = items ? d.getBooleanOr("auto_input", false) : liquids && d.getBooleanOr("auto_input_fluid", false);
        boolean eject = items ? d.getBooleanOr("auto_eject", false) : d.getBooleanOr("auto_eject_fluid", false);
        String mode = input ? eject ? "both" : "input" : eject ? "eject" : "off";
        screen.innerScreen(g, x + 38, y + 25, 80, 12);
        Component screenText = ejectable
                ? Component.translatable("gui.dimensionalfluxminer.side_config.auto." + mode)
                : Component.translatable("gui.dimensionalfluxminer.side_config.no_eject");
        screen.fitted(g, screenText, x + 41, y + 28, 74, DfmScreen.screenText(), 0.8F, false);
        if (!ejectable) {
            screen.hotspot(x + 38, y + 25, 80, 12, null, null,
                    () -> List.of(Component.translatable("gui.dimensionalfluxminer.side_config.cant_eject")), 0);
        }

        // Two switches in the top bar: Auto Input (items and tank liquids; never for an output-only fluid) and Auto Eject.
        int inputId = items ? AbstractMachineBlockEntity.BUTTON_AUTO_INPUT : AbstractMachineBlockEntity.BUTTON_AUTO_INPUT_FLUID;
        int ejectId = items ? AbstractMachineBlockEntity.BUTTON_AUTO_EJECT : AbstractMachineBlockEntity.BUTTON_AUTO_EJECT_FLUID;
        String inputKey = items ? "gui.dimensionalfluxminer.auto_input" : "gui.dimensionalfluxminer.auto_input_fluid";
        String ejectKey = items ? "gui.dimensionalfluxminer.auto_eject" : "gui.dimensionalfluxminer.auto_eject_fluid";
        screen.imageButton(g, x + INPUT_BUTTON_X, y + 6, DfmScreen.SMALL_BUTTON, GuiIcon.AUTO_INJECT, hasInput, input, mouseX, mouseY,
                () -> inputId, null, () -> automationTooltip(tabAutomation, hasInput,
                        Component.translatable(inputKey, MachineText.onOff(input))));
        screen.imageButton(g, x + EJECT_BUTTON_X, y + 6, DfmScreen.SMALL_BUTTON, GuiIcon.AUTO_EJECT, ejectable, eject, mouseX, mouseY,
                () -> ejectId, null, () -> automationTooltip(tabAutomation, ejectable,
                        Component.translatable(ejectKey, MachineText.onOff(eject))));

        // Click: every side to the next route (or NONE when they differ); right click: previous; Shift: clear all.
        Transmission tab = current;
        SideRoute nextTarget = config.batchTarget(tab, profile, true);
        screen.imageButton(g, x + 132, y + 93, 18, GuiIcon.CLEAR_SIDES, true, false, mouseX, mouseY,
                () -> Minecraft.getInstance().hasShiftDown() ? AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL
                        : AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + tab.ordinal() * 2,
                () -> Minecraft.getInstance().hasShiftDown() ? AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL
                        : AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + tab.ordinal() * 2 + 1,
                () -> Minecraft.getInstance().hasShiftDown() || nextTarget == SideRoute.NONE
                        ? List.of(Component.translatable("gui.dimensionalfluxminer.side_config.clear"),
                                Component.translatable("gui.dimensionalfluxminer.side_config.clear_all").withStyle(ChatFormatting.GRAY))
                        : List.of(Component.translatable("gui.dimensionalfluxminer.side_config.increment",
                                Component.translatable(nextTarget.translationKey()).withColor(nextTarget.color() & 0xFFFFFF))));

        if (neighbors == null) {
            neighbors = neighborItems();
        }
        focused = null;
        side(g, config, profile, RelativeFace.DOWN, x + 67, y + 92, mouseX, mouseY);
        side(g, config, profile, RelativeFace.UP, x + 67, y + 46, mouseX, mouseY);
        side(g, config, profile, RelativeFace.FRONT, x + 67, y + 69, mouseX, mouseY);
        side(g, config, profile, RelativeFace.BACK, x + 44, y + 92, mouseX, mouseY);
        side(g, config, profile, RelativeFace.LEFT, x + 44, y + 69, mouseX, mouseY);
        side(g, config, profile, RelativeFace.RIGHT, x + 90, y + 69, mouseX, mouseY);

        screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.side_config.slots"), x + 4, y + 120, width - 8,
                GuiTheme.subheading(), 1.0F, true);
    }

    private void side(GuiGraphicsExtractor g, SideConfiguration config, int profile, RelativeFace face, int x, int y, int mouseX, int mouseY) {
        List<SideRoute> allowed = rules.allowed(face, current, profile);
        boolean enabled = !allowed.isEmpty();
        // A Logistics Networks node or an active cable uses this face: its route is set from outside and cannot be changed here.
        boolean node = config.locked(face, current);
        boolean cable = node && current == Transmission.ITEM && (data.get().getIntOr("cable_sides", 0) & (1 << face.ordinal())) != 0;
        // A node's item input on a machine with dedicated lanes cannot move, but a click picks all / lane 1 / 2 / 3.
        boolean lanes = node && current == Transmission.ITEM && (data.get().getIntOr("node_lanes", 0) & (1 << face.ordinal())) != 0;
        boolean fixed = allowed.size() < 2 || node && !lanes;
        SideRoute route = enabled ? config.get(face, current, profile) : SideRoute.NONE;
        if (screen.hovered(x, y, SIDE, SIDE, mouseX, mouseY)) {
            focused = face;
        }
        int base = AbstractMachineBlockEntity.BUTTON_SIDE_BASE + face.ordinal() * 16 + current.ordinal() * 2;
        ItemStack neighbor = neighbors[face.ordinal()];
        // A face without a route is a plain button (like the Miner's), never a flat light gray one.
        Integer tint = enabled && route != SideRoute.NONE ? Integer.valueOf(route.color() & 0xFFFFFF) : null;
        screen.colorButton(g, x, y, SIDE, tint, enabled, mouseX, mouseY,
                fixed ? () -> -1 : () -> base, fixed ? () -> -1 : () -> base + 1, () -> {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable(face.translationKey()));
                    if (enabled) {
                        lines.add(Component.translatable(route.translationKey()).withColor(route.color() & 0xFFFFFF));
                    } else {
                        lines.add(Component.translatable("gui.dimensionalfluxminer.side_locked").withStyle(ChatFormatting.GRAY));
                    }
                    if (!neighbor.isEmpty()) {
                        lines.add(neighbor.getHoverName());
                    }
                    if (cable) {
                        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port.cable").withStyle(ChatFormatting.AQUA));
                        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port.cable_note").withStyle(ChatFormatting.GRAY));
                    } else if (node) {
                        lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port.node").withStyle(ChatFormatting.AQUA));
                        lines.add(Component.translatable(lanes ? "gui.dimensionalfluxminer.side_config.node_lanes"
                                : "gui.dimensionalfluxminer.miner.port.node_note").withStyle(ChatFormatting.GRAY));
                    } else if (enabled) {
                        lines.add(Component.translatable(fixed ? "gui.dimensionalfluxminer.side_fixed" : "gui.dimensionalfluxminer.side_click")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                    return lines;
                });
        if (!neighbor.isEmpty()) {
            g.item(neighbor, x + 3, y + 3);
        }
        if (node) {
            // Same frame as on the Miner's side window.
            g.pose().pushMatrix();
            g.pose().translate(0, 0);
            g.outline(x, y, SIDE, SIDE, 0xFF52D9DB);
            g.pose().popMatrix();
        }
    }

    /** Pick-block item of every neighbor, indexed by face ordinal (empty for air). */
    private ItemStack[] neighborItems() {
        ItemStack[] result = new ItemStack[RelativeFace.VALUES.length];
        java.util.Arrays.fill(result, ItemStack.EMPTY);
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return result;
        }
        BlockState self = level.getBlockState(pos);
        if (!self.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return result;
        }
        Direction facing = self.getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (RelativeFace face : RelativeFace.VALUES) {
            Direction side = face.toDirection(facing);
            BlockPos other = pos.relative(side);
            BlockState state = level.getBlockState(other);
            if (!state.isAir()) {
                result[face.ordinal()] = state.getCloneItemStack(other, level, false, mc.player);
            }
        }
        return result;
    }
}
