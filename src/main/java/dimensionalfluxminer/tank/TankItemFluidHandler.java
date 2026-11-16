package dimensionalfluxminer.tank;

//? if >=26.1 {
import dimensionalfluxminer.machine.transfer.Transfer;
//?}
import dimensionalfluxminer.registry.DfmDataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
//? if <26.1 {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?}
import net.neoforged.neoforge.server.ServerLifecycleHooks;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;
//?}

/**
 * A single or creative tank item as a fluid container. The single tank's liquid is its channel's
 * (the code on the item, white, white, white for a new one), so filling or draining the item changes the channel; it
 * only answers on the server thread. A locked tank instead stores finite liquid on its item.
 * The creative tank's liquid is on the item and never runs out.
 */
//? if >=26.1 {
public final class TankItemFluidHandler {
    private TankItemFluidHandler() {
//?} else {
/*public final class TankItemFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    private final TankKind kind;

    public TankItemFluidHandler(ItemStack stack, TankKind kind) {
        this.stack = stack;
        this.kind = kind;
*///?}
    }

    /** The liquid of a creative tank item (full), or empty. */
    public static FluidStack creativeFluid(ItemStack stack) {
        SimpleFluidContent content = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        return content == null || content.isEmpty() ? FluidStack.EMPTY : content.copy().copyWithAmount(TankKind.CREATIVE.capacity());
    }

