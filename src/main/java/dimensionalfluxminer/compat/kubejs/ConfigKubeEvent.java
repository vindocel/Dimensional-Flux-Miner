package dimensionalfluxminer.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dimensionalfluxminer.DfmConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code DimensionalFluxMinerEvents.config}: sets options of {@code dimensionalfluxminer-server.toml} by their key, with
 * the same types and limits as the file. A value set here wins over the file while the script sets it, is never written
 * to the file, and is sent to the players.
 */
public class ConfigKubeEvent implements KubeEvent {
    private final Map<String, Object> values = new LinkedHashMap<>();

    /** Sets one option, e.g. {@code event.set('crusher.speedByLevel', [0.01, 0.03, ...])}. */
    public void set(String key, Object value) {
        values.put(key, DfmConfig.checkedOverride(key, value));
    }

    /** Every option key, for example {@code miner.energyPerAttempt}. */
    public List<String> getKeys() {
        return List.copyOf(DfmConfig.keys().keySet());
    }

    Map<String, Object> values() {
        return values;
    }
}
