package dimensionalfluxminer.furnace;

import static dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity.LANES;
import static dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity.RATE_TEN_OPERATIONS_PER_LANE;
import static dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity.STANDARD_COOKING_TICKS;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.machine.math.LevelTable;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.recipe.AlloyingRecipe;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * Speed and the two energy profiles of the Electric Furnace. Every job is billed by the profile of the work really done:
 * <ul>
 *     <li>Furnace: a cost per input item, only on the lanes that work;</li>
 *     <li>Alloy: a cost per unit of work, an execution being as many units as its largest ingredient count.</li>
 * </ul>
 * By default both costs are the same at every Rate; with {@code electric_furnace.furnacePowerByLevel} /
 * {@code electric_furnace.alloyPowerByLevel} the cost of a Rate is its configured FE/t divided by its operations per tick. The
 * Efficiency Upgrade halves both.
 */
public final class ElectricFurnaceEnergy {
    private ElectricFurnaceEnergy() {
    }

    private static double upgradeFactor(boolean upgrade) {
        return upgrade ? 0.5 : 1.0;
    }

    /**
     * Operations per lane per tick at {@code level} for a recipe of {@code recipeTicks}: R(n,T) (64 at Rate 10), or,
     * with {@code electric_furnace.speedByLevel}, the configured value for a standard recipe in the same proportion R(n,T) / R(n,200),
     * never above 64.
     */
    public static double laneOperationsPerTick(int level, int recipeTicks) {
        return RateScale.electricFurnaceOperationsPerTick(level, recipeTicks, RATE_TEN_OPERATIONS_PER_LANE,
                DfmConfig.levelTable(DfmConfig.ELECTRIC_FURNACE_SPEED_BY_LEVEL), STANDARD_COOKING_TICKS);
    }

    /** Alloy profile: FE of one unit of alloy work at {@code level} (a standard execution, one unit of each ingredient). */
    public static double alloyEnergyPerUnit(int level, boolean upgrade) {
        LevelTable power = DfmConfig.levelTable(DfmConfig.ELECTRIC_FURNACE_ALLOY_POWER_BY_LEVEL);
        double cost = power != null ? power.at(level) / laneOperationsPerTick(level, STANDARD_COOKING_TICKS)
                : DfmConfig.energy(DfmConfig.ELECTRIC_FURNACE_ALLOY_ENERGY);
        return cost * upgradeFactor(upgrade);
    }

    /** Furnace profile: FE of one item smelted in a lane at {@code level} (a third of the configured three-item cost). */
    public static double furnaceEnergyPerItem(int level, boolean upgrade) {
        LevelTable power = DfmConfig.levelTable(DfmConfig.ELECTRIC_FURNACE_FURNACE_POWER_BY_LEVEL);
        double cost = power != null ? power.at(level) / (LANES * laneOperationsPerTick(level, STANDARD_COOKING_TICKS))
                : DfmConfig.energy(DfmConfig.ELECTRIC_FURNACE_FURNACE_ENERGY) / (double) LANES;
        return cost * upgradeFactor(upgrade);
    }

    /** Units of work of one execution of an alloy: its largest ingredient count (the progress pace uses the same). */
    public static int alloyWorkUnits(AlloyingRecipe recipe) {
        int units = 1;
        for (SizedIngredient sized : recipe.ingredients()) {
            units = Math.max(units, sized.count());
        }
        return units;
    }

    /**
     * Executions of {@code recipe} per tick at {@code level}: each lane may spend its operations per tick in items (64 at
     * Rate 10) and one execution needs every ingredient at once, so a recipe asking for N units of an ingredient runs N
     * times fewer executions.
     */
    public static double alloyExecutionsPerTick(int level, AlloyingRecipe recipe) {
        return laneOperationsPerTick(level, recipe.processingTicks()) / alloyWorkUnits(recipe);
    }

    /** FE of one execution of {@code recipe} at {@code level}: proportional to its units of work, never to its duration. */
    public static double alloyExecutionEnergy(AlloyingRecipe recipe, int level, boolean upgrade) {
        return alloyWorkUnits(recipe) * alloyEnergyPerUnit(level, upgrade);
    }

    /** Alloy profile FE/t of a standard recipe at {@code level}. */
    public static double alloyStandardPower(int level, boolean upgrade) {
        return RateScale.power(laneOperationsPerTick(level, STANDARD_COOKING_TICKS), alloyEnergyPerUnit(level, upgrade));
    }

    /** Furnace profile FE/t of the three lanes smelting standard recipes at {@code level}. */
    public static double furnaceStandardPower(int level, boolean upgrade) {
        return LANES * RateScale.power(laneOperationsPerTick(level, STANDARD_COOKING_TICKS), furnaceEnergyPerItem(level, upgrade));
    }
}
