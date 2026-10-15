package dimensionalfluxminer.miner;

/**
 * The eight visual stages of the formed machine. Selection order: no energy (gray, by installed core) ->
 * no core (yellow) -> idle in the core palette when blocked -> active while really processing.
 */
public enum MinerVisualStage {
    NO_ENERGY("no_energy"),
    NO_ENERGY_CORE_BASIC("no_energy_core_basic"),
    NO_ENERGY_CORE_STABILIZED("no_energy_core_stabilized"),
    ENERGY_NO_CORE("energy_no_core"),
    ENERGY_CORE_BASIC_IDLE("energy_core_basic_idle"),
    ENERGY_CORE_BASIC("energy_core_basic"),
    ENERGY_CORE_STABILIZED_IDLE("energy_core_stabilized_idle"),
    ENERGY_CORE_STABILIZED("energy_core_stabilized");

    private final String key;

    MinerVisualStage(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean processing() {
        return this == ENERGY_CORE_BASIC || this == ENERGY_CORE_STABILIZED;
    }

    public boolean hasCore() {
        return this != NO_ENERGY && this != ENERGY_NO_CORE;
    }

    public static MinerVisualStage byKey(String key) {
        for (MinerVisualStage stage : values()) {
            if (stage.key.equals(key)) {
                return stage;
            }
        }
        return NO_ENERGY;
    }

    /**
     * How fast the core spins and pulls its motes at each Flux Rate: a little more at every level up to 7, then a
     * clearer step at 8, 9 and 10, never so fast that the motion cannot be followed.
     */
    private static final float[] RATE_SPEED = {1.00F, 1.15F, 1.30F, 1.45F, 1.60F, 1.75F, 1.90F, 2.40F, 3.10F, 4.00F};

    public static float rateSpeed(int rate) {
        return RATE_SPEED[Math.max(1, Math.min(RATE_SPEED.length, rate)) - 1];
    }

    public enum CoreKind { NONE, BASIC, STABILIZED }

    public static MinerVisualStage select(CoreKind core, boolean hasEnergy, boolean processing) {
        if (!hasEnergy) {
            return switch (core) {
                case NONE -> NO_ENERGY;
                case BASIC -> NO_ENERGY_CORE_BASIC;
                case STABILIZED -> NO_ENERGY_CORE_STABILIZED;
            };
        }
        return switch (core) {
            case NONE -> ENERGY_NO_CORE;
            case BASIC -> processing ? ENERGY_CORE_BASIC : ENERGY_CORE_BASIC_IDLE;
            case STABILIZED -> processing ? ENERGY_CORE_STABILIZED : ENERGY_CORE_STABILIZED_IDLE;
        };
    }
}
