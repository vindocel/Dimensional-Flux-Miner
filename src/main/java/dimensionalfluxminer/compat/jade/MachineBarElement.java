package dimensionalfluxminer.compat.jade;

//? if <26.1 {
/*import com.mojang.math.Axis;
*///?}
import dimensionalfluxminer.DimensionalFluxMiner;
//? if >=26.1 {
import dimensionalfluxminer.client.render.FluidSprites;
//?}
import dimensionalfluxminer.client.screen.IconColor;
//? if <26.1 {
/*import net.minecraft.Util;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.util.Mth;
//? if >=26.1 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.world.inventory.InventoryMenu;
*///?}
import net.minecraft.world.level.material.Fluid;
//? if <26.1 {
/*import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;
import org.joml.Vector3f;
*///?}
import snownee.jade.api.ui.Element;
//? if >=26.1 {
import snownee.jade.api.ui.ResizeableElement;
//?}

/**
 * A horizontal bar stretched over the tooltip width with its text inside. Energy uses the machines' own energy bar
 * laid on its side; fluids are drawn with their still texture. Text longer than the bar slides from side to side.
 */
//? if >=26.1 {
final class MachineBarElement extends ResizeableElement {
    private static final Identifier ENERGY_FRAME = DimensionalFluxMiner.id("textures/gui/energy/energy_empty.png");
//?} else {
/*final class MachineBarElement extends Element {
    private static final ResourceLocation ENERGY_FRAME = DimensionalFluxMiner.id("textures/gui/energy/energy_empty.png");
*///?}
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
    //? if >=26.1 {
    private final Identifier energySheet;
    //?} else {
    /*private final ResourceLocation energySheet;
    *///?}
    /** Vertex color of the energy fill (the machine's custom color, otherwise white). */
    private final int energyTint;

    //? if >=26.1 {
    private MachineBarElement(double fraction, Component text, Fluid fluid, Identifier energySheet, int energyTint) {
    //?} else {
    /*private MachineBarElement(double fraction, Component text, Fluid fluid, ResourceLocation energySheet, int energyTint) {
    *///?}
        this.fraction = Mth.clamp(fraction, 0, 1);
        this.text = text;
        this.fluid = fluid;
        this.energySheet = energySheet;
        this.energyTint = energyTint;
        //? if >=26.1 {
        int width = Minecraft.getInstance().font.width(text) + 2 * TEXT_PADDING;
        // The fields themselves: Jade's size(...) returns a sized wrapper and leaves this element at 0 x 0.
        this.width = Mth.clamp(width, MIN_LENGTH, MAX_LENGTH);
        this.height = THICKNESS + 1;
        // Grows along the tooltip line; stretching across it would take the line's height, which can be zero.
        flexGrow(1);
        //?}
    }

    static MachineBarElement energy(double fraction, Component text, IconColor icons, int energyTint) {
        return new MachineBarElement(fraction, text, null, icons.energySheet(), energyTint);
    }

    static MachineBarElement fluid(Fluid fluid, double fraction, Component text) {
        return new MachineBarElement(fraction, text, fluid, null, 0xFFFFFFFF);
    }

    //? if >=26.1 {
    /** The bar takes the free width of the tooltip. */
    @Override
    public void setFreeSpace(int freeWidth, int freeHeight) {
        width = Math.max(width, freeWidth);
    //?} else {
    /*@Override
    public Vec2 getSize() {
        int width = Minecraft.getInstance().font.width(text) + 2 * TEXT_PADDING;
        return new Vec2(Mth.clamp(width, MIN_LENGTH, MAX_LENGTH), THICKNESS + 1);
    *///?}
    }

    @Override
    //? if >=26.1 {
    public Component getNarration() {
        return text;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int left = getX();
        int top = getY();
        int length = Math.max(MIN_LENGTH, getWidth());
    //?} else {
    /*public void render(GuiGraphics g, float x, float y, float maxX, float maxY) {
        int left = (int) x;
        int top = (int) y;
        int length = Math.max((int) getSize().x, (int) maxX - left);
    *///?}
        if (fluid == null) {
            drawEnergy(g, left, top, length);
        } else {
            drawFluid(g, left, top, length);
        }
        drawText(g, left + TEXT_PADDING, top + 3, left + length - TEXT_PADDING, top + THICKNESS - 2);
    }

