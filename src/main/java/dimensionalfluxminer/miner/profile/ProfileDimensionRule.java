package dimensionalfluxminer.miner.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Datapack entry of {@code data/<namespace>/dimensional_miner_resources/} that sets the void of a dimension: when the
 * dimension's worldgen yields fewer than {@code min_families} ore families, part of the attempts find nothing (they still
 * cost their energy and Dimensional Matter) and the families share the rest. The void is {@code empty_share} with a
 * single family and shrinks along {@code curve} to none at {@code min_families}.
 * <ul>
 *     <li>{@code dimension}: the dimension (required);</li>
 *     <li>{@code empty_share}: share of the attempts that find nothing with one family, in [0, 1) (required);</li>
 *     <li>{@code min_families}: number of families from which the void no longer applies (default 5);</li>
 *     <li>{@code curve}: {@code geometric} (default), {@code linear} or {@code step}, see {@link VoidCurve}.</li>
 * </ul>
 */
public record ProfileDimensionRule(ResourceKey<Level> dimension, double emptyShare, int minFamilies, VoidCurve curve) {
    public static final int DEFAULT_MIN_FAMILIES = 5;

    private static final Codec<VoidCurve> CURVE_CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(VoidCurve.byName(name));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }, VoidCurve::serializedName);

    public static final Codec<ProfileDimensionRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ProfileDimensionRule::dimension),
            Codec.doubleRange(0.0, 0.9999).fieldOf("empty_share").forGetter(ProfileDimensionRule::emptyShare),
            Codec.intRange(1, 1000).optionalFieldOf("min_families", DEFAULT_MIN_FAMILIES).forGetter(ProfileDimensionRule::minFamilies),
            CURVE_CODEC.optionalFieldOf("curve", VoidCurve.GEOMETRIC).forGetter(ProfileDimensionRule::curve)
    ).apply(i, ProfileDimensionRule::new));

    /** Share of the attempts that find nothing in a profile of {@code families} families (0 when the void is off). */
    public double voidShare(int families) {
        return curve.voidShare(emptyShare, minFamilies, families);
    }
}
