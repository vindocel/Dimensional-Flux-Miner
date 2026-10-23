package dimensionalfluxminer.machine.transfer;

import net.neoforged.neoforge.fluids.FluidStack;

/** Tank view used inside the mod (tank chambers, liquid experience); {@link Transfer} maps it to the NeoForge capability. */
public interface FluidTanks {
    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    boolean isFluidValid(int tank, FluidStack stack);

    /** Returns the amount accepted. */
    int fill(FluidStack resource, FluidAction action);

    /** Drains that fluid; returns what was drained. */
    FluidStack drain(FluidStack resource, FluidAction action);

    /** Drains whatever comes first; returns what was drained. */
    FluidStack drain(int maxDrain, FluidAction action);

    enum FluidAction {
        EXECUTE, SIMULATE;

        public boolean execute() {
            return this == EXECUTE;
        }

        public boolean simulate() {
            return this == SIMULATE;
        }
    }

    /** Moves up to {@code maxAmount} from {@code source} to {@code destination}; returns what moved. */
    static FluidStack move(FluidTanks destination, FluidTanks source, int maxAmount) {
        FluidStack drainable = source.drain(maxAmount, FluidAction.SIMULATE);
        if (drainable.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int fillable = destination.fill(drainable, FluidAction.SIMULATE);
        if (fillable <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = source.drain(drainable.copyWithAmount(fillable), FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int filled = destination.fill(drained, FluidAction.EXECUTE);
        return drained.copyWithAmount(filled);
    }
}
