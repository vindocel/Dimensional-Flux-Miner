package dimensionalfluxminer.machine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinerProductionCurveTest {
    private final MinerProductionCurve curve = new MinerProductionCurve(2_500, 5_000_000, 0.0125, 64);

    @Test
    void levelPowersFollowTheScaleThenCostDouble() {
        // Geometric scale from 2 500 to 5 000 000 up to level 5, doubled from 6 to 9, the maximum at 10.
        long[] expected = {2_500, 5_817, 13_536, 31_498, 73_293, 341_095, 793_701, 1_846_876, 4_297_530, 5_000_000};
        for (int level = 1; level <= 10; level++) {
            assertEquals(expected[level - 1], Math.round(curve.levelPower(level)), "level " + level);
        }
    }

    @Test
    void endpointsAndClamps() {
        assertEquals(0.0125, curve.rateForPower(2_500), 1e-12);
        assertEquals(64.0, curve.rateForPower(5_000_000), 1e-9);
        assertEquals(64.0, curve.rateForPower(9_000_000), 1e-9);
        assertEquals(0.0, curve.rateForPower(2_499.9));
    }

    @Test
    void levelRatesMatchContract() {
        // Production per level does not depend on the power limits.
        assertEquals(0.032288741625, curve.levelRate(2), 1e-6);
        assertEquals(0.08340502686, curve.levelRate(3), 1e-6);
        assertEquals(64.0, curve.levelRate(10), 1e-9);
        for (int level = 1; level <= 10; level++) {
            assertEquals(curve.levelRate(level), curve.rateForPower(curve.levelPower(level)), curve.levelRate(level) * 1e-9);
        }
    }

    @Test
    void inverseCurveChargesActualWork() {
        for (double power : new double[] {2_500, 9_000, 73_303, 200_000, 1_000_000, 4_500_000, 5_000_000}) {
            assertEquals(power, curve.powerForRate(curve.rateForPower(power)), power * 1e-9);
        }
        assertEquals(0.0, curve.powerForRate(0));
    }

    @Test
    void narrowLimitsStayStrictlyIncreasing() {
        MinerProductionCurve narrow = new MinerProductionCurve(1_000, 3_000, 0.0125, 64);
        for (int level = 2; level <= 10; level++) {
            assertTrue(narrow.levelPower(level) > narrow.levelPower(level - 1), "level " + level);
        }
    }

    @Test
    void configurationCannotRaiseTheHardCap() {
        assertEquals(64.0, new MinerProductionCurve(2_500, 5_000_000, 0.0125, 500).maxRate());
        assertThrows(IllegalArgumentException.class, () -> new MinerProductionCurve(500, 400, 0.0125, 64));
    }
}
