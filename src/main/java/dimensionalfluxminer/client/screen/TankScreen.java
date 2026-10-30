package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.client.render.FluidColors;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.tank.DimensionalTankBlockEntity;
import dimensionalfluxminer.tank.TankCode;
import dimensionalfluxminer.tank.TankKind;
import dimensionalfluxminer.tank.TankMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Dimensional Tank: one reservoir (single) or two (dual, laid out as on the block's front: B on the left, A on the
 * right) with the liquid, amount and capacity of each chamber, and the three-color code. Clicking a reservoir with a
 * bucket or fluid container fills or empties that chamber. Tabs: Side Configuration and the code window (left). No
 * energy, Rate, upgrade or redstone. The creative tank shows its endless liquid and, in place of the code (it has no
 * channels), a button that empties it.
 */
public class TankScreen extends DfmScreen<TankMenu> {
    /** Room kept for each chamber's gauge (the layout around it is measured on it); the gauge is centered in it. */
    private static final int RESERVOIR_W = 30;
    private static final int RESERVOIR_H = 66;
    /** Chamber letter of the dual tank, light on the gauge's dark interior. */
    private static final int LETTER = 0xFFE8EDEB;
    private static final int RESERVOIR_Y = 18;
    private static final int SWATCH = 12;
    /** Read-out window beside the gauges, and the liquid square before each liquid name in it. */
    private static final int INFO_H = 50;
    private static final int LIQUID = 7;

    private final TankKind kind;
    private final SideConfigWindow sideWindow;
    private final CodeWindow codeWindow;

    public TankScreen(TankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = TankMenu.INVENTORY_TOP + 82;
        kind = inventory.player.level().getBlockState(menu.pos()).getBlock() instanceof dimensionalfluxminer.tank.DimensionalTankBlock block
                ? block.kind() : TankKind.SINGLE;
        List<Component> help = new ArrayList<>();
        help.add(Component.translatable("gui.dimensionalfluxminer.tank.auto.help"));
        if (kind == TankKind.DUAL) {
            help.add(Component.translatable("gui.dimensionalfluxminer.tank.auto.dual_help"));
        }
        sideWindow = new SideConfigWindow(this, "tank.side_config", kind == TankKind.DUAL ? SideRules.TANK_DUAL : SideRules.TANK_SINGLE,
                List.of(Transmission.FLUID), this::data, menu.pos(),
                Map.of(Transmission.FLUID, new SideConfigWindow.AutomationTab(SideConfigWindow.Automation.FLUID, help)));
        codeWindow = new CodeWindow(this);
    }

    @Override
    protected int inventoryTop() {
        return TankMenu.INVENTORY_TOP;
    }

    private TankCode code() {
        return TankCode.load(data().getCompound("code"));
    }

    private FluidStack[] chambers() {
        return DimensionalTankBlockEntity.readChambers(data().getList("chambers", Tag.TAG_COMPOUND), kind.chambers(),
                menu.player().registryAccess());
    }

    private int capacity() {
        return Math.max(1, data().getInt("capacity"));
    }

