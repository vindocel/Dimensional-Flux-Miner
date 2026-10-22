package dimensionalfluxminer;

import com.mojang.logging.LogUtils;
import dimensionalfluxminer.content.ContentEvents;
import dimensionalfluxminer.miner.corruption.BlightSpawnManager;
import dimensionalfluxminer.miner.corruption.CorruptedWaterEffects;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.miner.survey.SurveyManager;
import dimensionalfluxminer.network.DfmNetwork;
import dimensionalfluxminer.registry.DfmBlockEntities;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmMenus;
import dimensionalfluxminer.registry.DfmCreativeTab;
import dimensionalfluxminer.registry.DfmDataComponents;
import dimensionalfluxminer.registry.DfmDataMaps;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmParticles;
import dimensionalfluxminer.registry.DfmRecipes;
import dimensionalfluxminer.registry.DfmSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(DimensionalFluxMiner.MOD_ID)
public final class DimensionalFluxMiner {
    public static final String MOD_ID = "dimensionalfluxminer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DimensionalFluxMiner(IEventBus modBus, ModContainer container) {
        DfmBlocks.BLOCKS.register(modBus);
        DfmItems.ITEMS.register(modBus);
        DfmDataComponents.COMPONENTS.register(modBus);
        DfmFluids.FLUID_TYPES.register(modBus);
        DfmFluids.FLUIDS.register(modBus);
        DfmSounds.SOUNDS.register(modBus);
        DfmParticles.PARTICLES.register(modBus);
        dimensionalfluxminer.advancement.DfmAdvancements.TRIGGERS.register(modBus);
        DfmRecipes.TYPES.register(modBus);
        DfmRecipes.SERIALIZERS.register(modBus);
        DfmCreativeTab.TABS.register(modBus);
        DfmBlockEntities.BLOCK_ENTITIES.register(modBus);
        DfmMenus.MENUS.register(modBus);
        modBus.addListener(DfmDataMaps::register);
        modBus.addListener(DfmBlockEntities::registerCapabilities);
        modBus.addListener(DfmNetwork::register);

        container.registerConfig(ModConfig.Type.SERVER, DfmConfig.SPEC);
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Loading.class, DfmConfig::onConfigChanged);
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Reloading.class, DfmConfig::onConfigChanged);
        modBus.addListener(net.neoforged.fml.event.config.ModConfigEvent.Unloading.class, DfmConfig::onConfigChanged);
        dimensionalfluxminer.compat.claims.ClaimAdapters.registerInstalled();
        modBus.addListener(dimensionalfluxminer.compat.DarkModeCompat::onInterModEnqueue);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(dimensionalfluxminer.compat.CognitionCompat::allowLiquidExperience));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent event) ->
                dimensionalfluxminer.compat.CognitionCompat.allowLiquidExperience());
        if (net.neoforged.fml.ModList.get().isLoaded("polymorph")) {
            dimensionalfluxminer.compat.polymorph.PolymorphCompat.register();
        }

        NeoForge.EVENT_BUS.addListener(ContentEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(ContentEvents::onLootTableLoad);
        NeoForge.EVENT_BUS.addListener(ResourceProfiles::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(ResourceProfiles::onServerStopped);
        NeoForge.EVENT_BUS.addListener(SurveyManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(SurveyManager::onServerStopped);
        NeoForge.EVENT_BUS.addListener(BlightSpawnManager::onLevelTick);
        NeoForge.EVENT_BUS.addListener(CorruptedWaterEffects::onEntityTick);
        NeoForge.EVENT_BUS.addListener(dimensionalfluxminer.machine.QuickInstall::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(dimensionalfluxminer.tank.TankInteraction::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, dimensionalfluxminer.compat.CarryOnCompat::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(dimensionalfluxminer.network.ConfigOverridesPayload::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> DfmConfig.setOverrides(java.util.Map.of()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
