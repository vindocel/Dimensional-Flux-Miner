package dimensionalfluxminer.machine.side;

import java.util.Locale;

/** Route types of every transmission. {@link #NONE} is shared by all transmissions. */
public enum SideRoute {
    NONE(null, 0xFFCFCFCF),
    ITEM_INPUT_ALL(Transmission.ITEM, 0xFFC9071F),
    ITEM_INPUT_1(Transmission.ITEM, 0xFFFFA160),
    ITEM_INPUT_2(Transmission.ITEM, 0xFFFFDD4F),
    ITEM_INPUT_3(Transmission.ITEM, 0xFFD55ECB),
    ITEM_OUTPUT(Transmission.ITEM, 0xFF366BD0),
    ITEM_INPUT_OUTPUT_ALL(Transmission.ITEM, 0xFFA460D9),
    /** Energy is always the light blue of the energy bar, on every screen, port and text. */
    ENERGY_INPUT(Transmission.ENERGY, 0xFF36CFEE),
    REDSTONE_INPUT(Transmission.REDSTONE, 0xFFFF383C),
    XP_OUTPUT(Transmission.EXPERIENCE, 0xFF75FF89),
    /** Dimensional Tank: the single chamber. */
    FLUID_INPUT(Transmission.FLUID, 0xFFC9071F),
    FLUID_OUTPUT(Transmission.FLUID, 0xFF366BD0),
    FLUID_INPUT_OUTPUT(Transmission.FLUID, 0xFFA460D9),
    /** Dual Dimensional Tank: each face serves one chamber, never both. */
    FLUID_A_INPUT(Transmission.FLUID, 0xFFC9071F),
    FLUID_A_OUTPUT(Transmission.FLUID, 0xFF366BD0),
    FLUID_A_INPUT_OUTPUT(Transmission.FLUID, 0xFFA460D9),
    FLUID_B_INPUT(Transmission.FLUID, 0xFFFFA160),
    FLUID_B_OUTPUT(Transmission.FLUID, 0xFF52D9DB),
    FLUID_B_INPUT_OUTPUT(Transmission.FLUID, 0xFFFFDD4F);

    public static final SideRoute[] VALUES = values();

    private final Transmission transmission;
    /** Configuration-only overlay color (ARGB). */
    private final int color;

    SideRoute(Transmission transmission, int color) {
        this.transmission = transmission;
        this.color = color;
    }

    public int color() {
        return color;
    }

    public boolean belongsTo(Transmission t) {
        return this == NONE || transmission == t;
    }

    public boolean insertsItems() {
        return this == ITEM_INPUT_ALL || this == ITEM_INPUT_OUTPUT_ALL || dedicatedLane() >= 0;
    }

    public boolean extractsItems() {
        return this == ITEM_OUTPUT || this == ITEM_INPUT_OUTPUT_ALL;
    }

    /** Lane index (0..2) for the dedicated Alloy routes, otherwise -1. */
    public int dedicatedLane() {
        return switch (this) {
            case ITEM_INPUT_1 -> 0;
            case ITEM_INPUT_2 -> 1;
            case ITEM_INPUT_3 -> 2;
            default -> -1;
        };
    }

    /** Tank chamber a FLUID route serves (0 = single / A, 1 = B), otherwise -1. */
    public int fluidChamber() {
        return switch (this) {
            case FLUID_INPUT, FLUID_OUTPUT, FLUID_INPUT_OUTPUT, FLUID_A_INPUT, FLUID_A_OUTPUT, FLUID_A_INPUT_OUTPUT -> 0;
            case FLUID_B_INPUT, FLUID_B_OUTPUT, FLUID_B_INPUT_OUTPUT -> 1;
            default -> -1;
        };
    }

    public boolean fillsFluid() {
        return this == FLUID_INPUT || this == FLUID_INPUT_OUTPUT || this == FLUID_A_INPUT || this == FLUID_A_INPUT_OUTPUT
                || this == FLUID_B_INPUT || this == FLUID_B_INPUT_OUTPUT;
    }

    public boolean drainsFluid() {
        return this == FLUID_OUTPUT || this == FLUID_INPUT_OUTPUT || this == FLUID_A_OUTPUT || this == FLUID_A_INPUT_OUTPUT
                || this == FLUID_B_OUTPUT || this == FLUID_B_INPUT_OUTPUT;
    }

    public String translationKey() {
        return "gui.dimensionalfluxminer.route." + name().toLowerCase(Locale.ROOT);
    }
}
