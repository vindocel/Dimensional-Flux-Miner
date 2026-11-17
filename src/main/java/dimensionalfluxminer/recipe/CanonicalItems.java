package dimensionalfluxminer.recipe;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Picks a deterministic representative of an item tag: vanilla first, then this mod, then the remaining
 * entries in registry-id order.
 */
public final class CanonicalItems {
    //? if >=26.1 {
    private static final Comparator<Identifier> ORDER = Comparator
            .comparingInt((Identifier id) -> switch (id.getNamespace()) {
    //?} else {
    /*private static final Comparator<ResourceLocation> ORDER = Comparator
            .comparingInt((ResourceLocation id) -> switch (id.getNamespace()) {
    *///?}
                case "minecraft" -> 0;
                case "dimensionalfluxminer" -> 1;
                default -> 2;
            })
            //? if >=26.1 {
            .thenComparing(Identifier::toString);
            //?} else {
            /*.thenComparing(ResourceLocation::toString);
            *///?}

    private CanonicalItems() {
    }

    public static Optional<Item> first(TagKey<Item> tag) {
        //? if >=26.1 {
        return BuiltInRegistries.ITEM.get(tag).stream()
        //?} else {
        /*return BuiltInRegistries.ITEM.getTag(tag).stream()
        *///?}
                .flatMap(set -> set.stream())
                .map(Holder::value)
                .min(Comparator.comparing(BuiltInRegistries.ITEM::getKey, ORDER));
    }
}
