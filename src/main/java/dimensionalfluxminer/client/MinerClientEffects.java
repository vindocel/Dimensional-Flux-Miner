package dimensionalfluxminer.client;

import dimensionalfluxminer.client.sound.MachineSounds;
import dimensionalfluxminer.content.CoreFluxOptions;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerVisualStage;
import dimensionalfluxminer.registry.DfmParticles;
import dimensionalfluxminer.registry.DfmSounds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Client-side Miner effects: the single running loop, the core's pull and the bounded formation burst. */
public final class MinerClientEffects {
    /** Particles per participant position; the burst happens once per real formation. */
    private static final int PARTICLES_PER_BLOCK = 6;
    /** Chance per tick of a new mote while the Miner works (about 11 per second). */
    private static final float MOTE_CHANCE = 0.55F;
    /** Motes appear in the inner space, this far from the center of the core. */
    private static final double MOTE_MIN_DISTANCE = 0.75;
    private static final double MOTE_MAX_DISTANCE = 1.2;
    /** Center of the energy core relative to its block (the renderer's pivot). */
    private static final double CORE_CENTER_Y = 0.5625;

    private MinerClientEffects() {
    }

    public static void tick(BlockEntity be) {
        runningSound(be);
        coreFlux(be);
    }

    public static void runningSound(BlockEntity be) {
        if (be instanceof MinerControllerBlockEntity miner && miner.formed()) {
            MachineSounds.loop(be, DfmSounds.MINER_RUNNING.get(), Vec3.atCenterOf(miner.corePos()),
                    () -> !be.isRemoved() && miner.formed() && miner.visualStage().processing());
        }
    }

    /**
     * While the Miner works, motes appear in the space around the core and are pulled into it: blue for the Basic
     * Core, violet for the Stabilized Core. Both speed up a little with the Flux Rate.
     */
    private static void coreFlux(BlockEntity be) {
        if (!(be instanceof MinerControllerBlockEntity miner) || !miner.formed() || be.getLevel() == null) {
            return;
        }
        MinerVisualStage stage = miner.visualStage();
        ParticleType<CoreFluxOptions> type = switch (stage) {
            case ENERGY_CORE_BASIC -> DfmParticles.CORE_FLUX_BASIC.get();
            case ENERGY_CORE_STABILIZED -> DfmParticles.CORE_FLUX_STABILIZED.get();
            default -> null;
        };
        RandomSource random = be.getLevel().getRandom();
        // Higher Rates pull faster and more often; each mote lives shorter, so the cloud keeps its density.
        float speed = MinerVisualStage.rateSpeed(miner.rate());
        if (type == null) {
            return;
        }
        // Whole motes this tick plus one more by chance for the fraction.
        float expected = MOTE_CHANCE * speed;
        int motes = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
        for (int i = 0; i < motes; i++) {
            spawnMote(be, miner, type, speed, random);
        }
    }

    private static void spawnMote(BlockEntity be, MinerControllerBlockEntity miner, ParticleType<CoreFluxOptions> type, float speed,
                                  RandomSource random) {
        BlockPos core = miner.corePos();
        Vec3 center = new Vec3(core.getX() + 0.5, core.getY() + CORE_CENTER_Y, core.getZ() + 0.5);
        // A uniform direction on the sphere, at a random distance inside the machine.
        double y = random.nextDouble() * 2 - 1;
        double angle = random.nextDouble() * Math.PI * 2;
        double ring = Math.sqrt(1 - y * y);
        double distance = MOTE_MIN_DISTANCE + random.nextDouble() * (MOTE_MAX_DISTANCE - MOTE_MIN_DISTANCE);
        Vec3 offset = new Vec3(ring * Math.cos(angle), y, ring * Math.sin(angle)).scale(distance);
        Vec3 start = center.add(offset);
        be.getLevel().addParticle(new CoreFluxOptions(type, speed), start.x, start.y, start.z, -offset.x, -offset.y, -offset.z);
    }

    public static void formationBurst(List<BlockPos> positions) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        for (BlockPos pos : positions) {
            for (int i = 0; i < PARTICLES_PER_BLOCK; i++) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + random.nextDouble();
                double z = pos.getZ() + random.nextDouble();
                // Snap one axis to a block boundary so sparks outline the assembled blocks.
                switch (random.nextInt(3)) {
                    case 0 -> x = pos.getX() + (random.nextBoolean() ? 0.0 : 1.0);
                    case 1 -> y = pos.getY() + (random.nextBoolean() ? 0.0 : 1.0);
                    default -> z = pos.getZ() + (random.nextBoolean() ? 0.0 : 1.0);
                }
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.02, 0);
            }
        }
    }
}
