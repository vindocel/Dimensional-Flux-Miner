package dimensionalfluxminer.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.render.ModelRendering;
import dimensionalfluxminer.miner.MinerLayout;
import dimensionalfluxminer.miner.MinerStructure;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/** Draws the guide's 3D view of the Miner (loose parts or the formed machine) into its own picture on the page. */
public final class GuideStructureRenderer extends PictureInPictureRenderer<GuideStructureRenderer.State> {
    private static final Identifier FORMED = DimensionalFluxMiner.id("block/dimensional_flux_miner/states/energy_core_basic_idle");

    /** The view: turned by {@code yaw} / {@code pitch}, one layer (0 = all) or one part type (null = all). */
    public record State(boolean formed, int layer, MinerLayout.@Nullable Part highlight, float yaw, float pitch,
                        int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea,
                        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        public State(boolean formed, int layer, MinerLayout.@Nullable Part highlight, float yaw, float pitch,
                     int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea) {
            this(formed, layer, highlight, yaw, pitch, x0, y0, x1, y1, scale, scissorArea,
                    PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose, SubmitNodeCollector collector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        // The picture's Y grows downward and its Z is flipped: a half turn on X puts the world's up on top.
        pose.rotate(Axis.XP.rotationDegrees(180));
        pose.rotate(Axis.XP.rotationDegrees(state.pitch()));
        pose.rotate(Axis.YP.rotationDegrees(state.yaw()));
        if (state.formed()) {
            // The formed model's origin is the block under the core; its native front is north.
            pose.pushPose();
            pose.rotate(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(Direction.SOUTH)));
            pose.translate(-0.5, -1.5, -0.5);
            ModelRendering.render(pose, collector, ModelRendering.model(FORMED), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            return;
        }
        RandomSource random = RandomSource.create(42L);
        for (MinerLayout.Slot slot : MinerLayout.BASE_SLOTS) {
            if (state.layer() != 0 && slot.dy() + 2 != state.layer()) {
                continue;
            }
            if (state.highlight() != null && slot.part() != state.highlight()) {
                continue;
            }
            BlockState block = blockState(slot.part());
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(random, parts);
            pose.pushPose();
            pose.translate(slot.dx() - 0.5, slot.dy() - 0.5, slot.dz() - 0.5);
            collector.submitMultiLayerBlockModel(pose, parts, model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT),
                    BlockModelRenderState.EMPTY_TINTS, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /** The structure is centered in the picture. */
    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "dimensionalfluxminer guide structure";
    }

    private static BlockState blockState(MinerLayout.Part part) {
        BlockState state = MinerStructure.blockFor(part).defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            // The terminal faces the viewer (south), like the base layout.
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }
        return state;
    }
}
