package dimensionalfluxminer.compat.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.event.KubeEvent;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * {@code DimensionalFluxMinerEvents.matterValues}: changes the {@code dimensionalfluxminer:matter_value} data map. The
 * result is a data map file above every datapack, so it merges with the mod's and the datapacks' values exactly like a
 * datapack of the highest priority, and reaches the clients the same way.
 */
public class MatterValuesKubeEvent implements KubeEvent {
    /** Same limits as the data map. */
    private static final double MAX_DM = 5000.0;

    private final Map<String, JsonObject> values = new LinkedHashMap<>();
    private final Set<String> removals = new LinkedHashSet<>();
    private boolean replace;

    /** Value of an item ({@code 'minecraft:cobblestone'}) or an item tag ({@code '#c:ores/tin'}), accepted in every dimension. */
    public void set(String item, double dm) {
        set(item, dm, List.of());
    }

    /** Value of an item or tag, accepted only in {@code dimensions} (an empty list means every dimension). */
    public void set(String item, double dm, List<String> dimensions) {
        String key = checkedKey(item);
        if (!(dm >= 0 && dm <= MAX_DM)) {
            throw new IllegalArgumentException("Invalid dm " + dm + " for " + item + " (allowed: 0 to " + MAX_DM + ")");
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("dm", dm);
        if (!dimensions.isEmpty()) {
            JsonArray dims = new JsonArray();
            for (String dimension : dimensions) {
                if (Identifier.tryParse(dimension) == null) {
                    throw new IllegalArgumentException("Invalid dimension id '" + dimension + "'");
                }
                dims.add(dimension);
            }
            entry.add("dimensions", dims);
        }
        removals.remove(key);
        values.put(key, entry);
    }

    /** Removes the value of an item or tag, so it is no longer accepted as matter (unless it is a variant or an ore). */
    public void remove(String item) {
        String key = checkedKey(item);
        values.remove(key);
        removals.add(key);
    }

    /** Drops every value of the mod and of the datapacks first; only the values set by scripts remain. */
    public void replaceAll() {
        replace = true;
    }

    private static String checkedKey(String item) {
        boolean tag = item.startsWith("#");
        Identifier id = Identifier.tryParse(tag ? item.substring(1) : item);
        if (id == null) {
            throw new IllegalArgumentException("Invalid item id '" + item + "'");
        }
        if (!tag && !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Unknown item '" + item + "'");
        }
        return (tag ? "#" : "") + id;
    }

    boolean isEmpty() {
        return values.isEmpty() && removals.isEmpty() && !replace;
    }

    JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("replace", replace);
        JsonObject map = new JsonObject();
        values.forEach(map::add);
        root.add("values", map);
        if (!removals.isEmpty()) {
            JsonArray remove = new JsonArray();
            removals.forEach(remove::add);
            root.add("remove", remove);
        }
        return root;
    }
}
