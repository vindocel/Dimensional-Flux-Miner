package dimensionalfluxminer.machine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThroughputStreakTest {
    /** Records {@code perTick} operations on every game tick from {@code from} (inclusive) to {@code to} (exclusive). */
    private static boolean run(ThroughputStreak streak, long from, long to, double perTick) {
        boolean reached = false;
        for (long t = from; t < to; t++) {
            reached = streak.record(t, perTick);
        }
        return reached;
    }

    @Test
    void fiveFullSecondsAboveTheThresholdAreNeeded() {
        ThroughputStreak streak = new ThroughputStreak(1000, 5);
        // 64 per tick = 1 280 per second; seconds 0..4 complete when second 5 starts.
        assertFalse(run(streak, 0, 100, 64), "four seconds and a bit are not enough");
        assertTrue(run(streak, 100, 101, 64), "the first tick of the sixth second closes the fifth full second");
    }

    @Test
    void aSlowSecondRestartsTheCount() {
        ThroughputStreak streak = new ThroughputStreak(1000, 5);
        run(streak, 0, 80, 64);
        run(streak, 80, 100, 10);
        assertFalse(run(streak, 100, 180, 64), "the slow second reset the streak");
        assertTrue(run(streak, 180, 201, 64));
    }

    @Test
    void aPauseRestartsTheCount() {
        ThroughputStreak streak = new ThroughputStreak(1000, 5);
        run(streak, 0, 80, 64);
        // Nothing recorded during seconds 4 and 5 (the machine stopped).
        assertFalse(run(streak, 120, 200, 64), "the pause reset the streak");
        assertTrue(run(streak, 200, 221, 64));
    }

    @Test
    void belowTheThresholdNeverCounts() {
        ThroughputStreak streak = new ThroughputStreak(1000, 5);
        assertFalse(run(streak, 0, 400, 49), "49 per tick = 980 per second");
    }
}
