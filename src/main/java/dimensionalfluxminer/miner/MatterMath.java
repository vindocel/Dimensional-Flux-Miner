package dimensionalfluxminer.miner;

import java.util.OptionalInt;

/**
 * Integer arithmetic of the Dimensional Matter (1 DM = 10 units), free of game classes. {@link MatterValues} applies it
 * to items.
 */
public final class MatterMath {
    public static final int UNITS_PER_DM = 10;
    public static final int ROLL_COST_UNITS = 2 * UNITS_PER_DM;
    /** Matter Input with the Efficiency Upgrade: + min(base, 0.2 DM) per item. */
    public static final int EFFICIENCY_BONUS_CAP_UNITS = 2;
    /** An item the Miner itself can produce never gives back more than the 2 DM of the attempt that made it. */
    public static final int PRODUCIBLE_CAP_UNITS = ROLL_COST_UNITS;
    /** Filter recovery (its own rule, unchanged): min(base + 0.5 DM, 2 DM). */
    public static final int FILTER_RECOVERY_BONUS_UNITS = 5;
    public static final int FILTER_RECOVERY_CAP_UNITS = ROLL_COST_UNITS;

    private MatterMath() {
    }

    /** Matter Input conversion of a substrate: {@code base + min(base, 2)} units with the Efficiency Upgrade. */
    public static int matterInputUnits(int base, boolean efficiencyUpgrade) {
        if (base <= 0) {
            return 0;
        }
        return efficiencyUpgrade ? base + Math.min(base, EFFICIENCY_BONUS_CAP_UNITS) : base;
    }

    /**
     * Units one item gives from its base value: the upgrade formula, a recipe variant never above its share of the
     * upgraded substrates ({@code upgradedVariant}), an item the Miner can produce never above 2 DM.
     */
    public static int inputUnits(int base, boolean efficiencyUpgrade, boolean producible, OptionalInt upgradedVariant) {
        int units = matterInputUnits(base, efficiencyUpgrade);
        if (efficiencyUpgrade && upgradedVariant.isPresent()) {
            units = Math.min(units, upgradedVariant.getAsInt());
        }
        return producible ? Math.min(units, PRODUCIBLE_CAP_UNITS) : units;
    }

    /** Filter recovery of a rejected generated ore: min(base + 0.5 DM, 2 DM). */
    public static int filterRecoveryUnits(int base) {
        return Math.min(base + FILTER_RECOVERY_BONUS_UNITS, FILTER_RECOVERY_CAP_UNITS);
    }
}
