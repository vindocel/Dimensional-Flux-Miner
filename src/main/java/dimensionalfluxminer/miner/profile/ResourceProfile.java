package dimensionalfluxminer.miner.profile;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Weighted synthesis profile of one dimension, shared by Basic and Stabilized Cores. */
public record ResourceProfile(ResourceKey<Level> dimension, List<ResourceFamily> families) {
    public static ResourceProfile empty(ResourceKey<Level> dimension) {
        return new ResourceProfile(dimension, List.of());
    }

    public boolean isEmpty() {
        return families.isEmpty();
    }

    public int totalWeight() {
        int total = 0;
        for (ResourceFamily f : families) {
            total += f.weight();
        }
        return total;
    }

    /** Weighted family roll; filters never change these weights and never reroll. */
    public ResourceFamily rollFamily(RandomSource random) {
        int total = totalWeight();
        int r = random.nextInt(Math.max(1, total));
        for (ResourceFamily f : families) {
            r -= f.weight();
            if (r < 0) {
                return f;
            }
        }
        return families.getLast();
    }

    public Optional<ResourceFamily> family(ResourceLocation key) {
        return families.stream().filter(f -> f.key().equals(key)).findFirst();
    }

    public Optional<ResourceFamily> familyOf(Item item) {
        return families.stream().filter(f -> f.variants().stream().anyMatch(v -> v.item() == item)).findFirst();
    }
}
