package dimensionalfluxminer.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.data.GeneratedDataStage;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.network.ConfigOverridesPayload;

/**
 * Optional KubeJS integration, listed in {@code kubejs.plugins.txt}: only KubeJS loads this class, so the mod never
 * needs it. Recipe types are exposed by the schemas in {@code data/dimensionalfluxminer/kubejs/recipe_schema/}, and the
 * mod's tags already work with {@code ServerEvents.tags}.
 */
public final class DfmKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(DfmKubeEvents.GROUP);
    }

    /**
     * Runs right after the server scripts load, before the datapacks are read, so the script values take part in the same
     * reload: configuration values and Resource Profile entries are kept in memory, Dimensional Matter values go to the
     * last generated data pack (above every other pack). Scripts without these events clear what an earlier load set.
     */
    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (manager.scriptType != ScriptType.SERVER || !(manager instanceof ServerScriptManager server)) {
            return;
        }
        ConfigKubeEvent config = new ConfigKubeEvent();
        DfmKubeEvents.CONFIG.post(ScriptType.SERVER, config);
        DfmConfig.setOverrides(config.values());
        ConfigOverridesPayload.sendToAll();

        ResourceProfilesKubeEvent profiles = new ResourceProfilesKubeEvent();
        DfmKubeEvents.RESOURCE_PROFILES.post(ScriptType.SERVER, profiles);
        ResourceProfiles.setScripted(profiles.entries(), profiles.voidRules());

        MatterValuesKubeEvent matter = new MatterValuesKubeEvent();
        DfmKubeEvents.MATTER_VALUES.post(ScriptType.SERVER, matter);
        if (!matter.isEmpty()) {
            server.virtualPacks.get(GeneratedDataStage.LAST).json(DimensionalFluxMiner.id("data_maps/item/matter_value"), matter.toJson());
        }
    }
}
