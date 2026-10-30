package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Vector2f;

/**
 * Liquid gauge of the screens, the liquid counterpart of the energy bar: {@value #WIDTH} x {@value #HEIGHT} px with a
 * {@value #FRAME} px frame ({@code fluid/fluid_empty.png}, darkened with the screen as chrome). The liquid fills the
 * interior from the bottom with its own animated texture and color at {@value #FLUID_OPACITY} of its opacity, a
 * transparent reflection ({@code fluid/fluid_shimmer_sheet.png}, {@value #FRAMES} frames, in step with the energy bar)
 * runs over the filled part, and the frame with the level marks ({@code fluid/fluid_marks.png}) goes on top. Drawn
 * for the wide chambers of the Dimensional Tanks: the textures are made at this width (interior columns repeated, the
 * reflection band redrawn with the same slope), never stretched.
 */
final class FluidGauge {
    static final int WIDTH = 30;
    static final int HEIGHT = 64;
    static final int FRAME = 3;
    private static final int INTERIOR_HEIGHT = HEIGHT - 2 * FRAME;
    private static final float FLUID_OPACITY = 0.8F;
    private static final int FRAMES = 42;
    private static final long FRAME_MS = 100;

    private static final Identifier EMPTY = DimensionalFluxMiner.id("textures/gui/fluid/fluid_empty.png");
    private static final Identifier MARKS = DimensionalFluxMiner.id("textures/gui/fluid/fluid_marks.png");
    private static final Identifier SHIMMER = DimensionalFluxMiner.id("textures/gui/fluid/fluid_shimmer_sheet.png");

    private FluidGauge() {
    }

    /** The gauge at (x, y) holding {@code stack} out of {@code capacity} mB. */
    static void draw(GuiGraphicsExtractor g, int x, int y, FluidStack stack, double capacity) {
        GuiTheme.chrome(() -> g.blit(RenderPipelines.GUI_TEXTURED, EMPTY, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT));
        int bottom = HEIGHT - FRAME;
        int top = bottom - filled(stack, capacity);
        if (top < bottom) {
            liquid(g, x, y, top, stack);
            // The reflection is mostly transparent (the GUI pipeline blends).
            int frameV = (int) ((Util.getMillis() / FRAME_MS) % FRAMES) * HEIGHT;
            g.blit(RenderPipelines.GUI_TEXTURED, SHIMMER, x + FRAME, y + top, FRAME, frameV + top, WIDTH - 2 * FRAME, bottom - top, WIDTH, HEIGHT * FRAMES);
        }
        GuiTheme.chrome(() -> g.blit(RenderPipelines.GUI_TEXTURED, MARKS, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT));
    }

    /** Interior rows the liquid covers; at least one while there is any. */
    private static int filled(FluidStack stack, double capacity) {
        if (stack.isEmpty() || !(capacity > 0)) {
            return 0;
        }
        return (int) Math.max(1, Math.min(INTERIOR_HEIGHT, INTERIOR_HEIGHT * (double) stack.getAmount() / capacity));
    }

    /** Interior rows {@code top..HEIGHT - FRAME} tiled with the liquid's still sprite, tinted with its color. */
    private static void liquid(GuiGraphicsExtractor g, int x, int y, int top, FluidStack stack) {
        TextureAtlasSprite sprite = dimensionalfluxminer.client.render.FluidSprites.still(stack.getFluid());
        int tint = dimensionalfluxminer.client.render.FluidSprites.tint(stack);
        int color = ARGB.color(Math.round(ARGB.alpha(tint) * FLUID_OPACITY), tint);
        // The scissor takes screen coordinates, while windows draw in a translated pose.
        Vector2f origin = g.pose().transformPosition(new Vector2f(x, y));
        int sx = Math.round(origin.x());
        int sy = Math.round(origin.y());
        g.enableScissor(sx + FRAME, sy + top, sx + WIDTH - FRAME, sy + HEIGHT - FRAME);
        for (int tileY = FRAME; tileY < HEIGHT - FRAME; tileY += 16) {
            for (int tileX = FRAME; tileX < WIDTH - FRAME; tileX += 16) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + tileX, y + tileY, 16, 16, color);
            }
        }
        g.disableScissor();
    }
}
