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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Draws the formed machine once at the controller. Standby/unpowered stages use the static {@code states}
 * model; while really processing, the fixed {@code body} is drawn plus the outer cage, the counter-rotating
 * inner cage and the breathing energy core. Rotation speed follows five load stages (with hysteresis) and is
 * never above stage 5, whatever the external acceleration, and a little faster at higher Flux Rates. The cages
 * speed up from rest, leaning into their tilt; after processing ends they slow down, straighten out and stop on
 * an aligned pose (square cages, every quarter turn).
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
        // Spinning up while processing; once it stops, the moving parts brake onto an aligned pose.
        Spin spin = spins.get(be);
        if (stage.processing() && spin == null) {
            spin = new Spin();
            spins.put(be, spin);
        }
        boolean moving = false;
        if (spin != null) {
            float now = be.getLevel().getGameTime() + partialTick;
            double wanted = 0;
            if (stage.processing()) {
                spin.updateStage(be.loadPercent() / 100.0);
                wanted = DEGREES_PER_TICK * STAGE_SPEED[spin.stage] * MinerVisualStage.rateSpeed(be.rate());
            }
            spin.advance(wanted, now);
            if (!stage.processing() && spin.atRestPose()) {
                spins.remove(be);
            } else {
                moving = true;
            }
        }
        ModelRendering.render(pose, buffers, ModelRendering.model(model(moving ? "body" : "states", stage)), fullLight, overlay);
        pose.popPose();
        // Configured ports of the lower layer, drawn in world axes over the casing.
        MinerPortRenderer.render(pose, buffers, be.portLooks(), be.nodeColors(), front, fullLight);

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
     * Client-only rotation state, driven like a magnetic motor: every change of target speed (start, a new Rate, a new
     * load stage) is covered at a constant acceleration in a short time that grows a little with the size of the
     * change, never instant and never sluggish. To stop, it keeps its pace (at least the base speed) and then brakes
     * at a constant rate so that it lands exactly on the next aligned pose (the cages are square: every quarter turn).
     */
    private static final class Spin {
        /** Shortest and longest time (ticks) of a change of speed while running. */
        private static final double RAMP_MIN_TICKS = 15;
        private static final double RAMP_MAX_TICKS = 40;
        /** Start from rest: the base speed in 40 ticks, then 10 more ticks per extra base speed, at most 5.5 s. */
        private static final double START_MIN_TICKS = 40;
        private static final double START_MAX_TICKS = 110;
        /** Shortest and longest braking time (ticks) of a stop. */
        private static final double STOP_MIN_TICKS = 30;
        private static final double STOP_MAX_TICKS = 70;
        /**
         * Starts and stops never accelerate harder than at this Flux Rate: faster Rates reach their speed (and come to
         * rest) with the same feel, just a little later.
         */
        private static final int GENTLEST_RATE = 6;
        /** The cages look the same every quarter turn; a stop always lands on one of these poses. */
        private static final double ALIGNED_STEP = 90.0;

        int stage = 1;
        double angle;
        double speed;
        double target;
        double acceleration;
        /** Angle where the current stop lands (NaN while running). */
        double restAngle = Double.NaN;
        double cruise;
        float lastTime = Float.NaN;

        /** Moves the speed toward {@code wanted} (0 to stop on an aligned pose) and turns the cages. */
        void advance(double wanted, float now) {
            float dt = Float.isNaN(lastTime) ? 0 : Math.max(0, Math.min(20, now - lastTime));
            lastTime = now;
            if (wanted > 0) {
                restAngle = Double.NaN;
                if (Math.abs(wanted - target) > 1.0E-6 || (acceleration == 0 && speed != wanted)) {
                    target = wanted;
                    double change = Math.abs(wanted - speed);
                    // From (almost) rest the motor spins up gently, more slowly the faster it has to go.
                    if (speed < DEGREES_PER_TICK * 0.25 && wanted > speed) {
                        acceleration = Math.min(startAcceleration(change), startAcceleration(gentlestSpeed()));
                    } else {
                        acceleration = change / Mth.clamp(RAMP_MIN_TICKS + 10 * change / DEGREES_PER_TICK, RAMP_MIN_TICKS, RAMP_MAX_TICKS);
                    }
                }
                speed = speed < target ? Math.min(target, speed + acceleration * dt) : Math.max(target, speed - acceleration * dt);
                angle = (angle + speed * dt) % 360.0;
                return;
            }
            if (speed == 0 && Double.isNaN(restAngle)) {
                return;
            }
            if (Double.isNaN(restAngle)) {
                // Plan the stop: brake in a comfortable time and land on the first aligned pose past that distance.
                target = 0;
                cruise = Math.max(speed, DEGREES_PER_TICK);
                acceleration = Math.min(stopAcceleration(cruise), stopAcceleration(Math.max(gentlestSpeed(), DEGREES_PER_TICK)));
                double braking = cruise * cruise / (2 * acceleration);
                restAngle = Math.ceil((angle + braking + 1.0E-3) / ALIGNED_STEP) * ALIGNED_STEP;
            }
            double remaining = restAngle - angle;
            // Keep the pace until the constant braking curve that ends right on the aligned pose, then follow it.
            double wantedSpeed = Math.min(cruise, Math.sqrt(2 * acceleration * Math.max(0, remaining)));
            speed = speed < wantedSpeed ? Math.min(wantedSpeed, speed + acceleration * dt) : wantedSpeed;
            double step = speed * dt;
            if (remaining <= 1.0E-3 || step >= remaining) {
                angle = restAngle % 360.0;
                speed = 0;
                restAngle = Double.NaN;
                return;
            }
            angle += step;
        }

        /** Running speed of the gentlest Rate at the current load stage. */
        private double gentlestSpeed() {
            return DEGREES_PER_TICK * STAGE_SPEED[stage] * MinerVisualStage.rateSpeed(GENTLEST_RATE);
        }

        /** Acceleration of a start from rest to {@code speed}. */
        private static double startAcceleration(double speed) {
            return speed / Mth.clamp(START_MIN_TICKS + 10 * (speed / DEGREES_PER_TICK - 1), START_MIN_TICKS, START_MAX_TICKS);
        }

        /** Deceleration of a stop from {@code speed}. */
        private static double stopAcceleration(double speed) {
            double ticks = Mth.clamp(STOP_MIN_TICKS + 10 * speed / DEGREES_PER_TICK, STOP_MIN_TICKS, STOP_MAX_TICKS);
            return Math.max(speed / ticks, DEGREES_PER_TICK / STOP_MIN_TICKS);
        }

        /** Stopped right on the rest pose of the static model. */
        boolean atRestPose() {
            double a = ((angle % 360.0) + 360.0) % 360.0;
            return speed == 0 && Double.isNaN(restAngle) && (a < 1.0E-3 || a > 360.0 - 1.0E-3);
        }
        /** Breathing strength of the core: 0 at rest, 1 from the base speed up. */
        float intensity() {
            return (float) Math.min(1.0, speed / DEGREES_PER_TICK);
        }

        /**
         * Share of the running tilt (eased): 0 at rest, where the cages stand straight as in the static model, and
         * 1 from the base speed up, so the cages straighten out as they stop.
         */
        float tilt() {
            float s = intensity();
            return s * s * (3 - 2 * s);
        }

        void updateStage(double load) {
            int wanted = load <= 0 ? 1 : Math.min(5, (int) Math.ceil(load / 0.2));
            if (wanted > stage && load > (stage * 0.2) + HYSTERESIS) {
                stage = wanted;
            } else if (wanted < stage && load < ((stage - 1) * 0.2) - HYSTERESIS) {
                stage = Math.max(1, wanted);
            }
        }
    }
}
