package dimensionalfluxminer.miner.profile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Weighted synthesis profile of one dimension, shared by Basic and Stabilized Cores. {@code emptyWeight} is the void:
 * the weight of the attempts that find nothing (see {@link ProfileDimensionRule}).
 * <p>
 * Immutable: the cumulative weights are computed once, so a roll is one {@code nextInt(total)} and a binary search,
 * with exactly the same outcome (and random draws) as walking the families one by one.
 */
public final class ResourceProfile {
    private final ResourceKey<Level> dimension;
    private final List<ResourceFamily> families;
    private final int emptyWeight;
    private final int totalWeight;
    /** {@code cumulative[i]} = weight of families 0..i. */
    private final int[] cumulative;

    public ResourceProfile(ResourceKey<Level> dimension, List<ResourceFamily> families, int emptyWeight) {
        this.dimension = dimension;
        this.families = List.copyOf(families);
        this.emptyWeight = emptyWeight;
        this.cumulative = new int[this.families.size()];
        int sum = 0;
        for (int i = 0; i < this.families.size(); i++) {
            sum += this.families.get(i).weight();
            cumulative[i] = sum;
        }
        this.totalWeight = sum;
    }

    public ResourceProfile(ResourceKey<Level> dimension, List<ResourceFamily> families) {
        this(dimension, families, 0);
    }

    public static ResourceProfile empty(ResourceKey<Level> dimension) {
        return new ResourceProfile(dimension, List.of());
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public List<ResourceFamily> families() {
        return families;
    }

    public int emptyWeight() {
        return emptyWeight;
    }

    public boolean isEmpty() {
        return families.isEmpty();
    }

    /** Weight of the families (the void not included). */
    public int totalWeight() {
        return totalWeight;
    }

    /** Share of the attempts that find nothing (0 without a void). */
    public double emptyShare() {
        int all = totalWeight + Math.max(0, emptyWeight);
        return all > 0 ? Math.max(0, emptyWeight) / (double) all : 0.0;
    }

    /**
     * Weighted roll of one attempt: the index of the family found, or -1 when it falls into the void. Filters never
     * reroll. Allocation free.
     */
    public int rollIndex(RandomSource random) {
        int r = random.nextInt(Math.max(1, totalWeight + Math.max(0, emptyWeight)));
        // First family whose cumulative weight is above r: the same family the one-by-one walk stops at.
        int low = 0;
        int high = cumulative.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (cumulative[mid] > r) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        int index = low;
        if (index < families.size()) {
            return index;
        }
        return families.isEmpty() || emptyWeight > 0 ? -1 : families.size() - 1;
    }

    /** Weighted roll of one attempt: a family, or empty when it falls into the void. */
    public Optional<ResourceFamily> roll(RandomSource random) {
        int index = rollIndex(random);
        return index < 0 ? Optional.empty() : Optional.of(families.get(index));
    }

    public Optional<ResourceFamily> family(Identifier key) {
        return families.stream().filter(f -> f.key().equals(key)).findFirst();
    }

    public Optional<ResourceFamily> familyOf(Item item) {
        return families.stream().filter(f -> f.variants().stream().anyMatch(v -> v.item() == item)).findFirst();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResourceProfile other && other.dimension.equals(dimension) && other.families.equals(families)
                && other.emptyWeight == emptyWeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, families, emptyWeight);
    }

    @Override
    public String toString() {
        return "ResourceProfile[" + dimension.identifier() + ", " + families.size() + " families, void " + emptyWeight + "]";
    }
}
