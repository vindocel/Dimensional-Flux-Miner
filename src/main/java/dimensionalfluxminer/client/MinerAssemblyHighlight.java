package dimensionalfluxminer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerStructure;
import dimensionalfluxminer.network.MinerAssemblyPayload;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

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

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
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
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        renderHighlight(level, pose, buffers);
        if (holding && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            renderHandPreview(level, player, hit, pose, buffers);
        }
        pose.popPose();
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.lines());
    }

    private static void renderHighlight(ClientLevel level, PoseStack pose, MultiBufferSource.BufferSource buffers) {
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
                outline(pose, buffers, entry.pos());
                continue;
            }
            ghost(pose, buffers, block.defaultBlockState(), entry.pos(), next ? pulse : GHOST_ALPHA);
            next = false;
        }
        if (!anyMissing) {
            clear();
        }
    }

    /** The whole machine where a terminal placed on {@code hit} would build it (the terminal faces the player). */
    private static void renderHandPreview(ClientLevel level, LocalPlayer player, BlockHitResult hit, PoseStack pose,
                                          MultiBufferSource.BufferSource buffers) {
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
                outline(pose, buffers, member.pos());
                continue;
            }
            BlockState ghost = block.defaultBlockState();
            if (ghost.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                ghost = ghost.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            ghost(pose, buffers, ghost, member.pos(), GHOST_ALPHA);
        }
    }

    private static void outline(PoseStack pose, MultiBufferSource buffers, BlockPos pos) {
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(pos).deflate(INSET),
                (OCCUPIED >> 16 & 0xFF) / 255.0F, (OCCUPIED >> 8 & 0xFF) / 255.0F, (OCCUPIED & 0xFF) / 255.0F, 0.9F);
    }

    /** The block's model at {@code pos}, translucent and fully lit. */
    private static void ghost(PoseStack pose, MultiBufferSource buffers, BlockState state, BlockPos pos, float alpha) {
        VertexConsumer translucent = new Translucent(buffers.getBuffer(Sheets.translucentCullBlockSheet()), alpha);
        pose.pushPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, type -> translucent,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
    }

    /** Scales the alpha of every vertex. */
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
    }
}
