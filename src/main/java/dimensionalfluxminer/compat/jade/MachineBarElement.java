package dimensionalfluxminer.compat.jade;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.render.FluidSprites;
import dimensionalfluxminer.client.screen.IconColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.material.Fluid;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.ResizeableElement;

/**
 * A horizontal bar stretched over the tooltip width with its text inside. Energy uses the machines' own energy bar
 * laid on its side; fluids are drawn with their still texture. Text longer than the bar slides from side to side.
 */
final class MachineBarElement extends ResizeableElement {
    private static final Identifier ENERGY_FRAME = DimensionalFluxMiner.id("textures/gui/energy/energy_empty.png");
    private static final int ENERGY_FRAME_LENGTH = 72;
    private static final int ENERGY_BORDER = 3;
    private static final int ENERGY_FILL_LENGTH = 66;
    private static final int ENERGY_FILL_THICKNESS = 8;
    private static final int ENERGY_FRAMES = 42;
    private static final long ENERGY_FRAME_MS = 100;

    private static final int THICKNESS = 14;
    private static final int MIN_LENGTH = 100;
    private static final int MAX_LENGTH = 140;
    private static final int TEXT_PADDING = 4;
    private static final int FLUID_BORDER = 0xFF6E6E6E;
    private static final int FLUID_BACKGROUND = 0xFF1E1E1E;
    private static final int TEXT = 0xFFFFFFFF;

    private final double fraction;
    private final Component text;
    /** Null for the energy bar. */
    private final Fluid fluid;
    /** Energy fill in the player's icon color for this machine; null for the fluid bars. */
    private final Identifier energySheet;

    private MachineBarElement(double fraction, Component text, Fluid fluid, Identifier energySheet) {
        this.fraction = Mth.clamp(fraction, 0, 1);
        this.text = text;
        this.fluid = fluid;
        this.energySheet = energySheet;
        int width = Minecraft.getInstance().font.width(text) + 2 * TEXT_PADDING;
        // The fields themselves: Jade's size(...) returns a sized wrapper and leaves this element at 0 x 0.
        this.width = Mth.clamp(width, MIN_LENGTH, MAX_LENGTH);
        this.height = THICKNESS + 1;
        // Grows along the tooltip line; stretching across it would take the line's height, which can be zero.
        flexGrow(1);
    }

    static MachineBarElement energy(double fraction, Component text, IconColor icons) {
        return new MachineBarElement(fraction, text, null, icons.energySheet());
    }

    static MachineBarElement fluid(Fluid fluid, double fraction, Component text) {
        return new MachineBarElement(fraction, text, fluid, null);
    }

    /** The bar takes the free width of the tooltip. */
    @Override
    public void setFreeSpace(int freeWidth, int freeHeight) {
        width = Math.max(width, freeWidth);
    }

    @Override
    public Component getNarration() {
        return text;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int length = Math.max(MIN_LENGTH, getWidth());
        if (fluid == null) {
            drawEnergy(g, left, top, length);
        } else {
            drawFluid(g, left, top, length);
        }
        drawText(g, left + TEXT_PADDING, top + 3, left + length - TEXT_PADDING, top + THICKNESS - 2);
    }

    private void drawEnergy(GuiGraphicsExtractor g, int x, int y, int length) {
        int area = length - 2 * ENERGY_BORDER;
        int filled = (int) Math.round(area * fraction);
        g.pose().pushMatrix();
        // The vertical bar turned a quarter: its bottom, where the fill starts, ends up on the left.
        g.pose().translate(x + length, y);
        g.pose().rotate((float) Math.toRadians(90));
        g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_FRAME, 0, 0, 0, 0, THICKNESS, ENERGY_BORDER, THICKNESS, ENERGY_FRAME_LENGTH);
        for (int pos = ENERGY_BORDER; pos < length - ENERGY_BORDER; pos += ENERGY_FILL_LENGTH) {
            int piece = Math.min(ENERGY_FILL_LENGTH, length - ENERGY_BORDER - pos);
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_FRAME, 0, pos, 0, ENERGY_BORDER, THICKNESS, piece, THICKNESS, ENERGY_FRAME_LENGTH);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_FRAME, 0, length - ENERGY_BORDER, 0, ENERGY_FRAME_LENGTH - ENERGY_BORDER, THICKNESS, ENERGY_BORDER,
                THICKNESS, ENERGY_FRAME_LENGTH);
        int frame = (int) ((Util.getMillis() / ENERGY_FRAME_MS) % ENERGY_FRAMES);
        for (int drawn = 0; drawn < filled; drawn += ENERGY_FILL_LENGTH) {
            int piece = Math.min(ENERGY_FILL_LENGTH, filled - drawn);
            g.blit(RenderPipelines.GUI_TEXTURED, energySheet, ENERGY_BORDER, ENERGY_BORDER + area - drawn - piece, 0,
                    frame * ENERGY_FILL_LENGTH + ENERGY_FILL_LENGTH - piece, ENERGY_FILL_THICKNESS, piece,
                    ENERGY_FILL_THICKNESS, ENERGY_FILL_LENGTH * ENERGY_FRAMES);
        }
        g.pose().popMatrix();
    }

    private void drawFluid(GuiGraphicsExtractor g, int x, int y, int length) {
        g.fill(x, y, x + length, y + THICKNESS, FLUID_BORDER);
        g.fill(x + 1, y + 1, x + length - 1, y + THICKNESS - 1, FLUID_BACKGROUND);
        int area = length - 2;
        int filled = (int) Math.round(area * fraction);
        if (filled <= 0) {
            return;
        }
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int tint = FluidSprites.tint(fluid);
        g.enableScissor(x + 1, y + 1, x + 1 + filled, y + THICKNESS - 1);
        for (int tx = 0; tx < filled; tx += 16) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1 + tx, y + 1, 16, 16, tint);
        }
        g.disableScissor();
    }

    /** White text centered in the bar; when it does not fit it slides back and forth so all of it can be read. */
    private void drawText(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        int available = right - left;
        if (width <= available) {
            g.text(font, text, left + (available - width) / 2, top, TEXT, true);
            return;
        }
        int overflow = width - available;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
        int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
        g.enableScissor(left, top - 1, right, bottom + 1);
        g.text(font, text, left - offset, top, TEXT, true);
        g.disableScissor();
    }
}
