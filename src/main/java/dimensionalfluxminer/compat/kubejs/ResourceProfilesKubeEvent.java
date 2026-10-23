package dimensionalfluxminer.compat.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dimensionalfluxminer.miner.profile.ProfileDimensionRule;
import dimensionalfluxminer.miner.profile.ProfileOverride;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * {@code DimensionalFluxMinerEvents.resourceProfiles}: entries of the Resource Profiles, in the same format as the
 * datapack files of {@code dimensional_miner_resources}. They are applied after the datapack entries, so they win any
 * conflict, and a void rule set here replaces the datapack rule of its dimension.
 */
public class ResourceProfilesKubeEvent implements KubeEvent {
    private final List<ProfileOverride> entries = new ArrayList<>();
    private final List<ProfileDimensionRule> voidRules = new ArrayList<>();

    /**
     * One entry, exactly as in a datapack file: {@code {resource, dimension, family, weight, enabled, crusher_output}},
     * or a void rule {@code {dimension, empty_share, min_families, curve}}.
     */
    public void add(JsonObject entry) {
        List<ProfileOverride> parsed = new ArrayList<>();
        ResourceProfiles.parseEntry(entry, parsed, voidRules, err -> {
            throw new IllegalArgumentException("Invalid Resource Profile entry " + entry + ": " + err);
        });
        for (ProfileOverride o : parsed) {
            checkItem(o.resource());
            o.crusherOutput().ifPresent(ResourceProfilesKubeEvent::checkItem);
            entries.add(o);
        }
    }

    /** Removes an ore item from every dimension's profile. */
    public void remove(String resource) {
        JsonObject entry = new JsonObject();
        entry.addProperty("resource", resource);
        entry.addProperty("enabled", false);
        add(entry);
    }

    /** Removes an ore item from one dimension's profile. */
    public void remove(String resource, String dimension) {
        JsonObject entry = new JsonObject();
        entry.addProperty("resource", resource);
        entry.addProperty("dimension", dimension);
        entry.addProperty("enabled", false);
        add(entry);
    }

    private static void checkItem(Identifier id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Unknown item '" + id + "'");
        }
    }

    List<ProfileOverride> entries() {
        return entries;
    }

    List<ProfileDimensionRule> voidRules() {
        return voidRules;
    }
}