    //? if >=26.1 {
    private void drawEnergy(GuiGraphicsExtractor g, int x, int y, int length) {
    //?} else {
    /*private void drawEnergy(GuiGraphics g, int x, int y, int length) {
    *///?}
        int area = length - 2 * ENERGY_BORDER;
        int filled = (int) Math.round(area * fraction);
        //? if >=26.1 {
        g.pose().pushMatrix();
        //?} else {
        /*g.pose().pushPose();
        *///?}
        // The vertical bar turned a quarter: its bottom, where the fill starts, ends up on the left.
        //? if >=26.1 {
        g.pose().translate(x + length, y);
        g.pose().rotate((float) Math.toRadians(90));
        g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_FRAME, 0, 0, 0, 0, THICKNESS, ENERGY_BORDER, THICKNESS, ENERGY_FRAME_LENGTH);
        //?} else {
        /*g.pose().translate(x + length, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(90));
        g.blit(ENERGY_FRAME, 0, 0, 0, 0, THICKNESS, ENERGY_BORDER, THICKNESS, ENERGY_FRAME_LENGTH);
        *///?}
        for (int pos = ENERGY_BORDER; pos < length - ENERGY_BORDER; pos += ENERGY_FILL_LENGTH) {
            int piece = Math.min(ENERGY_FILL_LENGTH, length - ENERGY_BORDER - pos);
            //? if >=26.1 {
            g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_FRAME, 0, pos, 0, ENERGY_BORDER, THICKNESS, piece, THICKNESS, ENERGY_FRAME_LENGTH);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, ENERGY_FRAME, 0, length - ENERGY_BORDER, 0, ENERGY_FRAME_LENGTH - ENERGY_BORDER, THICKNESS, ENERGY_BORDER,
            //?} else {
            /*g.blit(ENERGY_FRAME, 0, pos, 0, ENERGY_BORDER, THICKNESS, piece, THICKNESS, ENERGY_FRAME_LENGTH);
        }
        g.blit(ENERGY_FRAME, 0, length - ENERGY_BORDER, 0, ENERGY_FRAME_LENGTH - ENERGY_BORDER, THICKNESS, ENERGY_BORDER,
            *///?}
                THICKNESS, ENERGY_FRAME_LENGTH);
        int frame = (int) ((Util.getMillis() / ENERGY_FRAME_MS) % ENERGY_FRAMES);
        //? if <26.1 {
        /*g.setColor(((energyTint >> 16) & 0xFF) / 255.0F, ((energyTint >> 8) & 0xFF) / 255.0F, (energyTint & 0xFF) / 255.0F, 1.0F);
        *///?}
        for (int drawn = 0; drawn < filled; drawn += ENERGY_FILL_LENGTH) {
            int piece = Math.min(ENERGY_FILL_LENGTH, filled - drawn);
            //? if >=26.1 {
            g.blit(RenderPipelines.GUI_TEXTURED, energySheet, ENERGY_BORDER, ENERGY_BORDER + area - drawn - piece, 0,
                    frame * ENERGY_FILL_LENGTH + ENERGY_FILL_LENGTH - piece, ENERGY_FILL_THICKNESS, piece,
                    ENERGY_FILL_THICKNESS, ENERGY_FILL_LENGTH * ENERGY_FRAMES, energyTint);
            //?} else {
            /*g.blit(energySheet, ENERGY_BORDER, ENERGY_BORDER + area - drawn - piece, 0,
                    frame * ENERGY_FILL_LENGTH + ENERGY_FILL_LENGTH - piece, ENERGY_FILL_THICKNESS, piece,
                    ENERGY_FILL_THICKNESS, ENERGY_FILL_LENGTH * ENERGY_FRAMES);
            *///?}
        }
        //? if <26.1 {
        /*g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        *///?}
        //? if >=26.1 {
        g.pose().popMatrix();
        //?} else {
        /*g.pose().popPose();
        *///?}
    }

    //? if >=26.1 {
    private void drawFluid(GuiGraphicsExtractor g, int x, int y, int length) {
    //?} else {
    /*private void drawFluid(GuiGraphics g, int x, int y, int length) {
    *///?}
        g.fill(x, y, x + length, y + THICKNESS, FLUID_BORDER);
        g.fill(x + 1, y + 1, x + length - 1, y + THICKNESS - 1, FLUID_BACKGROUND);
        int area = length - 2;
        int filled = (int) Math.round(area * fraction);
        if (filled <= 0) {
            return;
        }
        //? if >=26.1 {
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int tint = FluidSprites.tint(fluid);
        g.enableScissor(x + 1, y + 1, x + 1 + filled, y + THICKNESS - 1);
        //?} else {
        /*IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture());
        int tint = extensions.getTintColor();
        float red = (tint >> 16 & 0xFF) / 255.0F;
        float green = (tint >> 8 & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = (tint >>> 24) / 255.0F;
        scissor(g, x + 1, y + 1, x + 1 + filled, y + THICKNESS - 1);
        *///?}
        for (int tx = 0; tx < filled; tx += 16) {
            //? if >=26.1 {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1 + tx, y + 1, 16, 16, tint);
            //?} else {
            /*g.blit(x + 1 + tx, y + 1, 0, 16, 16, sprite, red, green, blue, alpha);
            *///?}
        }
        g.disableScissor();
    }

    /** White text centered in the bar; when it does not fit it slides back and forth so all of it can be read. */
    //? if >=26.1 {
    private void drawText(GuiGraphicsExtractor g, int left, int top, int right, int bottom) {
    //?} else {
    /*private void drawText(GuiGraphics g, int left, int top, int right, int bottom) {
    *///?}
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        int available = right - left;
        if (width <= available) {
            //? if >=26.1 {
            g.text(font, text, left + (available - width) / 2, top, TEXT, true);
            //?} else {
            /*g.drawString(font, text, left + (available - width) / 2, top, TEXT, true);
            *///?}
            return;
        }
        int overflow = width - available;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double phase = Math.sin(Math.PI / 2 * Math.cos(2 * Math.PI * seconds / period)) / 2 + 0.5;
        int offset = (int) Math.round(Mth.lerp(phase, 0, overflow));
        //? if >=26.1 {
        g.enableScissor(left, top - 1, right, bottom + 1);
        g.text(font, text, left - offset, top, TEXT, true);
        //?} else {
        /*scissor(g, left, top - 1, right, bottom + 1);
        g.drawString(font, text, left - offset, top, TEXT, true);
        *///?}
        g.disableScissor();
    //? if <26.1 {
    /*}

    /^* The scissor works in screen pixels, so the tooltip's pose (position and scale) is applied to the area first. ^/
    private static void scissor(GuiGraphics g, int x0, int y0, int x1, int y1) {
        Matrix4f pose = g.pose().last().pose();
        Vector3f a = pose.transformPosition(x0, y0, 0, new Vector3f());
        Vector3f b = pose.transformPosition(x1, y1, 0, new Vector3f());
        g.enableScissor(Mth.floor(Math.min(a.x, b.x)), Mth.floor(Math.min(a.y, b.y)),
                Mth.ceil(Math.max(a.x, b.x)), Mth.ceil(Math.max(a.y, b.y)));
    *///?}
    }
}
