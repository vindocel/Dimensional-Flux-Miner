package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.smelter.AlloySmelterBlockEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the ingredients actually being processed inside the chamber: one visual per occupied lane, at the
 * marker positions of the approved model, without any bobbing or extra rotation.
 */
public class SmelterRenderer implements BlockEntityRenderer<AlloySmelterBlockEntity> {
    /** Marker centers for a north-facing Smelter, in block units. */
    private static final Vec3 FRONT = new Vec3(0.390625, 0.5625, 0.28125);
    private static final Vec3 MIDDLE = new Vec3(0.5, 0.5625, 0.46875);
    private static final Vec3 BACK = new Vec3(0.609375, 0.5625, 0.65625);
    /**
     * With ItemDisplayContext.NONE every item (flat sprite or block) is exactly one block, centered. The marker
     * envelope is 4 x 4 x 1 model units, so items are scaled to 3.5 units wide/high (a margin from the chamber
     * roof and walls) and 1 unit deep.
     */
    private static final float SCALE_XY = 3.5F / 16.0F;
    private static final float SCALE_Z = 1.0F / 16.0F;
    /** Keeps an extended block's back face just in front of the next item's front face (no z-fighting). */
    private static final float TOUCH_GAP = 0.002F;

    public SmelterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AlloySmelterBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        List<ItemStack> chamber = be.chamber();
        if (chamber.isEmpty()) {
            return;
        }
        List<Vec3> markers = switch (chamber.size()) {
            case 1 -> List.of(MIDDLE);
            case 2 -> List.of(FRONT, MIDDLE);
            default -> List.of(FRONT, MIDDLE, BACK);
        };
        Direction facing = be.facing();
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        int count = Math.min(markers.size(), chamber.size());
        for (int i = 0; i < count; i++) {
            ItemStack stack = chamber.get(i);
            Vec3 p = transform(markers.get(i), facing);
            // Blocks keep their front face and grow backwards until they touch the next item, so from the
            // outside every block reads as a solid cube; the last item keeps the thin depth.
            float depth = SCALE_Z;
            if (i + 1 < count && isBlockModel(items, stack, be)) {
                depth = (float) (markers.get(i + 1).z - markers.get(i).z) - TOUCH_GAP;
            }
            pose.pushPose();
            pose.translate(p.x, p.y, p.z);
            pose.mulPose(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(facing)));
            pose.translate(0, 0, (depth - SCALE_Z) / 2.0F);
            pose.scale(SCALE_XY, SCALE_XY, depth);
            items.renderStatic(stack, ItemDisplayContext.NONE, light, overlay, pose, buffers, be.getLevel(),
                    (int) be.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }

    /** Items drawn with a 3D model (blocks); flat item sprites keep the thin depth. */
    private static boolean isBlockModel(ItemRenderer items, ItemStack stack, AlloySmelterBlockEntity be) {
        BakedModel model = items.getModel(stack, be.getLevel(), null, 0);
        return model.isGui3d();
    }

    /** Facing transform of the display contract (rotation around the block center). */
    static Vec3 transform(Vec3 v, Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(1 - v.z, v.y, v.x);
            case SOUTH -> new Vec3(1 - v.x, v.y, 1 - v.z);
            case WEST -> new Vec3(v.z, v.y, 1 - v.x);
            default -> v;
        };
    }
}
