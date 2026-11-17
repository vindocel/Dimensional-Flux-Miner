package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import dimensionalfluxminer.furnace.ElectricFurnaceMenu;
import dimensionalfluxminer.furnace.ElectricFurnaceMode;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.entity.player.Inventory;

/**
 * Electric Furnace: the standard panel with the processing element ({@link ElectricFurnaceLayout}) in the middle: a furnace
 * layout (three lanes, three outputs) or an alloy layout (three inputs feeding one chamber, main output +
 * byproduct). Three heat openings show the real progress of the running recipe, consumed from the top; the energy
 * bar on the left shimmers while it holds energy. Stored XP is not shown: it
 * drops as orbs when the block is broken and leaves through pipes on the XP faces. Tabs: Side Configuration,
 * Details, Auto Split and problems (left), upgrades, Rate, mode and redstone (right).
 */
public class ElectricFurnaceScreen extends DfmScreen<ElectricFurnaceMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = ElectricFurnaceMenu.INVENTORY_TOP + 82;
    /** Top-left corner of the processing element: centered in the panel, right under the title. */
    private static final int ELEMENT_X = (WIDTH - ElectricFurnaceLayout.WIDTH) / 2;
    private static final int ELEMENT_Y = 16;
    private static final int[] INPUT_X = offset(ElectricFurnaceLayout.INPUT_X);
    private static final int INPUT_Y = ELEMENT_Y + ElectricFurnaceLayout.INPUT_Y;
    private static final int ENERGY_X = 7;
    /** The energy bar shares its middle with the burners. */
    private static final int ENERGY_Y = ELEMENT_Y + ElectricFurnaceLayout.BURNER_CENTER - ENERGY_FRAME_HEIGHT / 2;
    /** Recipe-selection button (16 x 16, optional integration): right of the element, level with the outputs. */
    public static final int RECIPE_BUTTON_X = ELEMENT_X + ElectricFurnaceLayout.WIDTH + 4;
    public static final int RECIPE_BUTTON_Y = ELEMENT_Y + ElectricFurnaceLayout.OUTPUT_Y + 1;

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

    public ElectricFurnaceScreen(ElectricFurnaceMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, WIDTH, HEIGHT);
        //?} else {
        /*super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        *///?}
        sideWindow = new SideConfigWindow(this, "electric_furnace.side_config", SideRules.ELECTRIC_FURNACE,
                List.of(Transmission.ITEM, Transmission.ENERGY, Transmission.EXPERIENCE), this::data, menu.pos(),
                Map.of(Transmission.ITEM, new SideConfigWindow.AutomationTab(SideConfigWindow.Automation.ITEMS, List.of(
                                Component.translatable("gui.dimensionalfluxminer.auto_input.help"),
                                Component.translatable("gui.dimensionalfluxminer.auto_eject.help"))),
                        Transmission.EXPERIENCE, new SideConfigWindow.AutomationTab(SideConfigWindow.Automation.FLUID_OUTPUT, List.of(
                                Component.translatable("gui.dimensionalfluxminer.electric_furnace.auto_eject_fluid.help")))));
        upgradeWindow = new MachineWindows.UpgradeWindow(this, "electric_furnace.upgrades", menu.upgradeSlot());
        detailsWindow = new MachineWindows.InfoWindow(this, "electric_furnace.details", Component.translatable("gui.dimensionalfluxminer.details"),
                () -> {
                    CompoundTag d = data();
                    return List.of(
                            MachineText.statusLine(d),
                            Component.translatable("gui.dimensionalfluxminer.electric_furnace.mode", modeName(d)),
                            //? if >=26.1 {
                            Component.translatable("gui.dimensionalfluxminer.rate.tooltip", d.getIntOr("rate", 0)),
                            Component.translatable("gui.dimensionalfluxminer.power_limit", fe(d.getLongOr("power", 0L)) + "/t"),
                            //?} else {
                            /*Component.translatable("gui.dimensionalfluxminer.rate.tooltip", d.getInt("rate")),
                            Component.translatable("gui.dimensionalfluxminer.power_limit", fe(d.getLong("power")) + "/t"),
                            *///?}
                            MachineText.energyLine(d),
                            //? if >=26.1 {
                            Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(d.getBooleanOr("auto_input", false))),
                            Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(d.getBooleanOr("auto_eject", false))),
                            Component.translatable("gui.dimensionalfluxminer.auto_eject_fluid", MachineText.onOff(d.getBooleanOr("auto_eject_fluid", false))),
                            Component.translatable("gui.dimensionalfluxminer.auto_split", MachineText.onOff(d.getBooleanOr("auto_split", false))),
                            //?} else {
                            /*Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(d.getBoolean("auto_input"))),
                            Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(d.getBoolean("auto_eject"))),
                            Component.translatable("gui.dimensionalfluxminer.auto_eject_fluid", MachineText.onOff(d.getBoolean("auto_eject_fluid"))),
                            Component.translatable("gui.dimensionalfluxminer.auto_split", MachineText.onOff(d.getBoolean("auto_split"))),
                            *///?}
                            MachineText.redstone(d));
                });
    }

    private static ElectricFurnaceMode mode(CompoundTag d) {
        try {
            //? if >=26.1 {
            return ElectricFurnaceMode.valueOf(d.getStringOr("mode", ""));
            //?} else {
            /*return ElectricFurnaceMode.valueOf(d.getString("mode"));
            *///?}
        } catch (IllegalArgumentException e) {
            return ElectricFurnaceMode.ALL;
        }
    }

    private static Component modeName(CompoundTag d) {
        return Component.translatable(mode(d).translationKey());
    }

    @Override
    protected int inventoryTop() {
        return ElectricFurnaceMenu.INVENTORY_TOP;
    }

    @Override
    //? if >=26.1 {
    protected void renderMain(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
    //?} else {
    /*protected void renderMain(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
    *///?}
        CompoundTag d = data();
        ElectricFurnaceLayout.Variant variant = ElectricFurnaceLayout.Variant.of(mode(d));
        //? if >=26.1 {
        boolean alloyRunning = d.getBooleanOr("alloy_running", false);
        //?} else {
        /*boolean alloyRunning = d.getBoolean("alloy_running");
        *///?}
        HolderLookup.Provider registries = menu.player().registryAccess();
        Component mode = modeName(d);
        //? if >=26.1 {
        boolean blocked = "output_blocked".equals(d.getStringOr("status", ""));
        boolean operating = "active".equals(d.getStringOr("state", ""));
        //?} else {
        /*boolean blocked = "output_blocked".equals(d.getString("status"));
        boolean operating = "active".equals(d.getString("state"));
        *///?}
        int ex = x + ELEMENT_X;
        int ey = y + ELEMENT_Y;
        ElectricFurnaceLayout.drawElement(g, ex, ey, variant);

        //? if >=26.1 {
        ListTag lanes = d.getListOrEmpty("lanes");
        //?} else {
        /*ListTag lanes = d.getList("lanes", Tag.TAG_COMPOUND);
        *///?}
        for (int lane = 0; lane < ElectricFurnaceBlockEntity.LANES; lane++) {
            //? if >=26.1 {
            CompoundTag laneTag = lane < lanes.size() ? lanes.getCompoundOrEmpty(lane) : new CompoundTag();
            //?} else {
            /*CompoundTag laneTag = lane < lanes.size() ? lanes.getCompound(lane) : new CompoundTag();
            *///?}
            int ix = ex + ElectricFurnaceLayout.INPUT_X[lane];
            int iy = ey + ElectricFurnaceLayout.INPUT_Y;
            //? if >=26.1 {
            QueueView in = QueueView.read(laneTag.getListOrEmpty("in"), registries);
            //?} else {
            /*QueueView in = QueueView.read(laneTag.getList("in", Tag.TAG_COMPOUND), registries);
            *///?}
            if (!in.isEmpty()) {
                queueEntry(g, in.prototype(0), in.count(0), ix + 1, iy + 1);
            }
            slotHover(g, ix, iy, mouseX, mouseY);
            int index = lane;
            hotspot(ix, iy, 18, 18,
                    () -> menu.getCarried().isEmpty() ? ElectricFurnaceBlockEntity.BUTTON_TAKE_INPUT + index
                            : ElectricFurnaceBlockEntity.BUTTON_INSERT_LANE + index,
                    () -> menu.getCarried().isEmpty() ? ElectricFurnaceBlockEntity.BUTTON_TAKE_INPUT_HALF + index
                            : ElectricFurnaceBlockEntity.BUTTON_INSERT_ONE_LANE + index,
                    () -> in.slotTooltip(0, "gui.dimensionalfluxminer.electric_furnace.input", index + 1),
                    ElectricFurnaceBlockEntity.BUTTON_SHIFT, creativePick(ElectricFurnaceBlockEntity.BUTTON_PICK_INPUT + index));
            // Smelting: each opening shows the progress of its own lane.
            if (operating && !alloyRunning && !in.isEmpty()) {
                //? if >=26.1 {
                ElectricFurnaceLayout.heat(g, ex + variant.heatX[lane], ey + variant.heatY, laneTag.getDoubleOr("progress", 0.0));
                //?} else {
                /*ElectricFurnaceLayout.heat(g, ex + variant.heatX[lane], ey + variant.heatY, laneTag.getDouble("progress"));
                *///?}
            }
        }
        // Alloy: one combined operation, the three openings share its progress.
        if (operating && alloyRunning) {
            for (int hx : variant.heatX) {
                //? if >=26.1 {
                ElectricFurnaceLayout.heat(g, ex + hx, ey + variant.heatY, d.getDoubleOr("alloy_progress", 0.0));
                //?} else {
                /*ElectricFurnaceLayout.heat(g, ex + hx, ey + variant.heatY, d.getDouble("alloy_progress"));
                *///?}
            }
        }
        for (int i = 0; i < variant.outputX.length; i++) {
            //? if >=26.1 {
            CompoundTag laneTag = i < lanes.size() ? lanes.getCompoundOrEmpty(i) : new CompoundTag();
            QueueView out = QueueView.read(laneTag.getListOrEmpty("out"), registries);
            //?} else {
            /*CompoundTag laneTag = i < lanes.size() ? lanes.getCompound(i) : new CompoundTag();
            QueueView out = QueueView.read(laneTag.getList("out", Tag.TAG_COMPOUND), registries);
            *///?}
            int number = i + 1;
            Supplier<List<Component>> tooltip = variant == ElectricFurnaceLayout.Variant.ALLOY
                    ? () -> out.slotTooltip(0, number == 1 ? "gui.dimensionalfluxminer.electric_furnace.main_output"
                            : "gui.dimensionalfluxminer.electric_furnace.byproduct", 0)
                    : () -> out.slotTooltip(0, "gui.dimensionalfluxminer.electric_furnace.output", number);
            outputBox(g, blocked, out, ex + variant.outputX[i], ey + ElectricFurnaceLayout.OUTPUT_Y,
                    ElectricFurnaceBlockEntity.BUTTON_TAKE_OUTPUT + i, tooltip, mouseX, mouseY);
        }
        // The heat chamber opens this machine's recipes in EMI / JEI.
        recipeArea(ex + ElectricFurnaceLayout.CHAMBER_X, ey + ElectricFurnaceLayout.CHAMBER_Y, ElectricFurnaceLayout.CHAMBER_W, ElectricFurnaceLayout.CHAMBER_H,
                () -> switch (variant) {
            //? if >=26.1 {
            case FURNACE -> new Identifier[] {RecipeViewers.SMELTING};
            case ALLOY -> new Identifier[] {RecipeViewers.ALLOYING};
            case ALL -> alloyRunning ? new Identifier[] {RecipeViewers.ALLOYING, RecipeViewers.SMELTING}
                    : new Identifier[] {RecipeViewers.SMELTING, RecipeViewers.ALLOYING};
            //?} else {
            /*case FURNACE -> new ResourceLocation[] {RecipeViewers.SMELTING};
            case ALLOY -> new ResourceLocation[] {RecipeViewers.ALLOYING};
            case ALL -> alloyRunning ? new ResourceLocation[] {RecipeViewers.ALLOYING, RecipeViewers.SMELTING}
                    : new ResourceLocation[] {RecipeViewers.SMELTING, RecipeViewers.ALLOYING};
            *///?}
        }, () -> List.of(MachineText.statusLine(d)));

        //? if >=26.1 {
        long cap = Math.max(1, d.getLongOr("fe_capacity", 0L));
        energyBar(g, x + ENERGY_X, y + ENERGY_Y, ENERGY_FRAME_HEIGHT, (double) d.getLongOr("fe", 0L) / cap);
        //?} else {
        /*long cap = Math.max(1, d.getLong("fe_capacity"));
        energyBar(g, x + ENERGY_X, y + ENERGY_Y, ENERGY_FRAME_HEIGHT, (double) d.getLong("fe") / cap);
        *///?}
        hotspot(x + ENERGY_X, y + ENERGY_Y, 14, ENERGY_FRAME_HEIGHT, null, null, () -> MachineText.energyTooltip(d), 0);

        MachineTabs.sideConfig(this, g, x, y, sideWindow, mouseX, mouseY);
        MachineTabs.details(this, g, x, y, true, MachineTabs.ROW_2, detailsWindow, mouseX, mouseY);
        MachineTabs.upgrades(this, g, x, y, upgradeWindow, mouseX, mouseY);
        MachineTabs.rate(this, g, x, y, d, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE_BACK,
                //? if >=26.1 {
                List.of(Component.translatable("gui.dimensionalfluxminer.rate.consumption", fe(d.getLongOr("power", 0L))),
                        Component.translatable("gui.dimensionalfluxminer.rate.speed_lane", number(d.getDoubleOr("lane_speed", 0.0) * 20))),
                //?} else {
                /*List.of(Component.translatable("gui.dimensionalfluxminer.rate.consumption", fe(d.getLong("power"))),
                        Component.translatable("gui.dimensionalfluxminer.rate.speed_lane", number(d.getDouble("lane_speed") * 20))),
                *///?}
                mouseX, mouseY);
        GuiIcon modeIcon = switch (variant) {
            case FURNACE -> ICON_SMELTING;
            case ALLOY -> ICON_SMELTING_ALLOY;
            case ALL -> ICON_SMELTING_ALLOY_FURNACE;
        };
        tab(g, x + imageWidth, y + MachineTabs.ROW_3, false, TAB_DEFAULT, modeIcon, true, mouseX, mouseY,
                () -> ElectricFurnaceBlockEntity.BUTTON_MODE, () -> ElectricFurnaceBlockEntity.BUTTON_MODE_BACK, () -> List.of(
                        Component.translatable("gui.dimensionalfluxminer.electric_furnace.mode", mode),
                        Component.translatable("gui.dimensionalfluxminer.electric_furnace.mode.tooltip").withStyle(ChatFormatting.DARK_GRAY)));
        //? if >=26.1 {
        boolean split = d.getBooleanOr("auto_split", false);
        //?} else {
        /*boolean split = d.getBoolean("auto_split");
        *///?}
        iconTab(g, x - TAB_SIZE, y + MachineTabs.ROW_3, true, TAB_DEFAULT, ICON_SORTING, TAB_INNER, true, split, mouseX, mouseY,
                () -> ElectricFurnaceBlockEntity.BUTTON_AUTO_SPLIT, null, () -> List.of(
                        Component.translatable("gui.dimensionalfluxminer.auto_split", MachineText.onOff(split)),
                        Component.translatable("gui.dimensionalfluxminer.auto_split.help").withStyle(ChatFormatting.GRAY)));
        themeTab(g, x, y, MachineTabs.ROW_4, mouseX, mouseY);
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
                    MachineTabs.outline(g, ex + ox - 1, ey + ElectricFurnaceLayout.OUTPUT_Y - 1, 20, 20, SideRoute.ITEM_OUTPUT.color());
                }
            }
        }
    }

    //? if >=26.1 {
    private void outputBox(GuiGraphicsExtractor g, boolean blocked, QueueView view, int bx, int by, int buttonId, Supplier<List<Component>> tooltip,
    //?} else {
    /*private void outputBox(GuiGraphics g, boolean blocked, QueueView view, int bx, int by, int buttonId, Supplier<List<Component>> tooltip,
    *///?}
                           int mouseX, int mouseY) {
        if (blocked) {
            slot(g, SlotKind.OUTPUT_WARNING, bx, by);
        }
        if (!view.isEmpty()) {
            queueEntry(g, view.prototype(0), view.count(0), bx + 1, by + 1);
        }
        slotHover(g, bx, by, mouseX, mouseY);
        int index = buttonId - ElectricFurnaceBlockEntity.BUTTON_TAKE_OUTPUT;
        hotspot(bx, by, 18, 18, () -> buttonId, () -> ElectricFurnaceBlockEntity.BUTTON_TAKE_OUTPUT_HALF + index, tooltip,
                ElectricFurnaceBlockEntity.BUTTON_SHIFT, creativePick(ElectricFurnaceBlockEntity.BUTTON_PICK_OUTPUT + index));
    }
}
