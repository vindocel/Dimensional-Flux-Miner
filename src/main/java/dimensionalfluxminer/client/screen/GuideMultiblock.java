package dimensionalfluxminer.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.client.render.ModelRendering;
import dimensionalfluxminer.miner.MinerLayout;
import dimensionalfluxminer.miner.MinerStructure;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Interactive 3D view of the Dimensional Flux Miner assembly for the guide: drag to turn it, show one layer at a
 * time, switch between the loose parts and the formed machine. The parts list highlights one part type on hover.
 */
final class GuideMultiblock {
    private static final ResourceLocation FORMED = DimensionalFluxMiner.id("block/dimensional_flux_miner/states/energy_core_basic_idle");
    private static final int VIEW_HEIGHT = 130;
    private static final int WINDOW = 0xFF343442;
    private static final int WINDOW_EDGE = 0xFF22232F;
    private static final int WINDOW_TEXT = 0xFFB5AEB8;
    private static final int BUTTON_H = 14;

    /** View state, kept while the game runs. */
    private static float yaw = 35;
    private static float pitch = 28;
    private static boolean autoRotate = true;
    private static boolean formed;
    /** 0 = every layer, 1..3 = bottom, middle, top. */
    private static int layer;
    private static long lastFrame;
    /** Part type hovered in the parts list (only it is drawn), or null. */
    private static MinerLayout.Part highlight;

    private GuideMultiblock() {
    }

    /** The 3D viewer with its buttons. */
    static final class Viewer implements GuideElement {
        private boolean dragging;

        @Override
        public int height(GuideScreen screen, int width) {
            return VIEW_HEIGHT;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int viewH = VIEW_HEIGHT - BUTTON_H - 4;
            // A small dark display set into the page.
            g.fill(x, y, x + width, y + viewH, WINDOW_EDGE);
            g.fill(x + 1, y + 1, x + width - 1, y + viewH - 1, WINDOW);

            long now = Util.getMillis();
            if (lastFrame != 0 && autoRotate && !dragging) {
                yaw = (yaw + (now - lastFrame) * 0.02F) % 360.0F;
            }
            lastFrame = now;

            if (!screen.turning()) {
                g.enableScissor(x + 1, y + 1, x + width - 1, y + viewH - 1);
                renderStructure(g, x + width / 2.0F, y + viewH / 2.0F + 4, Math.min(width, viewH) / 4.6F);
                g.disableScissor();
            }

            Component hint = Component.translatable("guide.dimensionalfluxminer.viewer.drag");
            screen.fittedText(g, hint, x + 4, y + 4, width - 8, WINDOW_TEXT);

            int by = y + viewH + 4;
            int bw = (width - 8) / 3;
            screen.textButton(g, x, by, bw, BUTTON_H, layerLabel(), mouseX, mouseY, !formed);
            screen.textButton(g, x + bw + 4, by, bw, BUTTON_H, Component.translatable(formed
                    ? "guide.dimensionalfluxminer.viewer.formed" : "guide.dimensionalfluxminer.viewer.parts"), mouseX, mouseY, true);
            screen.textButton(g, x + 2 * (bw + 4), by, width - 2 * (bw + 4), BUTTON_H, Component.translatable(autoRotate
                    ? "guide.dimensionalfluxminer.viewer.rotate_on" : "guide.dimensionalfluxminer.viewer.rotate_off"), mouseX, mouseY, true);
        }

        private static Component layerLabel() {
            return layer == 0 ? Component.translatable("guide.dimensionalfluxminer.viewer.layer_all")
                    : Component.translatable("guide.dimensionalfluxminer.viewer.layer", layer);
        }

