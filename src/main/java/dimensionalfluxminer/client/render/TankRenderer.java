package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.tank.DimensionalTankBlockEntity;
import dimensionalfluxminer.tank.TankCode;
import dimensionalfluxminer.tank.TankKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Dynamic parts of the Dimensional Tanks, drawn behind the cut-outs of the static model: the liquid volume of each
 * chamber, its gauge, the three code colors of the front panel and, on the dual tank, the two lid stripes with the
 * liquid of A and of B. Coordinates are those of the final models (0..16, native front north), rotated with the
 * facing around the block center.
 */
public class TankRenderer implements BlockEntityRenderer<DimensionalTankBlockEntity, TankRenderer.State> {
    private static final Identifier SOLID = DimensionalFluxMiner.id("block/dimensional_tank/solid_white");
    /** Lid stripe of an empty chamber. */
    private static final int EMPTY_STRIPE = 0xFF7A7A7A;

    /** A chamber: liquid volume (x0, y0, z0, x1, yMax, z1) and gauge fill (x0, y0, x1, yMax, z) at 100%. */
    private record Chamber(float[] volume, float[] gauge) {
    }

    /**
     * One front panel field (x0, y0, x1, y1, z) per code position, left to right; the same code on the lid panel
     * (x0, z0, x1, z1 at {@link #LID_CODE_Y}), left to right for a player at the front.
     */
    private record Layout(Chamber[] chambers, float[][] code, float[][] lidCode, float[][] stripes) {
    }

    /** The lid code panel's face is at Y 16.03; its fields are drawn just under it, above the lid (Y 16). */
    private static final float LID_CODE_Y = 16.02F;

    /** Top of the liquid at 100%: just under the window's upper edge. */
    private static final float LIQUID_TOP = 13.25F;
    private static final float GAUGE_BOTTOM = 4.213889F;
    private static final float GAUGE_TOP = 12.436111F;

    private static final Layout SINGLE = new Layout(
            new Chamber[] {new Chamber(new float[] {2.3F, 2.5F, 2.3F, 13.7F, LIQUID_TOP, 13.7F},
                    new float[] {12.8125F, GAUGE_BOTTOM, 13.154167F, GAUGE_TOP, 0.7385F})},
            new float[][] {
                    {4.496875F, 3.0625F, 5.153125F, 4.6875F, 0.1885F},
                    {3.621875F, 3.0625F, 4.278125F, 4.6875F, 0.1885F},
                    {2.746875F, 3.0625F, 3.403125F, 4.6875F, 0.1885F}},
            // Lid panel x 6.25..9.75, z 6.375..9.625, texture turned 180 degrees to read from the front.
            new float[][] {
                    {8.546875F, 7.1875F, 9.203125F, 8.8125F},
                    {7.671875F, 7.1875F, 8.328125F, 8.8125F},
                    {6.796875F, 7.1875F, 7.453125F, 8.8125F}},
            new float[0][]);

    private static final Layout DUAL = new Layout(
            new Chamber[] {
                    new Chamber(new float[] {2.3F, 2.5F, 2.3F, 7.65F, LIQUID_TOP, 13.7F},
                            new float[] {2.845833F, GAUGE_BOTTOM, 3.1875F, GAUGE_TOP, 0.7385F}),
                    new Chamber(new float[] {8.35F, 2.5F, 2.3F, 13.7F, LIQUID_TOP, 13.7F},
                            new float[] {12.8125F, GAUGE_BOTTOM, 13.154167F, GAUGE_TOP, 0.7385F})},
            new float[][] {
                    {8.546875F, 3.0625F, 9.203125F, 4.6875F, 0.2385F},
                    {7.671875F, 3.0625F, 8.328125F, 4.6875F, 0.2385F},
                    {6.796875F, 3.0625F, 7.453125F, 4.6875F, 0.2385F}},
            // Lid panel between the stripes: x 6.5..9.5, z 6.607..9.393 (the front panel's proportions).
            new float[][] {
                    {8.46875F, 7.303571F, 9.03125F, 8.696429F},
                    {7.71875F, 7.303571F, 8.28125F, 8.696429F},
                    {6.96875F, 7.303571F, 7.53125F, 8.696429F}},
            // Lid stripes (x0, z0, x1, z1) on top, y = 15.999: A then B.
            new float[][] {
                    {6.03125F, 6.59375F, 6.3125F, 9.40625F},
                    {9.6875F, 6.59375F, 9.96875F, 9.40625F}});
    private static final float STRIPE_Y = 15.999F;

