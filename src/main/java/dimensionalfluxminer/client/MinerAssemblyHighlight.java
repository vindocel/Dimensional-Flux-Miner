package dimensionalfluxminer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerStructure;
import dimensionalfluxminer.network.MinerAssemblyPayload;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Outlines where the parts still missing in a Miner assembly go, colored by part; red where another block is in the
 * way. The next part to place pulses. Positions filled in the meantime disappear; the highlight fades out after a
 * while and is cleared when the machine forms.
 */
@EventBusSubscriber(modid = DimensionalFluxMiner.MOD_ID, value = Dist.CLIENT)
public final class MinerAssemblyHighlight {
    private static final long DURATION_MS = 30_000;
    private static final int OCCUPIED = 0xFF3B3B;
    private static final double INSET = 0.02;

    private static List<MinerAssemblyPayload.Entry> entries = List.of();
    private static long shownAt;

    private MinerAssemblyHighlight() {
    }

    public static void show(List<MinerAssemblyPayload.Entry> missing) {
        entries = List.copyOf(missing);
        shownAt = Util.getMillis();
    }

    private static int color(Part part) {
        return switch (part) {
            case STEEL_CHASSIS -> 0xA9B4C8;
            case VENTILATION_UNIT -> 0x4FC3F7;
            case REINFORCED_STRUCTURAL_GLASS -> 0xC8F0FF;
            case DIMENSIONAL_PORT -> 0xFFA726;
            case QUANTUM_CORE -> 0xC77DFF;
            case MINER_TERMINAL -> 0x66DD6A;
        };
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || entries.isEmpty()) {
            return;
        }
        long age = Util.getMillis() - shownAt;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || age > DURATION_MS) {
            entries = List.of();
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float pulse = 0.35F + 0.65F * (float) (0.5 + 0.5 * Math.sin(Util.getMillis() / 150.0));
        boolean next = true;
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (MinerAssemblyPayload.Entry entry : entries) {
            Part part = Part.values()[entry.part()];
            BlockState state = level.getBlockState(entry.pos());
            if (state.is(MinerStructure.blockFor(part))) {
                continue;
            }
            boolean occupied = !state.canBeReplaced();
            int rgb = occupied ? OCCUPIED : color(part);
            float alpha = next && !occupied ? pulse : 0.9F;
            next &= occupied;
            AABB box = new AABB(entry.pos()).deflate(INSET);
            LevelRenderer.renderLineBox(pose, lines, box, (rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F, alpha);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
