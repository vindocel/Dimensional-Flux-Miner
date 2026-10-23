package dimensionalfluxminer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerStructure;
import dimensionalfluxminer.network.MinerAssemblyPayload;
import dimensionalfluxminer.registry.DfmBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
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

    /** A ghost block (alpha above 0) or a red outline (alpha 0) to draw this frame. */
    private record Mark(BlockPos pos, BlockState state, float alpha) {
    }

    /** What the frame being drawn shows, found while the level is extracted. */
    private static List<Mark> marks = List.of();
    private static Vec3 camera = Vec3.ZERO;

    @SubscribeEvent
    static void onExtract(ExtractLevelRenderStateEvent event) {
        marks = List.of();
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
        camera = event.getCamera().position();
        List<Mark> found = new ArrayList<>();
        collectHighlight(level, found);
        if (holding && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            collectHandPreview(level, player, hit, found);
        }
        marks = found;
    }

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
                collector.submitShapeOutline(pose, Shapes.create(new AABB(BlockPos.ZERO).deflate(INSET)), RenderTypes.lines(),
                        0xE6000000 | OCCUPIED, 2.0F, true);
            } else {
                ghost(pose, collector, mark.state(), mark.alpha());
            }
            pose.popPose();
        }
    }

    private static void collectHighlight(ClientLevel level, List<Mark> found) {
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
                found.add(new Mark(entry.pos(), state, 0));
                continue;
            }
            found.add(new Mark(entry.pos(), block.defaultBlockState(), next ? pulse : GHOST_ALPHA));
            next = false;
        }
        if (!anyMissing) {
            clear();
        }
    }

    /** The whole machine where a terminal placed on {@code hit} would build it (the terminal faces the player). */
    private static void collectHandPreview(ClientLevel level, LocalPlayer player, BlockHitResult hit, List<Mark> found) {
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
                found.add(new Mark(member.pos(), state, 0));
                continue;
            }
            BlockState ghost = block.defaultBlockState();
            if (ghost.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                ghost = ghost.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            found.add(new Mark(member.pos(), ghost, GHOST_ALPHA));
        }
    }

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
    }
}