    //? if >=26.1 {
    /** The item reached through {@code access} (a hand, a slot): the fluid capability of single and creative tanks. */
    public static @Nullable ResourceHandler<FluidResource> forAccess(ItemAccess access, TankKind kind) {
    //?} else {
    /*private IFluidHandler storage() {
    *///?}
        if (kind.creative()) {
            //? if >=26.1 {
            return new Creative(access);
            //?} else {
            /*return new CreativeFluidHandler(() -> creativeFluid(stack),
                    fluid -> stack.set(DfmDataComponents.CREATIVE_TANK_FLUID.get(), SimpleFluidContent.copyOf(fluid.copyWithAmount(1))), () -> true, true);
        }
        if (DimensionalTankItem.channelLocked(stack)) {
            return new Local(stack);
            *///?}
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return null;
        }
        //? if >=26.1 {
        ItemStack stack = access.getResource().toStack();
        if (DimensionalTankItem.channelLocked(stack)) {
            return new Local(access);
        }
        //?}
        TankChannels.Channel channel = TankChannels.get(server).channel(kind, stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT));
        //? if >=26.1 {
        Transfer.Journal<FluidStack> journal = new Transfer.Journal<>(() -> channel.get(0).copy(), saved -> channel.set(0, saved));
        return Transfer.exposeFluids(new ChamberFluidHandler(() -> channel, 0, true, true), journal);
        //?} else {
        /*return new ChamberFluidHandler(() -> channel, 0, true, true);
        *///?}
    }

    //? if >=26.1 {
    /**
     * The creative tank item: drains give as much as asked and never lower it; while empty, the first liquid poured in
     * becomes its endless liquid (the item is exchanged for one that carries it).
     */
    private record Local(ItemAccess access) implements ResourceHandler<FluidResource> {
    //?} else {
    /*@Override
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

    /^* A locked tank item: finite liquid kept on the item itself (chamber A component). ^/
    private record Local(ItemStack stack) implements IFluidHandler {
    *///?}
        private FluidStack stored() {
            //? if >=26.1 {
            SimpleFluidContent fluid = access.getResource().toStack().get(DfmDataComponents.TANK_LOCAL_A.get());
            //?} else {
            /*SimpleFluidContent fluid = stack.get(DfmDataComponents.TANK_LOCAL_A.get());
            *///?}
            return fluid == null ? FluidStack.EMPTY : fluid.copy();
        }

        //? if >=26.1 {
        @Override
        public int size() { return 1; }

        @Override
        public FluidResource getResource(int index) { return FluidResource.of(stored()); }

        @Override
        public long getAmountAsLong(int index) { return stored().getAmount(); }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) { return TankKind.CHAMBER_CAPACITY; }

        @Override
        public boolean isValid(int index, FluidResource resource) { return index == 0; }

        private boolean replace(FluidStack fluid, TransactionContext transaction) {
            ItemResource next = access.getResource().with(DfmDataComponents.TANK_LOCAL_A.get(), SimpleFluidContent.copyOf(fluid));
            return access.exchange(next, 1, transaction) == 1;
        //?} else {
        /*private void store(FluidStack fluid) {
            if (fluid.isEmpty()) {
                stack.remove(DfmDataComponents.TANK_LOCAL_A.get());
            } else {
                stack.set(DfmDataComponents.TANK_LOCAL_A.get(), SimpleFluidContent.copyOf(fluid));
            }
        *///?}
        }

        @Override
        //? if >=26.1 {
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            FluidStack fluid = stored();
            if (index != 0 || resource.isEmpty() || amount <= 0 || !fluid.isEmpty() && !resource.matches(fluid)) {
                return 0;
            }
            int moved = Math.min(amount, Math.max(0, TankKind.CHAMBER_CAPACITY - fluid.getAmount()));
            return moved > 0 && replace(resource.toStack(fluid.getAmount() + moved), transaction) ? moved : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            FluidStack fluid = stored();
            if (index != 0 || resource.isEmpty() || amount <= 0 || !resource.matches(fluid)) {
                return 0;
            }
            int moved = Math.min(amount, fluid.getAmount());
            return moved > 0 && replace(fluid.copyWithAmount(fluid.getAmount() - moved), transaction) ? moved : 0;
        }
    }

    private record Creative(ItemAccess access) implements ResourceHandler<FluidResource> {
        private FluidStack stored() {
            return creativeFluid(access.getResource().toStack());
        }

        @Override
        public int size() {
        //?} else {
        /*public int getTanks() {
        *///?}
            return 1;
        }

        @Override
        //? if >=26.1 {
        public FluidResource getResource(int index) {
            return FluidResource.of(stored());
        //?} else {
        /*public FluidStack getFluidInTank(int tank) {
            return stored();
        *///?}
        }

        @Override
        //? if >=26.1 {
        public long getAmountAsLong(int index) {
            return stored().getAmount();
        //?} else {
        /*public int getTankCapacity(int tank) {
            return TankKind.CHAMBER_CAPACITY;
        *///?}
        }

        @Override
        //? if >=26.1 {
        public long getCapacityAsLong(int index, FluidResource resource) {
            return TankKind.CREATIVE.capacity();
        //?} else {
        /*public boolean isFluidValid(int tank, FluidStack fluid) {
            return tank == 0;
        *///?}
        }

        @Override
        //? if >=26.1 {
        public boolean isValid(int index, FluidResource resource) {
            return true;
        //?} else {
        /*public int fill(FluidStack resource, FluidAction action) {
            FluidStack fluid = stored();
            if (resource.isEmpty() || !fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, fluid)) {
                return 0;
            }
            int moved = Math.min(resource.getAmount(), Math.max(0, TankKind.CHAMBER_CAPACITY - fluid.getAmount()));
            if (moved > 0 && action.execute()) {
                store(resource.copyWithAmount(fluid.getAmount() + moved));
            }
            return moved;
        *///?}
        }

        @Override
        //? if >=26.1 {
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (index != 0 || resource.isEmpty() || amount <= 0 || !stored().isEmpty()) {
                return 0;
            }
            ItemResource filled = access.getResource().with(DfmDataComponents.CREATIVE_TANK_FLUID.get(),
                    SimpleFluidContent.copyOf(resource.toStack(1)));
            return access.exchange(filled, 1, transaction) == 1 ? amount : 0;
        //?} else {
        /*public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack fluid = stored();
            if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        *///?}
        }

        @Override
        //? if >=26.1 {
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 && amount > 0 && !resource.isEmpty() && resource.matches(stored()) ? amount : 0;
        //?} else {
        /*public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack fluid = stored();
            int moved = Math.min(maxDrain, fluid.getAmount());
            if (moved <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                store(fluid.copyWithAmount(fluid.getAmount() - moved));
            }
            return fluid.copyWithAmount(moved);
        *///?}
        }
    }
}
