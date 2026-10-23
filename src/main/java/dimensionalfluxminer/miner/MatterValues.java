package dimensionalfluxminer.miner;

import dimensionalfluxminer.miner.profile.ProfileBuilder;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.registry.DfmDataMaps;
import dimensionalfluxminer.registry.DfmTags;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dimensional Matter values in fixed point (1 DM = 10 units). Values are data driven through the
 * {@code dimensionalfluxminer:matter_value} item data map; any other ore ({@code #c:ores}) is worth 0.5 DM.
 * <p>
 * The Matter Input only takes matter of its own dimension: an ore is accepted only where that exact ore item
 * generates (it is part of the dimension's Resource Profile), and any other item only in the dimensions its data map
 * entry lists (an entry without dimensions, such as obsidian, is accepted everywhere). Blocks made from those items
 * (stairs, slabs, polished...) count too, see {@link MatterVariants}.
 */
public final class MatterValues {
    public static final int UNITS_PER_DM = MatterMath.UNITS_PER_DM;
    public static final int BUFFER_UNITS = 5000 * UNITS_PER_DM;
    public static final int ROLL_COST_UNITS = MatterMath.ROLL_COST_UNITS;
    public static final int COLUMN_UNITS = 64 * UNITS_PER_DM;
    private static final int DEFAULT_ORE_UNITS = 5;

    private MatterValues() {
    }

    /** Base value of an item regardless of dimension (used for filter recovery of generated ores). */
    public static OptionalInt baseUnits(ItemStack stack) {
        DfmDataMaps.MatterValue value = stack.typeHolder().getData(DfmDataMaps.MATTER_VALUE);
        if (value != null) {
            return OptionalInt.of(value.units());
        }
        if (stack.is(DfmTags.Items.common("ores"))) {
            return OptionalInt.of(DEFAULT_ORE_UNITS);
        }
        return OptionalInt.empty();
    }

    /** Base value of a substrate accepted by the Matter Input of a Miner standing in {@code level}. */
    public static OptionalInt substrateUnits(ItemStack stack, ServerLevel level) {
        return substrateUnits(stack, level.dimension(), item -> ResourceProfiles.get(level).familyOf(item).isPresent(),
                item -> MatterVariants.value(dimensionalfluxminer.recipe.Recipes.map(level), level.registryAccess(), level.dimension(), item));
    }

    /**
     * @param generatesHere whether an ore item generates in {@code dimension}
     * @param variants      value of a variant of the substrates of {@code dimension} (empty for any other item)
     */
    public static OptionalInt substrateUnits(ItemStack stack, ResourceKey<Level> dimension, Predicate<Item> generatesHere,
                                             Function<Item, OptionalInt> variants) {
        boolean ore = isOre(stack);
        if (ore && !generatesHere.test(stack.getItem())) {
            return OptionalInt.empty();
        }
        DfmDataMaps.MatterValue value = stack.typeHolder().getData(DfmDataMaps.MATTER_VALUE);
        if (value != null) {
            return ore || value.acceptedIn(dimension) ? OptionalInt.of(value.units()) : OptionalInt.empty();
        }
        if (ore) {
            return OptionalInt.of(DEFAULT_ORE_UNITS);
        }
        return variants.apply(stack.getItem());
    }

    /** Ore block items: {@code #c:ores} members and the built-in vanilla ore families. */
    public static boolean isOre(ItemStack stack) {
        return stack.is(DfmTags.Items.common("ores")) || ProfileBuilder.familyOf(stack.getItem()).isPresent();
    }

    /** Matter Input conversion of a substrate: {@code base + min(base, 2)} units with the Efficiency Upgrade. */
    public static int matterInputUnits(int base, boolean efficiencyUpgrade) {
        return MatterMath.matterInputUnits(base, efficiencyUpgrade);
    }

    /**
     * Units one item gives to the Matter Input of a Miner in {@code level} (empty when it is not accepted there):
     * <ul>
     *     <li>its value plus, with the upgrade, {@code min(value, 2)} units;</li>
     *     <li>a recipe variant with the upgrade: its share of the upgraded substrates (never above that formula);</li>
     *     <li>an item this Miner can produce (an ore of the dimension): at most 2 DM, the cost of the attempt.</li>
     * </ul>
     */
    public static OptionalInt matterInputUnits(ItemStack stack, ServerLevel level, boolean efficiencyUpgrade) {
        OptionalInt base = substrateUnits(stack, level);
        if (base.isEmpty()) {
            return base;
        }
        boolean producible = isOre(stack) || ResourceProfiles.get(level).familyOf(stack.getItem()).isPresent();
        OptionalInt variant = OptionalInt.empty();
        if (efficiencyUpgrade && !producible && stack.typeHolder().getData(DfmDataMaps.MATTER_VALUE) == null) {
            variant = MatterVariants.value(dimensionalfluxminer.recipe.Recipes.map(level), level.registryAccess(), level.dimension(), stack.getItem(), true);
        }
        return OptionalInt.of(MatterMath.inputUnits(base.getAsInt(), efficiencyUpgrade, producible, variant));
    }

    /** Filter recovery of a rejected generated ore: min(base + 0.5 DM, 2 DM). */
    public static int filterRecoveryUnits(int base) {
        return MatterMath.filterRecoveryUnits(base);
    }
}
