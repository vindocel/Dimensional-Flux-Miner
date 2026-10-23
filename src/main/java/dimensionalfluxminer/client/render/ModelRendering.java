package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** Helpers to draw standalone models (loaded through ModelEvent.RegisterStandalone) from renderers. */
public final class ModelRendering {
    /** One key per model file: keys are compared by identity. */
    private static final Map<Identifier, StandaloneModelKey<BlockStateModelPart>> KEYS = new ConcurrentHashMap<>();

    private ModelRendering() {
    }

    public static StandaloneModelKey<BlockStateModelPart> standalone(Identifier id) {
        return KEYS.computeIfAbsent(id, key -> new StandaloneModelKey<>((ModelDebugName) key::toString));
    }

    /** Registers {@code id} to be loaded as a standalone model. */
    public static void register(ModelEvent.RegisterStandalone event, Identifier id) {
        event.register(standalone(id), SimpleUnbakedStandaloneModel.simpleModelWrapper(id));
    }

    public static BlockStateModelPart model(Identifier id) {
        return Minecraft.getInstance().getModelManager().getStandaloneModel(standalone(id));
    }

    /** Draws the model with the render type of each of its quads (cutout unless the model says otherwise). */
    public static void render(PoseStack pose, SubmitNodeCollector collector, BlockStateModelPart model, int light, int overlay) {
        collector.submitMultiLayerBlockModel(pose, List.of(model), false, BlockModelRenderState.EMPTY_TINTS, light, overlay, 0);
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
