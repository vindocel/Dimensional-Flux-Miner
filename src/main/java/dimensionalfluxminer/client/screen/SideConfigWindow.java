package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideConfiguration;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Side Configuration window (156 x 135): one tab per transmission on its left edge, the six faces as 22 px buttons
 * (top / left-front-right / back-bottom) tinted with the route color, the automation button (OFF, Auto Input,
 * Auto Eject, both) and the batch button
 * (every side to the next/previous route, Shift clears every transmission). Left click selects the next route of a
 * face, right click the previous one.
 */
final class SideConfigWindow extends DfmScreen.Window {
    private static final int SIDE = 22;

    private final SideRules rules;
    private final List<Transmission> transmissions;
    private final Supplier<CompoundTag> data;
    private final BlockPos pos;
    private final Set<Transmission> ejectable;
    private final List<Component> automationHelp;
    private Transmission current;
    private RelativeFace focused;
    private ItemStack[] neighbors;

    /**
     * @param ejectable transmissions moved by the automation button (it is active on their tabs)
     * @param automationHelp lines explaining Auto Input and Auto Eject on this machine
     */
    SideConfigWindow(DfmScreen<?> screen, String key, SideRules rules, List<Transmission> transmissions, Supplier<CompoundTag> data,
                     BlockPos pos, Set<Transmission> ejectable, List<Component> automationHelp) {
        super(screen, key, 156);
        this.rules = rules;
        this.transmissions = transmissions;
        this.data = data;
        this.pos = pos;
        this.ejectable = ejectable;
        this.automationHelp = automationHelp;
        this.current = transmissions.getFirst();
    }

    private boolean ejectable(Transmission transmission) {
        return ejectable.contains(transmission);
    }

    @Override
    public Component title() {
        return Component.translatable("gui.dimensionalfluxminer.side_config.type", Component.translatable(current.translationKey()));
    }

    @Override
    public int height() {
        return 135;
    }

    @Override
    protected int titlePadEnd() {
        // Clears the automation button (12 px at x 136) with a 2 px gap.
        return 22;
    }

    Transmission current() {
        return current;
    }

    /** Route of the face under the mouse in the current tab (slot highlighting), NONE otherwise. */
    SideRoute focusedRoute() {
        if (focused == null) {
            return SideRoute.NONE;
        }
        return config().get(focused, current, data.get().getInt("profile"));
    }

    @Override
    protected void onClose() {
        focused = null;
        neighbors = null;
    }

    private SideConfiguration config() {
        SideConfiguration config = new SideConfiguration(rules);
        config.load(data.get().getCompound("sides"));
        return config;
    }

    static int color(Transmission t) {
        return switch (t) {
            case ITEM -> DfmScreen.TAB_ITEM;
            case ENERGY -> DfmScreen.TAB_ENERGY;
            case REDSTONE -> DfmScreen.TAB_REDSTONE;
            case EXPERIENCE -> DfmScreen.TAB_FLUID;
        };
    }

    static ResourceLocation icon(Transmission t) {
        return switch (t) {
            case ITEM -> DfmScreen.ICON_ITEMS;
            case ENERGY -> DfmScreen.ICON_ENERGY;
            case REDSTONE -> DfmScreen.ICON_REDSTONE_ON;
            case EXPERIENCE -> DfmScreen.ICON_FLUIDS;
        };
    }

    @Override
    protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data.get();
        SideConfiguration config = config();
        int profile = d.getInt("profile");

        for (int i = 0; i < transmissions.size(); i++) {
            Transmission t = transmissions.get(i);
            if (t == current) {
                continue;
            }
            boolean left = i < 4;
            screen.tab(g, left ? x - DfmScreen.TAB_SIZE : x + width, y + 2 + 28 * (i % 4), left, color(t), icon(t), true,
                    mouseX, mouseY, () -> {
                        current = t;
                        return -1;
                    }, null, () -> List.of(Component.translatable(t.translationKey())));
        }

