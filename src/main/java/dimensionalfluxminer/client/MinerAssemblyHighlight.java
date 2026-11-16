package dimensionalfluxminer.client;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.QuadInstance;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
*///?}
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerStructure;
import dimensionalfluxminer.network.MinerAssemblyPayload;
import dimensionalfluxminer.registry.DfmBlocks;
//? if >=26.1 {
import java.util.ArrayList;
//?}
import java.util.List;
//? if <26.1 {
/*import net.minecraft.Util;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
//? if =26.1.2 {
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else if >=26.3 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
*///?}
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=26.1 {
import net.minecraft.client.resources.model.geometry.BakedQuad;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=26.1 {
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
//?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.world.phys.shapes.Shapes;
//?}
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//? if >=26.1 {
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
//?} else {
/*import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

*///?}
/**
 * Assembly preview of the Miner, drawn as translucent ghosts of the blocks that go in each position (a red outline
 * where another block is in the way).
 * <ul>
 * <li>Highlight of a placed terminal: the parts still missing; the next one to place pulses. It shows when the
 * terminal is placed, stays until the machine forms (positions filled in the meantime disappear) and a click on the
 * terminal that places nothing switches it off and on.</li>
 * <li>With a terminal in hand: the whole machine where the terminal would go under the crosshair, facing the player.</li>
 * </ul>
 */
@EventBusSubscriber(modid = DimensionalFluxMiner.MOD_ID, value = Dist.CLIENT)
public final class MinerAssemblyHighlight {
    private static final int OCCUPIED = 0xFF3B3B;
    private static final double INSET = 0.02;
    private static final float GHOST_ALPHA = 0.4F;

    private static BlockPos terminal;
    private static List<MinerAssemblyPayload.Entry> entries = List.of();
    private static ClientLevel shownIn;

    private MinerAssemblyHighlight() {
    }

    public static void show(MinerAssemblyPayload payload) {
        terminal = payload.terminal();
        entries = List.copyOf(payload.entries());
        shownIn = Minecraft.getInstance().level;
    }

    private static void clear() {
        terminal = null;
        entries = List.of();
        shownIn = null;
    }

    //? if >=26.1 {
    /** A ghost block (alpha above 0) or a red outline (alpha 0) to draw this frame. */
    private record Mark(BlockPos pos, BlockState state, float alpha) {
    }

    /** What the frame being drawn shows, found while the level is extracted. */
    private static List<Mark> marks = List.of();
    private static Vec3 camera = Vec3.ZERO;

    //?}
    @SubscribeEvent
    //? if >=26.1 {
    static void onExtract(ExtractLevelRenderStateEvent event) {
        marks = List.of();
    //?} else {
    /*static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
    *///?}
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            clear();
            return;
        }
        boolean holding = player.getMainHandItem().is(DfmBlocks.MINER_TERMINAL.get().asItem())
                || player.getOffhandItem().is(DfmBlocks.MINER_TERMINAL.get().asItem());
        if (entries.isEmpty() && !holding) {
            return;
        }
        //? if >=26.1 {
        camera = event.getCamera().position();
        List<Mark> found = new ArrayList<>();
        collectHighlight(level, found);
        //?} else {
        /*Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        renderHighlight(level, pose, buffers);
        *///?}
        if (holding && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            //? if >=26.1 {
            collectHandPreview(level, player, hit, found);
        }
        marks = found;
            //?} else {
            /*renderHandPreview(level, player, hit, pose, buffers);
        }
        pose.popPose();
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.lines());
            *///?}
    }

    //? if =26.1.2 {
    @SubscribeEvent
    static void onSubmit(SubmitCustomGeometryEvent event) {
        if (marks.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (Mark mark : marks) {
            pose.pushPose();
            pose.translate(mark.pos().getX() - camera.x, mark.pos().getY() - camera.y, mark.pos().getZ() - camera.z);
            if (mark.alpha() <= 0) {
                collector.submitCustomGeometry(pose, RenderTypes.lines(), (last, consumer) -> {
                    PoseStack shapePose = new PoseStack();
                    shapePose.last().set(last);
                    ShapeRenderer.renderShape(shapePose, consumer, Shapes.create(new AABB(BlockPos.ZERO).deflate(INSET)), 0, 0, 0,
                            0xE6000000 | OCCUPIED, 2.0F);
                });
            } else {
                ghost(pose, collector, mark.state(), mark.alpha());
            }
            pose.popPose();
        }
    }

    private static void collectHighlight(ClientLevel level, List<Mark> found) {
    //?} else if >=26.3 {
    /*@SubscribeEvent
    static void onSubmit(SubmitCustomGeometryEvent event) {
        if (marks.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (Mark mark : marks) {
            pose.pushPose();
            pose.translate(mark.pos().getX() - camera.x, mark.pos().getY() - camera.y, mark.pos().getZ() - camera.z);
            if (mark.alpha() <= 0) {
                collector.submitShapeOutline(pose, Shapes.create(new AABB(BlockPos.ZERO).deflate(INSET)), RenderTypes.lines(),
                        0xE6000000 | OCCUPIED, 2.0F, true);
            } else {
                ghost(pose, collector, mark.state(), mark.alpha());
            }
            pose.popPose();
        }
    }

    private static void collectHighlight(ClientLevel level, List<Mark> found) {
    *///?} else {
    /*private static void renderHighlight(ClientLevel level, PoseStack pose, MultiBufferSource.BufferSource buffers) {
    *///?}
        if (entries.isEmpty()) {
            return;
        }
        // Gone with the terminal, in another level, or once every part is in place.
        if (level != shownIn || !level.getBlockState(terminal).is(DfmBlocks.MINER_TERMINAL.get())) {
            clear();
            return;
        }
        float pulse = 0.25F + 0.4F * (float) (0.5 + 0.5 * Math.sin(Util.getMillis() / 150.0));
        boolean next = true;
        boolean anyMissing = false;
        for (MinerAssemblyPayload.Entry entry : entries) {
            Block block = MinerStructure.blockFor(Part.values()[entry.part()]);
            BlockState state = level.getBlockState(entry.pos());
            if (state.is(block)) {
                continue;
            }
            anyMissing = true;
            if (!state.canBeReplaced()) {
                //? if >=26.1 {
                found.add(new Mark(entry.pos(), state, 0));
                //?} else {
                /*outline(pose, buffers, entry.pos());
                *///?}
                continue;
            }
            //? if >=26.1 {
            found.add(new Mark(entry.pos(), block.defaultBlockState(), next ? pulse : GHOST_ALPHA));
            //?} else {
            /*ghost(pose, buffers, block.defaultBlockState(), entry.pos(), next ? pulse : GHOST_ALPHA);
            *///?}
            next = false;
        }
        if (!anyMissing) {
            clear();
        }
    }

    /** The whole machine where a terminal placed on {@code hit} would build it (the terminal faces the player). */
    //? if >=26.1 {
    private static void collectHandPreview(ClientLevel level, LocalPlayer player, BlockHitResult hit, List<Mark> found) {
    //?} else {
    /*private static void renderHandPreview(ClientLevel level, LocalPlayer player, BlockHitResult hit, PoseStack pose,
                                          MultiBufferSource.BufferSource buffers) {
    *///?}
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(hit.getDirection());
        }
        Direction facing = player.getDirection().getOpposite();
        for (MinerStructure.Member member : MinerStructure.layout(pos, facing).members()) {
            Block block = MinerStructure.blockFor(member.slot().part());
            BlockState state = level.getBlockState(member.pos());
            if (state.is(block)) {
                continue;
            }
            if (!state.canBeReplaced()) {
                //? if >=26.1 {
                found.add(new Mark(member.pos(), state, 0));
                //?} else {
                /*outline(pose, buffers, member.pos());
                *///?}
                continue;
            }
            BlockState ghost = block.defaultBlockState();
            if (ghost.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                ghost = ghost.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            //? if >=26.1 {
            found.add(new Mark(member.pos(), ghost, GHOST_ALPHA));
            //?} else {
            /*ghost(pose, buffers, ghost, member.pos(), GHOST_ALPHA);
            *///?}
        }
    }

    //? if >=26.1 {
    /** The block's model, translucent and fully lit. */
    private static void ghost(PoseStack pose, SubmitNodeCollector collector, BlockState state, float alpha) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        QuadInstance instance = new QuadInstance();
        instance.setColor(Math.round(alpha * 255) << 24 | 0xFFFFFF);
        instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
        collector.submitCustomGeometry(pose, RenderTypes.translucentMovingBlock(), (last, consumer) -> {
            for (BlockStateModelPart part : parts) {
                for (Direction direction : Direction.values()) {
                    for (BakedQuad quad : part.getQuads(direction)) {
                        consumer.putBakedQuad(last, quad, instance);
                    }
                }
                for (BakedQuad quad : part.getQuads(null)) {
                    consumer.putBakedQuad(last, quad, instance);
                }
            }
        });
    //?} else {
    /*private static void outline(PoseStack pose, MultiBufferSource buffers, BlockPos pos) {
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(pos).deflate(INSET),
                (OCCUPIED >> 16 & 0xFF) / 255.0F, (OCCUPIED >> 8 & 0xFF) / 255.0F, (OCCUPIED & 0xFF) / 255.0F, 0.9F);
    }

    /^* The block's model at {@code pos}, translucent and fully lit. ^/
    private static void ghost(PoseStack pose, MultiBufferSource buffers, BlockState state, BlockPos pos, float alpha) {
        VertexConsumer translucent = new Translucent(buffers.getBuffer(Sheets.translucentCullBlockSheet()), alpha);
        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, type -> translucent,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
    }

    /^* Scales the alpha of every vertex. ^/
    private record Translucent(VertexConsumer inner, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int a) {
            inner.setColor(red, green, blue, Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    *///?}
    }
}
