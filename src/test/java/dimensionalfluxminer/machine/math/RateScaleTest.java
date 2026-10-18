package dimensionalfluxminer.machine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RateScaleTest {
    @Test
    void crusherPowerAndThroughputMatchContract() {
        // 20 000 FE per operation (10 000 with the upgrade), 1 operation / 200 ticks -> 64 per tick.
        long[] power = {100, 286, 818, 2_339, 6_690, 19_133, 54_719, 156_494, 447_562, 1_280_000};
        long[] efficiency = {50, 143, 409, 1_170, 3_345, 9_567, 27_360, 78_247, 223_781, 640_000};
        double[] ops = {0.005, 0.014299691, 0.040896235, 0.11696071, 0.334500412, 0.95665054, 2.735961515, 7.824681114, 22.378105176, 64.0};
        for (int n = 1; n <= 10; n++) {
            double rate = RateScale.value(n, 0.005, 64);
            assertEquals(ops[n - 1], rate, 1e-6, "ops " + n);
            assertEquals(power[n - 1], RateScale.powerLimit(RateScale.power(rate, 20_000)), "power " + n);
            assertEquals(efficiency[n - 1], RateScale.powerLimit(RateScale.power(rate, 10_000)), "efficiency " + n);
        }
    }

    @Test
    void electricFurnaceProfilesFollowTheWorkOfAStandardRecipe() {
        // Standard recipe (200 ticks). Alloy: 30 000 FE per execution; furnace: 50 000 / 3 FE per item, three lanes.
        long[] alloy = {150, 429, 1_227, 3_509, 10_035, 28_700, 82_079, 234_740, 671_343, 1_920_000};
        long[] furnace = {250, 715, 2_045, 5_848, 16_725, 47_833, 136_798, 391_234, 1_118_905, 3_200_000};
        for (int n = 1; n <= 10; n++) {
            double lane = RateScale.electricFurnaceOperationsPerTick(n, 200, 64);
            assertEquals(alloy[n - 1], RateScale.powerLimit(RateScale.power(lane, 30_000)), "alloy " + n);
            assertEquals(furnace[n - 1], RateScale.powerLimit(3 * RateScale.power(lane, 50_000 / 3.0)), "furnace " + n);
        }
    }

    @Test
    void recipeDurationChangesTheDemandButNotTheCostPerItem() {
        for (int n = 1; n <= 10; n++) {
            double fast = RateScale.electricFurnaceOperationsPerTick(n, 100, 64);
            double standard = RateScale.electricFurnaceOperationsPerTick(n, 200, 64);
            assertEquals(30_000, RateScale.power(fast, 30_000) / fast, 1e-6);
            assertEquals(30_000, RateScale.power(standard, 30_000) / standard, 1e-6);
        }
        assertEquals(2 * RateScale.electricFurnaceOperationsPerTick(1, 200, 64), RateScale.electricFurnaceOperationsPerTick(1, 100, 64), 1e-12);
    }

    @Test
    void laneThroughputMatchesContract() {
        double[] lane = {0.01, 0.026479445, 0.070116103, 0.185663553, 0.491626794, 1.301800488, 3.447095504, 9.127717745, 24.169690434, 64.0};
        for (int n = 1; n <= 10; n++) {
            assertEquals(lane[n - 1], RateScale.electricFurnaceOperationsPerTick(n, 100, 64), 1e-6, "lane " + n);
        }
    }

    @Test
    void aLaneSpeedTableKeepsTheProportionOfOtherDurations() {
        double[] values = {0.1, 0.2, 0.4, 0.8, 1.6, 3.2, 6.4, 12.8, 25.6, 32};
        LevelTable table = new LevelTable(values);
        for (int n = 1; n <= 10; n++) {
            assertEquals(values[n - 1], RateScale.electricFurnaceOperationsPerTick(n, 200, 64, table, 200), 1e-12, "standard recipe, level " + n);
            double ratio = RateScale.electricFurnaceOperationsPerTick(n, 100, 64) / RateScale.electricFurnaceOperationsPerTick(n, 200, 64);
            assertEquals(Math.min(64, values[n - 1] * ratio), RateScale.electricFurnaceOperationsPerTick(n, 100, 64, table, 200), 1e-9, "100 ticks, level " + n);
            assertEquals(RateScale.electricFurnaceOperationsPerTick(n, 100, 64), RateScale.electricFurnaceOperationsPerTick(n, 100, 64, null, 200), 1e-12,
                    "no table: the default curve");
        }
        LevelTable fast = new LevelTable(new double[] {64, 64, 64, 64, 64, 64, 64, 64, 64, 64});
        assertEquals(64, RateScale.electricFurnaceOperationsPerTick(1, 100, 64, fast, 200), 1e-12, "never above 64 per lane");
    }

    @Test
    void rateCycleWrapsBothWays() {
        assertEquals(2, RateScale.cycleLevel(1, true));
        assertEquals(1, RateScale.cycleLevel(10, true));
        assertEquals(10, RateScale.cycleLevel(1, false));
        assertEquals(9, RateScale.cycleLevel(10, false));
    }

    @Test
    void bufferIsTenTicksOfEffectivePowerAndNeverOverflows() {
        assertEquals(128_000_000, RateScale.energyBufferTarget(12_800_000));
        assertEquals(1_000, RateScale.energyBufferTarget(100));
        assertEquals(Long.MAX_VALUE, RateScale.energyBufferTarget(Long.MAX_VALUE / 2));
        assertEquals(Long.MAX_VALUE, RateScale.saturatedMultiply(Integer.MAX_VALUE, Long.MAX_VALUE / 1000));
        assertEquals(Long.MAX_VALUE, RateScale.powerLimit(1e30));
        assertEquals(0, RateScale.powerLimit(Double.NaN));
    }
}
