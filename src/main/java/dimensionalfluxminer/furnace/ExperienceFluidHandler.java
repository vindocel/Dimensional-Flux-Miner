package dimensionalfluxminer.furnace;

import dimensionalfluxminer.registry.DfmFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Output-only virtual fluid view over the Electric Furnace's stored XP ({@code #c:experience}, 1 XP = 20 mB).
 * There is no second tank: draining N mB subtracts exactly N * 50 milli-XP.
 */
public final class ExperienceFluidHandler implements IFluidHandler {
    private final StoredExperience experience;
    private final Runnable onChange;

    public ExperienceFluidHandler(StoredExperience experience, Runnable onChange) {
        this.experience = experience;
        this.onChange = onChange;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        long mb = experience.availableMillibuckets();
        return mb <= 0 ? FluidStack.EMPTY : new FluidStack(DfmFluids.LIQUID_EXPERIENCE.get(), (int) Math.min(Integer.MAX_VALUE, mb));
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.is(DfmFluids.LIQUID_EXPERIENCE.get())) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        long drained = experience.drain(maxDrain, action.simulate());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            onChange.run();
        }
        return new FluidStack(DfmFluids.LIQUID_EXPERIENCE.get(), (int) drained);
    }
}
