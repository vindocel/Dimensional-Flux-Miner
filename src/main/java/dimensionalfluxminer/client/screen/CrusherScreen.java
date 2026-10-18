package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.compat.RecipeViewers;
import dimensionalfluxminer.crusher.CrusherMenu;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Crusher: the processing mechanism ({@link CrusherLayout}: input, grinding chamber with the mill and two progress
 * bars, output pipe) feeding the 2 x 2 output slots, and the energy bar. Tabs: Side Configuration, Details and
 * problems (left), upgrades, Rate and redstone (right).
 */
public class CrusherScreen extends DfmScreen<CrusherMenu> {
    /** Mechanism position; the 2 x 2 output slots continue its output pipe, centered on it. */
    private static final int MECH_X = 33;
    private static final int MECH_Y = 37;
    private static final int ENERGY_Y = 13;
    private static final int OUT_X = CrusherLayout.PIPE_END;
    private static final int OUT_Y = (CrusherLayout.HEIGHT - 36) / 2;

    private final SideConfigWindow sideWindow;
    private final MachineWindows.UpgradeWindow upgradeWindow;
    private final MachineWindows.InfoWindow detailsWindow;

    public CrusherScreen(CrusherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = CrusherMenu.INVENTORY_TOP + 82;
        sideWindow = new SideConfigWindow(this, "crusher.side_config", SideRules.CRUSHER,
                List.of(Transmission.ITEM, Transmission.ENERGY), this::data, menu.pos(),
                Map.of(Transmission.ITEM, new SideConfigWindow.AutomationTab(SideConfigWindow.Automation.ITEMS, List.of(
                        Component.translatable("gui.dimensionalfluxminer.auto_input.help"),
                        Component.translatable("gui.dimensionalfluxminer.auto_eject.help"),
                        Component.translatable("gui.dimensionalfluxminer.crusher.top_note")))));
        upgradeWindow = new MachineWindows.UpgradeWindow(this, "crusher.upgrades", menu.upgradeSlot());
        detailsWindow = new MachineWindows.InfoWindow(this, "crusher.details", Component.translatable("gui.dimensionalfluxminer.details"),
                () -> {
                    CompoundTag d = data();
                    return List.of(
                            MachineText.statusLine(d),
                            Component.translatable("gui.dimensionalfluxminer.rate.tooltip", d.getInt("rate")),
                            Component.translatable("gui.dimensionalfluxminer.power_limit", fe(d.getLong("power")) + "/t"),
                            Component.translatable("gui.dimensionalfluxminer.throughput", number(d.getDouble("ops"))),
                            MachineText.energyLine(d),
                            Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(d.getBoolean("auto_input"))),
                            Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(d.getBoolean("auto_eject"))),
                            MachineText.redstone(d));
                });
    }

    @Override
    protected int inventoryTop() {
        return CrusherMenu.INVENTORY_TOP;
    }

    @Override
    protected void renderMain(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data();
        var registries = menu.player().registryAccess();
        int mx = x + MECH_X;
        int my = y + MECH_Y;
        boolean operating = "active".equals(d.getString("state"));

        // Mechanism up to the end of its output pipe, then the four output slots.
        CrusherLayout.drawMechanism(g, mx, my, CrusherLayout.PIPE_END);
        CrusherLayout.drawMill(g, mx, my, operating);
        CrusherLayout.drawProgress(g, mx, my, operating ? d.getDouble("progress") : 0);
        recipeArea(mx + CrusherLayout.CHAMBER_X, my, CrusherLayout.CHAMBER_WIDTH, CrusherLayout.HEIGHT,
                () -> new ResourceLocation[] {RecipeViewers.CRUSHING}, () -> List.of(
                        MachineText.statusLine(d),
                        Component.translatable("gui.dimensionalfluxminer.throughput", number(d.getDouble("ops")))));

        // Input queue (click with an item to insert, empty hand to take back, shift = to inventory)
        int ix = mx + CrusherLayout.INPUT_X;
        int iy = my + CrusherLayout.INPUT_Y;
        QueueView input = QueueView.read(d.getList("input", Tag.TAG_COMPOUND), registries);
        if (!input.isEmpty()) {
            queueEntry(g, input.prototype(0), input.count(0), ix + 1, iy + 1);
        }
        slotHover(g, ix, iy, mouseX, mouseY);
        hotspot(ix, iy, 18, 18, () -> menu.getCarried().isEmpty() ? MechanicalCrusherBlockEntity.BUTTON_TAKE_INPUT
                        : MechanicalCrusherBlockEntity.BUTTON_INSERT_CARRIED,
                () -> menu.getCarried().isEmpty() ? MechanicalCrusherBlockEntity.BUTTON_TAKE_INPUT_HALF
                        : MechanicalCrusherBlockEntity.BUTTON_INSERT_ONE,
                () -> input.slotTooltip(0, "gui.dimensionalfluxminer.crusher.input"),
                MechanicalCrusherBlockEntity.BUTTON_SHIFT, creativePick(MechanicalCrusherBlockEntity.BUTTON_PICK_INPUT));

        boolean blocked = "output_blocked".equals(d.getString("status"));
        QueueView output = QueueView.read(d.getList("output", Tag.TAG_COMPOUND), registries);
        for (int i = 0; i < MechanicalCrusherBlockEntity.OUTPUT_KINDS; i++) {
            int ox = mx + OUT_X + (i % 2) * 18;
            int oy = my + OUT_Y + (i / 2) * 18;
            slot(g, blocked ? SlotKind.OUTPUT_WARNING : SlotKind.NORMAL, ox, oy);
            if (i < output.size()) {
                queueEntry(g, output.prototype(i), output.count(i), ox + 1, oy + 1);
            }
            slotHover(g, ox, oy, mouseX, mouseY);
            int kind = i;
            hotspot(ox, oy, 18, 18, () -> MechanicalCrusherBlockEntity.BUTTON_TAKE_OUTPUT + kind,
                    () -> MechanicalCrusherBlockEntity.BUTTON_TAKE_OUTPUT_HALF + kind,
                    () -> output.slotTooltip(kind, "gui.dimensionalfluxminer.crusher.output"),
                    MechanicalCrusherBlockEntity.BUTTON_SHIFT, creativePick(MechanicalCrusherBlockEntity.BUTTON_PICK_OUTPUT + kind));
        }

        long cap = Math.max(1, d.getLong("fe_capacity"));
        energyBar(g, x + 7, y + ENERGY_Y, ENERGY_FRAME_HEIGHT, (double) d.getLong("fe") / cap);
        hotspot(x + 7, y + ENERGY_Y, 14, ENERGY_FRAME_HEIGHT, null, null, () -> MachineText.energyTooltip(d), 0);

        MachineTabs.sideConfig(this, g, x, y, sideWindow, mouseX, mouseY);
        MachineTabs.details(this, g, x, y, true, MachineTabs.ROW_2, detailsWindow, mouseX, mouseY);
        MachineTabs.upgrades(this, g, x, y, upgradeWindow, mouseX, mouseY);
        MachineTabs.rate(this, g, x, y, d, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE, AbstractMachineBlockEntity.BUTTON_RATE_CYCLE_BACK,
                List.of(Component.translatable("gui.dimensionalfluxminer.rate.consumption", fe(d.getLong("power"))),
                        Component.translatable("gui.dimensionalfluxminer.rate.speed", number(d.getDouble("ops") * 20))), mouseX, mouseY);
        MachineTabs.redstone(this, g, x, y, d, mouseX, mouseY);
        MachineTabs.problems(this, g, x, y, d);

        if (isWindowOpen(sideWindow) && sideWindow.current() == Transmission.ITEM) {
            SideRoute route = sideWindow.focusedRoute();
            if (route.insertsItems()) {
                MachineTabs.outline(g, x + MECH_X + CrusherLayout.INPUT_X - 1, y + MECH_Y + CrusherLayout.INPUT_Y - 1, 20, 20, route.color());
            }
            if (route.extractsItems()) {
                MachineTabs.outline(g, x + MECH_X + OUT_X - 1, y + MECH_Y + OUT_Y - 1, 38, 38, SideRoute.ITEM_OUTPUT.color());
            }
        }
    }
}
