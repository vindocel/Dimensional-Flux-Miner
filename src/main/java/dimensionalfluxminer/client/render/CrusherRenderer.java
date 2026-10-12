package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.crusher.DimensionalCrusherBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the two counter-rotating rollers while the Crusher is ACTIVE. The body model used in that state has
 * no rollers; OFF/IDLE body models contain them in their rest pose. Speed follows the approved 2-second clip
 * (180 degrees per second) independently of Rate.
 */
public class CrusherRenderer implements BlockEntityRenderer<DimensionalCrusherBlockEntity> {
    public static final ResourceLocation ROLLER_LEFT = DimensionalFluxMiner.id("block/dimensional_crusher/roller_left");
    public static final ResourceLocation ROLLER_RIGHT = DimensionalFluxMiner.id("block/dimensional_crusher/roller_right");
    private static final float DEGREES_PER_TICK = 180.0F / 20.0F;

    public CrusherRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DimensionalCrusherBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null || be.machineState() != MachineState.ACTIVE) {
            return;
        }
        float angle = ((be.getLevel().getGameTime() % 40L) + partialTick) * DEGREES_PER_TICK;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(be.facing())));
        pose.translate(-0.5, 0, -0.5);
        roller(pose, buffers, ROLLER_LEFT, 8 - 2.625, -angle, light, overlay);
        roller(pose, buffers, ROLLER_RIGHT, 8 + 2.625, angle + 90.0F, light, overlay);
        pose.popPose();
    }

    private static void roller(PoseStack pose, MultiBufferSource buffers, ResourceLocation model, double pivotX, float degrees, int light, int overlay) {
        pose.pushPose();
        pose.translate(pivotX / 16.0, 10.375 / 16.0, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        ModelRendering.render(pose, buffers, ModelRendering.model(model), light, overlay);
        pose.popPose();
    }
}
