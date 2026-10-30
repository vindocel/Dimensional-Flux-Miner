package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmDataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * A single or creative tank item as a fluid container. The single tank's liquid is its channel's
 * (the code on the item, white, white, white for a new one), so filling or draining the item changes the channel; it
 * only answers on the server thread. The creative tank's liquid is on the item and never runs out. The item itself
 * never changes, except an empty creative tank picking its liquid.
 */
public final class TankItemFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    private final TankKind kind;

    public TankItemFluidHandler(ItemStack stack, TankKind kind) {
        this.stack = stack;
        this.kind = kind;
    }

    /** The liquid of a creative tank item (full), or empty. */
    public static FluidStack creativeFluid(ItemStack stack) {
        SimpleFluidContent content = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        return content == null || content.isEmpty() ? FluidStack.EMPTY : content.copy().copyWithAmount(TankKind.CREATIVE.capacity());
    }

    private IFluidHandler storage() {
        if (kind.creative()) {
            return new CreativeFluidHandler(() -> creativeFluid(stack),
                    fluid -> stack.set(DfmDataComponents.CREATIVE_TANK_FLUID.get(), SimpleFluidContent.copyOf(fluid.copyWithAmount(1))), () -> true, true);
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return null;
        }
        TankChannels.Channel channel = TankChannels.get(server).channel(kind, stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT));
        return new ChamberFluidHandler(() -> channel, 0, true, true);
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        IFluidHandler storage = storage();
        return storage == null ? FluidStack.EMPTY : storage.getFluidInTank(0);
    }

    @Override
    public int getTankCapacity(int tank) {
        return kind.capacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        IFluidHandler storage = storage();
        return storage == null ? 0 : storage.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        IFluidHandler storage = storage();
        return storage == null ? FluidStack.EMPTY : storage.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        IFluidHandler storage = storage();
        return storage == null ? FluidStack.EMPTY : storage.drain(maxDrain, action);
    }
}
