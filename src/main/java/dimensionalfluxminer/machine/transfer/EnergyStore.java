package dimensionalfluxminer.machine.transfer;

/** Energy view used inside the mod; {@link Transfer} maps it to and from the NeoForge energy capability. */
public interface EnergyStore {
    /** Returns the FE accepted. */
    int receiveEnergy(int toReceive, boolean simulate);

    /** Returns the FE given. */
    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
