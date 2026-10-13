package dimensionalfluxminer.client;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.render.CrusherRenderer;
import dimensionalfluxminer.client.render.GuideBookAnimation;
import dimensionalfluxminer.client.render.GuideBookRenderer;
import dimensionalfluxminer.registry.DfmItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import dimensionalfluxminer.client.render.ModelRendering;
import dimensionalfluxminer.client.render.SmelterRenderer;
import dimensionalfluxminer.client.screen.CrusherScreen;
import dimensionalfluxminer.client.screen.SmelterScreen;
import dimensionalfluxminer.client.sound.MachineSounds;
import dimensionalfluxminer.machine.ClientHooks;
import dimensionalfluxminer.registry.DfmBlockEntities;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmMenus;
import dimensionalfluxminer.registry.DfmParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.BubblePopParticle;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.joml.Vector3f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = DimensionalFluxMiner.MOD_ID, value = Dist.CLIENT)
public final class DfmClient {
    public static final ResourceLocation LIQUID_EXPERIENCE_STILL = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_still");
    public static final ResourceLocation LIQUID_EXPERIENCE_FLOW = DimensionalFluxMiner.id("block/liquid_experience/liquid_experience_flow");
    private static final ResourceLocation CORRUPTED_WATER_STILL = DimensionalFluxMiner.id("block/corruption/corrupted_water_still");
    private static final ResourceLocation CORRUPTED_WATER_FLOW = DimensionalFluxMiner.id("block/corruption/corrupted_water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");
    /** Fog color seen under Corrupted Water (red, green, blue), a dark purple matching the fluid. */
    private static final float[] CORRUPTED_WATER_FOG = {0.17F, 0.04F, 0.24F};

    private DfmClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ClientHooks.machineSound = MachineSounds::update;
        ClientHooks.minerSound = MinerClientEffects::runningSound;
        ClientHooks.formationEffect = MinerClientEffects::formationBurst;
        ClientHooks.assemblyHighlight = MinerAssemblyHighlight::show;
        // Using the guide opens the book in the hand first; the screen follows once it is open.
        ClientHooks.openGuide = GuideBookAnimation::requestOpen;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GuideBookAnimation::onClientTick);
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(DfmFluids.CORRUPTED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(DfmFluids.FLOWING_CORRUPTED_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(DfmFluids.LIQUID_EXPERIENCE.get(), RenderType.translucent());
            // The corrupted-water kelp and seagrass use the vanilla plant models, which render as cutout.
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_KELP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_KELP_PLANT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_SEAGRASS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(DfmBlocks.CORRUPTED_TALL_SEAGRASS.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(DfmMenus.CRUSHER.get(), CrusherScreen::new);
        event.register(DfmMenus.SMELTER.get(), SmelterScreen::new);
        event.register(DfmMenus.MINER.get(), dimensionalfluxminer.client.screen.MinerScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DfmBlockEntities.DIMENSIONAL_CRUSHER.get(), CrusherRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.ALLOY_SMELTER.get(), SmelterRenderer::new);
        event.registerBlockEntityRenderer(DfmBlockEntities.MINER_CONTROLLER.get(), dimensionalfluxminer.client.render.MinerRenderer::new);
    }

    /** The purple water particles behave exactly like the vanilla ones, with their own sprites. */
    @SubscribeEvent
    static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DfmParticles.CORRUPTED_BUBBLE.get(), BubbleParticle.Provider::new);
        event.registerSpriteSet(DfmParticles.CORRUPTED_BUBBLE_POP.get(), BubblePopParticle.Provider::new);
        event.registerSpriteSet(DfmParticles.CORRUPTED_SPLASH.get(), SplashParticle.Provider::new);
    }

    @SubscribeEvent
    static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelRendering.standalone(CrusherRenderer.ROLLER_LEFT));
        event.register(ModelRendering.standalone(CrusherRenderer.ROLLER_RIGHT));
        for (ResourceLocation model : dimensionalfluxminer.client.render.MinerRenderer.models()) {
            event.register(ModelRendering.standalone(model));
        }
        for (ResourceLocation model : GuideBookRenderer.models()) {
            event.register(ModelRendering.standalone(model));
        }
    }

    @SubscribeEvent
    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private GuideBookRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new GuideBookRenderer();
                }
                return renderer;
            }
        }, DfmItems.GUIDE_BOOK.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
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

            /** Under Corrupted Water the view is tinted purple instead of the blue of water. */
            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                           Vector3f fluidFogColor) {
                return new Vector3f(CORRUPTED_WATER_FOG[0], CORRUPTED_WATER_FOG[1], CORRUPTED_WATER_FOG[2]);
            }
        }, DfmFluids.CORRUPTED_WATER_TYPE.get());
    }
}