    @Override
    protected void renderMain(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        FluidStack[] chambers = chambers();
        int focusedChamber = isWindowOpen(sideWindow) ? sideWindow.focusedRoute().fluidChamber() : -1;
        int infoX;
        int infoW;
        int top = y + RESERVOIR_Y;
        if (kind == TankKind.DUAL) {
            // As seen on the block: B on the left, A on the right.
            reservoir(g, x + 8, top, chambers[1], 1, focusedChamber == 1, mouseX, mouseY);
            reservoir(g, x + 138, top, chambers[0], 0, focusedChamber == 0, mouseX, mouseY);
            infoX = x + 42;
            infoW = 92;
            innerScreen(g, infoX, top, infoW, INFO_H);
            // One half of the glass per chamber, split by a faint line.
            g.fill(infoX + 4, top + INFO_H / 2, infoX + infoW - 4, top + INFO_H / 2 + 1, 0x30FFFFFF);
            int[] order = {1, 0};
            for (int k = 0; k < order.length; k++) {
                int i = order[k];
                int rowY = top + 5 + k * 23;
                liquidLine(g, Component.translatable("gui.dimensionalfluxminer.tank.chamber_short", chamberLetter(i), fluidName(chambers[i])),
                        chambers[i], infoX + 4, rowY, infoW - 8);
                screenLines(g, List.of(amountLine(chambers[i])), infoX + 4 + LIQUID + 3, rowY + 10, infoW - 8 - LIQUID - 3, 10);
            }
        } else {
            reservoir(g, x + 16, top, chambers[0], 0, focusedChamber == 0, mouseX, mouseY);
            infoX = x + 54;
            infoW = 114;
            innerScreen(g, infoX, top, infoW, INFO_H);
            liquidLine(g, fluidName(chambers[0]), chambers[0], infoX + 4, top + 6, infoW - 8);
            Component second = kind.creative()
                    ? Component.translatable(chambers[0].isEmpty() ? "gui.dimensionalfluxminer.creative_tank.void"
                            : "gui.dimensionalfluxminer.creative_tank.endless")
                    : Component.translatable("gui.dimensionalfluxminer.tank.percent", percent(chambers[0]));
            screenLines(g, List.of(amountLine(chambers[0]), second), infoX + 4, top + 20, infoW - 8, 12);
        }

        if (kind.creative()) {
            creativeRow(g, x, y, infoX, infoW, chambers[0], mouseX, mouseY);
            MachineTabs.sideConfig(this, g, x, y, sideWindow, mouseX, mouseY);
            themeTab(g, x, y, MachineTabs.ROW_2, mouseX, mouseY);
            return;
        }

        // The code, left to right as on the panel; clicking it opens the code window.
        int codeY = y + RESERVOIR_Y + RESERVOIR_H - SWATCH;
        int codeX = infoX + infoW - (3 * SWATCH + 2 * 3);
        fitted(g, Component.translatable("gui.dimensionalfluxminer.tank.code"), infoX, codeY + 2, codeX - infoX - 4, GuiTheme.subheading(), 1.0F, false);
        TankCode code = code();
        for (int p = 0; p < TankCode.POSITIONS; p++) {
            swatch(g, codeX + p * (SWATCH + 3), codeY, SWATCH, code.get(p), false);
        }
        hotspot(codeX, codeY, 3 * SWATCH + 6, SWATCH, () -> {
            openWindow(codeWindow);
            return -1;
        }, null, () -> codeTooltip(code), 0);

        MachineTabs.sideConfig(this, g, x, y, sideWindow, mouseX, mouseY);
        themeTab(g, x, y, MachineTabs.ROW_2, mouseX, mouseY);
    }

    /** Creative tank, in the code's place: channels are off, and a button forgets the liquid. */
    private void creativeRow(GuiGraphics g, int x, int y, int infoX, int infoW, FluidStack stack, int mouseX, int mouseY) {
        int rowY = y + RESERVOIR_Y + RESERVOIR_H - SWATCH - 2;
        int buttonW = 52;
        int buttonX = infoX + infoW - buttonW;
        fitted(g, Component.translatable("gui.dimensionalfluxminer.creative_tank.no_channels"), infoX, rowY + 4, buttonX - infoX - 4,
                GuiTheme.subheading(), 1.0F, false);
        Component label = Component.translatable("gui.dimensionalfluxminer.creative_tank.clear");
        button(g, buttonX, rowY, buttonW, 16, label, !stack.isEmpty(), mouseX, mouseY,
                () -> DimensionalTankBlockEntity.BUTTON_CLEAR, null, () -> List.of(label,
                        Component.translatable("gui.dimensionalfluxminer.creative_tank.clear.help").withStyle(ChatFormatting.GRAY)));
    }

