package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the guide from the book model's parts: standing upright with the cover to the front, and opening like the
 * model's animation when the local player uses it (see {@link GuideBookAnimation}). Every part pivots on the spine.
 */
public class GuideBookRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation BACK_COVER = part("back_cover");
    public static final ResourceLocation FRONT_COVER = part("front_cover");
    public static final ResourceLocation LEAF_1 = part("leaf_1");
    public static final ResourceLocation LEAF_2 = part("leaf_2");
    public static final ResourceLocation SPINE = part("spine");
    /** Half the width of the closed book, in pixels: the closed book is centered, the open one on its spine. */
    private static final float CLOSED_HALF_WIDTH = 4.1F;

    public GuideBookRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static ResourceLocation part(String name) {
        return DimensionalFluxMiner.id("item/guide_book/" + name);
    }

    public static List<ResourceLocation> models() {
        return List.of(BACK_COVER, FRONT_COVER, LEAF_1, LEAF_2, SPINE);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        GuideBookAnimation.Pose p = heldByLocalPlayer(stack, context) ? GuideBookAnimation.pose() : GuideBookAnimation.Pose.CLOSED;
        pose.pushPose();
        // The parts are modeled lying on the table with the cover up; stand the book up with the cover to the front.
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.translate(-CLOSED_HALF_WIDTH / 16.0 * (1.0F - p.openness()), 0, 0);
        pose.translate(-0.5, -0.5, -0.5);
        part(pose, buffers, BACK_COVER, 0, 0, light, overlay);
        part(pose, buffers, FRONT_COVER, p.cover(), 0, light, overlay);
        part(pose, buffers, LEAF_1, p.leaf1(), 0, light, overlay);
        part(pose, buffers, LEAF_2, p.leaf2(), 0, light, overlay);
        part(pose, buffers, SPINE, p.spine(), p.spineDrop(), light, overlay);
        pose.popPose();
    }

    /** A part turned around the spine (the block center) by {@code angle} degrees and moved down by {@code drop} pixels. */
    private static void part(PoseStack pose, MultiBufferSource buffers, ResourceLocation model, float angle, float drop, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5 + drop / 16.0, 0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5, -0.5, -0.5);
        ModelRendering.render(pose, buffers, ModelRendering.model(model), light, overlay);
        pose.popPose();
    }

    /** Only the book in the local player's own hand opens. */
    private static boolean heldByLocalPlayer(ItemStack stack, ItemDisplayContext context) {
        if (!context.firstPerson() && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (stack == player.getMainHandItem() || stack == player.getOffhandItem());
    }
}
