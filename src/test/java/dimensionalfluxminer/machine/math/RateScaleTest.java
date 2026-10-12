package dimensionalfluxminer.machine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RateScaleTest {
    @Test
    void minerPowerLevelsMatchContract() {
        long[] expected = {500, 1217, 2963, 7211, 17553, 42727, 104004, 253161, 616232, 1500000};
        for (int n = 1; n <= 10; n++) {
            assertEquals(expected[n - 1], RateScale.powerLimit(n, 500, 1_500_000, 1.0), "level " + n);
        }
    }

    @Test
    void crusherPowerAndThroughputMatchContract() {
        long[] power = {250, 582, 1354, 3150, 7329, 17055, 39685, 92344, 214876, 500000};
        long[] efficiency = {125, 291, 677, 1575, 3665, 8527, 19843, 46172, 107438, 250000};
        double[] ops = {0.005, 0.014299691, 0.040896235, 0.11696071, 0.334500412, 0.95665054, 2.735961515, 7.824681114, 22.378105176, 64.0};
        for (int n = 1; n <= 10; n++) {
            assertEquals(power[n - 1], RateScale.powerLimit(n, 250, 500_000, 1.0), "power " + n);
            assertEquals(efficiency[n - 1], RateScale.powerLimit(n, 250, 500_000, 0.5), "efficiency " + n);
            assertEquals(ops[n - 1], RateScale.value(n, 0.005, 64), 1e-6, "ops " + n);
        }
    }

    @Test
    void smelterPowerAndLaneThroughputMatchContract() {
        long[] power = {600, 1293, 2785, 6000, 12927, 27850, 60000, 129266, 278495, 600000};
        long[] efficiency = {300, 646, 1392, 3000, 6463, 13925, 30000, 64633, 139248, 300000};
        double[] lane = {0.01, 0.029917297, 0.089504468, 0.26777318, 0.801104986, 2.396689613, 7.170247594, 21.451442975, 64.176919927, 192.0};
        for (int n = 1; n <= 10; n++) {
            assertEquals(power[n - 1], RateScale.powerLimit(n, 600, 600_000, 1.0), "power " + n);
            assertEquals(efficiency[n - 1], RateScale.powerLimit(n, 600, 600_000, 0.5), "efficiency " + n);
            assertEquals(lane[n - 1], RateScale.smelterOperationsPerTick(n, 100, 192), 1e-6, "lane " + n);
        }
    }

    @Test
    void rateCycleWrapsBothWays() {
        assertEquals(2, RateScale.cycleLevel(1, true));
        assertEquals(1, RateScale.cycleLevel(10, true));
        assertEquals(10, RateScale.cycleLevel(1, false));
        assertEquals(9, RateScale.cycleLevel(10, false));
    }

    @Test
    void bufferIsTenTicksOfEffectivePower() {
        assertEquals(15_000_000, RateScale.energyBufferTarget(RateScale.powerLimit(10, 500, 1_500_000, 1.0)));
        assertEquals(1_250, RateScale.energyBufferTarget(RateScale.powerLimit(1, 250, 500_000, 0.5)));
        assertEquals(3_000_000, RateScale.energyBufferTarget(RateScale.powerLimit(10, 600, 600_000, 0.5)));
    }
}
