package dimensionalfluxminer.miner.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.crusher.CrusherProcessing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Per-dimension Resource Profile cache, rebuilt on demand after every datapack reload. */
public final class ResourceProfiles {
    private static final Map<ResourceKey<Level>, ResourceProfile> CACHE = new ConcurrentHashMap<>();
    private static volatile List<ProfileOverride> overrides = List.of();
    private static volatile List<ProfileDimensionRule> rules = List.of();
    /** Incremented on every rebuild so machines can prune filter selections. */
    private static volatile int generation;

    private ResourceProfiles() {
    }

    public static ResourceProfile get(ServerLevel level) {
        return CACHE.computeIfAbsent(level.dimension(), key -> ProfileBuilder.build(level, overrides, rules));
    }

    public static int generation() {
        return generation;
    }

    public static void invalidate() {
        CACHE.clear();
        generation++;
    }

    public static List<ProfileOverride> overrides() {
        return overrides;
    }

    public static List<ProfileDimensionRule> rules() {
        return rules;
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        invalidate();
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(new com.google.gson.Gson(), "dimensional_miner_resources");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            List<ProfileOverride> parsed = new ArrayList<>();
            List<ProfileDimensionRule> parsedRules = new ArrayList<>();
            files.forEach((id, json) -> {
                List<JsonElement> entries = new ArrayList<>();
                if (json instanceof JsonArray array) {
                    array.forEach(entries::add);
                } else if (json instanceof JsonObject obj && obj.has("entries") && obj.get("entries").isJsonArray()) {
                    obj.getAsJsonArray("entries").forEach(entries::add);
                } else {
                    entries.add(json);
                }
                for (JsonElement entry : entries) {
                    // An entry with "empty_share" sets the void of a dimension; any other entry overrides a resource.
                    if (entry instanceof JsonObject obj && obj.has("empty_share")) {
                        ProfileDimensionRule.CODEC.parse(JsonOps.INSTANCE, entry)
                                .resultOrPartial(err -> DimensionalFluxMiner.LOGGER.warn("Invalid dimensional miner rule {}: {}", id, err))
                                .ifPresent(parsedRules::add);
                        continue;
                    }
                    ProfileOverride.CODEC.parse(JsonOps.INSTANCE, entry)
                            .resultOrPartial(err -> DimensionalFluxMiner.LOGGER.warn("Invalid dimensional miner resource {}: {}", id, err))
                            .ifPresent(parsed::add);
                }
            });
            overrides = List.copyOf(parsed);
            rules = List.copyOf(parsedRules);
            Map<Item, Item> crusherOverrides = new HashMap<>();
            for (ProfileOverride o : parsed) {
                o.crusherOutput().ifPresent(out -> {
                    Item ore = BuiltInRegistries.ITEM.get(o.resource());
                    Item product = BuiltInRegistries.ITEM.get(out);
                    if (ore != Items.AIR && product != Items.AIR) {
                        crusherOverrides.put(ore, product);
                    }
                });
            }
            CrusherProcessing.setProfileOverrides(crusherOverrides);
            invalidate();
        }
    }
}
