package dimensionalfluxminer.client.sound;

import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.crusher.MechanicalCrusherBlockEntity;
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.registry.DfmSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps at most one looping sound instance per machine block entity. Extra logical cycles from tick
 * accelerators never create overlapping loops because the instance is keyed by the block entity.
 */
public final class MachineSounds {
    private static final Map<BlockEntity, MachineLoopSound> PLAYING = new WeakHashMap<>();

    private MachineSounds() {
    }

    public static void update(BlockEntity be) {
        if (!(be instanceof AbstractMachineBlockEntity machine)) {
            return;
        }
        SoundEvent event = machine instanceof MechanicalCrusherBlockEntity
                ? DfmSounds.CRUSHER_RUNNING.get()
                : DfmSounds.ALLOY_SMELTER_RUNNING.get();
        loop(be, event, Vec3.atCenterOf(be.getBlockPos()), () -> !be.isRemoved() && machine.machineState() == MachineState.ACTIVE);
    }

    /** Starts the loop for {@code key} when {@code active} holds and no instance is playing yet. */
    public static void loop(BlockEntity key, SoundEvent event, Vec3 position, java.util.function.BooleanSupplier active) {
        if (!active.getAsBoolean()) {
            return;
        }
        MachineLoopSound current = PLAYING.get(key);
        if (current != null && !current.isStopped()) {
            return;
        }
        MachineLoopSound sound = new MachineLoopSound(event, position, active);
        PLAYING.put(key, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
