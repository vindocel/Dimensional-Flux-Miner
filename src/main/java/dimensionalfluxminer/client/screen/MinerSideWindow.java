package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLayout;
import dimensionalfluxminer.miner.MinerPorts;
import dimensionalfluxminer.miner.MinerStructure;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Side Configuration of the Dimensional Flux Miner: the nine port faces of the lower layer seen from above, with the
 * terminal at the bottom (back row on top, left and right sides as columns, front to the bottom). Two tabs:
 * <ul>
 *     <li>Items: each face cycles none, Item Output, Matter Input (Stabilized Core only) and both; one face per role.
 *     The automation button sets Auto Input / Auto Eject.</li>
 *     <li>Energy: by default every face accepts energy; clicking faces picks them by hand, and the bottom button returns
 *     to the default.</li>
 * </ul>
 */
final class MinerSideWindow extends DfmScreen.Window {
    static final int OUTPUT_COLOR = 0xF08A24;
    static final int MATTER_COLOR = 0xA05CF0;
    static final int ENERGY_COLOR = dimensionalfluxminer.machine.side.SideRoute.ENERGY_INPUT.color() & 0xFFFFFF;
    private static final int SIDE = 20;
    private static final int STEP = 21;
    /** Positions of the faces: back row, left column (back to front), right column (back to front). */
    private static final int[][] POSITIONS = new int[MinerPorts.Face.VALUES.length][];

    static {
        int top = 42;
        int left = 27;
        int right = 111;
        POSITIONS[MinerPorts.Face.REAR_WEST.ordinal()] = new int[] {48, top};
        POSITIONS[MinerPorts.Face.REAR_CENTER.ordinal()] = new int[] {48 + STEP, top};
        POSITIONS[MinerPorts.Face.REAR_EAST.ordinal()] = new int[] {48 + 2 * STEP, top};
        POSITIONS[MinerPorts.Face.WEST_REAR.ordinal()] = new int[] {left, top + STEP};
        POSITIONS[MinerPorts.Face.WEST_CENTER.ordinal()] = new int[] {left, top + 2 * STEP};
        POSITIONS[MinerPorts.Face.WEST_FRONT.ordinal()] = new int[] {left, top + 3 * STEP};
        POSITIONS[MinerPorts.Face.EAST_REAR.ordinal()] = new int[] {right, top + STEP};
        POSITIONS[MinerPorts.Face.EAST_CENTER.ordinal()] = new int[] {right, top + 2 * STEP};
        POSITIONS[MinerPorts.Face.EAST_FRONT.ordinal()] = new int[] {right, top + 3 * STEP};
    }

    private final Supplier<CompoundTag> data;
    private final BlockPos terminal;
    private Transmission current = Transmission.ITEM;
    private ItemStack[] neighbors;

    MinerSideWindow(DfmScreen<?> screen, Supplier<CompoundTag> data, BlockPos terminal) {
        super(screen, "miner.side_config", 156);
        this.data = data;
        this.terminal = terminal;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.dimensionalfluxminer.side_config.type", Component.translatable(current.translationKey()));
    }

    @Override
    public int height() {
        return 146;
    }

    @Override
    protected int titlePadEnd() {
        // Clears the two automation switches with a 2 px gap.
        return 156 - SideConfigWindow.INPUT_BUTTON_X + 2;
    }

