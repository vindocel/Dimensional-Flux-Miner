package dimensionalfluxminer.tank;

import java.util.function.Supplier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One chamber of a tank's channel as a fluid tank. It holds a single liquid (components included) up to
 * {@link TankKind#CHAMBER_CAPACITY}; a different liquid is refused while the chamber is not empty. Simulated calls
 * never change the channel. The channel is looked up on every call, so a code change takes effect at once.
 */
public final class ChamberFluidHandler implements IFluidHandler {
    private final Supplier<TankChannels.Channel> channel;
    private final int chamber;
    private final java.util.function.BooleanSupplier fills;
    private final boolean drains;

    public ChamberFluidHandler(Supplier<TankChannels.Channel> channel, int chamber, boolean fills, boolean drains) {
        this(channel, chamber, () -> fills, drains);
    }

    /** {@code fills} is asked on every fill, for faces whose input depends on the tank's state. */
    public ChamberFluidHandler(Supplier<TankChannels.Channel> channel, int chamber, java.util.function.BooleanSupplier fills, boolean drains) {
        this.channel = channel;
        this.chamber = chamber;
        this.fills = fills;
        this.drains = drains;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        TankChannels.Channel c = channel.get();
        return c == null ? FluidStack.EMPTY : c.get(chamber).copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        return TankKind.CHAMBER_CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        TankChannels.Channel c = channel.get();
        if (c == null || resource.isEmpty() || !fills.getAsBoolean()) {
            return 0;
        }
        FluidStack stored = c.get(chamber);
        if (!stored.isEmpty() && !FluidStack.isSameFluidSameComponents(stored, resource)) {
            return 0;
        }
        int amount = Math.min(resource.getAmount(), TankKind.CHAMBER_CAPACITY - stored.getAmount());
        if (amount <= 0) {
            return 0;
        }
        if (action.execute()) {
            c.set(chamber, resource.copyWithAmount(stored.getAmount() + amount));
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        TankChannels.Channel c = channel.get();
        if (c == null || resource.isEmpty() || !FluidStack.isSameFluidSameComponents(c.get(chamber), resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        TankChannels.Channel c = channel.get();
        if (!drains || c == null || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack stored = c.get(chamber);
        if (stored.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int amount = Math.min(maxDrain, stored.getAmount());
        FluidStack drained = stored.copyWithAmount(amount);
        if (action.execute()) {
            c.set(chamber, stored.getAmount() == amount ? FluidStack.EMPTY : stored.copyWithAmount(stored.getAmount() - amount));
        }
        return drained;
    }
}
