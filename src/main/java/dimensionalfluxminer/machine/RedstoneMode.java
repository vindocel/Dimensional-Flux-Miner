package dimensionalfluxminer.machine;

import java.util.Locale;

/** Whole-block redstone control: the signal may reach the machine from any side. */
public enum RedstoneMode {
    /** Redstone has no effect. */
    IGNORED,
    /** Runs only while powered. */
    HIGH,
    /** Runs only while unpowered. */
    LOW;

    public static final RedstoneMode[] VALUES = values();

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    public RedstoneMode cycle(boolean forward) {
        return VALUES[Math.floorMod(ordinal() + (forward ? 1 : -1), VALUES.length)];
    }

    public String translationKey() {
        return "gui.dimensionalfluxminer.redstone_mode." + name().toLowerCase(Locale.ROOT);
    }

    public static RedstoneMode byName(String name) {
        for (RedstoneMode mode : VALUES) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return IGNORED;
    }
}
