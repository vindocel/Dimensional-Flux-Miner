package dimensionalfluxminer.miner.profile;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

/**
 * One resource family of a dimension (for example Iron = Iron Ore + Deepslate Iron Ore), with its integer
 * weight in the profile and the canonical Crusher product of the family.
 */
public record ResourceFamily(ResourceLocation key, List<Variant> variants, int weight, Item crusherOutput) {
    public record Variant(Item item, double weight) {
    }

    public Item icon() {
        return variants.getFirst().item();
    }

    public Item pickVariant(RandomSource random) {
        double total = 0;
        for (Variant v : variants) {
            total += v.weight();
        }
        double r = random.nextDouble() * total;
        for (Variant v : variants) {
            r -= v.weight();
            if (r < 0) {
                return v.item();
            }
        }
        return variants.getLast().item();
    }
}
