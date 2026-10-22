package dimensionalfluxminer.client.sound;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * One looping, positional machine sound; it stops itself as soon as the machine leaves its active state. Its volume
 * follows the machine's own volume setting every tick (it keeps playing silently while muted).
 */
public class MachineLoopSound extends AbstractTickableSoundInstance {
    private final BooleanSupplier active;
    private final DoubleSupplier machineVolume;

    public MachineLoopSound(SoundEvent event, Vec3 position, BooleanSupplier active, DoubleSupplier machineVolume) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.active = active;
        this.machineVolume = machineVolume;
        this.looping = true;
        this.delay = 0;
        this.volume = (float) machineVolume.getAsDouble();
        this.pitch = 1.0F;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    @Override
    public void tick() {
        if (!active.getAsBoolean()) {
            stop();
            return;
        }
        volume = (float) machineVolume.getAsDouble();
    }

    /** A muted machine still starts its loop, so raising the volume brings the sound back at once. */
    @Override
    public boolean canStartSilent() {
        return true;
    }
}
