package dimensionalfluxminer.tank;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The chamber of a creative tank (block or item). Draining gives as much as asked and never lowers the liquid. A tank
 * that holds a liquid takes nothing in. An empty one takes everything: from a player (bucket, container, screen, the
 * item itself) the liquid becomes the tank's endless liquid; from pipes and automation it simply disappears, so an
 * empty creative tank is a void.
 */
public final class CreativeFluidHandler implements IFluidHandler {
    private final Supplier<FluidStack> stored;
    /** Picks the endless liquid; null voids what comes in. */
    private final Consumer<FluidStack> picker;
    private final BooleanSupplier fills;
    private final boolean drains;

    public CreativeFluidHandler(Supplier<FluidStack> stored, Consumer<FluidStack> picker, BooleanSupplier fills, boolean drains) {
        this.stored = stored;
        this.picker = picker;
        this.fills = fills;
        this.drains = drains;
    }

    /** The block, both directions, as used by players. */
    public static CreativeFluidHandler forPlayer(DimensionalTankBlockEntity tank) {
        return new CreativeFluidHandler(() -> tank.chamber(0), tank::setCreativeFluid, () -> true, true);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return stored.get().copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        return TankKind.CREATIVE.capacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !fills.getAsBoolean() || !stored.get().isEmpty()) {
            return 0;
        }
        if (picker != null && action.execute()) {
            picker.accept(resource);
        }
        return resource.getAmount();
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(stored.get(), resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack current = stored.get();
        if (!drains || maxDrain <= 0 || current.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return current.copyWithAmount(maxDrain);
    }
}
