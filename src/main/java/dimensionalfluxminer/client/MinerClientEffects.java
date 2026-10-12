package dimensionalfluxminer.client;

import dimensionalfluxminer.client.sound.MachineSounds;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.registry.DfmSounds;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Client-side Miner effects: the single running loop and the bounded formation burst. */
public final class MinerClientEffects {
    /** Particles per participant position; the burst happens once per real formation. */
    private static final int PARTICLES_PER_BLOCK = 6;

    private MinerClientEffects() {
    }

    public static void runningSound(BlockEntity be) {
        if (be instanceof MinerControllerBlockEntity miner && miner.formed()) {
            MachineSounds.loop(be, DfmSounds.MINER_RUNNING.get(), Vec3.atCenterOf(miner.corePos()),
                    () -> !be.isRemoved() && miner.formed() && miner.visualStage().processing());
        }
    }

    public static void formationBurst(List<BlockPos> positions) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
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
