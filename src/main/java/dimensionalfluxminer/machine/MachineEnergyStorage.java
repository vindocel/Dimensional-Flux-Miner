package dimensionalfluxminer.machine;

import dimensionalfluxminer.machine.math.DynamicEnergyBuffer;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Input-only NeoForge energy view of a {@link DynamicEnergyBuffer}; DFM machines never output FE. */
public final class MachineEnergyStorage implements IEnergyStorage {
    private final DynamicEnergyBuffer buffer;
    private final Runnable onChange;

    public MachineEnergyStorage(DynamicEnergyBuffer buffer, Runnable onChange) {
        this.buffer = buffer;
        this.onChange = onChange;
    }

    public DynamicEnergyBuffer buffer() {
        return buffer;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        long accepted = buffer.receive(toReceive, simulate);
        if (!simulate && accepted > 0) {
            onChange.run();
        }
        return (int) accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, buffer.stored());
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, buffer.capacity());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    /** Charges operational FE, keeping fractions so low rates and fractional costs are billed exactly over time. */
    public void consume(double fe) {
        if (!(fe > 0)) {
            return;
        }
        buffer.charge(fe);
        onChange.run();
    }
}
