package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.smelter.AlloySmelterBlockEntity;
import dimensionalfluxminer.smelter.SmelterMenu;
import dimensionalfluxminer.smelter.SmelterMode;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Alloy Smelter: the standard panel with the processing element ({@link SmelterLayout}) in the middle: a furnace
 * layout (three lanes, three outputs) or an alloy layout (three inputs feeding one chamber, main output +
 * byproduct). Three heat openings show the real progress of the running recipe, consumed from the top; the energy
 * bar on the left shimmers while it holds energy. Stored XP is not shown: it
 * drops as orbs when the block is broken and leaves through pipes on the XP faces. Tabs: Side Configuration,
 * Details, Auto Split and problems (left), upgrades, Rate, mode and redstone (right).
 */
public class SmelterScreen extends DfmScreen<SmelterMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = SmelterMenu.INVENTORY_TOP + 82;
    /** Top-left corner of the processing element: centered in the panel, right under the title. */
    private static final int ELEMENT_X = (WIDTH - SmelterLayout.WIDTH) / 2;
    private static final int ELEMENT_Y = 16;
    private static final int[] INPUT_X = offset(SmelterLayout.INPUT_X);
    private static final int INPUT_Y = ELEMENT_Y + SmelterLayout.INPUT_Y;
    private static final int ENERGY_X = 7;
    /** The energy bar shares its middle with the burners. */
    private static final int ENERGY_Y = ELEMENT_Y + SmelterLayout.BURNER_CENTER - ENERGY_FRAME_HEIGHT / 2;

    private static int[] offset(int[] xs) {
        int[] out = new int[xs.length];
        for (int i = 0; i < xs.length; i++) {
            out[i] = ELEMENT_X + xs[i];
        }
        return out;
    }

    private final SideConfigWindow sideWindow;
    private final MachineWindows.UpgradeWindow upgradeWindow;
    private final MachineWindows.InfoWindow detailsWindow;

    public SmelterScreen(SmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        sideWindow = new SideConfigWindow(this, "smelter.side_config", SideRules.SMELTER,
                List.of(Transmission.ITEM, Transmission.ENERGY, Transmission.EXPERIENCE), this::data, menu.pos(),
                Set.of(Transmission.ITEM, Transmission.EXPERIENCE), List.of(
                        Component.translatable("gui.dimensionalfluxminer.auto_input.help"),
                        Component.translatable("gui.dimensionalfluxminer.smelter.auto_eject.help")));
        upgradeWindow = new MachineWindows.UpgradeWindow(this, "smelter.upgrades", menu.upgradeSlot());
        detailsWindow = new MachineWindows.InfoWindow(this, "smelter.details", Component.translatable("gui.dimensionalfluxminer.details"),
                () -> {
                    CompoundTag d = data();
                    return List.of(
                            MachineText.statusLine(d),
                            Component.translatable("gui.dimensionalfluxminer.smelter.mode", modeName(d)),
                            Component.translatable("gui.dimensionalfluxminer.rate.tooltip", d.getInt("rate")),
                            Component.translatable("gui.dimensionalfluxminer.power_limit", fe(d.getLong("power")) + "/t"),
                            MachineText.energyLine(d),
                            Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(d.getBoolean("auto_input"))),
                            Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(d.getBoolean("auto_eject"))),
                            Component.translatable("gui.dimensionalfluxminer.auto_split", MachineText.onOff(d.getBoolean("auto_split"))),
                            MachineText.redstone(d));
                });
    }

    private static SmelterMode mode(CompoundTag d) {
        try {
            return SmelterMode.valueOf(d.getString("mode"));
        } catch (IllegalArgumentException e) {
            return SmelterMode.ALL;
        }
    }

    private static Component modeName(CompoundTag d) {
        return Component.translatable(mode(d).translationKey());
    }

    @Override
    protected int inventoryTop() {
        return SmelterMenu.INVENTORY_TOP;
    }

    @Override
    protected void renderMain(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data();
        SmelterLayout.Variant variant = SmelterLayout.Variant.of(mode(d));
        boolean alloyRunning = d.getBoolean("alloy_running");
        HolderLookup.Provider registries = menu.player().registryAccess();
        Component mode = modeName(d);
        boolean blocked = "output_blocked".equals(d.getString("status"));
        boolean operating = "active".equals(d.getString("state"));
        int ex = x + ELEMENT_X;
        int ey = y + ELEMENT_Y;
        SmelterLayout.drawElement(g, ex, ey, variant);

        ListTag lanes = d.getList("lanes", Tag.TAG_COMPOUND);
        for (int lane = 0; lane < AlloySmelterBlockEntity.LANES; lane++) {
            CompoundTag laneTag = lane < lanes.size() ? lanes.getCompound(lane) : new CompoundTag();
            int ix = ex + SmelterLayout.INPUT_X[lane];
            int iy = ey + SmelterLayout.INPUT_Y;
            QueueView in = QueueView.read(laneTag.getList("in", Tag.TAG_COMPOUND), registries);
            if (!in.isEmpty()) {
                queueEntry(g, in.prototype(0), in.count(0), ix + 1, iy + 1);
            }
            slotHover(g, ix, iy, mouseX, mouseY);
            int index = lane;
            hotspot(ix, iy, 18, 18,
                    () -> menu.getCarried().isEmpty() ? AlloySmelterBlockEntity.BUTTON_TAKE_INPUT + index
                            : AlloySmelterBlockEntity.BUTTON_INSERT_LANE + index,
                    () -> menu.getCarried().isEmpty() ? AlloySmelterBlockEntity.BUTTON_TAKE_INPUT_HALF + index
                            : AlloySmelterBlockEntity.BUTTON_INSERT_ONE_LANE + index,
                    () -> in.slotTooltip(0, "gui.dimensionalfluxminer.smelter.input", index + 1),
                    AlloySmelterBlockEntity.BUTTON_SHIFT, creativePick(AlloySmelterBlockEntity.BUTTON_PICK_INPUT + index));
            // Smelting: each opening shows the progress of its own lane.
            if (operating && !alloyRunning && !in.isEmpty()) {
                SmelterLayout.heat(g, ex + variant.heatX[lane], ey + variant.heatY, laneTag.getDouble("progress"));
            }
        }
        // Alloy: one combined operation, the three openings share its progress.
        if (operating && alloyRunning) {
            for (int hx : variant.heatX) {
                SmelterLayout.heat(g, ex + hx, ey + variant.heatY, d.getDouble("alloy_progress"));
            }
        }
        for (int i = 0; i < variant.outputX.length; i++) {
            CompoundTag laneTag = i < lanes.size() ? lanes.getCompound(i) : new CompoundTag();
            QueueView out = QueueView.read(laneTag.getList("out", Tag.TAG_COMPOUND), registries);
            int number = i + 1;
            Supplier<List<Component>> tooltip = variant == SmelterLayout.Variant.ALLOY
                    ? () -> out.slotTooltip(0, number == 1 ? "gui.dimensionalfluxminer.smelter.main_output"
                            : "gui.dimensionalfluxminer.smelter.byproduct", 0)
                    : () -> out.slotTooltip(0, "gui.dimensionalfluxminer.smelter.output", number);
            outputBox(g, blocked, out, ex + variant.outputX[i], ey + SmelterLayout.OUTPUT_Y,
                    AlloySmelterBlockEntity.BUTTON_TAKE_OUTPUT + i, tooltip, mouseX, mouseY);
        }
        // The heat chamber opens this machine's recipes in EMI / JEI.
        recipeArea(ex + SmelterLayout.CHAMBER_X, ey + SmelterLayout.CHAMBER_Y, SmelterLayout.CHAMBER_W, SmelterLayout.CHAMBER_H,
                () -> switch (variant) {
            case FURNACE -> new ResourceLocation[] {RecipeViewers.SMELTING};
            case ALLOY -> new ResourceLocation[] {RecipeViewers.ALLOYING};
            case ALL -> alloyRunning ? new ResourceLocation[] {RecipeViewers.ALLOYING, RecipeViewers.SMELTING}
                    : new ResourceLocation[] {RecipeViewers.SMELTING, RecipeViewers.ALLOYING};
        }, () -> List.of(MachineText.statusLine(d)));

        long cap = Math.max(1, d.getLong("fe_capacity"));
        energyBar(g, x + ENERGY_X, y + ENERGY_Y, ENERGY_FRAME_HEIGHT, (double) d.getLong("fe") / cap);
        hotspot(x + ENERGY_X, y + ENERGY_Y, 14, ENERGY_FRAME_HEIGHT, null, null, () -> MachineText.energyTooltip(d), 0);

        MachineTabs.sideConfig(this, g, x, y, sideWindow, mouseX, mouseY);
        MachineTabs.details(this, g, x, y, true, MachineTabs.ROW_2, detailsWindow, mouseX, mouseY);
        MachineTabs.upgrades(this, g, x, y, upgradeWindow, mouseX, mouseY);
        MachineTabs.rate(this, g, x, y, d, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE_BACK,
                List.of(Component.translatable("gui.dimensionalfluxminer.rate.consumption", fe(d.getLong("power"))),
                        Component.translatable("gui.dimensionalfluxminer.rate.speed_lane", number(d.getDouble("lane_speed") * 20))),
                mouseX, mouseY);
        ResourceLocation modeIcon = switch (variant) {
            case FURNACE -> ICON_SMELTING;
            case ALLOY -> ICON_SMELTING_ALLOY;
            case ALL -> ICON_SMELTING_ALLOY_FURNACE;
        };
        tab(g, x + imageWidth, y + MachineTabs.ROW_3, false, TAB_DEFAULT, modeIcon, true, mouseX, mouseY,
                () -> AlloySmelterBlockEntity.BUTTON_MODE, () -> AlloySmelterBlockEntity.BUTTON_MODE_BACK, () -> List.of(
                        Component.translatable("gui.dimensionalfluxminer.smelter.mode", mode),
                        Component.translatable("gui.dimensionalfluxminer.smelter.mode.tooltip").withStyle(ChatFormatting.DARK_GRAY)));
        boolean split = d.getBoolean("auto_split");
        iconTab(g, x - TAB_SIZE, y + MachineTabs.ROW_3, true, TAB_DEFAULT, ICON_SORTING, TAB_INNER, true, split, mouseX, mouseY,
                () -> AlloySmelterBlockEntity.BUTTON_AUTO_SPLIT, null, () -> List.of(
                        Component.translatable("gui.dimensionalfluxminer.auto_split", MachineText.onOff(split)),
                        Component.translatable("gui.dimensionalfluxminer.auto_split.help").withStyle(ChatFormatting.GRAY)));
        MachineTabs.redstone(this, g, x, y, d, mouseX, mouseY);
        MachineTabs.problems(this, g, x, y, d);

        if (isWindowOpen(sideWindow) && sideWindow.current() == Transmission.ITEM) {
            SideRoute route = sideWindow.focusedRoute();
            int lane = route.dedicatedLane();
            if (lane >= 0) {
                MachineTabs.outline(g, x + INPUT_X[lane] - 1, y + INPUT_Y - 1, 20, 20, route.color());
            } else if (route.insertsItems()) {
                for (int ix : INPUT_X) {
                    MachineTabs.outline(g, x + ix - 1, y + INPUT_Y - 1, 20, 20, SideRoute.ITEM_INPUT_ALL.color());
                }
            }
            if (route.extractsItems()) {
                for (int ox : variant.outputX) {
                    MachineTabs.outline(g, ex + ox - 1, ey + SmelterLayout.OUTPUT_Y - 1, 20, 20, SideRoute.ITEM_OUTPUT.color());
                }
            }
        }
    }

    private void outputBox(GuiGraphics g, boolean blocked, QueueView view, int bx, int by, int buttonId, Supplier<List<Component>> tooltip,
                           int mouseX, int mouseY) {
        if (blocked) {
            slot(g, SlotKind.OUTPUT_WARNING, bx, by);
        }
        if (!view.isEmpty()) {
            queueEntry(g, view.prototype(0), view.count(0), bx + 1, by + 1);
        }
        slotHover(g, bx, by, mouseX, mouseY);
        int index = buttonId - AlloySmelterBlockEntity.BUTTON_TAKE_OUTPUT;
        hotspot(bx, by, 18, 18, () -> buttonId, () -> AlloySmelterBlockEntity.BUTTON_TAKE_OUTPUT_HALF + index, tooltip,
                AlloySmelterBlockEntity.BUTTON_SHIFT, creativePick(AlloySmelterBlockEntity.BUTTON_PICK_OUTPUT + index));
    }
}
