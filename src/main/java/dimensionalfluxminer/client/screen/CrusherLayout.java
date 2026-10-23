package dimensionalfluxminer.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Crusher processing mechanism (input slot, pipe, grinding chamber with the mill and two progress bars, output
 * pipe), shared by the machine screen and the recipe viewers. Coordinates are relative to the mechanism's top-left
 * corner (109 x 36 artwork).
 */
public final class CrusherLayout {
    private static final Identifier MECHANISM = DimensionalFluxMiner.id("textures/gui/crusher/mechanism.png");
    private static final Identifier MILL = DimensionalFluxMiner.id("textures/gui/crusher/mill.png");
    private static final Identifier MILL_SHEET = DimensionalFluxMiner.id("textures/gui/crusher/mill_sheet.png");
    private static final Identifier PROGRESS_LEFT = DimensionalFluxMiner.id("textures/gui/crusher/progress_left.png");
    private static final Identifier PROGRESS_RIGHT = DimensionalFluxMiner.id("textures/gui/crusher/progress_right.png");

    public static final int WIDTH = 109;
    public static final int HEIGHT = 36;
    /** Width up to the end of the output pipe (the artwork's single output slot starts here). */
    public static final int PIPE_END = 91;
    public static final int INPUT_X = 0;
    public static final int INPUT_Y = 8;
    /** The artwork's single output slot (recipe viewers). */
    public static final int OUTPUT_X = 91;
    public static final int OUTPUT_Y = 8;
    /** Mill (42 x 36) and the two progress bars (2 x 34). */
    public static final int MILL_X = 31;
    public static final int MILL_Y = 0;
    public static final int LEFT_BAR_X = 28;
    public static final int RIGHT_BAR_X = 74;
    public static final int BAR_Y = 1;
    public static final int BAR_HEIGHT = 34;
    /** Chamber area (clickable for recipes). */
    public static final int CHAMBER_X = 27;
    public static final int CHAMBER_WIDTH = 50;

    private static final int MILL_W = 42;
    private static final int MILL_H = 36;
    private static final int MILL_FRAMES = 17;
    private static final long MILL_FRAME_MS = 100;

    private CrusherLayout() {
    }

    /** The mechanism background; {@code width} crops it ({@link #WIDTH} for the whole artwork). */
    public static void drawMechanism(GuiGraphicsExtractor g, int x, int y, int width) {
        GuiTheme.chrome(() -> g.blit(RenderPipelines.GUI_TEXTURED, MECHANISM, x, y, 0, 0, width, HEIGHT, WIDTH, HEIGHT));
    }

    /** The mill: still while idle, turning (one frame every 100 ms) while the machine really processes. */
    public static void drawMill(GuiGraphicsExtractor g, int x, int y, boolean turning) {
        int frame = (int) ((Util.getMillis() / MILL_FRAME_MS) % MILL_FRAMES);
        GuiTheme.chrome(() -> {
            if (turning) {
                g.blit(RenderPipelines.GUI_TEXTURED, MILL_SHEET, x + MILL_X, y + MILL_Y, 0, frame * MILL_H, MILL_W, MILL_H, MILL_W, MILL_H * MILL_FRAMES);
            } else {
                g.blit(RenderPipelines.GUI_TEXTURED, MILL, x + MILL_X, y + MILL_Y, 0, 0, MILL_W, MILL_H, MILL_W, MILL_H);
            }
        });
    }

    /**
     * Both progress bars follow the real progress of the operation: the left one fills from the bottom, the right one
     * from the top; both are full at 100% (cropped, never stretched).
     */
    public static void drawProgress(GuiGraphicsExtractor g, int x, int y, double progress) {
        int h = (int) Math.floor(BAR_HEIGHT * Mth.clamp(progress, 0, 1));
        if (h <= 0) {
            return;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_LEFT, x + LEFT_BAR_X, y + BAR_Y + BAR_HEIGHT - h, 0, BAR_HEIGHT - h, 2, h, 2, BAR_HEIGHT);
        g.blit(RenderPipelines.GUI_TEXTURED, PROGRESS_RIGHT, x + RIGHT_BAR_X, y + BAR_Y, 0, 0, 2, h, 2, BAR_HEIGHT);
    }

    /** Recipe previews: the mill turns and the bars fill once every {@code cycleMs}. */
    public static void drawPreview(GuiGraphicsExtractor g, int x, int y, long cycleMs) {
        drawMill(g, x, y, true);
        drawProgress(g, x, y, (Util.getMillis() % cycleMs) / (double) cycleMs);
    }
}
