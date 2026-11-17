package dimensionalfluxminer.client.screen;

//? if <26.1 {
/*import com.mojang.blaze3d.systems.RenderSystem;
*///?}
import dimensionalfluxminer.DimensionalFluxMiner;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//?} else {
/*import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
*///?}
import net.neoforged.neoforge.fluids.FluidStack;
//? if >=26.1 {
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
//?} else {
/*import org.joml.Vector3f;
*///?}

/**
 * Liquid gauge of the screens, the liquid counterpart of the energy bar: {@value #WIDTH} x {@value #HEIGHT} px with a
 * {@value #FRAME} px frame ({@code fluid/fluid_empty.png}, darkened with the screen as chrome). The liquid fills the
 * interior from the bottom with its own animated texture and color at {@value #FLUID_OPACITY} of its opacity, a
 * transparent reflection ({@code fluid/fluid_shimmer_sheet.png}, {@value #FRAMES} frames, in step with the energy bar)
 * runs over the filled part, and the frame with the level marks ({@code fluid/fluid_marks.png}) goes on top. Drawn
 * for the wide chambers of the Dimensional Tanks: the textures are made at this width (interior columns repeated, the
 * reflection band redrawn with the same slope), never stretched.
 */
public final class FluidGauge {
    public static final int WIDTH = 30;
    public static final int HEIGHT = 64;
    public static final int NARROW_WIDTH = 18;
    static final int FRAME = 3;
    private static final int INTERIOR_HEIGHT = HEIGHT - 2 * FRAME;
    private static final float FLUID_OPACITY = 0.8F;
    private static final int FRAMES = 42;
    private static final long FRAME_MS = 100;

    //? if >=26.1 {
    private static final Identifier EMPTY = DimensionalFluxMiner.id("textures/gui/fluid/fluid_empty.png");
    private static final Identifier MARKS = DimensionalFluxMiner.id("textures/gui/fluid/fluid_marks.png");
    private static final Identifier SHIMMER = DimensionalFluxMiner.id("textures/gui/fluid/fluid_shimmer_sheet.png");
    //?} else {
    /*private static final ResourceLocation EMPTY = DimensionalFluxMiner.id("textures/gui/fluid/fluid_empty.png");
    private static final ResourceLocation MARKS = DimensionalFluxMiner.id("textures/gui/fluid/fluid_marks.png");
    private static final ResourceLocation SHIMMER = DimensionalFluxMiner.id("textures/gui/fluid/fluid_shimmer_sheet.png");
    *///?}

    private FluidGauge() {
    }

    /** The gauge at (x, y) holding {@code stack} out of {@code capacity} mB. */
    //? if >=26.1 {
    public static void draw(GuiGraphicsExtractor g, int x, int y, FluidStack stack, double capacity) {
    //?} else {
    /*public static void draw(GuiGraphics g, int x, int y, FluidStack stack, double capacity) {
    *///?}
        draw(g, x, y, stack, capacity, WIDTH);
    }

    /** Recipe pages use a narrower gauge, retaining the frame and marks at their native pixel scale. */
    //? if >=26.1 {
    public static void drawNarrow(GuiGraphicsExtractor g, int x, int y, FluidStack stack, double capacity) {
    //?} else {
    /*public static void drawNarrow(GuiGraphics g, int x, int y, FluidStack stack, double capacity) {
    *///?}
        draw(g, x, y, stack, capacity, NARROW_WIDTH);
    }

    //? if >=26.1 {
    private static void draw(GuiGraphicsExtractor g, int x, int y, FluidStack stack, double capacity, int width) {
        GuiTheme.chrome(() -> texture(g, EMPTY, x, y, width, 0, width, 0, HEIGHT, HEIGHT));
    //?} else {
    /*private static void draw(GuiGraphics g, int x, int y, FluidStack stack, double capacity, int width) {
        RenderSystem.enableBlend();
        GuiTheme.chrome(() -> texture(g, EMPTY, x, y, width));
    *///?}
        int bottom = HEIGHT - FRAME;
        int top = bottom - filled(stack, capacity);
        if (top < bottom) {
            liquid(g, x, y, top, stack, width);
            // The reflection is mostly transparent: it needs blending.
            //? if <26.1 {
            /*RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            *///?}
            int frameV = (int) ((Util.getMillis() / FRAME_MS) % FRAMES) * HEIGHT;
            // Keep the diagonal reflection contiguous when narrowing the gauge.
            int shimmerU = FRAME + (WIDTH - width) / 2;
            //? if >=26.1 {
            g.blit(RenderPipelines.GUI_TEXTURED, SHIMMER, x + FRAME, y + top, shimmerU, frameV + top,
                    width - 2 * FRAME, bottom - top, WIDTH, HEIGHT * FRAMES);
        }
        GuiTheme.chrome(() -> texture(g, MARKS, x, y, width, 0, width, 0, HEIGHT, HEIGHT));
            //?} else {
            /*g.blit(SHIMMER, x + FRAME, y + top, shimmerU, frameV + top, width - 2 * FRAME, bottom - top, WIDTH, HEIGHT * FRAMES);
        }
        RenderSystem.enableBlend();
        GuiTheme.chrome(() -> texture(g, MARKS, x, y, width));
            *///?}
    }

