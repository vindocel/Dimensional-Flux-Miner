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
    private static volatile List<ProfileOverride> dataOverrides = List.of();
    private static volatile List<ProfileDimensionRule> dataRules = List.of();
    /** Entries added by server scripts; they come after the datapack ones, so they win any conflict. */
    private static volatile List<ProfileOverride> scriptOverrides = List.of();
    private static volatile List<ProfileDimensionRule> scriptRules = List.of();
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

    /**
     * Replaces the entries set by server scripts. Resource entries are applied after the datapack ones (the last entry
     * of an ore wins), and a script void rule of a dimension replaces the datapack rule of that dimension.
     */
    public static void setScripted(List<ProfileOverride> entries, List<ProfileDimensionRule> voidRules) {
        scriptOverrides = List.copyOf(entries);
        scriptRules = List.copyOf(voidRules);
        combine();
    }

    /**
     * Reads one entry in the datapack format: a void rule when it has {@code empty_share}, a resource entry otherwise.
     * {@code onError} receives the reason of an invalid entry (a partially valid one is still kept, as in a datapack).
     */
    public static void parseEntry(JsonElement entry, List<ProfileOverride> entries, List<ProfileDimensionRule> voidRules,
                                  java.util.function.Consumer<String> onError) {
        if (isRule(entry)) {
            ProfileDimensionRule.CODEC.parse(JsonOps.INSTANCE, entry).resultOrPartial(onError).ifPresent(voidRules::add);
        } else {
            ProfileOverride.CODEC.parse(JsonOps.INSTANCE, entry).resultOrPartial(onError).ifPresent(entries::add);
        }
    }

    private static boolean isRule(JsonElement entry) {
        return entry instanceof JsonObject obj && obj.has("empty_share");
    }

    private static void combine() {
        List<ProfileOverride> combined = new ArrayList<>(dataOverrides);
        combined.addAll(scriptOverrides);
        List<ProfileDimensionRule> combinedRules = new ArrayList<>(scriptRules);
        combinedRules.addAll(dataRules);
        overrides = List.copyOf(combined);
        rules = List.copyOf(combinedRules);
        Map<Item, Item> crusherOverrides = new HashMap<>();
        for (ProfileOverride o : combined) {
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
            var conditionalOps = new net.neoforged.neoforge.common.conditions.ConditionalOps<>(
                    net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE, net.minecraft.core.RegistryAccess.EMPTY),
                    net.neoforged.neoforge.common.conditions.ICondition.IContext.EMPTY);
            files.forEach((id, json) -> {
                // A file with "neoforge:conditions" (for example "neoforge:mod_loaded") only applies when they hold.
                if (json instanceof JsonObject root && root.has("neoforge:conditions")
                        && !net.neoforged.neoforge.common.conditions.ICondition.conditionsMatched(conditionalOps, json)) {
                    return;
                }
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
                    parseEntry(entry, parsed, parsedRules, err -> DimensionalFluxMiner.LOGGER.warn("Invalid dimensional miner {} {}: {}",
                            isRule(entry) ? "rule" : "resource", id, err));
                }
            });
            dataOverrides = List.copyOf(parsed);
            dataRules = List.copyOf(parsedRules);
            combine();
        }
    }
}
