package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Renders the ingredients actually being processed inside the chamber: one visual per occupied lane, at the
 * marker positions of the approved model, without any bobbing or extra rotation.
 */
public class ElectricFurnaceRenderer implements BlockEntityRenderer<ElectricFurnaceBlockEntity, ElectricFurnaceRenderer.State> {
    /** Marker centers for a north-facing Electric Furnace, in block units. */
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

    private final ItemModelResolver itemModelResolver;

    public ElectricFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> items = new ArrayList<>();
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElectricFurnaceBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.facing();
        state.items.clear();
        List<ItemStack> chamber = be.chamber();
        for (int i = 0; i < chamber.size() && i < 3; i++) {
            ItemStackRenderState item = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(item, chamber.get(i), ItemDisplayContext.NONE, be.getLevel(), null,
                    (int) be.getBlockPos().asLong() + i);
            state.items.add(item);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        List<ItemStackRenderState> chamber = state.items;
        if (chamber.isEmpty()) {
            return;
        }
        List<Vec3> markers = switch (chamber.size()) {
            case 1 -> List.of(MIDDLE);
            case 2 -> List.of(FRONT, MIDDLE);
            default -> List.of(FRONT, MIDDLE, BACK);
        };
        Direction facing = state.facing;
        int count = Math.min(markers.size(), chamber.size());
        for (int i = 0; i < count; i++) {
            ItemStackRenderState item = chamber.get(i);
            Vec3 p = transform(markers.get(i), facing);
            // Blocks keep their front face and grow backwards until they touch the next item, so from the
            // outside every block reads as a solid cube; the last item keeps the thin depth.
            float depth = SCALE_Z;
            if (i + 1 < count && item.usesBlockLight()) {
                depth = (float) (markers.get(i + 1).z - markers.get(i).z) - TOUCH_GAP;
            }
            pose.pushPose();
            pose.translate(p.x, p.y, p.z);
            pose.rotate(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(facing)));
            pose.translate(0, 0, (depth - SCALE_Z) / 2.0F);
            pose.scale(SCALE_XY, SCALE_XY, depth);
            item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
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
