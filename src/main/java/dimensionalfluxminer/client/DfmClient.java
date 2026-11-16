package dimensionalfluxminer.client;

import dimensionalfluxminer.DimensionalFluxMiner;
//? if >=26.1 {
import dimensionalfluxminer.client.particle.CoreFluxParticle;
import dimensionalfluxminer.client.render.ConnectedTextureModel;
//?}
import dimensionalfluxminer.client.render.CrusherRenderer;
//? if >=26.1 {
import dimensionalfluxminer.client.render.ElectricFurnaceRenderer;
//?}
import dimensionalfluxminer.client.render.GuideBookAnimation;
import dimensionalfluxminer.client.render.GuideBookRenderer;
//? if >=26.1 {
import dimensionalfluxminer.client.render.MinerRenderer;
//?} else {
/*import dimensionalfluxminer.registry.DfmItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
*///?}
import dimensionalfluxminer.client.render.ModelRendering;
//? if >=26.1 {
import dimensionalfluxminer.client.render.TankRenderer;
//?} else {
/*import dimensionalfluxminer.client.render.ElectricFurnaceRenderer;
*///?}
import dimensionalfluxminer.client.screen.CrusherScreen;
import dimensionalfluxminer.client.screen.ElectricFurnaceScreen;
//? if >=26.1 {
import dimensionalfluxminer.client.screen.GuiTheme;
import dimensionalfluxminer.client.screen.GuideStructureRenderer;
import dimensionalfluxminer.client.screen.MinerScreen;
import dimensionalfluxminer.client.screen.TankScreen;
//?}
import dimensionalfluxminer.client.sound.MachineSounds;
import dimensionalfluxminer.machine.ClientHooks;
import dimensionalfluxminer.registry.DfmBlockEntities;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmMenus;
//? if <26.1 {
/*import dimensionalfluxminer.client.particle.CoreFluxParticle;
*///?}
import dimensionalfluxminer.registry.DfmParticles;
//? if >=26.1 {
import java.util.EnumMap;
import java.util.Map;
//?}
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.BubblePopParticle;
import net.minecraft.client.particle.SplashParticle;
//? if >=26.1 {
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//?} else {
/*import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.joml.Vector3f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
*///?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
//? if >=26.1 {
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
//?}
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
//? if >=26.1 {
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
//?}
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
//? if >=26.1 {
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import org.joml.Vector4f;
//?}

@EventBusSubscriber(modid = DimensionalFluxMiner.MOD_ID, value = Dist.CLIENT)
public final class DfmClient {
    //? if >=26.1 {
    public static final Identifier LIQUID_EXPERIENCE_STILL = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_still");
    public static final Identifier LIQUID_EXPERIENCE_FLOW = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_flow");
    private static final Identifier CORRUPTED_WATER_STILL = DimensionalFluxMiner.id("block/corruption/corrupted_water_still");
    private static final Identifier CORRUPTED_WATER_FLOW = DimensionalFluxMiner.id("block/corruption/corrupted_water_flow");
    private static final Identifier WATER_OVERLAY = Identifier.withDefaultNamespace("block/water_overlay");
    //?} else {
    /*public static final ResourceLocation LIQUID_EXPERIENCE_STILL = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_still");
    public static final ResourceLocation LIQUID_EXPERIENCE_FLOW = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_flow");
    private static final ResourceLocation CORRUPTED_WATER_STILL = DimensionalFluxMiner.id("block/corruption/corrupted_water_still");
    private static final ResourceLocation CORRUPTED_WATER_FLOW = DimensionalFluxMiner.id("block/corruption/corrupted_water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");
    *///?}
    /** Fog color seen under Corrupted Water (red, green, blue), a dark purple matching the fluid. */
    private static final float[] CORRUPTED_WATER_FOG = {0.17F, 0.04F, 0.24F};