    /** Tooltip of an automation switch of the item ports (only on the Items tab). */
    private static List<Component> switchTooltip(boolean items, Component state, String noteKey) {
        if (!items) {
            return List.of(Component.translatable("gui.dimensionalfluxminer.side_config.cant_eject"));
        }
        return List.of(state, Component.translatable(noteKey).withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.dimensionalfluxminer.side_config.auto.toggle").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void onClose() {
        neighbors = null;
    }

    @Override
    protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        CompoundTag d = data.get();
        MinerPorts ports = new MinerPorts();
        ports.load(d.getCompound("ports"));
        int connected = d.getInt("energy_connected");
        boolean stabilized = "STABILIZED".equals(d.getString("core"));
        boolean items = current == Transmission.ITEM;

        // Items and Energy tabs, each always in its own place; the current one is gray and sunk, like the tab of an open window.
        Transmission[] tabs = {Transmission.ITEM, Transmission.ENERGY};
        for (int i = 0; i < tabs.length; i++) {
            Transmission t = tabs[i];
            screen.materialTab(g, x - DfmScreen.MATERIAL_TAB_SIZE, y + 2 + DfmScreen.MATERIAL_TAB_PITCH * i, true,
                    SideConfigWindow.icon(t), t != current, mouseX, mouseY, () -> {
                        current = t;
                        return -1;
                    }, () -> List.of(Component.translatable(t.translationKey())));
        }

        // Screen: the automation of the item ports, or the energy mode.
        screen.innerScreen(g, x + 38, y + 22, 80, 12);
        Component screenText;
        if (items) {
            boolean input = d.getBoolean("auto_input");
            boolean eject = d.getBoolean("auto_eject");
            String mode = input ? eject ? "both" : "input" : eject ? "eject" : "off";
            screenText = Component.translatable("gui.dimensionalfluxminer.side_config.auto." + mode);
        } else {
            screenText = Component.translatable(ports.energyManual() ? "gui.dimensionalfluxminer.miner.energy.manual"
                    : "gui.dimensionalfluxminer.miner.energy.auto");
        }
        screen.fitted(g, screenText, x + 41, y + 25, 74, DfmScreen.screenText(), 0.8F, false);

        // Automation of the item ports: two switches, Auto Input (Matter Input face) and Auto Eject (Item Output face).
        boolean input = d.getBoolean("auto_input");
        boolean eject = d.getBoolean("auto_eject");
        screen.imageButton(g, x + SideConfigWindow.INPUT_BUTTON_X, y + 6, DfmScreen.SMALL_BUTTON, GuiIcon.AUTO_INJECT, items, input,
                mouseX, mouseY, () -> MinerControllerBlockEntity.BUTTON_AUTO_INPUT, null,
                () -> switchTooltip(items, Component.translatable("gui.dimensionalfluxminer.auto_input", MachineText.onOff(input)),
                        "gui.dimensionalfluxminer.miner.auto_input.note"));
        screen.imageButton(g, x + SideConfigWindow.EJECT_BUTTON_X, y + 6, DfmScreen.SMALL_BUTTON, GuiIcon.AUTO_EJECT, items, eject,
                mouseX, mouseY, () -> MinerControllerBlockEntity.BUTTON_AUTO_EJECT, null,
                () -> switchTooltip(items, Component.translatable("gui.dimensionalfluxminer.auto_eject", MachineText.onOff(eject)),
                        "gui.dimensionalfluxminer.miner.auto_eject.note"));

        // Clear: the item roles, or back to the default energy.
        screen.imageButton(g, x + 132, y + 108, 18, GuiIcon.CLEAR_SIDES, true, false, mouseX, mouseY,
                () -> items ? MinerControllerBlockEntity.BUTTON_PORTS_CLEAR_ITEMS : MinerControllerBlockEntity.BUTTON_PORTS_RESET_ENERGY, null,
                () -> List.of(Component.translatable(items ? "gui.dimensionalfluxminer.miner.ports.clear_items"
                        : "gui.dimensionalfluxminer.miner.ports.reset_energy")));

        if (neighbors == null) {
            neighbors = neighborItems();
        }
        int nodeItems = d.getInt("node_item_faces");
        int nodeEnergy = d.getInt("node_energy_faces");
        int cables = d.getInt("cable_faces");
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            int[] p = POSITIONS[face.ordinal()];
            boolean node = ((items ? nodeItems : nodeEnergy) & face.bit()) != 0;
            boolean cable = items && (cables & face.bit()) != 0;
            face(g, ports, face, connected, stabilized, items, node, cable, x + p[0], y + p[1], mouseX, mouseY);
        }

        screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.side_config.slots"), x + 4, y + 132, width - 8,
                GuiTheme.subheading(), 1.0F, true);
    }

    /**
     * @param node  something outside sets this face's role (a Logistics Networks node or an active cable): it cannot
     *              be changed here
     * @param cable it is a cable that pulls from or pushes into the face
     */
    private void face(GuiGraphics g, MinerPorts ports, MinerPorts.Face face, int connected, boolean stabilized, boolean items,
                      boolean node, boolean cable, int x, int y, int mouseX, int mouseY) {
        boolean output = ports.isOutput(face);
        boolean matter = ports.isMatter(face);
        Integer color;
        if (items) {
            color = output ? Integer.valueOf(OUTPUT_COLOR) : matter ? Integer.valueOf(MATTER_COLOR) : null;
        } else if (node) {
            color = ENERGY_COLOR;
        } else if (ports.energyManual()) {
            color = ports.acceptsEnergy(face) ? Integer.valueOf(ENERGY_COLOR) : null;
        } else {
            // Automatic: every face accepts energy; the ones touching something with energy are shown brighter.
            color = (connected & face.bit()) != 0 ? Integer.valueOf(ENERGY_COLOR) : Integer.valueOf(0x7FA9B4);
        }
        int base = items ? MinerControllerBlockEntity.BUTTON_PORT_ITEMS + face.ordinal() * 2
                : MinerControllerBlockEntity.BUTTON_PORT_ENERGY + face.ordinal();
        ItemStack neighbor = neighbors[face.ordinal()];
        int left = node ? -1 : base;
        int right = node ? -1 : items ? base + 1 : base;
        screen.colorButton(g, x, y, SIDE, color, true, mouseX, mouseY, () -> left, () -> right, () -> {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.dimensionalfluxminer.miner.face." + face.key()));
            if (node) {
                String role = !items ? "energy_on" : output && matter ? "both" : output ? "output" : "matter";
                lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port." + role));
                String by = cable ? "cable" : "node";
                lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port." + by).withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port." + by + "_note").withStyle(ChatFormatting.GRAY));
                return lines;
            }
            if (items) {
                String role = output && matter ? "both" : output ? "output" : matter ? "matter" : "none";
                lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port." + role));
                if (!stabilized) {
                    lines.add(Component.translatable("gui.dimensionalfluxminer.miner.port.matter_locked").withStyle(ChatFormatting.GRAY));
                }
            } else {
                lines.add(Component.translatable(ports.energyManual()
                        ? ports.acceptsEnergy(face) ? "gui.dimensionalfluxminer.miner.port.energy_on" : "gui.dimensionalfluxminer.miner.port.energy_off"
                        : "gui.dimensionalfluxminer.miner.port.energy_auto"));
            }
            if (!neighbor.isEmpty()) {
                lines.add(neighbor.getHoverName());
            }
            lines.add(Component.translatable(items ? "gui.dimensionalfluxminer.side_click" : "gui.dimensionalfluxminer.miner.port.energy_click")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        });
        if (items && output && matter) {
            // Both roles on one face: Item Output on the left half, Matter Input on the right half.
            g.fill(x + SIDE / 2, y + 2, x + SIDE - 2, y + SIDE - 2, 0xC0000000 | MATTER_COLOR);
        }
        if (!neighbor.isEmpty()) {
            g.renderItem(neighbor, x + 2, y + 2);
        }
        if (node) {
            // Set by a network node: a frame around the button, like the one drawn on the port.
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.renderOutline(x, y, SIDE, SIDE, 0xFF52D9DB);
            g.pose().popPose();
        }
    }

    /** Pick-block item of the block touching every face, indexed by face ordinal (empty for air). */
    private ItemStack[] neighborItems() {
        ItemStack[] result = new ItemStack[MinerPorts.Face.VALUES.length];
        java.util.Arrays.fill(result, ItemStack.EMPTY);
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return result;
        }
        BlockState self = level.getBlockState(terminal);
        if (!self.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return result;
        }
        Direction facing = self.getValue(BlockStateProperties.HORIZONTAL_FACING);
        MinerStructure layout = MinerStructure.layout(terminal, facing);
        MinerLayout.Front front = MinerStructure.toFront(facing);
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            Direction out = face.outward(front);
            BlockPos other = face.owner(layout.core(), front).relative(out);
            BlockState state = level.getBlockState(other);
            if (!state.isAir()) {
                BlockHitResult hit = new BlockHitResult(other.getCenter().relative(out.getOpposite(), 0.5), out.getOpposite(), other, false);
                result[face.ordinal()] = state.getCloneItemStack(hit, level, other, mc.player);
            }
        }
        return result;
    }
}
