package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerVisualStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * Draws the formed machine once at the controller. Standby/unpowered stages use the static {@code states}
 * model; while really processing, the fixed {@code body} is drawn plus the outer cage, the counter-rotating
 * inner cage and the breathing energy core. Rotation speed follows five load stages (with hysteresis) and is
 * never above stage 5, whatever the external acceleration. The cages speed up from rest, leaning into their
 * tilt, and after processing ends they slow down and straighten out until they stop on the rest pose of the
 * static model (straight cages, whole revolution).
 */
public class MinerRenderer implements BlockEntityRenderer<MinerControllerBlockEntity> {
    private static final String[] PARTS = {"states", "body", "outer", "inner", "energy"};
    private static final float[] STAGE_SPEED = {0.0F, 1.0F, 1.25F, 1.5F, 1.75F, 2.0F};
    private static final double HYSTERESIS = 0.03;
    /** Base outer cage revolution: 8 s = 160 ticks. */
    private static final float DEGREES_PER_TICK = 360.0F / 160.0F;
    private static final Vec PIVOT = new Vec(0.5, 1.5625, 0.5);

    private final Map<MinerControllerBlockEntity, Spin> spins = new WeakHashMap<>();

    public MinerRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static List<ResourceLocation> models() {
        List<ResourceLocation> list = new ArrayList<>();
        for (String part : PARTS) {
            for (MinerVisualStage stage : MinerVisualStage.values()) {
                list.add(model(part, stage));
            }
        }
        return list;
    }

    private static ResourceLocation model(String part, MinerVisualStage stage) {
        return DimensionalFluxMiner.id("block/dimensional_flux_miner/" + part + "/" + stage.key());
    }

