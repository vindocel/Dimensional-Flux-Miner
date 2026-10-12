package dimensionalfluxminer.miner.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Datapack entry of {@code data/<namespace>/dimensional_miner_resources/}.
 * <ul>
 *     <li>{@code resource}: ore item/block id (required);</li>
 *     <li>{@code dimension}: dimension the entry applies to (required to add a resource, optional to disable);</li>
 *     <li>{@code family}: stable family key (defaults to the recognized family of the resource);</li>
 *     <li>{@code weight}: share of the final profile reserved for the family, in (0, 1];</li>
 *     <li>{@code enabled}: false removes the resource (and an emptied family) from the profile;</li>
 *     <li>{@code crusher_output}: canonical Crusher product for the family.</li>
 * </ul>
 */
public record ProfileOverride(ResourceLocation resource, Optional<ResourceKey<Level>> dimension, Optional<ResourceLocation> family,
                              Optional<Double> weight, boolean enabled, Optional<ResourceLocation> crusherOutput) {
    public static final Codec<ProfileOverride> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(ProfileOverride::resource),
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(ProfileOverride::dimension),
            ResourceLocation.CODEC.optionalFieldOf("family").forGetter(ProfileOverride::family),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("weight").forGetter(ProfileOverride::weight),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(ProfileOverride::enabled),
            ResourceLocation.CODEC.optionalFieldOf("crusher_output").forGetter(ProfileOverride::crusherOutput)
    ).apply(i, ProfileOverride::new));

    public boolean appliesTo(ResourceKey<Level> level) {
        return dimension.isEmpty() || dimension.get().equals(level);
    }
}
