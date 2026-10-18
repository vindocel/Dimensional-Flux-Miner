package dimensionalfluxminer.registry;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DfmSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DimensionalFluxMiner.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ALLOY_SMELTER_RUNNING = register("machine.alloy_smelter.running");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINER_RUNNING = register("machine.dimensional_flux_miner.running");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINER_POWER_ON = register("machine.dimensional_flux_miner.power_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUSHER_RUNNING = register("machine.mechanical_crusher.running");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(DimensionalFluxMiner.id(name)));
    }

    private DfmSounds() {
    }
}
