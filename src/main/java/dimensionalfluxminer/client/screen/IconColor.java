package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.Locale;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * Theme color the player picks for a machine (blue by default, purple, or a custom color). It colors the frame of the
 * screens, the selected controls, the outline of the button under the mouse, the text and glass of the read-out
 * windows, the energy fill (screens and Jade), the volume bar, the mode icon of the Electric Furnace and the animated
 * terminal of the machines in the world. The other icons stay white. Every choice uses the same source artwork and a
 * runtime palette.
 */
public enum IconColor {
    BLUE(0xFF20C8D1, DfmScreen.SCREEN_TEXT, DfmScreen.SCREEN_TEXT, "energy_shimmer_sheet",
            0x14243A, 0x1F3852, 0x0F1A2B),
    PURPLE(0xFF9366DE, 0xFFB592FF, 0xFF9366DE, "energy_shimmer_sheet_purple",
            0x1E1430, 0x2E1F4A, 0x170F24),
    CUSTOM(0xFFEF8B32, DfmScreen.SCREEN_TEXT, DfmScreen.SCREEN_TEXT, "energy_shimmer_sheet_neutral",
            0x14243A, 0x1F3852, 0x0F1A2B);

    private final int preview;
    private final int screenText;
    private final int volumeLit;
    //? if >=26.1 {
    private final Identifier energySheet;
    //?} else {
    /*private final ResourceLocation energySheet;
    *///?}
    private final int glassTop;
    private final int glassBottom;
    private final int glassRim;

    IconColor(int preview, int screenText, int volumeLit, String energySheet, int glassTop,
            int glassBottom, int glassRim) {
        this.preview = preview;
        this.screenText = screenText;
        this.volumeLit = volumeLit;
        this.energySheet = DimensionalFluxMiner.id("textures/gui/energy/" + energySheet + ".png");
        this.glassTop = glassTop;
        this.glassBottom = glassBottom;
        this.glassRim = glassRim;
    }

    /** Name saved in the player's settings file and used in the translation key ({@code blue}, {@code purple}, ...). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The color saved as {@code id}; blue (the default) when unknown, including the former white choice. */
    static IconColor byId(String id) {
        for (IconColor color : values()) {
            if (color.id().equals(id)) {
                return color;
            }
        }
        return BLUE;
    }

    /** Color shown for this choice in the settings window. */
    int preview() {
        return this == CUSTOM ? iconTint() : preview;
    }

    /** The theme color itself (ARGB): accents, furnace mode icon, terminal of the machines. */
    int iconTint() {
        return switch (this) {
            case BLUE -> 0xFF45E5E9;
            case PURPLE -> 0xFFC6A5FF;
            case CUSTOM -> 0xFF000000 | GuiTheme.customRgb();
        };
    }

    /** Text of the terminal screens. */
    int screenText() {
        return this == CUSTOM ? GuiTheme.customText() : screenText;
    }

    /** Lit segments of the volume bar. */
    int volumeLit() {
        return this == CUSTOM ? iconTint() : volumeLit;
    }

    /** Glass of the read-out windows at the top and at the bottom (0xRRGGBB): navy, or dark purple with the purple theme. */
    int glassTop() {
        return this == CUSTOM ? GuiTheme.customTop() : glassTop;
    }

    int glassBottom() {
        return this == CUSTOM ? GuiTheme.customBottom() : glassBottom;
    }

    /** Dark rim around the glass (0xRRGGBB). */
    int glassRim() {
        return this == CUSTOM ? GuiTheme.customRim() : glassRim;
    }

    /**
     * Color (0xRRGGBB) of the animated terminal of the machines in the world, over its neutral gray texture: the hue of
     * the theme color at a mid lightness (see {@link ThemeAccent#terminal}), so light colors such as the purple do not
     * wash out to pink over the bright pixels of the screen.
     */
    public int terminalRgb(int customRgb) {
        return ThemeAccent.terminal(this == CUSTOM ? customRgb & 0xFFFFFF : iconTint() & 0xFFFFFF);
    }

    /** Vertex color of the energy fill: the chosen color over the gray sheet with a custom color, otherwise white. */
    public int energyTint(int customRgb) {
        return this == CUSTOM ? 0xFF000000 | customRgb : 0xFFFFFFFF;
    }

    /** Vertex color of the energy fill on the screen being drawn. */
    int energyTint() {
        return energyTint(GuiTheme.customRgb());
    }

    /** Shimmering energy fill (42 frames of 8 x 66); gray with a custom color, tinted by {@link #energyTint}. */
    //? if >=26.1 {
    public Identifier energySheet() {
    //?} else {
    /*public ResourceLocation energySheet() {
    *///?}
        return energySheet;
    }
}
