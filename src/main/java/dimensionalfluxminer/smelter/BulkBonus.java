package dimensionalfluxminer.smelter;

/**
 * Raw -> Ingot yield of the Smelter NORMAL mode.
 * <pre>
 * without upgrade: floor(N/2)*3 + (N mod 2)   (each pair of Raw -> 3 Ingots, a single Raw -> 1 Ingot)
 * with upgrade:    2*N
 * </pre>
 */
public final class BulkBonus {
    private BulkBonus() {
    }

    public static long ingots(long raws, boolean efficiencyUpgrade) {
        if (raws <= 0) {
            return 0;
        }
        return efficiencyUpgrade ? 2 * raws : (raws / 2) * 3 + (raws % 2);
    }
}