    /** A lamp in the liquid's color (off while empty) followed by a line of screen text about it. */
    private void liquidLine(GuiGraphics g, Component text, FluidStack stack, int x, int y, int maxWidth) {
        GlassViewport.lamp(g, x, y, LIQUID, liquidColor(stack));
        screenLines(g, List.of(text), x + LIQUID + 3, y, maxWidth - LIQUID - 3, 10);
    }

    /** Flat color of the liquid, the same as the level shown on the block ({@link FluidColors}); -1 while empty. */
    private static int liquidColor(FluidStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation still = extension.getStillTexture(stack);
        if (still == null) {
            return extension.getTintColor(stack) & 0xFFFFFF;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        return FluidColors.color(sprite, extension.getTintColor(stack)) & 0xFFFFFF;
    }

    private List<Component> codeTooltip(TankCode code) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.dimensionalfluxminer.tank.code"));
        lines.add(codeText(code));
        lines.add(code.personal() ? Component.translatable("gui.dimensionalfluxminer.tank.personal.owner", code.ownerName().orElse("?"))
                .withStyle(ChatFormatting.LIGHT_PURPLE) : Component.translatable("gui.dimensionalfluxminer.tank.public").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.dimensionalfluxminer.tank.code.kind_note").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.dimensionalfluxminer.tank.code.open").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    static Component codeText(TankCode code) {
        return Component.translatable("gui.dimensionalfluxminer.tank.code.colors", colorName(code.first()), colorName(code.second()),
                colorName(code.third()));
    }

    private static Component colorName(DyeColor color) {
        return Component.translatable("color.minecraft." + color.getName()).withColor(color.getTextureDiffuseColor() & 0xFFFFFF);
    }

    private static Component chamberLetter(int chamber) {
        return Component.literal(chamber == 0 ? "A" : "B");
    }

    private static Component fluidName(FluidStack stack) {
        return stack.isEmpty() ? Component.translatable("gui.dimensionalfluxminer.tank.empty") : stack.getHoverName();
    }

    private Component amountLine(FluidStack stack) {
        if (kind.creative()) {
            return Component.translatable(stack.isEmpty() ? "gui.dimensionalfluxminer.tank.empty" : "gui.dimensionalfluxminer.creative_tank.infinite");
        }
        return Component.translatable("gui.dimensionalfluxminer.tank.amount", compact(stack.getAmount()), compact(capacity()));
    }

    private String percent(FluidStack stack) {
        return String.format(Locale.ROOT, "%.1f", 100.0 * stack.getAmount() / capacity());
    }

    /**
     * Gauge of one chamber ({@link FluidGauge}, like the energy bar of the machines), centered in the room kept for it:
     * the liquid fills it from the bottom with its own texture and color under a moving reflection.
     */
    private void reservoir(GuiGraphics g, int areaX, int areaY, FluidStack stack, int chamber, boolean highlighted, int mouseX, int mouseY) {
        int x = areaX + (RESERVOIR_W - FluidGauge.WIDTH) / 2;
        int y = areaY;
        int w = FluidGauge.WIDTH;
        int h = FluidGauge.HEIGHT;
        FluidGauge.draw(g, x, y, stack, capacity());
        int ix = x + FluidGauge.FRAME;
        int iy = y + FluidGauge.FRAME;
        int iw = w - 2 * FluidGauge.FRAME;
        int ih = h - 2 * FluidGauge.FRAME;
        if (kind == TankKind.DUAL) {
            fitted(g, chamberLetter(chamber), ix, iy + 2, iw, LETTER, 0.8F, true);
        }
        if (highlighted) {
            MachineTabs.outline(g, x - 1, y - 1, w + 2, h + 2, sideWindow.focusedRoute().color());
        }
        if (hovered(x, y, w, h, mouseX, mouseY)) {
            g.fill(ix, iy, ix + iw, iy + ih, 0x30FFFFFF);
        }
        hotspot(x, y, w, h,
                () -> menu.getCarried().isEmpty() ? -1 : DimensionalTankBlockEntity.BUTTON_CONTAINER + chamber, null, () -> {
                    List<Component> lines = new ArrayList<>();
                    if (kind == TankKind.DUAL) {
                        lines.add(Component.translatable("gui.dimensionalfluxminer.tank.chamber", chamberLetter(chamber)));
                    }
                    lines.add(fluidName(stack));
                    lines.add(kind.creative() ? amountLine(stack).copy().withStyle(ChatFormatting.GRAY)
                            : Component.translatable("gui.dimensionalfluxminer.tank.amount_exact",
                            String.format(Locale.ROOT, "%,d", stack.getAmount()), String.format(Locale.ROOT, "%,d", capacity()))
                            .withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("gui.dimensionalfluxminer.tank.container_hint").withStyle(ChatFormatting.DARK_GRAY));
                    return lines;
                }, 0);
    }

    /**
     * A code color drawn as an item slot holding the color: the slot's sunken bevel (dark top and left, light bottom and
     * right) around the color. {@code selected} adds a frame in the screen color around it.
     */
    void swatch(GuiGraphics g, int x, int y, int size, DyeColor color, boolean selected) {
        int dark = GuiTheme.gray(0xFF373737);
        int light = GuiTheme.gray(0xFFFFFFFF);
        int corner = GuiTheme.gray(0xFF8B8B8B);
        g.fill(x, y, x + size - 1, y + 1, dark);
        g.fill(x, y, x + 1, y + size - 1, dark);
        g.fill(x + 1, y + size - 1, x + size, y + size, light);
        g.fill(x + size - 1, y + 1, x + size, y + size, light);
        g.fill(x + size - 1, y, x + size, y + 1, corner);
        g.fill(x, y + size - 1, x + 1, y + size, corner);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFF000000 | color.getTextureDiffuseColor());
        if (selected) {
            MachineTabs.outline(g, x - 1, y - 1, size + 2, size + 2, SCREEN_TEXT);
        }
    }

