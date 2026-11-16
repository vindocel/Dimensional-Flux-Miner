package dimensionalfluxminer.client.screen;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.neoforged.neoforge.fluids.FluidStack;

/** Shared positions and tank artwork for container conversion recipe pages. */
public final class TankRecipeLayout {
    public static final int WIDTH = 154;
    public static final int HEIGHT = 72;
    public static final int INPUT_ITEM_X = 42;
    public static final int OUTPUT_ITEM_X = 94;
    public static final int ITEM_Y = 28;
    public static final int FLUID_Y = 4;
    public static final int INPUT_FLUID_X = 4;
    public static final int OUTPUT_FLUID_X = WIDTH - INPUT_FLUID_X - FluidGauge.NARROW_WIDTH;

    private TankRecipeLayout() {
    }

    public static int fluidX(boolean filling) {
        return filling ? INPUT_FLUID_X : OUTPUT_FLUID_X;
    }

    //? if >=26.1 {
    public static void drawEmptySideAndArrow(GuiGraphicsExtractor g, int x, int y, boolean filling) {
    //?} else {
    /*public static void drawEmptySideAndArrow(GuiGraphics g, int x, int y, boolean filling) {
    *///?}
        FluidGauge.drawNarrow(g, x + fluidX(!filling), y + FLUID_Y, FluidStack.EMPTY, 1);
        int color = GuiTheme.chromeColor(0xFF888888);
        g.fill(x + 69, y + 34, x + 79, y + 37, color);
        for (int i = 0; i < 6; i++) {
            g.fill(x + 77 + i, y + 30 + i, x + 78 + i, y + 41 - i, color);
        }
    }
}