    /** Remove central columns rather than stretching the gauge's borders or level marks. */
    //? if >=26.1 {
    private static void texture(GuiGraphicsExtractor g, Identifier texture, int x, int y, int width,
                                int from, int to, int v, int height, int textureHeight) {
    //?} else {
    /*private static void texture(GuiGraphics g, ResourceLocation texture, int x, int y, int width) {
    *///?}
        int split = width - 2 * FRAME;
        //? if >=26.1 {
        int leftEnd = Math.min(to, split);
        if (leftEnd > from) {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x + from, y, from, v, leftEnd - from, height, WIDTH, textureHeight);
        }
        int rightStart = Math.max(from, split);
        if (to > rightStart) {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x + rightStart, y, rightStart + WIDTH - width, v,
                    to - rightStart, height, WIDTH, textureHeight);
        }
        //?} else {
        /*g.blit(texture, x, y, 0, 0, split, HEIGHT, WIDTH, HEIGHT);
        g.blit(texture, x + split, y, split + WIDTH - width, 0, width - split, HEIGHT, WIDTH, HEIGHT);
        *///?}
    }

    /** Interior rows the liquid covers; at least one while there is any. */
    private static int filled(FluidStack stack, double capacity) {
        if (stack.isEmpty() || !(capacity > 0)) {
            return 0;
        }
        return (int) Math.max(1, Math.min(INTERIOR_HEIGHT, INTERIOR_HEIGHT * (double) stack.getAmount() / capacity));
    }

    /** Interior rows {@code top..HEIGHT - FRAME} tiled with the liquid's still sprite, tinted with its color. */
    //? if >=26.1 {
    private static void liquid(GuiGraphicsExtractor g, int x, int y, int top, FluidStack stack, int width) {
        TextureAtlasSprite sprite = dimensionalfluxminer.client.render.FluidSprites.still(stack.getFluid());
        int tint = dimensionalfluxminer.client.render.FluidSprites.tint(stack);
        int color = ARGB.color(Math.round(ARGB.alpha(tint) * FLUID_OPACITY), tint);
        int spriteWidth = sprite.contents().width();
        int spriteHeight = sprite.contents().height();
        int atlasWidth = Math.round(spriteWidth / (sprite.getU1() - sprite.getU0()));
        int atlasHeight = Math.round(spriteHeight / (sprite.getV1() - sprite.getV0()));
        // UV bounds exclude atlas padding; getX/getY point at the padded allocation instead.
        float spriteU = sprite.getU0() * atlasWidth;
        float spriteV = sprite.getV0() * atlasHeight;
        // Crop each quad directly, so translated recipe renderers do not need a second screen-space scissor.
    //?} else {
    /*private static void liquid(GuiGraphics g, int x, int y, int top, FluidStack stack, int width) {
        IClientFluidTypeExtensions extension = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation still = extension.getStillTexture(stack);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int tint = extension.getTintColor(stack);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F * FLUID_OPACITY;
        // The scissor takes screen coordinates, while windows draw in a translated pose.
        Vector3f origin = g.pose().last().pose().transformPosition(new Vector3f(x, y, 0));
        int sx = Math.round(origin.x());
        int sy = Math.round(origin.y());
        RenderSystem.enableBlend();
        g.enableScissor(sx + FRAME, sy + top, sx + width - FRAME, sy + HEIGHT - FRAME);
    *///?}
        for (int tileY = FRAME; tileY < HEIGHT - FRAME; tileY += 16) {
            //? if >=26.1 {
            int drawY = Math.max(tileY, top);
            int drawHeight = Math.min(tileY + 16, HEIGHT - FRAME) - drawY;
            if (drawHeight <= 0) {
                continue;
            }
            //?}
            for (int tileX = FRAME; tileX < width - FRAME; tileX += 16) {
                //? if >=26.1 {
                int drawWidth = Math.min(16, width - FRAME - tileX);
                g.blit(RenderPipelines.GUI_TEXTURED, sprite.atlasLocation(), x + tileX, y + drawY,
                        spriteU, spriteV + (drawY - tileY) * spriteHeight / 16.0F,
                        drawWidth, drawHeight, drawWidth * spriteWidth / 16, drawHeight * spriteHeight / 16,
                        atlasWidth, atlasHeight, color);
                //?} else {
                /*g.blit(x + tileX, y + tileY, 0, 16, 16, sprite, red, green, blue, alpha);
                *///?}
            }
        }
        //? if <26.1 {
        /*g.disableScissor();
        *///?}
    }
}
