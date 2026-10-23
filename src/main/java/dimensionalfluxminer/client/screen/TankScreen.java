package dimensionalfluxminer.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Dimensional Tank: one reservoir (single) or two (dual, laid out as on the block's front: B on the left, A on the
 * right) with the liquid, amount and capacity of each chamber, and the three-color code. Clicking a reservoir with a
 * bucket or fluid container fills or empties that chamber. Tabs: Side Configuration and the code window (left). No
 * energy, Rate, upgrade or redstone.
 */
public class TankScreen extends DfmScreen<TankMenu> {
    private static final int RESERVOIR_W = 30;
    private static final int RESERVOIR_H = 66;
    private static final int RESERVOIR_Y = 18;
    private static final int SWATCH = 12;

    private final TankKind kind;
    private final SideConfigWindow sideWindow;
    private final CodeWindow codeWindow;

    public TankScreen(TankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, TankMenu.INVENTORY_TOP + 82);
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
        return TankCode.load(data().getCompoundOrEmpty("code"));
    }

    private FluidStack[] chambers() {
        return DimensionalTankBlockEntity.readChambers(data().getListOrEmpty("chambers"), kind.chambers(),
                menu.player().registryAccess());
    }

    private int capacity() {
        return Math.max(1, data().getIntOr("capacity", 0));
    }

    @Override
    protected void renderMain(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
        FluidStack[] chambers = chambers();
        int focusedChamber = isWindowOpen(sideWindow) ? sideWindow.focusedRoute().fluidChamber() : -1;
        List<Component> lines = new ArrayList<>();
        int infoX;
        int infoW;
        if (kind == TankKind.DUAL) {
            // As seen on the block: B on the left, A on the right.
            reservoir(g, x + 8, y + RESERVOIR_Y, chambers[1], 1, focusedChamber == 1, mouseX, mouseY);
            reservoir(g, x + 138, y + RESERVOIR_Y, chambers[0], 0, focusedChamber == 0, mouseX, mouseY);
            infoX = x + 44;
            infoW = 88;
            for (int i : new int[] {1, 0}) {
                lines.add(Component.translatable("gui.dimensionalfluxminer.tank.chamber_short", chamberLetter(i), fluidName(chambers[i])));
                lines.add(Component.literal("  ").append(amountLine(chambers[i])));
            }
        } else {
            reservoir(g, x + 20, y + RESERVOIR_Y, chambers[0], 0, focusedChamber == 0, mouseX, mouseY);
            infoX = x + 58;
            infoW = 98;
            lines.add(fluidName(chambers[0]));
            lines.add(amountLine(chambers[0]));
            lines.add(Component.translatable("gui.dimensionalfluxminer.tank.percent", percent(chambers[0])));
        }
        innerScreen(g, infoX, y + RESERVOIR_Y, infoW, lines.size() * 10 + 6);
        screenLines(g, lines, infoX + 4, y + RESERVOIR_Y + 4, infoW - 8, 10);

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
        return Component.translatable("gui.dimensionalfluxminer.tank.amount", compact(stack.getAmount()), compact(capacity()));
    }

    private String percent(FluidStack stack) {
        return String.format(Locale.ROOT, "%.1f", 100.0 * stack.getAmount() / capacity());
    }

    /**
     * Reservoir of one chamber, drawn like a large item slot (dark top and left edge, white bottom and right edge, the
     * slot's gray inside): the liquid fills from the bottom with its own sprite and color, and dark marks on the left
     * show every eighth, with a line across the whole reservoir at half.
     */
    private void reservoir(GuiGraphicsExtractor g, int x, int y, FluidStack stack, int chamber, boolean highlighted, int mouseX, int mouseY) {
        int dark = GuiTheme.gray(0xFF373737);
        int light = GuiTheme.gray(0xFFFFFFFF);
        int mid = GuiTheme.gray(0xFF8B8B8B);
        int w = RESERVOIR_W;
        int h = RESERVOIR_H;
        g.fill(x, y, x + w - 1, y + 1, dark);
        g.fill(x, y, x + 1, y + h - 1, dark);
        g.fill(x + 1, y + h - 1, x + w, y + h, light);
        g.fill(x + w - 1, y + 1, x + w, y + h, light);
        g.fill(x + w - 1, y, x + w, y + 1, mid);
        g.fill(x, y + h - 1, x + 1, y + h, mid);
        int ix = x + 1;
        int iy = y + 1;
        int iw = w - 2;
        int ih = h - 2;
        g.fill(ix, iy, ix + iw, iy + ih, mid);
        int filled = stack.isEmpty() ? 0 : Math.max(1, (int) Math.round(ih * Mth.clamp(stack.getAmount() / (double) capacity(), 0, 1)));
        if (filled > 0) {
            fluid(g, stack, ix, iy + ih - filled, iw, filled);
        }
        for (int mark = 1; mark < 8; mark++) {
            int my = iy + ih - ih * mark / 8;
            // Short marks start at the left edge and reach almost half the reservoir; the half mark crosses it.
            g.fill(ix, my, ix + (mark == 4 ? iw : iw * 2 / 5), my + 1, dark);
        }
        if (kind == TankKind.DUAL) {
            fitted(g, chamberLetter(chamber), ix + iw / 2, iy + 2, iw / 2, GuiTheme.title(), 0.8F, true);
        }
        if (highlighted) {
            MachineTabs.outline(g, x - 1, y - 1, RESERVOIR_W + 2, RESERVOIR_H + 2, sideWindow.focusedRoute().color());
        }
        if (hovered(x, y, RESERVOIR_W, RESERVOIR_H, mouseX, mouseY)) {
            g.fill(ix, iy, ix + iw, iy + ih, 0x30FFFFFF);
        }
        hotspot(x, y, RESERVOIR_W, RESERVOIR_H,
                () -> menu.getCarried().isEmpty() ? -1 : DimensionalTankBlockEntity.BUTTON_CONTAINER + chamber, null, () -> {
                    List<Component> lines = new ArrayList<>();
                    if (kind == TankKind.DUAL) {
                        lines.add(Component.translatable("gui.dimensionalfluxminer.tank.chamber", chamberLetter(chamber)));
                    }
                    lines.add(fluidName(stack));
                    lines.add(Component.translatable("gui.dimensionalfluxminer.tank.amount_exact",
                            String.format(Locale.ROOT, "%,d", stack.getAmount()), String.format(Locale.ROOT, "%,d", capacity()))
                            .withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("gui.dimensionalfluxminer.tank.container_hint").withStyle(ChatFormatting.DARK_GRAY));
                    return lines;
                }, 0);
    }

    /** Tiles the liquid's still sprite (16 x 16 pieces, cut at the edges), tinted with its color. */
    private void fluid(GuiGraphicsExtractor g, FluidStack stack, int x, int y, int w, int h) {
        TextureAtlasSprite sprite = dimensionalfluxminer.client.render.FluidSprites.still(stack.getFluid());
        int tint = dimensionalfluxminer.client.render.FluidSprites.tint(stack);
        g.enableScissor(x, y, x + w, y + h);
        for (int ty = y + h - 16; ty > y - 16; ty -= 16) {
            for (int tx = x; tx < x + w; tx += 16) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 16, 16, tint);
            }
        }
        g.disableScissor();
    }

    /**
     * A code color drawn as an item slot holding the color: the slot's sunken bevel (dark top and left, light bottom and
     * right) around the color. {@code selected} adds a frame in the screen color around it.
     */
    void swatch(GuiGraphicsExtractor g, int x, int y, int size, DyeColor color, boolean selected) {
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
            return 126;
        }

        @Override
        protected void onClose() {
            selected = 0;
        }

        @Override
        protected void render(GuiGraphicsExtractor g, int x, int y, int mouseX, int mouseY) {
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
            screen.fitted(g, Component.translatable("gui.dimensionalfluxminer.tank.code.kind_note"), x + 4, y + height() - 12, width - 8,
                    GuiTheme.subheading(), 0.8F, true);
        }
    }
}