        @Override
        public boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
            int viewH = VIEW_HEIGHT - BUTTON_H - 4;
            int by = y + viewH + 4;
            int bw = (width - 8) / 3;
            if (mouseY >= by && mouseY < by + BUTTON_H) {
                if (mouseX >= x && mouseX < x + bw && !formed) {
                    layer = button == 1 ? (layer + 3) % 4 : (layer + 1) % 4;
                } else if (mouseX >= x + bw + 4 && mouseX < x + 2 * bw + 4) {
                    formed = !formed;
                } else if (mouseX >= x + 2 * (bw + 4) && mouseX < x + width) {
                    autoRotate = !autoRotate;
                } else {
                    return false;
                }
                screen.playClick();
                return true;
            }
            if (mouseY >= y && mouseY < y + viewH && mouseX >= x && mouseX < x + width) {
                dragging = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean drag(GuideScreen screen, double dragX, double dragY) {
            if (!dragging) {
                return false;
            }
            yaw = (float) ((yaw + dragX * 1.5) % 360.0);
            pitch = Mth.clamp((float) (pitch + dragY * 1.2), -10.0F, 85.0F);
            return true;
        }

        void release() {
            dragging = false;
        }
    }

    private static void renderStructure(GuiGraphics g, float centerX, float centerY, float scale) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 200);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupFor3DItems();
        MultiBufferSource.BufferSource buffers = g.bufferSource();
        if (formed) {
            // The formed model's origin is the block under the core; its native front is north.
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(Direction.SOUTH)));
            pose.translate(-0.5, -1.5, -0.5);
            ModelRendering.render(pose, buffers, ModelRendering.model(FORMED), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        } else {
            var dispatcher = Minecraft.getInstance().getBlockRenderer();
            for (MinerLayout.Slot slot : MinerLayout.BASE_SLOTS) {
                if (layer != 0 && slot.dy() + 2 != layer) {
                    continue;
                }
                if (highlight != null && slot.part() != highlight) {
                    continue;
                }
                pose.pushPose();
                pose.translate(slot.dx() - 0.5, slot.dy() - 0.5, slot.dz() - 0.5);
                dispatcher.renderSingleBlock(state(slot.part()), pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        buffers.endBatch();
        pose.popPose();
    }

    private static BlockState state(MinerLayout.Part part) {
        BlockState state = MinerStructure.blockFor(part).defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            // The terminal faces the viewer (south), like the base layout.
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }
        return state;
    }

    /** One row per part type: icon, count, name and where it goes; hovering a row shows only that part. */
    static final class PartList implements GuideElement {
        private static final int ROW = 24;
        private static final MinerLayout.Part[] ORDER = MinerLayout.Part.values();

        @Override
        public int height(GuideScreen screen, int width) {
            return ORDER.length * ROW;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            MinerLayout.Part hovered = null;
            for (int i = 0; i < ORDER.length; i++) {
                MinerLayout.Part part = ORDER[i];
                int ry = y + i * ROW;
                boolean over = mouseX >= x && mouseX < x + width && mouseY >= ry && mouseY < ry + ROW - 2;
                if (over) {
                    hovered = part;
                    g.fill(x, ry - 1, x + width, ry + ROW - 3, 0x2052D9DB);
                }
                ItemStack stack = new ItemStack(MinerStructure.blockFor(part));
                screen.itemSlot(g, stack, x, ry, -1, -1);
                Component name = Component.literal(MinerLayout.count(part) + "× ").append(stack.getHoverName());
                screen.fittedText(g, name, x + 24, ry + 1, width - 26, GuideElements.HEADING);
                screen.fittedText(g, Component.translatable("guide.dimensionalfluxminer.part." + part.name().toLowerCase(java.util.Locale.ROOT)),
                        x + 24, ry + 12, width - 26, GuideElements.BODY);
            }
            highlight = hovered;
            listDrawn = true;
        }
    }

    /** Whether the parts list was drawn this frame; a list scrolled out of view highlights nothing. */
    private static boolean listDrawn;

    static void endFrame() {
        if (!listDrawn) {
            highlight = null;
        }
        listDrawn = false;
    }

    /** Clears the hover highlight when the page changes. */
    static void reset() {
        highlight = null;
        lastFrame = 0;
    }

    static List<GuideElement> elements() {
        return List.of(new Viewer(), new PartList());
    }
}
