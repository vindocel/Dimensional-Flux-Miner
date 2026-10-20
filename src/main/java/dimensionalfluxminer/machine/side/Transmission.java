package dimensionalfluxminer.machine.side;

import java.util.Locale;

/** Independent transmissions configured per face. */
public enum Transmission {
    ITEM,
    ENERGY,
    REDSTONE,
    EXPERIENCE,
    /** Liquids of the Dimensional Tanks (input and output). */
    FLUID;

    public static final Transmission[] VALUES = values();

    public String translationKey() {
        return "gui.dimensionalfluxminer.transmission." + name().toLowerCase(Locale.ROOT);
    }
}
