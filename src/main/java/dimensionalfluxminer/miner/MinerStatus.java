package dimensionalfluxminer.miner;

import java.util.Locale;

/** Operational states of the Dimensional Flux Miner shown by the GUI and used by the renderer. */
public enum MinerStatus {
    MULTIBLOCK_INVALID,
    CORE_MISSING,
    MANUAL_STOP,
    REDSTONE_HALT,
    NO_RESOURCES,
    DIMENSIONAL_SURVEY,
    OUTPUT_BLOCKED,
    WAITING_FOR_ENERGY,
    WAITING_FOR_MATTER,
    NO_DRAINABLE_MATTER,
    RUNNING;

    public String key() {
        return "gui.dimensionalfluxminer.miner.status." + name().toLowerCase(Locale.ROOT);
    }

    public LimitingFactor limitingFactor() {
        return switch (this) {
            case REDSTONE_HALT -> LimitingFactor.REDSTONE;
            case OUTPUT_BLOCKED -> LimitingFactor.OUTPUT;
            case WAITING_FOR_ENERGY -> LimitingFactor.FLUX;
            case WAITING_FOR_MATTER -> LimitingFactor.DIMENSIONAL_MATTER;
            case NO_DRAINABLE_MATTER, DIMENSIONAL_SURVEY -> LimitingFactor.DIMENSIONAL_INTEGRITY;
            default -> LimitingFactor.NONE;
        };
    }

    /** What held the production back, in priority order (the strongest one met in a tick is shown). */
    public enum LimitingFactor {
        REDSTONE, OUTPUT, FLUX, DIMENSIONAL_MATTER, DIMENSIONAL_INTEGRITY, NONE;

        public String key() {
            return "gui.dimensionalfluxminer.miner.limit." + name().toLowerCase(Locale.ROOT);
        }
    }
}