    @Override
    public void render(MinerControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!be.formed() || be.getLevel() == null) {
            return;
        }
        MinerVisualStage stage = be.visualStage();
        Direction front = be.clientFront();
        BlockPos core = be.corePos();
        BlockPos origin = core.below().subtract(be.getBlockPos());
        // Sample the light at the center and in front of the terminal; the controller block itself may be dark.
        int coreLight = LevelRenderer.getLightColor(be.getLevel(), core);
        int frontLight = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(front));
        int fullLight = LightTexture.pack(
                Math.max(LightTexture.block(light), Math.max(LightTexture.block(coreLight), LightTexture.block(frontLight))),
                Math.max(LightTexture.sky(light), Math.max(LightTexture.sky(coreLight), LightTexture.sky(frontLight))));
        pose.pushPose();
        pose.translate(origin.getX(), origin.getY(), origin.getZ());
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-ModelRendering.blockstateY(front)));
        pose.translate(-0.5, 0, -0.5);
        // Spinning up while processing; once it stops, spinning down until the rest pose of the static model.
        Spin spin = spins.get(be);
        if (stage.processing() && spin == null) {
            spin = new Spin();
            spins.put(be, spin);
        }
        boolean moving = false;
        if (spin != null) {
            float now = be.getLevel().getGameTime() + partialTick;
            moving = spin.advance(stage.processing(), be.loadPercent() / 100.0, now);
            if (!moving) {
                spins.remove(be);
            }
        }
        ModelRendering.render(pose, buffers, ModelRendering.model(model(moving ? "body" : "states", stage)), fullLight, overlay);
        pose.popPose();

        if (moving) {
            float angle = (float) (spin.angle % 360.0);
            float seconds = (be.getLevel().getGameTime() - be.animationStartTick() + partialTick) / 20.0F;
            // The core breathes with the rotation: full at running speed, still at rest.
            float scale = 1.0F + (breathing(seconds) - 1.0F) * spin.intensity();
            // The cages lean into their running tilt as they speed up and straighten out as they stop.
            float tilt = spin.tilt();
            pose.pushPose();
            pose.translate(PIVOT.x, PIVOT.y, PIVOT.z);
            pose.mulPose(Axis.YP.rotationDegrees(movingPoseY(front)));
            renderMoving(pose, buffers, model("outer", stage), 35 * tilt, angle, 20 * tilt, 1.0F, fullLight, overlay);
            renderMoving(pose, buffers, model("inner", stage), -25 * tilt, -2 * angle, 45 * tilt, 1.0F, fullLight, overlay);
            if (stage.hasCore()) {
                renderMoving(pose, buffers, model("energy", stage), -25 * tilt, -2 * angle, 45 * tilt, scale, fullLight, overlay);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    /** OBJ moving parts are centered at zero; Euler ZYX order: Rz * Ry * Rx. */
    private static void renderMoving(PoseStack pose, MultiBufferSource buffers, ResourceLocation model, float rx, float ry, float rz,
                                     float scale, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(rz));
        pose.mulPose(Axis.YP.rotationDegrees(ry));
        pose.mulPose(Axis.XP.rotationDegrees(rx));
        pose.scale(scale, scale, scale);
        ModelRendering.render(pose, buffers, ModelRendering.model(model), light, overlay);
        pose.popPose();
    }

    /** Core breathing, 4 s period: 1 + (1.5 sin(90t) + 0.5 sin^2(90t)) / 16, t in seconds. */
    static float breathing(float seconds) {
        double s = Math.sin(Math.toRadians(seconds * 90.0));
        return (float) (1.0 + (1.5 * s + 0.5 * s * s) / 16.0);
    }

    /** Pose of the moving OBJ parts, whose native front is south. */
    private static float movingPoseY(Direction front) {
        return switch (front) {
            case EAST -> 90;
            case NORTH -> 180;
            case WEST -> -90;
            default -> 0;
        };
    }

    @Override
    public AABB getRenderBoundingBox(MinerControllerBlockEntity be) {
        return be.renderBounds();
    }

    @Override
    public boolean shouldRenderOffScreen(MinerControllerBlockEntity be) {
        return true;
    }

    private record Vec(double x, double y, double z) {
    }

    /**
     * Client-only rotation state. From rest the cages speed up at {@link #ACCELERATION} to the speed of the load
     * stage (smoothed with hysteresis). When processing ends they keep turning, then brake at the same rate so
     * they come to rest exactly on a whole revolution, the pose of the static model.
     */
    private static final class Spin {
        /** Degrees per tick squared: the base speed is reached in 2 s from rest. */
        private static final double ACCELERATION = DEGREES_PER_TICK / 40.0;

        int stage = 1;
        double angle;
        double speed;
        double restAngle = Double.NaN;
        float lastTime = Float.NaN;

        /** Advances the rotation; false once it is back at rest. */
        boolean advance(boolean processing, double load, float now) {
            float dt = Float.isNaN(lastTime) ? 0 : Math.max(0, Math.min(20, now - lastTime));
            lastTime = now;
            if (processing) {
                restAngle = Double.NaN;
                updateStage(load);
                double target = DEGREES_PER_TICK * STAGE_SPEED[stage];
                speed = speed < target ? Math.min(target, speed + ACCELERATION * dt) : Math.max(target, speed - ACCELERATION * dt);
                angle = (angle + speed * dt) % 360.0;
                return true;
            }
            if (Double.isNaN(restAngle)) {
                // The first whole revolution it can still brake smoothly into.
                restAngle = Math.ceil((angle + speed * speed / (2 * ACCELERATION) + 1.0E-3) / 360.0) * 360.0;
            }
            double remaining = restAngle - angle;
            // Keeps turning (at least at the base speed) until the braking curve that ends right on the rest pose.
            double cruise = Math.max(DEGREES_PER_TICK, Math.min(speed, DEGREES_PER_TICK * STAGE_SPEED[5]));
            double braking = Math.sqrt(2 * ACCELERATION * Math.max(0, remaining));
            double wanted = Math.min(cruise, braking);
            speed = speed < wanted ? Math.min(wanted, speed + ACCELERATION * dt) : wanted;
            double step = speed * dt;
            if (remaining <= 1.0E-3 || step >= remaining) {
                angle = 0;
                speed = 0;
                return false;
            }
            angle += step;
            return true;
        }

        /** Breathing strength of the core: 0 at rest, 1 from the base speed up. */
        float intensity() {
            return (float) Math.min(1.0, speed / DEGREES_PER_TICK);
        }

        /**
         * Share of the running tilt (eased): 0 at rest, where the cages stand straight as in the static model, and
         * 1 from the base speed up. Below the base speed the stop is the final braking curve onto the rest pose,
         * so the cages straighten out exactly as they land on it.
         */
        float tilt() {
            float s = intensity();
            return s * s * (3 - 2 * s);
        }

        private void updateStage(double load) {
            int target = load <= 0 ? 1 : Math.min(5, (int) Math.ceil(load / 0.2));
            if (target > stage && load > (stage * 0.2) + HYSTERESIS) {
                stage = target;
            } else if (target < stage && load < ((stage - 1) * 0.2) - HYSTERESIS) {
                stage = Math.max(1, target);
            }
        }
    }
}
