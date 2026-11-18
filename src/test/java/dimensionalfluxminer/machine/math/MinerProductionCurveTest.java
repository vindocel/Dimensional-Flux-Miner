package dimensionalfluxminer.machine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MinerProductionCurveTest {
    private final MinerProductionCurve curve = new MinerProductionCurve(0.0125, 64, 24_000);

    @Test
    void levelPowersAreAttemptsTimesTheFixedCost() {
        // FE/t = attempts per tick x 24 000: no discount and no penalty at any level.
        long[] expected = {300, 775, 2_002, 5_171, 13_356, 34_501, 89_119, 230_202, 594_635, 1_536_000};
        for (int level = 1; level <= 10; level++) {
            assertEquals(expected[level - 1], RateScale.powerLimit(curve.levelPower(level)), "level " + level);
            assertEquals(24_000.0, curve.levelPower(level) / curve.levelRate(level), 1e-6, "FE per attempt at level " + level);
        }
    }

    @Test
    void levelRatesKeepTheGeometricScale() {
        assertEquals(0.0125, curve.levelRate(1), 1e-12);
        assertEquals(0.032288741625, curve.levelRate(2), 1e-6);
        assertEquals(0.08340502686, curve.levelRate(3), 1e-6);
        assertEquals(64.0, curve.levelRate(10), 1e-9);
        double step = curve.levelRate(2) / curve.levelRate(1);
        for (int level = 2; level <= 10; level++) {
            assertEquals(step, curve.levelRate(level) / curve.levelRate(level - 1), 1e-9, "level " + level);
        }
    }

    @Test
    void chargesFollowTheWorkReallyPerformed() {
        for (double rate : new double[] {0.0001, 0.0125, 0.5, 3.7, 64}) {
            assertEquals(rate * 24_000, curve.powerForRate(10, rate), rate * 1e-6);
            assertEquals(rate, curve.rateForPower(10, curve.powerForRate(10, rate)), rate * 1e-12);
        }
        assertEquals(0.0, curve.powerForRate(10, 0));
        assertEquals(0.0, curve.rateForPower(10, 0));
        // A part of the FE pays the same part of the attempts (no minimum power below which nothing works).
        assertEquals(0.00625, curve.rateForPower(1, 150), 1e-12);
        assertEquals(64.0, curve.rateForPower(10, 50_000_000), 1e-9, "never above the level's production");
        assertEquals(0.0125, curve.rateForPower(1, 50_000_000), 1e-12);
    }

    @Test
    void configurationCannotRaiseTheHardCap() {
        assertEquals(64.0, new MinerProductionCurve(0.0125, 500, 24_000).levelRate(10));
        assertThrows(IllegalArgumentException.class, () -> new MinerProductionCurve(0.5, 0.4, 24_000));
        assertThrows(IllegalArgumentException.class, () -> new MinerProductionCurve(0.0125, 64, 0));
    }

    private static LevelTable table(double... values) {
        return new LevelTable(values);
    }

    @Test
    void aSpeedTableKeepsTheCostPerAttempt() {
        LevelTable speed = table(0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 30, 100);
        MinerProductionCurve custom = new MinerProductionCurve(speed, null, 24_000);
        assertEquals(0.5, custom.levelRate(4), 1e-12);
        assertEquals(12_000, custom.levelPower(4), 1e-6);
        assertEquals(64.0, custom.levelRate(10), 1e-12, "a table is capped at 64 attempts per cycle too");
        for (int level = 1; level <= 10; level++) {
            assertEquals(24_000, custom.energyPerAttempt(level), 1e-6);
        }
    }

    @Test
    void aPowerTableSetsTheCostOfEachLevel() {
        LevelTable power = table(1_000, 2_000, 4_000, 8_000, 16_000, 32_000, 64_000, 128_000, 256_000, 1_000_000);
        MinerProductionCurve custom = new MinerProductionCurve(LevelTable.geometric(0.0125, 64), power, 24_000);
        for (int level = 1; level <= 10; level++) {
            assertEquals(power.at(level), custom.levelPower(level), 1e-6, "FE/t of level " + level);
            assertEquals(power.at(level) / custom.levelRate(level), custom.energyPerAttempt(level), 1e-9);
        }
        assertEquals(0.0125, custom.levelRate(1), 1e-12, "production unchanged");
        assertEquals(1_000, custom.minPower(), 1e-9);
        // Half the FE of a level pays half of its attempts, at that level's cost.
        assertEquals(32.0, custom.rateForPower(10, 500_000), 1e-9);
        assertEquals(500_000, custom.powerForRate(10, 32.0), 1e-6);
    }

    @Test
    void aLevelTableNeedsTenPositiveValues() {
        assertThrows(IllegalArgumentException.class, () -> table(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> table(1, 2, 3, 4, 5, 6, 7, 8, 9, 0));
        assertEquals(LevelTable.geometric(0.0125, 64).at(10), 64.0, 1e-12);
    }
}
