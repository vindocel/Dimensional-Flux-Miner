package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.Locale;
import net.minecraft.resources.Identifier;

/**
 * Icon color the player picks for a machine screen; white is the default. The energy fill (screens and Jade), the
 * text and glass of the read-out windows, the volume bar and the outline of the button under the mouse are blue with white and
 * blue icons, purple with purple icons. Each
 * color has the same icons in its own folder of {@code sprites}; a new icon goes in every folder.
 */
public enum IconColor {
    WHITE("icon_white", 0xFFFFFFFF, DfmScreen.SCREEN_TEXT, DfmScreen.SCREEN_TEXT, 0xFF4AABB9, "energy_shimmer_sheet",
            0x14243A, 0x1F3852, 0x0F1A2B),
    BLUE("icon", 0xFF20C8D1, DfmScreen.SCREEN_TEXT, DfmScreen.SCREEN_TEXT, 0xFF4AABB9, "energy_shimmer_sheet",
            0x14243A, 0x1F3852, 0x0F1A2B),
    PURPLE("icon_purple", 0xFF9366DE, 0xFFB592FF, 0xFF9366DE, 0xFF784AB9, "energy_shimmer_sheet_purple",
            0x1E1430, 0x2E1F4A, 0x170F24);

    private final String folder;
    private final int preview;
    private final int screenText;
    private final int volumeLit;
    private final int hoverOutline;
    private final Identifier energySheet;
    private final int glassTop;
    private final int glassBottom;
    private final int glassRim;

    IconColor(String folder, int preview, int screenText, int volumeLit, int hoverOutline, String energySheet, int glassTop,
            int glassBottom, int glassRim) {
        this.folder = folder;
        this.preview = preview;
        this.screenText = screenText;
        this.volumeLit = volumeLit;
        this.hoverOutline = hoverOutline;
        this.energySheet = DimensionalFluxMiner.id("textures/gui/energy/" + energySheet + ".png");
        this.glassTop = glassTop;
        this.glassBottom = glassBottom;
        this.glassRim = glassRim;
    }

    /** Name saved in the player's settings file and used in the translation key ({@code blue}, {@code purple}, ...). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The color saved as {@code id}; white (the default) when unknown. */
    static IconColor byId(String id) {
        for (IconColor color : values()) {
            if (color.id().equals(id)) {
                return color;
            }
        }
        return WHITE;
    }

    /** Folder of this color's icons in the GUI atlas ({@code sprites/<folder>/<name>.png}). */
    String folder() {
        return folder;
    }

    /** Color shown for this choice in the settings window. */
    int preview() {
        return preview;
    }

    /** Text of the terminal screens. */
    int screenText() {
        return screenText;
    }

    /** Lit segments of the volume bar. */
    int volumeLit() {
        return volumeLit;
    }

    /** Outline of the button under the mouse: cyan, or the same tone turned purple (hue 265) with purple icons. */
    int hoverOutline() {
        return hoverOutline;
    }

    /** Glass of the read-out windows at the top and at the bottom (0xRRGGBB): navy, or dark purple with purple icons. */
    int glassTop() {
        return glassTop;
    }

    int glassBottom() {
        return glassBottom;
    }

    /** Dark rim around the glass (0xRRGGBB). */
    int glassRim() {
        return glassRim;
    }

    /** Shimmering energy fill (42 frames of 8 x 66). */
    public Identifier energySheet() {
        return energySheet;
    }
}
