package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.1 {
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//?}
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.resources.model.ModelDebugName;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
*///?}
import net.minecraft.core.Direction;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** Helpers to draw standalone models (loaded through ModelEvent.RegisterStandalone) from renderers. */
//?} else {
/*import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

/^* Helpers to draw standalone baked models (loaded through ModelEvent.RegisterAdditional) from renderers. ^/
*///?}
public final class ModelRendering {
    //? if >=26.1 {
    /** One key per model file: keys are compared by identity. */
    private static final Map<Identifier, StandaloneModelKey<BlockStateModelPart>> KEYS = new ConcurrentHashMap<>();

    //?}
    private ModelRendering() {
    }

    //? if >=26.1 {
    public static StandaloneModelKey<BlockStateModelPart> standalone(Identifier id) {
        return KEYS.computeIfAbsent(id, key -> new StandaloneModelKey<>((ModelDebugName) key::toString));
    //?} else {
    /*public static ModelResourceLocation standalone(ResourceLocation id) {
        return ModelResourceLocation.standalone(id);
    *///?}
    }

    //? if >=26.1 {
    /** Registers {@code id} to be loaded as a standalone model. */
    public static void register(ModelEvent.RegisterStandalone event, Identifier id) {
        event.register(standalone(id), SimpleUnbakedStandaloneModel.simpleModelWrapper(id));
    //?} else {
    /*public static BakedModel model(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(standalone(id));
    *///?}
    }

    //? if >=26.1 {
    public static BlockStateModelPart model(Identifier id) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(standalone(id));
    }

    /** Draws the model with the render type of each of its quads (cutout unless the model says otherwise). */
    public static void render(PoseStack pose, SubmitNodeCollector collector, BlockStateModelPart model, int light, int overlay) {
        collector.submitMultiLayerBlockModel(pose, List.of(model), false, BlockModelRenderState.EMPTY_TINTS, light, overlay, 0);
    }

    /** Same, with {@code tint} (ARGB) on the quads of tint layer 0. */
    public static void render(PoseStack pose, SubmitNodeCollector collector, BlockStateModelPart model, int light, int overlay, int tint) {
        collector.submitMultiLayerBlockModel(pose, List.of(model), false, new int[]{tint}, light, overlay, 0);
    //?} else {
    /*public static void render(PoseStack pose, MultiBufferSource buffers, BakedModel model, int light, int overlay) {
        render(pose, buffers, model, light, overlay, 0xFFFFFFFF);
    }

    /^* Same, with {@code tint} (ARGB) on the tinted quads (only the terminal glow uses a tint layer). ^/
    public static void render(PoseStack pose, MultiBufferSource buffers, BakedModel model, int light, int overlay, int tint) {
        RenderType type = RenderType.cutout();
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(type), null,
                model, (tint >> 16 & 255) / 255.0F, (tint >> 8 & 255) / 255.0F, (tint & 255) / 255.0F, light, overlay,
                ModelData.EMPTY, type);
    *///?}
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
