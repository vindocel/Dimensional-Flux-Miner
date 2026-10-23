package dimensionalfluxminer.client;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.particle.CoreFluxParticle;
import dimensionalfluxminer.client.render.ConnectedTextureModel;
import dimensionalfluxminer.client.render.CrusherRenderer;
import dimensionalfluxminer.client.render.ElectricFurnaceRenderer;
import dimensionalfluxminer.client.render.GuideBookAnimation;
import dimensionalfluxminer.client.render.GuideBookRenderer;
import dimensionalfluxminer.client.render.MinerRenderer;
import dimensionalfluxminer.client.render.ModelRendering;
import dimensionalfluxminer.client.render.TankRenderer;
import dimensionalfluxminer.client.screen.CrusherScreen;
import dimensionalfluxminer.client.screen.ElectricFurnaceScreen;
import dimensionalfluxminer.client.screen.GuiTheme;
import dimensionalfluxminer.client.screen.GuideStructureRenderer;
import dimensionalfluxminer.client.screen.MinerScreen;
import dimensionalfluxminer.client.screen.TankScreen;
import dimensionalfluxminer.client.sound.MachineSounds;
import dimensionalfluxminer.machine.ClientHooks;
import dimensionalfluxminer.registry.DfmBlockEntities;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmMenus;
import dimensionalfluxminer.registry.DfmParticles;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.BubblePopParticle;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import org.joml.Vector4f;

@EventBusSubscriber(modid = DimensionalFluxMiner.MOD_ID, value = Dist.CLIENT)
public final class DfmClient {
    public static final Identifier LIQUID_EXPERIENCE_STILL = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_still");
    public static final Identifier LIQUID_EXPERIENCE_FLOW = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_flow");
    private static final Identifier CORRUPTED_WATER_STILL = DimensionalFluxMiner.id("block/corruption/corrupted_water_still");
    private static final Identifier CORRUPTED_WATER_FLOW = DimensionalFluxMiner.id("block/corruption/corrupted_water_flow");
    private static final Identifier WATER_OVERLAY = Identifier.withDefaultNamespace("block/water_overlay");
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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GuideBookAnimation::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesReceivedEvent received) ->
                dimensionalfluxminer.recipe.Recipes.setClientRecipes(received.getRecipeMap()));
        // Script values of the configuration belong to the server just left.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut loggingOut) -> dimensionalfluxminer.DfmConfig.setOverrides(java.util.Map.of()));
    }

    /** Pipelines of the dark look of the machine screens. */
    @SubscribeEvent
    static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GuiTheme.DARK_PIPELINE);
        event.registerPipeline(GuiTheme.LIGHT_PIPELINE);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(DfmMenus.CRUSHER.get(), CrusherScreen::new);
        event.register(DfmMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        event.register(DfmMenus.MINER.get(), MinerScreen::new);
        event.register(DfmMenus.DIMENSIONAL_TANK.get(), TankScreen::new);
    }

    @SubscribeEvent
    static void onRegisterPictureInPicture(RegisterPictureInPictureRenderersEvent event) {
        event.register(GuideStructureRenderer.State.class, GuideStructureRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DfmBlockEntities.MECHANICAL_CRUSHER.get(), CrusherRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.ELECTRIC_FURNACE.get(), ElectricFurnaceRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.MINER_CONTROLLER.get(), MinerRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.DIMENSIONAL_TANK.get(), TankRenderer::new);
    }

    /** The guide book in the hand, on the ground and in frames (the GUI shows its flat icon). */
    @SubscribeEvent
    static void onRegisterSpecialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(GuideBookRenderer.ID, GuideBookRenderer.Unbaked.MAP_CODEC);
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
    static void onRegisterModels(ModelEvent.RegisterStandalone event) {
        ModelRendering.register(event, CrusherRenderer.ROLLER_LEFT);
        ModelRendering.register(event, CrusherRenderer.ROLLER_RIGHT);
        for (Identifier model : MinerRenderer.models()) {
            ModelRendering.register(event, model);
        }
        for (Identifier model : GuideBookRenderer.models()) {
            ModelRendering.register(event, model);
        }
    }

    /** Connected textures on the loose Steel Chassis, glass and ventilation sides (every block state of each). */
    @SubscribeEvent
    static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Identifier chassis = DimensionalFluxMiner.id("block/parts/steel_chassis");
        Identifier glass = DimensionalFluxMiner.id("block/dimensional_flux_miner/structural_glass_containment");
        Identifier vent = DimensionalFluxMiner.id("block/parts/ventilation_side");
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        connect(models, DfmBlocks.STEEL_CHASSIS.get(), all(chassis), true);
        connect(models, DfmBlocks.REINFORCED_STRUCTURAL_GLASS.get(), all(glass), true);
        // Vents side by side make one long grille, as on the back of the Miner; stacked rows keep their plates.
        connect(models, DfmBlocks.VENTILATION_UNIT.get(), Map.of(Direction.NORTH, vent, Direction.SOUTH, vent,
                Direction.EAST, vent, Direction.WEST, vent), false);
    }

    private static Map<Direction, Identifier> all(Identifier texture) {
        Map<Direction, Identifier> map = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            map.put(face, texture);
        }
        return map;
    }

    private static void connect(Map<BlockState, BlockStateModel> models, Block block, Map<Direction, Identifier> textures, boolean vertical) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            BlockStateModel model = models.get(state);
            if (model != null) {
                models.put(state, new ConnectedTextureModel(model, block, textures, vertical));
            }
        }
    }

    /** Liquid experience and Corrupted Water: their textures, drawn translucent; the water keeps the vanilla overlay. */
    @SubscribeEvent
    static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(new Material(LIQUID_EXPERIENCE_STILL, true), new Material(LIQUID_EXPERIENCE_FLOW, true),
                null, FluidTintSources.constant(0xFFFFFFFF)), DfmFluids.LIQUID_EXPERIENCE);
        event.register(new FluidModel.Unbaked(new Material(CORRUPTED_WATER_STILL, true), new Material(CORRUPTED_WATER_FLOW, true),
                new Material(WATER_OVERLAY), FluidTintSources.constant(0xBFFFFFFF)), DfmFluids.CORRUPTED_WATER, DfmFluids.FLOWING_CORRUPTED_WATER);
    }

    @SubscribeEvent
    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            /** Under Corrupted Water the view is tinted purple instead of the blue of water. */
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                       Vector4f fluidFogColor) {
                fluidFogColor.set(CORRUPTED_WATER_FOG[0], CORRUPTED_WATER_FOG[1], CORRUPTED_WATER_FOG[2], fluidFogColor.w());
            }
        }, DfmFluids.CORRUPTED_WATER_TYPE.get());
    }
}