    public TankRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** What a frame needs of the tank, read on the client thread before drawing. */
    public static class State extends BlockEntityRenderState {
        TankKind kind = TankKind.SINGLE;
        Direction facing = Direction.NORTH;
        FluidStack[] chambers = new FluidStack[0];
        TankCode code = TankCode.DEFAULT;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DimensionalTankBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.kind = be.kind();
        state.facing = be.facing();
        state.chambers = new FluidStack[be.kind().chambers()];
        for (int i = 0; i < state.chambers.length; i++) {
            state.chambers[i] = be.chamber(i).copy();
        }
        state.code = be.code();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Layout layout = state.kind == TankKind.DUAL ? DUAL : SINGLE;
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5F, 0, 0.5F);
        pose.rotate(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(state.facing)));
        pose.translate(-0.5F, 0, -0.5F);
        pose.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
        collector.submitCustomGeometry(pose, RenderTypes.translucentMovingBlock(), (last, translucent) -> draw(state, layout, light, last, translucent));
        pose.popPose();
    }

    private static void draw(State state, Layout layout, int light, PoseStack.Pose last, VertexConsumer translucent) {
        TextureAtlasSprite solid = sprite(SOLID);
        for (int i = 0; i < layout.chambers().length; i++) {
            FluidStack stack = i < state.chambers.length ? state.chambers[i] : FluidStack.EMPTY;
            Chamber chamber = layout.chambers()[i];
            if (stack.isEmpty()) {
                if (i < layout.stripes().length) {
                    stripe(last, translucent, layout.stripes()[i], solid, EMPTY_STRIPE, light);
                }
                continue;
            }
            float fraction = Mth.clamp(stack.getAmount() / (float) TankKind.CHAMBER_CAPACITY, 0, 1);
            FluidModel fluid = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState());
            TextureAtlasSprite still = fluid.stillMaterial().sprite();
            int color = fluid.fluidTintSource() != null ? fluid.fluidTintSource().colorAsStack(stack) : 0xFFFFFFFF;
            int fluidLight = fluidLight(stack, light);
            float[] v = chamber.volume();
            float top = v[1] + fraction * (v[4] - v[1]);
            if (top > v[1]) {
                box(last, translucent, v[0], v[1], v[2], v[3], top, v[5], still, color, fluidLight);
            }
            // Gauge and lid stripe: the liquid's own color as a flat fill (average of its texture times its tint).
            int flat = FluidColors.color(still, color);
            float[] g = chamber.gauge();
            float gaugeTop = g[1] + fraction * (g[3] - g[1]);
            if (gaugeTop > g[1]) {
                face(last, translucent, Direction.NORTH, g[0], g[1], g[4], g[2], gaugeTop, g[4], solid, flat, fluidLight);
            }
            if (i < layout.stripes().length) {
                stripe(last, translucent, layout.stripes()[i], solid, flat, fluidLight);
            }
        }

        // The dimensional address, left to right as seen from the front; it stays visible with empty chambers.
        TankCode code = state.code;
        for (int p = 0; p < TankCode.POSITIONS; p++) {
            float[] f = layout.code()[p];
            int color = 0xFF000000 | code.get(p).getTextureDiffuseColor();
            face(last, translucent, Direction.NORTH, f[0], f[1], f[4], f[2], f[3], f[4], solid, color, light);
            float[] l = layout.lidCode()[p];
            face(last, translucent, Direction.UP, l[0], LID_CODE_Y, l[1], l[2], LID_CODE_Y, l[3], solid, color, light);
        }
    }

    private static void stripe(PoseStack.Pose pose, VertexConsumer consumer, float[] s, TextureAtlasSprite sprite, int color, int light) {
        face(pose, consumer, Direction.UP, s[0], STRIPE_Y, s[1], s[2], STRIPE_Y, s[3], sprite, color, light);
    }

    private static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(id);
    }

    /** Liquids that glow (lava) light themselves. */
    private static int fluidLight(FluidStack stack, int light) {
        int own = stack.getFluid().getFluidType().getLightLevel(stack);
        return own <= 0 ? light : LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(light), own), LightCoordsUtil.sky(light));
    }

    private static void box(PoseStack.Pose pose, VertexConsumer consumer, float x0, float y0, float z0, float x1, float y1, float z1,
                            TextureAtlasSprite sprite, int color, int light) {
        for (Direction direction : Direction.values()) {
            face(pose, consumer, direction, x0, y0, z0, x1, y1, z1, sprite, color, light);
        }
    }

    /**
     * One face of the box (model units) facing {@code direction}, counter-clockwise seen from outside. The sprite is
     * laid at one model unit per texel, so it repeats its own pixels instead of stretching them.
     */
    private static void face(PoseStack.Pose last, VertexConsumer consumer, Direction direction, float x0, float y0, float z0, float x1, float y1,
                             float z1, TextureAtlasSprite sprite, int color, int light) {
        float[][] corners = switch (direction) {
            case DOWN -> new float[][] {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case UP -> new float[][] {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case NORTH -> new float[][] {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case SOUTH -> new float[][] {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case WEST -> new float[][] {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case EAST -> new float[][] {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
        };
        for (float[] c : corners) {
            float u;
            float v;
            switch (direction.getAxis()) {
                case Y -> {
                    u = c[0];
                    v = c[2];
                }
                case Z -> {
                    u = c[0];
                    v = 16 - c[1];
                }
                default -> {
                    u = c[2];
                    v = 16 - c[1];
                }
            }
            consumer.addVertex(last, c[0], c[1], c[2])
                    .setColor(color)
                    .setUv(sprite.getU(u / 16.0F), sprite.getV(v / 16.0F))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(last, direction.getStepX(), direction.getStepY(), direction.getStepZ());
        }
    }
}
