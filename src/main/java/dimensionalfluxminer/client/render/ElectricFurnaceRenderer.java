package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.furnace.ElectricFurnaceBlockEntity;
//? if >=26.1 {
import java.util.ArrayList;
//?}
import java.util.List;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if >=26.1 {
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
//?} else {
/*import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
*///?}
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import org.jspecify.annotations.Nullable;
//?}

/**
 * Renders the ingredients actually being processed inside the chamber: one visual per occupied lane, at the
 * marker positions of the approved model, without any bobbing or extra rotation.
 */
//? if >=26.1 {
public class ElectricFurnaceRenderer implements BlockEntityRenderer<ElectricFurnaceBlockEntity, ElectricFurnaceRenderer.State> {
//?} else {
/*public class ElectricFurnaceRenderer implements BlockEntityRenderer<ElectricFurnaceBlockEntity> {
*///?}
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

    //? if >=26.1 {
    private final ItemModelResolver itemModelResolver;

    //?}
    public ElectricFurnaceRenderer(BlockEntityRendererProvider.Context context) {
        //? if >=26.1 {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> items = new ArrayList<>();
        Direction facing = Direction.NORTH;
        //?}
    }

    @Override
    //? if >=26.1 {
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElectricFurnaceBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.facing();
        state.items.clear();
    //?} else {
    /*public void render(ElectricFurnaceBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
    *///?}
        List<ItemStack> chamber = be.chamber();
        //? if >=26.1 {
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
        //?}
        if (chamber.isEmpty()) {
            return;
        }
        List<Vec3> markers = switch (chamber.size()) {
            case 1 -> List.of(MIDDLE);
            case 2 -> List.of(FRONT, MIDDLE);
            default -> List.of(FRONT, MIDDLE, BACK);
        };
        //? if >=26.1 {
        Direction facing = state.facing;
        //?} else {
        /*Direction facing = be.facing();
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        *///?}
        int count = Math.min(markers.size(), chamber.size());
        for (int i = 0; i < count; i++) {
            //? if >=26.1 {
            ItemStackRenderState item = chamber.get(i);
            //?} else {
            /*ItemStack stack = chamber.get(i);
            *///?}
            Vec3 p = transform(markers.get(i), facing);
            // Blocks keep their front face and grow backwards until they touch the next item, so from the
            // outside every block reads as a solid cube; the last item keeps the thin depth.
            float depth = SCALE_Z;
            //? if >=26.1 {
            if (i + 1 < count && item.usesBlockLight()) {
            //?} else {
            /*if (i + 1 < count && isBlockModel(items, stack, be)) {
            *///?}
                depth = (float) (markers.get(i + 1).z - markers.get(i).z) - TOUCH_GAP;
            }
            pose.pushPose();
            pose.translate(p.x, p.y, p.z);
            //? if <26.3 {
            pose.mulPose(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(facing)));
            //?} else {
            /*pose.rotate(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(facing)));
            *///?}
            pose.translate(0, 0, (depth - SCALE_Z) / 2.0F);
            pose.scale(SCALE_XY, SCALE_XY, depth);
            //? if >=26.1 {
            item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            //?} else {
            /*items.renderStatic(stack, ItemDisplayContext.NONE, light, overlay, pose, buffers, be.getLevel(),
                    (int) be.getBlockPos().asLong() + i);
            *///?}
            pose.popPose();
        }
    //? if <26.1 {
    /*}

    /^* Items drawn with a 3D model (blocks); flat item sprites keep the thin depth. ^/
    private static boolean isBlockModel(ItemRenderer items, ItemStack stack, ElectricFurnaceBlockEntity be) {
        BakedModel model = items.getModel(stack, be.getLevel(), null, 0);
        return model.isGui3d();
    *///?}
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