    private DfmClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ClientHooks.machineSound = MachineSounds::update;
        ClientHooks.minerTick = MinerClientEffects::tick;
        ClientHooks.formationEffect = MinerClientEffects::formationBurst;
        ClientHooks.assemblyHighlight = MinerAssemblyHighlight::show;
        // Using the guide opens the book in the hand first; the screen follows once it is open.
        ClientHooks.openGuide = GuideBookAnimation::requestOpen;
        //? if <26.1 {
        /*if (net.neoforged.fml.ModList.get().isLoaded("polymorph")) {
            dimensionalfluxminer.compat.polymorph.PolymorphClientCompat.register();
        }
        *///?}
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GuideBookAnimation::onClientTick);
        //? if >=26.1 {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesReceivedEvent received) ->
                dimensionalfluxminer.recipe.Recipes.setClientRecipes(received.getRecipeMap()));
        //?}
        // Script values of the configuration belong to the server just left.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut loggingOut) -> dimensionalfluxminer.DfmConfig.setOverrides(java.util.Map.of()));
        //? if <26.1 {
        /*event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(DfmFluids.CORRUPTED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(DfmFluids.FLOWING_CORRUPTED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(DfmFluids.LIQUID_EXPERIENCE.get(), RenderType.translucent());
            // The corrupted-water kelp and seagrass use the vanilla plant models, which render as cutout.
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_KELP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_KELP_PLANT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_SEAGRASS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_TALL_SEAGRASS.get(), RenderType.cutout());
        });
        *///?}
    }

    //? if >=26.1 {
    /** Pipelines of the dark look of the machine screens. */
    @SubscribeEvent
    static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GuiTheme.DARK_PIPELINE);
        event.registerPipeline(GuiTheme.LIGHT_PIPELINE);
        event.registerPipeline(dimensionalfluxminer.client.screen.IconPalette.PIPELINE);
        event.registerPipeline(dimensionalfluxminer.client.screen.IconPalette.MONOCHROME_PIPELINE);
    //?} else {
    /*/^* Shader of the dark look of the machine screens (reloaded with the resource packs). ^/
    @SubscribeEvent
    static void onRegisterShaders(net.neoforged.neoforge.client.event.RegisterShadersEvent event) throws java.io.IOException {
        event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(), DimensionalFluxMiner.id("dark_gui"),
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX), dimensionalfluxminer.client.screen.GuiTheme::setShader);
        event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(), DimensionalFluxMiner.id("icon_palette"),
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX), dimensionalfluxminer.client.screen.IconPalette::setShader);
    *///?}
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(DfmMenus.CRUSHER.get(), CrusherScreen::new);
        event.register(DfmMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        //? if >=26.1 {
        event.register(DfmMenus.MINER.get(), MinerScreen::new);
        event.register(DfmMenus.DIMENSIONAL_TANK.get(), TankScreen::new);
    }

    @SubscribeEvent
    static void onRegisterPictureInPicture(RegisterPictureInPictureRenderersEvent event) {
        event.register(GuideStructureRenderer.State.class, GuideStructureRenderer::new);
        //?} else {
        /*event.register(DfmMenus.MINER.get(), dimensionalfluxminer.client.screen.MinerScreen::new);
        event.register(DfmMenus.DIMENSIONAL_TANK.get(), dimensionalfluxminer.client.screen.TankScreen::new);
        *///?}
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DfmBlockEntities.MECHANICAL_CRUSHER.get(), CrusherRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.ELECTRIC_FURNACE.get(), ElectricFurnaceRenderer::new);
        //? if >=26.1 {
        event.registerBlockEntityRenderer(DfmBlockEntities.MINER_CONTROLLER.get(), MinerRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.DIMENSIONAL_TANK.get(), TankRenderer::new);
    }

    /** The guide book in the hand, on the ground and in frames (the GUI shows its flat icon). */
    @SubscribeEvent
    static void onRegisterSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(GuideBookRenderer.ID, GuideBookRenderer.Unbaked.MAP_CODEC);
        event.register(dimensionalfluxminer.client.render.TankItemRenderer.ID, dimensionalfluxminer.client.render.TankItemRenderer.Unbaked.MAP_CODEC);
        //?} else {
        /*event.registerBlockEntityRenderer(DfmBlockEntities.MINER_CONTROLLER.get(), dimensionalfluxminer.client.render.MinerRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.DIMENSIONAL_TANK.get(), dimensionalfluxminer.client.render.TankRenderer::new);
        *///?}
    }

    /** The purple water particles behave exactly like the vanilla ones, with their own sprites. */
    @SubscribeEvent
    static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DfmParticles.CORRUPTED_BUBBLE.get(), BubbleParticle.Provider::new);
        event.registerSpriteSet(DfmParticles.CORRUPTED_BUBBLE_POP.get(), BubblePopParticle.Provider::new);
        event.registerSpriteSet(DfmParticles.CORRUPTED_SPLASH.get(), SplashParticle.Provider::new);
        event.registerSpriteSet(DfmParticles.CORE_FLUX_BASIC.get(), CoreFluxParticle::basic);
        event.registerSpriteSet(DfmParticles.CORE_FLUX_STABILIZED.get(), CoreFluxParticle::stabilized);
    }

    @SubscribeEvent
    //? if >=26.1 {
    static void onRegisterModels(ModelEvent.RegisterStandalone event) {
        ModelRendering.register(event, CrusherRenderer.ROLLER_LEFT);
        ModelRendering.register(event, CrusherRenderer.ROLLER_RIGHT);
        for (Identifier model : MinerRenderer.models()) {
            ModelRendering.register(event, model);
        }
        for (Identifier model : GuideBookRenderer.models()) {
            ModelRendering.register(event, model);
    //?} else {
    /*static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelRendering.standalone(CrusherRenderer.ROLLER_LEFT));
        event.register(ModelRendering.standalone(CrusherRenderer.ROLLER_RIGHT));
        for (ResourceLocation model : dimensionalfluxminer.client.render.MinerRenderer.models()) {
            event.register(ModelRendering.standalone(model));
        }
        for (ResourceLocation model : GuideBookRenderer.models()) {
            event.register(ModelRendering.standalone(model));
    *///?}
        }
    }

    /** Connected textures on the loose Steel Chassis, glass and ventilation sides (every block state of each). */
    @SubscribeEvent
    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        //? if >=26.1 {
        Identifier chassis = DimensionalFluxMiner.id("block/parts/steel_chassis");
        Identifier glass = DimensionalFluxMiner.id("block/dimensional_flux_miner/structural_glass_containment");
        Identifier vent = DimensionalFluxMiner.id("block/parts/ventilation_side");
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        connect(models, DfmBlocks.STEEL_CHASSIS.get(), all(chassis), true);
        connect(models, DfmBlocks.REINFORCED_STRUCTURAL_GLASS.get(), all(glass), true);
        //?} else {
        /*ResourceLocation chassis = DimensionalFluxMiner.id("block/parts/steel_chassis");
        ResourceLocation glass = DimensionalFluxMiner.id("block/dimensional_flux_miner/structural_glass_containment");
        ResourceLocation vent = DimensionalFluxMiner.id("block/parts/ventilation_side");
        connect(event, DfmBlocks.STEEL_CHASSIS.get(), all(chassis), true);
        connect(event, DfmBlocks.REINFORCED_STRUCTURAL_GLASS.get(), all(glass), true);
        *///?}
        // Vents side by side make one long grille, as on the back of the Miner; stacked rows keep their plates.
        //? if >=26.1 {
        connect(models, DfmBlocks.VENTILATION_UNIT.get(), Map.of(Direction.NORTH, vent, Direction.SOUTH, vent,
                Direction.EAST, vent, Direction.WEST, vent), false);
        //?} else {
        /*connect(event, DfmBlocks.VENTILATION_UNIT.get(), java.util.Map.of(net.minecraft.core.Direction.NORTH, vent,
                net.minecraft.core.Direction.SOUTH, vent, net.minecraft.core.Direction.EAST, vent, net.minecraft.core.Direction.WEST, vent), false);
        *///?}
    }

    //? if >=26.1 {
    private static Map<Direction, Identifier> all(Identifier texture) {
        Map<Direction, Identifier> map = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
    //?} else {
    /*private static java.util.Map<net.minecraft.core.Direction, ResourceLocation> all(ResourceLocation texture) {
        java.util.Map<net.minecraft.core.Direction, ResourceLocation> map = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
        for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
    *///?}
            map.put(face, texture);
        }
        return map;
    }

    //? if >=26.1 {
    private static void connect(Map<BlockState, BlockStateModel> models, Block block, Map<Direction, Identifier> textures, boolean vertical) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            BlockStateModel model = models.get(state);
    //?} else {
    /*private static void connect(ModelEvent.ModifyBakingResult event, net.minecraft.world.level.block.Block block,
                                java.util.Map<net.minecraft.core.Direction, ResourceLocation> textures, boolean vertical) {
        for (net.minecraft.world.level.block.state.BlockState state : block.getStateDefinition().getPossibleStates()) {
            var key = net.minecraft.client.renderer.block.BlockModelShaper.stateToModelLocation(state);
            var model = event.getModels().get(key);
    *///?}
            if (model != null) {
                //? if >=26.1 {
                models.put(state, new ConnectedTextureModel(model, block, textures, vertical));
                //?} else {
                /*event.getModels().put(key, new dimensionalfluxminer.client.render.ConnectedTextureModel(model, block, textures, vertical));
                *///?}
            }
        }
    }

    //? if >=26.1 {
    /** Liquid experience and Corrupted Water: their textures, drawn translucent; the water keeps the vanilla overlay. */
    @SubscribeEvent
    static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(new Material(LIQUID_EXPERIENCE_STILL, true), new Material(LIQUID_EXPERIENCE_FLOW, true),
                null, FluidTintSources.constant(0xFFFFFFFF)), DfmFluids.LIQUID_EXPERIENCE);
        event.register(new FluidModel.Unbaked(new Material(CORRUPTED_WATER_STILL, true), new Material(CORRUPTED_WATER_FLOW, true),
                new Material(WATER_OVERLAY), FluidTintSources.constant(0xBFFFFFFF)), DfmFluids.CORRUPTED_WATER, DfmFluids.FLOWING_CORRUPTED_WATER);
    }

    //?}
    @SubscribeEvent
    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        //? if <26.1 {
        /*event.registerItem(new IClientItemExtensions() {
            private GuideBookRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GuideBookRenderer();
                }
                return renderer;
            }
        }, DfmItems.GUIDE_BOOK.get());
        event.registerItem(new IClientItemExtensions() {
            private dimensionalfluxminer.client.render.TankItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new dimensionalfluxminer.client.render.TankItemRenderer();
                }
                return renderer;
            }
        }, DfmItems.DIMENSIONAL_TANK.get(), DfmItems.DIMENSIONAL_DUAL_TANK.get(), DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
        *///?}
        event.registerFluidType(new IClientFluidTypeExtensions() {
            //? if <26.1 {
            /*@Override
            public ResourceLocation getStillTexture() {
                return LIQUID_EXPERIENCE_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return LIQUID_EXPERIENCE_FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        }, DfmFluids.LIQUID_EXPERIENCE_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return CORRUPTED_WATER_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return CORRUPTED_WATER_FLOW;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return WATER_OVERLAY;
            }

            @Override
            public int getTintColor() {
                return 0xBFFFFFFF;
            }

            *///?}
            /** Under Corrupted Water the view is tinted purple instead of the blue of water. */
            @Override
            //? if >=26.1 {
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                       Vector4f fluidFogColor) {
                fluidFogColor.set(CORRUPTED_WATER_FOG[0], CORRUPTED_WATER_FOG[1], CORRUPTED_WATER_FOG[2], fluidFogColor.w());
            //?} else {
            /*public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                           Vector3f fluidFogColor) {
                return new Vector3f(CORRUPTED_WATER_FOG[0], CORRUPTED_WATER_FOG[1], CORRUPTED_WATER_FOG[2]);
            *///?}
            }
        }, DfmFluids.CORRUPTED_WATER_TYPE.get());
    }
}
