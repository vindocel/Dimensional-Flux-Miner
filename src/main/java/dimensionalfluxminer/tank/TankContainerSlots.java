package dimensionalfluxminer.tank;

import dimensionalfluxminer.machine.ReadOnlyViews;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.SlotStorage;
import dimensionalfluxminer.machine.transfer.Transfer;
import dimensionalfluxminer.recipe.Recipes;
import dimensionalfluxminer.registry.DfmRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Two local, single-container slots. Finished containers stay in place until removed. */
public final class TankContainerSlots extends SlotStorage {
    private final DimensionalTankBlockEntity tank;
    private final boolean[] finished = new boolean[2];
    private final int[] progress = new int[2];
    private long lastTick = Long.MIN_VALUE;

    public TankContainerSlots(DimensionalTankBlockEntity tank) {
        super(2);
        this.tank = tank;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        finished[slot] = false;
        progress[slot] = 0;
        tank.setChanged();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || tank.kind() != TankKind.SINGLE) {
            return false;
        }
        if (tank.getLevel() != null && Recipes.byType(tank.getLevel(), DfmRecipes.TANK_CONTAINER.get()).stream()
                .anyMatch(h -> h.value().filling() == (slot == 1) && h.value().ingredient().test(stack))) {
            return true;
        }
        if (stack.getItem() instanceof DimensionalTankItem) {
            return false;
        }
        var handler = ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            for (int i = 0; i < handler.size(); i++) {
                long amount = handler.getAmountAsLong(i);
                if (slot == 0 ? amount > 0 : handler.getCapacityAsLong(i, handler.getResource(i)) > amount) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean finished(int slot) {
        return finished[slot];
    }

    public boolean[] completionSnapshot() {
        return finished.clone();
    }

    public void restoreCompletion(boolean[] snapshot) {
        System.arraycopy(snapshot, 0, finished, 0, finished.length);
    }

    public int[] progressSnapshot() {
        return progress.clone();
    }

    public void restoreProgress(int[] snapshot) {
        System.arraycopy(snapshot, 0, progress, 0, progress.length);
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag fixed = tag.copy();
        fixed.putInt("Size", 2);
        deserializeNBT(registries, fixed);
        for (int slot = 0; slot < 2; slot++) {
            finished[slot] = tag.getBooleanOr("finished_" + slot, false);
            progress[slot] = finished[slot] || getStackInSlot(slot).isEmpty() ? 0
                    : Math.clamp(tag.getIntOr("progress_" + slot, 0), 0, dimensionalfluxminer.DfmConfig.tankContainerTicks() - 1);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = super.serializeNBT(registries);
        for (int slot = 0; slot < 2; slot++) {
            tag.putBoolean("finished_" + slot, finished[slot]);
            tag.putInt("progress_" + slot, progress[slot]);
        }
        return tag;
    }

    public ItemSlots face(SideRoute route) {
        int index = route.containerSlot();
        if (index < 0) {
            return null;
        }
        return new ItemSlots() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return TankContainerSlots.this.getStackInSlot(index);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return route.insertsContainers() && TankContainerSlots.this.isItemValid(index, stack);
            }
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return route.insertsContainers() && !finished[index]
                        ? TankContainerSlots.this.insertItem(index, stack, simulate) : stack;
            }
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return route.extractsContainers() && finished[index]
                        ? TankContainerSlots.this.extractItem(index, amount, simulate) : ItemStack.EMPTY;
            }
        };
    }

    public ItemSlots readOnly() {
        return ReadOnlyViews.items(this);
    }

    /** One timed update per world tick, even if an external ticker calls the block more often. */
    public void tick() {
        long now = tank.getLevel().getGameTime();
        if (lastTick == now) {
            return;
        }
        lastTick = now;
        int ready = 0;
        int duration = dimensionalfluxminer.DfmConfig.tankContainerTicks();
        for (int slot = 0; slot < 2; slot++) {
            if (finished[slot] || getStackInSlot(slot).isEmpty()) {
                continue;
            }
            if (++progress[slot] >= duration) {
                progress[slot] = 0;
                ready |= 1 << slot;
            }
            tank.setChanged();
        }
        if (ready != 0) {
            process(ready);
        }
    }

    /** One conversion cycle; the world ticker schedules these through {@link #tick()}. */
    public void process() {
        process(3);
    }

    private void process(int ready) {
        for (int slot = 0; slot < 2; slot++) {
            if ((ready & 1 << slot) == 0 || finished[slot] || getStackInSlot(slot).isEmpty()) {
                continue;
            }
            ItemStack input = getStackInSlot(slot);
            boolean filling = slot == 1;
            var recipe = Recipes.byType(tank.getLevel(), DfmRecipes.TANK_CONTAINER.get()).stream()
                    .filter(h -> h.value().filling() == filling && h.value().ingredient().test(input)).findFirst();
            if (recipe.isPresent()) {
                var conversion = recipe.get().value();
                FluidStack fluid = conversion.fluidFor(tank.chamber(0));
                try (Transaction transaction = Transaction.openRoot()) {
                    tank.journal().updateSnapshots(transaction);
                    var handler = tank.chamberHandler(0);
                    int moved = filling ? handler.drain(fluid, dimensionalfluxminer.machine.transfer.FluidTanks.FluidAction.EXECUTE).getAmount()
                            : handler.fill(fluid, dimensionalfluxminer.machine.transfer.FluidTanks.FluidAction.EXECUTE);
                    if (moved != fluid.getAmount() || conversion.result().getCount() != 1) {
                        continue;
                    }
                    complete(slot, conversion.result());
                    transaction.commit();
                }
                continue;
            }
            // A temporary container permits item replacement (a bucket becoming an empty bucket) atomically.
            SimpleContainer container = new SimpleContainer(input.copy());
            var access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.ofUncached(container), 0);
            var item = access.getCapability(Capabilities.Fluid.ITEM);
            if (item == null) {
                continue;
            }
            var fluid = Transfer.exposeFluids(tank.chamberHandler(0), tank.journal());
            try (Transaction transaction = Transaction.openRoot()) {
                var moved = filling ? ResourceHandlerUtil.moveFirst(fluid, item, resource -> true, Integer.MAX_VALUE, transaction)
                        : ResourceHandlerUtil.moveFirst(item, fluid, resource -> true, Integer.MAX_VALUE, transaction);
                if (moved != null) {
                    complete(slot, container.getItem(0).copy());
                    transaction.commit();
                }
            }
        }
    }

    private void complete(int slot, ItemStack result) {
        setStackInSlot(slot, result);
        finished[slot] = true;
    }
}
