package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the two counter-rotating rollers while the Crusher is ACTIVE. The body model used in that state has
 * no rollers; OFF/IDLE body models contain them in their rest pose. Speed follows the approved 2-second clip
 * (180 degrees per second) independently of Rate.
 */
public class CrusherRenderer implements BlockEntityRenderer<MechanicalCrusherBlockEntity, CrusherRenderer.State> {
    public static final Identifier ROLLER_LEFT = DimensionalFluxMiner.id("block/mechanical_crusher/roller_left");
    public static final Identifier ROLLER_RIGHT = DimensionalFluxMiner.id("block/mechanical_crusher/roller_right");
    private static final float DEGREES_PER_TICK = 180.0F / 20.0F;

    public CrusherRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        boolean active;
        float angle;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MechanicalCrusherBlockEntity be, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPosition, breakProgress);
        state.active = be.getLevel() != null && be.machineState() == MachineState.ACTIVE;
        state.angle = state.active ? ((be.getLevel().getGameTime() % 40L) + partialTick) * DEGREES_PER_TICK : 0;
        state.facing = be.facing();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.active) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.rotate(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(state.facing)));
        pose.translate(-0.5, 0, -0.5);
        roller(pose, collector, ROLLER_LEFT, 8 - 2.625, -state.angle, state.lightCoords);
        roller(pose, collector, ROLLER_RIGHT, 8 + 2.625, state.angle + 90.0F, state.lightCoords);
        pose.popPose();
    }

    private static void roller(PoseStack pose, SubmitNodeCollector collector, Identifier model, double pivotX, float degrees, int light) {
        pose.pushPose();
        pose.translate(pivotX / 16.0, 10.375 / 16.0, 0.5);
        pose.rotate(Axis.ZP.rotationDegrees(degrees));
        ModelRendering.render(pose, collector, ModelRendering.model(model), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
