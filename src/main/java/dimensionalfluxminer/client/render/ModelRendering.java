package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Helpers to draw standalone baked models (loaded through ModelEvent.RegisterAdditional) from renderers. */
public final class ModelRendering {
    private ModelRendering() {
    }

    public static ModelResourceLocation standalone(ResourceLocation id) {
        return ModelResourceLocation.standalone(id);
    }

    public static BakedModel model(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(standalone(id));
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, BakedModel model, int light, int overlay) {
        RenderType type = RenderType.cutout();
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(type), null,
                model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, type);
    }

    /** Blockstate-style Y rotation (clockwise seen from above) of a model whose native front is north. */
    public static float blockstateY(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
