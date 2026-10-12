package dimensionalfluxminer.smelter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmelterMathTest {
    @Test
    void bulkBonusWithoutUpgrade() {
        long[] expected = {0, 1, 3, 4, 6, 7};
        for (int n = 0; n <= 5; n++) {
            assertEquals(expected[n], BulkBonus.ingots(n, false), "N=" + n);
        }
        assertEquals(288, BulkBonus.ingots(192, false));
        assertEquals(864, 3 * BulkBonus.ingots(192, false));
    }

    @Test
    void bulkBonusWithUpgradeReplacesPairs() {
        assertEquals(2, BulkBonus.ingots(1, true));
        assertEquals(4, BulkBonus.ingots(2, true));
        assertEquals(384, BulkBonus.ingots(192, true));
    }

    @Test
    void autoSplitBalancesEvenly() {
        long[] add = AutoSplit.distribute(new long[] {0, 0, 0}, new long[] {192, 192, 192}, 96, false);
        assertArrayEquals(new long[] {32, 32, 32}, add);
    }

    @Test
    void autoSplitPreservesRawPairs() {
        // Odd lanes are completed first, then pairs; a single leftover comes last.
        long[] add = AutoSplit.distribute(new long[] {3, 0, 0}, new long[] {189, 192, 192}, 9, true);
        assertEquals(9, add[0] + add[1] + add[2]);
        assertEquals(0, (3 + add[0]) % 2, "odd lane completed first");
        assertEquals(0, add[1] % 2, "pairs only");
        assertEquals(0, add[2] % 2, "pairs only");
        long[] odd = AutoSplit.distribute(new long[] {0, 0, 0}, new long[] {192, 192, 192}, 5, true);
        long oddLanes = java.util.Arrays.stream(odd).filter(v -> v % 2 == 1).count();
        assertEquals(1, oddLanes);
    }

    @Test
    void experienceUsesWholeMillibucketsOnly() {
        StoredExperience xp = new StoredExperience();
        xp.add(1.234);
        assertEquals(1234, xp.milli());
        assertEquals(24, xp.availableMillibuckets());
        assertEquals(24, xp.drain(1000, false));
        assertEquals(34, xp.milli());
        assertEquals(0, xp.availableMillibuckets());
    }

    @Test
    void payoutRoundsFractionProbabilistically() {
        assertEquals(2, StoredExperience.payout(2500, 0.6));
        assertEquals(3, StoredExperience.payout(2500, 0.4));
        assertEquals(1, StoredExperience.payout(1000, 0.0));
    }
}
