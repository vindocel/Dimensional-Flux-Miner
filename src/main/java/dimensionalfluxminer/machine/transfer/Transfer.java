package dimensionalfluxminer.machine.transfer;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Bridge between the mod's slot, energy and tank views and the NeoForge transfer capabilities.
 * <ul>
 *     <li>{@code expose*}: what other blocks see. A change made inside a transaction is real at once; the
 *     {@link Journal} of the block keeps a copy of its contents from before, put back if the transaction is
 *     cancelled.</li>
 *     <li>{@link #items}, {@link #energy}, {@link #fluids}: the capability of a neighbour, used like the mod's own
 *     views; each call is one transaction of its own (committed unless simulated).</li>
 * </ul>
 */
public final class Transfer {
    private Transfer() {
    }

    // ---------------------------------------------------------------- journals

    /** Copy of a block's contents taken before the first change of each transaction, restored if it is cancelled. */
    public static final class Journal<S> extends SnapshotJournal<S> {
        private final Supplier<S> snapshot;
        private final Consumer<S> restore;

        public Journal(Supplier<S> snapshot, Consumer<S> restore) {
            this.snapshot = snapshot;
            this.restore = restore;
        }

        @Override
        protected S createSnapshot() {
            return snapshot.get();
        }

        @Override
        protected void revertToSnapshot(S state) {
            restore.accept(state);
        }
    }

    // ---------------------------------------------------------------- what other blocks see

    public static @Nullable ResourceHandler<ItemResource> exposeItems(@Nullable ItemSlots slots, SnapshotJournal<?> journal) {
        return slots == null ? null : new ExposedItems(slots, journal);
    }

    public static @Nullable EnergyHandler exposeEnergy(@Nullable EnergyStore store, SnapshotJournal<?> journal) {
        return store == null ? null : new ExposedEnergy(store, journal);
    }

    public static @Nullable ResourceHandler<FluidResource> exposeFluids(@Nullable FluidTanks tanks, SnapshotJournal<?> journal) {
        return tanks == null ? null : new ExposedFluids(tanks, journal);
    }

    private record ExposedItems(ItemSlots slots, SnapshotJournal<?> journal) implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return slots.getSlots();
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(slots.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return slots.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            int limit = slots.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty() && slots.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            int fits = amount - slots.insertItem(index, resource.toStack(amount), true).getCount();
            if (fits <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return fits - slots.insertItem(index, resource.toStack(fits), false).getCount();
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !resource.matches(slots.getStackInSlot(index))) {
                return 0;
            }
            ItemStack available = slots.extractItem(index, amount, true);
            if (available.isEmpty() || !resource.matches(available)) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return slots.extractItem(index, available.getCount(), false).getCount();
        }
    }

    private record ExposedEnergy(EnergyStore store, SnapshotJournal<?> journal) implements EnergyHandler {
        @Override
        public long getAmountAsLong() {
            return store.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return store.getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0 || !store.canReceive()) {
                return 0;
            }
            int fits = store.receiveEnergy(amount, true);
            if (fits <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return store.receiveEnergy(fits, false);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (amount <= 0 || !store.canExtract()) {
                return 0;
            }
            int available = store.extractEnergy(amount, true);
            if (available <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return store.extractEnergy(available, false);
        }
    }

    /** Tanks fill and drain as a whole (the mod's tank views pick the chamber): only index 0 takes part. */
    private record ExposedFluids(FluidTanks tanks, SnapshotJournal<?> journal) implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return tanks.getTanks();
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.of(tanks.getFluidInTank(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return tanks.getFluidInTank(index).getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return tanks.getTankCapacity(index);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return !resource.isEmpty() && tanks.isFluidValid(index, resource.toStack(1));
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 ? insert(resource, amount, transaction) : 0;
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            int fits = tanks.fill(resource.toStack(amount), FluidTanks.FluidAction.SIMULATE);
            if (fits <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return tanks.fill(resource.toStack(fits), FluidTanks.FluidAction.EXECUTE);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 ? extract(resource, amount, transaction) : 0;
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            FluidStack available = tanks.drain(resource.toStack(amount), FluidTanks.FluidAction.SIMULATE);
            if (available.isEmpty() || !resource.matches(available)) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return tanks.drain(available, FluidTanks.FluidAction.EXECUTE).getAmount();
        }
    }

    // ---------------------------------------------------------------- capabilities of other blocks

    public static @Nullable ItemSlots items(Level level, BlockPos pos, @Nullable Direction side) {
        return items(level.getCapability(Capabilities.Item.BLOCK, pos, side));
    }

    public static @Nullable EnergyStore energy(Level level, BlockPos pos, @Nullable Direction side) {
        return energy(level.getCapability(Capabilities.Energy.BLOCK, pos, side));
    }

    public static @Nullable FluidTanks fluids(Level level, BlockPos pos, @Nullable Direction side) {
        return fluids(level.getCapability(Capabilities.Fluid.BLOCK, pos, side));
    }

    /** The transaction a call runs in: nested in the one already open on this thread, if any. */
    @SuppressWarnings("deprecation")
    private static Transaction open() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    public static @Nullable ItemSlots items(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ExposedItems exposed) {
            return exposed.slots();
        }
        return new ItemSlots() {
            @Override
            public int getSlots() {
                return handler.size();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                ItemResource resource = handler.getResource(slot);
                return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                int inserted;
                try (Transaction transaction = open()) {
                    inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                }
                return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemResource resource = handler.getResource(slot);
                if (resource.isEmpty() || amount <= 0) {
                    return ItemStack.EMPTY;
                }
                int extracted;
                try (Transaction transaction = open()) {
                    extracted = handler.extract(slot, resource, Math.min(amount, resource.getMaxStackSize()), transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                }
                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
            }

            @Override
            public int getSlotLimit(int slot) {
                ItemResource resource = handler.getResource(slot);
                return resource.isEmpty() ? 64 :handler.getCapacityAsInt(slot, resource);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !stack.isEmpty() && handler.isValid(slot, ItemResource.of(stack));
            }
        };
    }

    public static @Nullable EnergyStore energy(@Nullable EnergyHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ExposedEnergy exposed) {
            return exposed.store();
        }
        return new EnergyStore() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                if (toReceive <= 0) {
                    return 0;
                }
                try (Transaction transaction = open()) {
                    int accepted = handler.insert(toReceive, transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                    return accepted;
                }
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                if (toExtract <= 0) {
                    return 0;
                }
                try (Transaction transaction = open()) {
                    int given = handler.extract(toExtract, transaction);
                    if (!simulate) {
                        transaction.commit();
                    }
                    return given;
                }
            }

            @Override
            public int getEnergyStored() {
                return handler.getAmountAsInt();
            }

            @Override
            public int getMaxEnergyStored() {
                return handler.getCapacityAsInt();
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    public static @Nullable FluidTanks fluids(@Nullable ResourceHandler<FluidResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ExposedFluids exposed) {
            return exposed.tanks();
        }
        return new FluidTanks() {
            @Override
            public int getTanks() {
                return handler.size();
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                FluidResource resource = handler.getResource(tank);
                return resource.isEmpty() ? FluidStack.EMPTY : resource.toStack(handler.getAmountAsInt(tank));
            }

            @Override
            public int getTankCapacity(int tank) {
                return handler.getCapacityAsInt(tank, handler.getResource(tank));
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return !stack.isEmpty() && handler.isValid(tank, FluidResource.of(stack));
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return 0;
                }
                try (Transaction transaction = open()) {
                    int filled = handler.insert(FluidResource.of(resource), resource.getAmount(), transaction);
                    if (action.execute()) {
                        transaction.commit();
                    }
                    return filled;
                }
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return FluidStack.EMPTY;
                }
                try (Transaction transaction = open()) {
                    int drained = handler.extract(FluidResource.of(resource), resource.getAmount(), transaction);
                    if (action.execute()) {
                        transaction.commit();
                    }
                    return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
                }
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                for (int i = 0; i < handler.size() && maxDrain > 0; i++) {
                    FluidResource resource = handler.getResource(i);
                    if (!resource.isEmpty() && handler.getAmountAsLong(i) > 0) {
                        FluidStack drained = drain(resource.toStack(maxDrain), action);
                        if (!drained.isEmpty()) {
                            return drained;
                        }
                    }
                }
                return FluidStack.EMPTY;
            }
        };
    }
}
