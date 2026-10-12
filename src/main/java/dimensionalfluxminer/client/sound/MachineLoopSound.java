package dimensionalfluxminer.client.sound;

import java.util.function.BooleanSupplier;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** One looping, positional machine sound; it stops itself as soon as the machine leaves its active state. */
public class MachineLoopSound extends AbstractTickableSoundInstance {
    private final BooleanSupplier active;

    public MachineLoopSound(SoundEvent event, Vec3 position, BooleanSupplier active) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.active = active;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    @Override
    public void tick() {
        if (!active.getAsBoolean()) {
            stop();
        }
    }
}
