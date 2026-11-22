package dimensionalfluxminer.client;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmWood;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client side of the corrupted wood: boat models and renderers, and the sign textures of the wood type. The blocks
 * themselves only need their block models (on 1.21.1 the models name their render type).
 */
@EventBusSubscriber(modid = DimensionalFluxMiner.MOD_ID, value = Dist.CLIENT)
public final class CorruptedWoodClient {
    //? if >=26.1 {
    // Same layer names as vanilla ("boat/<wood>"), so BoatRenderer finds textures/entity/boat/corrupted.png.
    public static final ModelLayerLocation BOAT_LAYER = new ModelLayerLocation(DimensionalFluxMiner.id("boat/corrupted"), "main");
    public static final ModelLayerLocation CHEST_BOAT_LAYER = new ModelLayerLocation(DimensionalFluxMiner.id("chest_boat/corrupted"), "main");
    //?}

    private CorruptedWoodClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        //? if <26.3 {
        // Standing and hanging sign textures: textures/entity/signs/corrupted.png and entity/signs/hanging/corrupted.png
        // (26.3 draws signs from block models instead).
        event.enqueueWork(() -> net.minecraft.client.renderer.Sheets.addWoodType(DfmWood.WOOD_TYPE));
        //?}
    }

    //? if >=26.1 {
    @SubscribeEvent
    static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BOAT_LAYER, net.minecraft.client.model.object.boat.BoatModel::createBoatModel);
        event.registerLayerDefinition(CHEST_BOAT_LAYER, net.minecraft.client.model.object.boat.BoatModel::createChestBoatModel);
    }
    //?}
    // 1.21.1 registers no layers: vanilla LayerDefinitions already adds the boat and chest boat layers of every
    // Boat.Type, the corrupted one included; registering them again breaks the resource reload (duplicate key).

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        //? if >=26.1 {
        event.registerEntityRenderer(DfmWood.BOAT_ENTITY.get(), context -> new BoatRenderer(context, BOAT_LAYER));
        event.registerEntityRenderer(DfmWood.CHEST_BOAT_ENTITY.get(), context -> new BoatRenderer(context, CHEST_BOAT_LAYER));
        //?}
    }
}
