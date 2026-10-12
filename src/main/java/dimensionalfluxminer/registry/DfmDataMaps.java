package dimensionalfluxminer.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class DfmDataMaps {
    /**
     * Base Dimensional Matter value of an item, used by the Stabilized Matter Input and by the Efficiency
     * Upgrade filter recovery. {@code dimensions} restricts where the item is accepted as substrate (empty = any).
     */
    public static final DataMapType<Item, MatterValue> MATTER_VALUE = DataMapType
            .builder(DimensionalFluxMiner.id("matter_value"), Registries.ITEM, MatterValue.CODEC)
            .synced(MatterValue.CODEC, false)
            .build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(MATTER_VALUE);
    }

    /** @param units Dimensional Matter in fixed point (1 DM = 10 units). */
    public record MatterValue(int units, List<ResourceKey<Level>> dimensions) {
        public static final Codec<MatterValue> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.0, 5000.0).fieldOf("dm").forGetter(v -> v.units / 10.0),
                ResourceKey.codec(Registries.DIMENSION).listOf().optionalFieldOf("dimensions", List.of()).forGetter(MatterValue::dimensions)
        ).apply(i, (dm, dims) -> new MatterValue((int) Math.round(dm * 10.0), dims)));

        public boolean acceptedIn(ResourceKey<Level> dimension) {
            return dimensions.isEmpty() || dimensions.contains(dimension);
        }
    }

    private DfmDataMaps() {
    }
}