    /**
     * Code window: the three positions in order (click one to choose which position changes) and the 16 dye colors;
     * a color applies to the chosen position and the choice moves to the next one.
     */
    private static final class CodeWindow extends Window {
        private static final int FIELD = 18;
        private static final int CELL = 16;
        private final TankScreen tank;
        private int selected;
        /** Chamber whose Empty button was clicked once (a second click within {@link #CONFIRM_MS} empties it), or -1. */
        private int armed = -1;
        private long armedAt;
        private static final long CONFIRM_MS = 3000;

        CodeWindow(TankScreen screen) {
            super(screen, "tank.code", 156);
            this.tank = screen;
        }

        @Override
        public Component title() {
            return Component.translatable("gui.dimensionalfluxminer.tank.code");
        }

        @Override
        public int height() {
            return 144;
        }

        @Override
        protected void onClose() {
            selected = 0;
            armed = -1;
        }

        @Override
        protected void render(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
            TankCode code = tank.code();
            int fieldsX = x + (width - (3 * FIELD + 2 * 8)) / 2;
            for (int p = 0; p < TankCode.POSITIONS; p++) {
                int fx = fieldsX + p * (FIELD + 8);
                int position = p;
                tank.swatch(g, fx, y + 22, FIELD, code.get(p), p == selected);
                screen.fitted(g, Component.literal(Integer.toString(p + 1)), fx, y + 22 + FIELD + 2, FIELD,
                        p == selected ? GuiTheme.heading() : GuiTheme.subheading(), 1.0F, true);
                screen.hotspot(fx, y + 22, FIELD, FIELD, () -> {
                    selected = position;
                    return -1;
                }, null, () -> List.of(Component.translatable("gui.dimensionalfluxminer.tank.code.position", position + 1, colorName(code.get(position))),
                        Component.translatable("gui.dimensionalfluxminer.tank.code.select").withStyle(ChatFormatting.DARK_GRAY)), 0);
            }
            int paletteX = x + (width - 8 * (CELL + 2) + 2) / 2;
            int paletteY = y + 56;
            for (DyeColor color : DyeColor.values()) {
                int cx = paletteX + (color.getId() % 8) * (CELL + 2);
                int cy = paletteY + (color.getId() / 8) * (CELL + 2);
                tank.swatch(g, cx, cy, CELL, color, false);
                if (screen.hovered(cx, cy, CELL, CELL, mouseX, mouseY)) {
                    g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x40FFFFFF);
                }
                screen.hotspot(cx, cy, CELL, CELL, () -> {
                    int id = DimensionalTankBlockEntity.BUTTON_CODE_BASE + selected * 16 + color.getId();
                    selected = (selected + 1) % TankCode.POSITIONS;
                    return id;
                }, null, () -> List.of(colorName(color),
                        Component.translatable("gui.dimensionalfluxminer.tank.code.apply", selected + 1).withStyle(ChatFormatting.DARK_GRAY)), 0);
            }
            // Personal code: with the personal item on the cursor a public code becomes this player's own channel.
            Component itemName = new net.minecraft.world.item.ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem()).getHoverName();
            boolean personal = code.personal();
            boolean canLock = personal || tank.menu.getCarried().is(dimensionalfluxminer.DfmConfig.tankPersonalItem());
            Component label = personal
                    ? Component.translatable("gui.dimensionalfluxminer.tank.personal.owner", code.ownerName().orElse("?"))
                    : Component.translatable("gui.dimensionalfluxminer.tank.public");
            screen.button(g, x + 6, y + 94, width - 12, 14, label, canLock, mouseX, mouseY,
                    () -> DimensionalTankBlockEntity.BUTTON_PERSONAL, null, () -> List.of(
                            label,
                            Component.translatable(personal ? "gui.dimensionalfluxminer.tank.personal.help"
                                    : "gui.dimensionalfluxminer.tank.public.help").withStyle(ChatFormatting.GRAY),
                            Component.translatable(personal ? "gui.dimensionalfluxminer.tank.personal.remove"
                                    : "gui.dimensionalfluxminer.tank.public.make", itemName).withStyle(ChatFormatting.DARK_GRAY)));
            // Empty the channel (one button per chamber on the dual tank, B on the left as on the block). Two clicks: the
            // liquid is deleted for every tank with this code.
            FluidStack[] chambers = tank.chambers();
            int[] order = tank.kind == TankKind.DUAL ? new int[] {1, 0} : new int[] {0};
            int gap = 4;
            int buttonW = (width - 12 - gap * (order.length - 1)) / order.length;
            if (armed >= 0 && net.minecraft.Util.getMillis() - armedAt > CONFIRM_MS) {
                armed = -1;
            }
            for (int k = 0; k < order.length; k++) {
                int chamber = order[k];
                boolean confirm = armed == chamber;
                Component clearLabel = confirm ? Component.translatable("gui.dimensionalfluxminer.tank.clear.confirm")
                        : tank.kind == TankKind.DUAL ? Component.translatable("gui.dimensionalfluxminer.tank.clear.chamber", chamberLetter(chamber))
                        : Component.translatable("gui.dimensionalfluxminer.creative_tank.clear");
                screen.button(g, x + 6 + k * (buttonW + gap), y + 112, buttonW, 14, clearLabel,!chambers[chamber].isEmpty(), mouseX, mouseY, () -> {
                    if (armed == chamber && net.minecraft.Util.getMillis() - armedAt <= CONFIRM_MS) {
                        armed = -1;
                        return DimensionalTankBlockEntity.BUTTON_CLEAR + chamber;
                    }
                    armed = chamber;
                    armedAt = net.minecraft.Util.getMillis();
                    return -1;
                }, null, () -> List.of(clearLabel,
                        Component.translatable("gui.dimensionalfluxminer.tank.clear.help").withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.dimensionalfluxminer.tank.clear.twice").withStyle(ChatFormatting.DARK_GRAY)));
            }
            screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.tank.code.kind_note"), x + 4, y + height() - 12, width - 8,
                    GuiTheme.subheading(), 0.8F, true);
        }
    }
}
