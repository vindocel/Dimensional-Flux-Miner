package dimensionalfluxminer.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * Server script events, all fired every time the server scripts load (world start and {@code /reload}):
 * <pre>
 * DimensionalFluxMinerEvents.config(event =&gt; event.set('miner.energyPerAttempt', 100000))
 * DimensionalFluxMinerEvents.matterValues(event =&gt; event.set('minecraft:cobblestone', 0.4, ['minecraft:overworld']))
 * DimensionalFluxMinerEvents.resourceProfiles(event =&gt; event.add({resource: 'minecraft:diamond_ore', dimension: 'minecraft:overworld', weight: 0.03}))
 * </pre>
 */
public interface DfmKubeEvents {
    EventGroup GROUP = EventGroup.of("DimensionalFluxMinerEvents");

    EventHandler CONFIG = GROUP.server("config", () -> ConfigKubeEvent.class);
    EventHandler MATTER_VALUES = GROUP.server("matterValues", () -> MatterValuesKubeEvent.class);
    EventHandler RESOURCE_PROFILES = GROUP.server("resourceProfiles", () -> ResourceProfilesKubeEvent.class);
}
