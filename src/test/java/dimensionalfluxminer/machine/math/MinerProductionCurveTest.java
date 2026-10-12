package dimensionalfluxminer.machine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MinerProductionCurveTest {
    private final MinerProductionCurve curve = new MinerProductionCurve(500, 1_500_000, 0.0125, 64);

    @Test
    void exponentMatchesContract() {
        assertEquals(1.0667646277624234, curve.k(), 1e-12);
    }

    @Test
    void endpointsAndClamps() {
        assertEquals(0.0125, curve.rateForPower(500), 1e-12);
        assertEquals(64.0, curve.rateForPower(1_500_000), 1e-9);
        assertEquals(64.0, curve.rateForPower(9_000_000), 1e-9);
        assertEquals(0.0, curve.rateForPower(499.9));
    }

    @Test
    void levelRatesMatchContract() {
        assertEquals(0.032288741625, curve.levelRate(2), 1e-6);
        assertEquals(0.08340502686, curve.levelRate(3), 1e-6);
        assertEquals(64.0, curve.levelRate(10), 1e-9);
    }

    @Test
    void inverseCurveChargesActualWork() {
        for (double power : new double[] {500, 2963, 104004, 1_500_000}) {
            assertEquals(power, curve.powerForRate(curve.rateForPower(power)), power * 1e-9);
        }
        assertEquals(0.0, curve.powerForRate(0));
    }

    @Test
    void configurationCannotRaiseTheHardCap() {
        assertEquals(64.0, new MinerProductionCurve(500, 1_500_000, 0.0125, 500).maxRate());
        assertThrows(IllegalArgumentException.class, () -> new MinerProductionCurve(500, 400, 0.0125, 64));
    }
}
