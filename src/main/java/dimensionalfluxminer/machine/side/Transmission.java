package dimensionalfluxminer.machine.side;

import java.util.Locale;

/** Independent transmissions configured per face. */
public enum Transmission {
    ITEM,
    ENERGY,
    REDSTONE,
    EXPERIENCE;

    public static final Transmission[] VALUES = values();

    public String translationKey() {
        return "gui.dimensionalfluxminer.transmission." + name().toLowerCase(Locale.ROOT);
    }
}
