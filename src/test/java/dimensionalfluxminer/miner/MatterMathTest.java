package dimensionalfluxminer.miner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class MatterMathTest {
    private static int input(int base, boolean upgrade) {
        return MatterMath.inputUnits(base, upgrade, false, OptionalInt.empty());
    }

    @Test
    void upgradeBonusIsTheSmallerOfTheValueAndPointTwoDm() {
        // dirt 0.1, cobblestone 0.3, stone 0.5, obsidian 1.0, nether bricks 1.5, dragon egg 10 (units of 0.1 DM).
        int[][] cases = {{1, 2}, {3, 5}, {5, 7}, {10, 12}, {15, 17}, {100, 102}};
        for (int[] c : cases) {
            assertEquals(c[0], input(c[0], false), "no upgrade keeps the base " + c[0]);
            assertEquals(c[1], input(c[0], true), "upgrade of " + c[0]);
        }
        assertEquals(2, MatterMath.matterInputUnits(1, true), "a value below the bonus is only doubled");
        assertEquals(0, MatterMath.matterInputUnits(0, true), "nothing gives nothing");
    }

    @Test
    void itemsTheMinerCanMakeNeverReturnMoreThanTheAttempt() {
        // ore 0.5, diamond 1, emerald 2, and a datapack ore of 3 DM.
        int[][] cases = {{5, 5, 7}, {10, 10, 12}, {20, 20, 20}, {30, 20, 20}};
        for (int[] c : cases) {
            assertEquals(c[1], MatterMath.inputUnits(c[0], false, true, OptionalInt.empty()), "ore " + c[0]);
            assertEquals(c[2], MatterMath.inputUnits(c[0], true, true, OptionalInt.empty()), "ore " + c[0] + " upgraded");
            assertTrue(MatterMath.inputUnits(c[0], true, true, OptionalInt.empty()) <= MatterMath.ROLL_COST_UNITS);
        }
    }

    @Test
    void upgradedVariantsAreTheirShareOfTheUpgradedBlock() {
        // A stone slab is 0.2 DM (half of 0.5, rounded down); two slabs with the upgrade must not beat one stone (0.7 DM).
        int slab = MatterMath.inputUnits(2, true, false, OptionalInt.of(3));
        assertEquals(3, slab);
        assertTrue(2 * slab <= input(5, true), "cutting never multiplies the bonus");
        assertEquals(4, MatterMath.inputUnits(2, true, false, OptionalInt.of(9)), "never above base + min(base, 2)");
    }

    @Test
    void filterRecoveryKeepsItsOwnRule() {
        assertEquals(10, MatterMath.filterRecoveryUnits(5));
        assertEquals(15, MatterMath.filterRecoveryUnits(10));
        assertEquals(20, MatterMath.filterRecoveryUnits(15));
        assertEquals(20, MatterMath.filterRecoveryUnits(20));
    }

    @Test
    void materialNeededForOneHundredToTenThousandOres() {
        // Every attempt costs 2 DM (20 units); integer units only.
        int[] bases = {1, 3, 5, 10, 15};
        long[][] expected = {
                // ores:      100     1 000    10 000   (items without / with upgrade)
                {2_000, 1_000, 20_000, 10_000, 200_000, 100_000},  // dirt
                {667, 400, 6_667, 4_000, 66_667, 40_000},          // cobblestone
                {400, 286, 4_000, 2_858, 40_000, 28_572},          // stone
                {200, 167, 2_000, 1_667, 20_000, 16_667},          // obsidian
                {134, 118, 1_334, 1_177, 13_334, 11_765},          // nether bricks
        };
        long[] ores = {100, 1_000, 10_000};
        for (int i = 0; i < bases.length; i++) {
            for (int j = 0; j < ores.length; j++) {
                long units = ores[j] * MatterMath.ROLL_COST_UNITS;
                long plain = (units + input(bases[i], false) - 1) / input(bases[i], false);
                long upgraded = (units + input(bases[i], true) - 1) / input(bases[i], true);
                assertEquals(expected[i][2 * j], plain, "base " + bases[i] + ", " + ores[j] + " ores");
                assertEquals(expected[i][2 * j + 1], upgraded, "base " + bases[i] + ", " + ores[j] + " ores, upgrade");
            }
        }
    }
}
