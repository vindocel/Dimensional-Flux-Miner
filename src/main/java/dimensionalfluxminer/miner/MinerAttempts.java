package dimensionalfluxminer.miner;

import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The mining attempts of one cycle. Each attempt pays 2 DM (topping the buffer up from {@code refill} first when it
 * runs short), rolls a family and a variant, and then:
 * <ul>
 *     <li>falls into the void (sparse dimensions): nothing found, the DM is spent;</li>
 *     <li>a kept result goes to the output; when it does not fit, the attempt is lost but its DM is kept, and it still
 *     takes its time and FE like any attempt, so a nearly full output never becomes fast production of one ore;</li>
 *     <li>a filtered result is discarded; with the Efficiency Upgrade it returns {@code min(base + 0.5, 2)} DM.</li>
 * </ul>
 * The caller charges FE for the {@link Result#executed} attempts and marks the machine changed.
 */
final class MinerAttempts {
    /**
     * @param executed     attempts performed (lost and filtered ones included)
     * @param missed       a kept result found no room
     * @param producedOre  at least one result entered the output
     * @param filteredOre  at least one result was discarded by the filter
     * @param matterRanOut the attempts stopped because no matter was left
     */
    record Result(long executed, boolean missed, boolean producedOre, boolean filteredOre, boolean matterRanOut) {
    }

    private MinerAttempts() {
    }

    static Result run(long attempts, ResourceProfile profile, RandomSource random, Set<ResourceLocation> filter, boolean upgrade,
                      MatterBuffer matter, BooleanSupplier refill, MinerOutput output) {
        long executed = 0;
        boolean missed = false;
        boolean producedOre = false;
        boolean filteredOre = false;
        boolean ranOut = false;
        List<ResourceFamily> families = profile.families();
        output.beginBatch();
        try {
            for (long i = 0; i < attempts; i++) {
                if (!matter.canPayAttempt()) {
                    refill.getAsBoolean();
                    if (!matter.canPayAttempt()) {
                        ranOut = true;
                        break;
                    }
                }
                int rolled = profile.rollIndex(random);
                if (rolled < 0) {
                    matter.payAttempt();
                    executed++;
                    continue;
                }
                ResourceFamily family = families.get(rolled);
                Item variant = family.pickVariant(random);
                if (filter.isEmpty() || filter.contains(family.key())) {
                    ItemStack result = new ItemStack(variant);
                    int slot = output.slotFor(result);
                    if (slot < 0) {
                        missed = true;
                        executed++;
                        continue;
                    }
                    matter.payAttempt();
                    output.insert(slot, result);
                    producedOre = true;
                } else {
                    matter.payAttempt();
                    filteredOre = true;
                    if (upgrade) {
                        OptionalInt base = MatterValues.baseUnits(new ItemStack(variant));
                        matter.addCapped(MatterValues.filterRecoveryUnits(base.orElse(5)));
                    }
                }
                executed++;
            }
        } finally {
            output.endBatch();
        }
        return new Result(executed, missed, producedOre, filteredOre, ranOut);
    }
}
