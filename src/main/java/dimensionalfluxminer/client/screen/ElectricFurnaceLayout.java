package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.furnace.ElectricFurnaceMode;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Electric Furnace processing element (inputs, pipes, heat chamber, outputs), shared by the machine screen and
 * the recipe viewers. Coordinates are relative to the top-left corner of the first input slot frame.
 */
public final class ElectricFurnaceLayout {
    private static final ResourceLocation HEAT = DimensionalFluxMiner.id("textures/gui/electric_furnace/heat.png");

    public static final int WIDTH = 68;
    public static final int HEIGHT = 75;
    /** Slot frames (outer 18 x 18); items sit 1 px inside. */
    public static final int[] INPUT_X = {0, 25, 50};
    public static final int INPUT_Y = 0;
    public static final int OUTPUT_Y = 57;
    public static final int HEAT_HEIGHT = 10;
    /** Vertical center of the furnace burners, lined up with the middle of the energy bar. */
    public static final int BURNER_CENTER = 40;
    /** The heat chamber (all variants), which opens the recipe viewers. */
    public static final int CHAMBER_X = 24;
    public static final int CHAMBER_Y = 28;
    public static final int CHAMBER_W = 21;
    public static final int CHAMBER_H = 19;

    /** One element per mode: its texture, output slots and heat openings (2 x 10). */
    public enum Variant {
        /** Furnace only: three independent lanes, three outputs. */
        FURNACE("element_furnace", new int[] {0, 25, 50}, new int[] {26, 33, 41}, 35),
        /** Alloy only: one chamber, main output and byproduct. */
        ALLOY("element_alloy", new int[] {13, 38}, new int[] {27, 33, 39}, 32),
        /** Alloy + furnace: one chamber feeding the three outputs. */
        ALL("element_alloy_furnace", new int[] {0, 25, 50}, new int[] {26, 33, 41}, 35);

        private final ResourceLocation texture;
        public final int[] outputX;
        public final int[] heatX;
        public final int heatY;

        Variant(String texture, int[] outputX, int[] heatX, int heatY) {
            this.texture = DimensionalFluxMiner.id("textures/gui/electric_furnace/" + texture + ".png");
            this.outputX = outputX;
            this.heatX = heatX;
            this.heatY = heatY;
        }

        public static Variant of(ElectricFurnaceMode mode) {
            return switch (mode) {
                case NORMAL -> FURNACE;
                case ALLOY -> ALLOY;
                case ALL -> ALL;
            };
        }
    }

    private ElectricFurnaceLayout() {
    }

    public static void drawElement(GuiGraphics g, int x, int y, Variant variant) {
        g.blit(variant.texture, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    /**
     * Heat opening: full at the start of the recipe, consumed from the top as it progresses (cropped, never
     * stretched); nothing is drawn once the last row is gone.
     */
    public static void heat(GuiGraphics g, int x, int y, double progress) {
        int removed = Math.min(HEAT_HEIGHT, (int) Math.floor(Mth.clamp(progress, 0, 1) * HEAT_HEIGHT));
        if (removed >= HEAT_HEIGHT) {
            return;
        }
        g.blit(HEAT, x, y + removed, 0, removed, 2, HEAT_HEIGHT - removed, 2, HEAT_HEIGHT);
    }

    /** Heat of all the openings for recipe previews: one recipe every {@code cycleMs}. */
    public static void previewHeat(GuiGraphics g, int x, int y, Variant variant, long cycleMs) {
        double progress = (Util.getMillis() % cycleMs) / (double) cycleMs;
        for (int hx : variant.heatX) {
            heat(g, x + hx, y + variant.heatY, progress);
        }
    }
}