        boolean ejectable = ejectable(current);
        boolean input = d.getBoolean("auto_input");
        boolean eject = d.getBoolean("auto_eject");
        String mode = input ? eject ? "both" : "input" : eject ? "eject" : "off";
        screen.innerScreen(g, x + 38, y + 25, 80, 12);
        Component screenText = ejectable
                ? Component.translatable("gui.dimensionalfluxminer.side_config.auto." + mode)
                : Component.translatable("gui.dimensionalfluxminer.side_config.no_eject");
        screen.fitted(g, screenText, x + 41, y + 28, 74, DfmScreen.SCREEN_TEXT, 0.8F, false);
        if (!ejectable) {
            screen.hotspot(x + 38, y + 25, 80, 12, null, null,
                    () -> List.of(Component.translatable("gui.dimensionalfluxminer.side_config.cant_eject")), 0);
        }

        // Automation mode: OFF -> Auto Input -> Auto Eject -> Input + Eject (right click goes back).
        screen.imageButton(g, x + 136, y + 6, 14, DfmScreen.sprite("button/auto_eject"), ejectable, false, mouseX, mouseY,
                () -> AbstractMachineBlockEntity.BUTTON_AUTO_IO_NEXT, () -> AbstractMachineBlockEntity.BUTTON_AUTO_IO_PREVIOUS, () -> {
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(input)));
                    lines.add(Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(eject)));
                    for (Component help : automationHelp) {
                        lines.add(help.copy().withStyle(ChatFormatting.GRAY));
                    }
                    lines.add(Component.translatable("gui.dimensionalfluxminer.side_config.auto.cycle").withStyle(ChatFormatting.DARK_GRAY));
                    return lines;
                });

        // Click: every side to the next route (or NONE when they differ); right click: previous; Shift: clear all.
        Transmission tab = current;
        SideRoute nextTarget = config.batchTarget(tab, profile, true);
        screen.imageButton(g, x + 136, y + 95, 14, DfmScreen.sprite("button/clear_sides"), true, false, mouseX, mouseY,
                () -> Screen.hasShiftDown() ? AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL
                        : AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + tab.ordinal() * 2,
                () -> Screen.hasShiftDown() ? AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL
                        : AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + tab.ordinal() * 2 + 1,
                () -> Screen.hasShiftDown() || nextTarget == SideRoute.NONE
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
                DfmScreen.SUBHEADING, 1.0F, true);
    }

    private void side(GuiGraphics g, SideConfiguration config, int profile, RelativeFace face, int x, int y, int mouseX, int mouseY) {
        List<SideRoute> allowed = rules.allowed(face, current, profile);
        boolean enabled = !allowed.isEmpty();
        boolean fixed = allowed.size() < 2;
        SideRoute route = enabled ? config.get(face, current, profile) : SideRoute.NONE;
        if (screen.hovered(x, y, SIDE, SIDE, mouseX, mouseY)) {
            focused = face;
        }
        int base = AbstractMachineBlockEntity.BUTTON_SIDE_BASE + face.ordinal() * 16 + current.ordinal() * 2;
        ItemStack neighbor = neighbors[face.ordinal()];
        screen.colorButton(g, x, y, SIDE, enabled ? route.color() & 0xFFFFFF : null, enabled, mouseX, mouseY,
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
                    if (enabled) {
                        lines.add(Component.translatable(fixed ? "gui.dimensionalfluxminer.side_fixed" : "gui.dimensionalfluxminer.side_click")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                    return lines;
                });
        if (!neighbor.isEmpty()) {
            g.renderItem(neighbor, x + 3, y + 3);
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
                BlockHitResult hit = new BlockHitResult(other.getCenter().relative(side.getOpposite(), 0.5), side.getOpposite(), other, false);
                result[face.ordinal()] = state.getCloneItemStack(hit, level, other, mc.player);
            }
        }
        return result;
    }
}
