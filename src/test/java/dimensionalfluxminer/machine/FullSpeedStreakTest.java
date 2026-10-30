package dimensionalfluxminer.machine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FullSpeedStreakTest {
    /** Records every game tick from {@code from} (inclusive) to {@code to} (exclusive); true if the goal was reached. */
    private static boolean run(FullSpeedStreak streak, long from, long to, boolean fullSpeed) {
        boolean reached = false;
        for (long t = from; t < to; t++) {
            reached |= streak.record(t, fullSpeed);
        }
        return reached;
    }

    @Test
    void tenSecondsInARowAreNeeded() {
        FullSpeedStreak streak = new FullSpeedStreak(FullSpeedStreak.REQUIRED_TICKS);
        assertFalse(run(streak, 0, 199, true), "one tick short");
        assertTrue(run(streak, 199, 200, true), "the 200th tick completes the ten seconds");
    }

    @Test
    void reportedOncePerStreak() {
        FullSpeedStreak streak = new FullSpeedStreak(FullSpeedStreak.REQUIRED_TICKS);
        assertTrue(run(streak, 0, 200, true));
        assertFalse(run(streak, 200, 1000, true), "the same streak is not reported again");
    }

    @Test
    void aTickBelowFullProductionRestartsTheCount() {
        FullSpeedStreak streak = new FullSpeedStreak(FullSpeedStreak.REQUIRED_TICKS);
        run(streak, 0, 150, true);
        run(streak, 150, 151, false);
        assertFalse(run(streak, 151, 350, true), "the energy shortage reset the streak");
        assertTrue(run(streak, 350, 351, true));
    }

    @Test
    void aGapRestartsTheCount() {
        FullSpeedStreak streak = new FullSpeedStreak(FullSpeedStreak.REQUIRED_TICKS);
        run(streak, 0, 150, true);
        // Ticks 150..159 were never recorded (the machine was not ticking).
        assertFalse(run(streak, 160, 359, true), "the gap reset the streak");
        assertTrue(run(streak, 359, 360, true));
    }
}
