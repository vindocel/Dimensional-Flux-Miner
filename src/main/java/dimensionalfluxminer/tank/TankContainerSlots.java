package dimensionalfluxminer.tank;

import dimensionalfluxminer.advancement.DfmAdvancements;
import dimensionalfluxminer.machine.ReadOnlyViews;
import dimensionalfluxminer.machine.side.SideRoute;
//? if >=26.1 {
import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.SlotStorage;
import dimensionalfluxminer.machine.transfer.Transfer;
import dimensionalfluxminer.recipe.Recipes;
//?} else {
/*import dimensionalfluxminer.recipe.TankContainerRecipe;
*///?}
import dimensionalfluxminer.registry.DfmRecipes;
//? if <26.1 {
/*import java.util.List;
import java.util.Optional;
*///?}
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
//? if >=26.1 {
import net.minecraft.world.SimpleContainer;
//?}
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.neoforged.neoforge.capabilities.Capabilities;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidActionResult;
*///?}
import net.neoforged.neoforge.fluids.FluidStack;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?}

/** Two local, single-container slots. Finished containers stay in place until removed. */
//? if >=26.1 {
public final class TankContainerSlots extends SlotStorage {
//?} else {
/*public final class TankContainerSlots extends ItemStackHandler {
*///?}
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

    //? if <26.1 {
    /*private static List<RecipeHolder<TankContainerRecipe>> recipes(Level level) {
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(DfmRecipes.TANK_CONTAINER.get());
    }

    *///?}
    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty() || tank.kind() != TankKind.SINGLE) {
            return false;
        }
        //? if >=26.1 {
        if (tank.getLevel() != null && Recipes.byType(tank.getLevel(), DfmRecipes.TANK_CONTAINER.get()).stream()
                .anyMatch(h -> h.value().filling() == (slot == 1) && h.value().ingredient().test(stack))) {
        //?} else {
        /*if (recipes(tank.getLevel()).stream().anyMatch(h -> h.value().filling() == (slot == 1) && h.value().ingredient().test(stack))) {
        *///?}
            return true;
        }
        if (stack.getItem() instanceof DimensionalTankItem) {
            return false;
        }
        //? if >=26.1 {
        var handler = ItemAccess.forStack(stack.copyWithCount(1)).getCapability(Capabilities.Fluid.ITEM);
        if (handler != null) {
            for (int i = 0; i < handler.size(); i++) {
                long amount = handler.getAmountAsLong(i);
                if (slot == 0 ? amount > 0 : handler.getCapacityAsLong(i, handler.getResource(i)) > amount) {
        //?} else {
        /*Optional<IFluidHandlerItem> handler = FluidUtil.getFluidHandler(stack.copyWithCount(1));
        if (handler.isPresent()) {
            IFluidHandlerItem item = handler.get();
            for (int i = 0; i < item.getTanks(); i++) {
                int amount = item.getFluidInTank(i).getAmount();
                if (slot == 0 ? amount > 0 : item.getTankCapacity(i) > amount) {
        *///?}
                    return true;
                }
            }
        }
        return false;
    }

    public boolean finished(int slot) {
        return finished[slot];
    }

    //? if >=26.1 {
    public boolean[] completionSnapshot() {
        return finished.clone();
    }

    public void restoreCompletion(boolean[] snapshot) {
        System.arraycopy(snapshot, 0, finished, 0, finished.length);
    }

    public int[] progressSnapshot() {
    //?} else {
    /*/^* Ticks each slot has counted toward its next conversion. ^/
    public int[] progressSnapshot() {
    *///?}
        return progress.clone();
    //? if >=26.1 {
    }

    public void restoreProgress(int[] snapshot) {
        System.arraycopy(snapshot, 0, progress, 0, progress.length);
    //?}
    }

    public void load(HolderLookup.Provider registries, CompoundTag tag) {
        CompoundTag fixed = tag.copy();
        fixed.putInt("Size", 2);
        deserializeNBT(registries, fixed);
        for (int slot = 0; slot < 2; slot++) {
            //? if >=26.1 {
            finished[slot] = tag.getBooleanOr("finished_" + slot, false);
            //?} else {
            /*finished[slot] = tag.getBoolean("finished_" + slot);
            *///?}
            progress[slot] = finished[slot] || getStackInSlot(slot).isEmpty() ? 0
                    //? if >=26.1 {
                    : Math.clamp(tag.getIntOr("progress_" + slot, 0), 0, dimensionalfluxminer.DfmConfig.tankContainerTicks() - 1);
                    //?} else {
                    /*: Math.clamp(tag.getInt("progress_" + slot), 0, dimensionalfluxminer.DfmConfig.tankContainerTicks() - 1);
                    *///?}
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

    //? if >=26.1 {
    public ItemSlots face(SideRoute route) {
    //?} else {
    /*/^* The slot a face route serves: inserts only new containers, extracts only finished ones. ^/
    public IItemHandler face(SideRoute route) {
    *///?}
        int index = route.containerSlot();
        if (index < 0) {
            return null;
        }
        //? if >=26.1 {
        return new ItemSlots() {
        //?} else {
        /*return new IItemHandler() {
        *///?}
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
//? if <26.1 {

//?}
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return route.insertsContainers() && !finished[index]
                        ? TankContainerSlots.this.insertItem(index, stack, simulate) : stack;
            }
//? if <26.1 {

//?}
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return route.extractsContainers() && finished[index]
                        ? TankContainerSlots.this.extractItem(index, amount, simulate) : ItemStack.EMPTY;
            }
        };
    }

    //? if >=26.1 {
    public ItemSlots readOnly() {
    //?} else {
    /*public IItemHandler readOnly() {
    *///?}
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
            //? if >=26.1 {
            var recipe = Recipes.byType(tank.getLevel(), DfmRecipes.TANK_CONTAINER.get()).stream()
            //?} else {
            /*var recipe = recipes(tank.getLevel()).stream()
            *///?}
                    .filter(h -> h.value().filling() == filling && h.value().ingredient().test(input)).findFirst();
            //? if <26.1 {
            /*IFluidHandler handler = tank.chamberHandler(0);
            *///?}
            if (recipe.isPresent()) {
                //? if >=26.1 {
                var conversion = recipe.get().value();
                //?} else {
                /*TankContainerRecipe conversion = recipe.get().value();
                *///?}
                FluidStack fluid = conversion.fluidFor(tank.chamber(0));
                //? if >=26.1 {
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
                //?} else {
                /*ItemStack result = conversion.result();
                if (fluid.isEmpty() || result.getCount() != 1) {
                    continue;
                }
                // Simulated first, so the container and the liquid move together or not at all.
                boolean fits = filling ? handler.drain(fluid, IFluidHandler.FluidAction.SIMULATE).getAmount() == fluid.getAmount()
                        : handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
                if (!fits) {
                    continue;
                }
                if (filling) {
                    handler.drain(fluid, IFluidHandler.FluidAction.EXECUTE);
                } else {
                    handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
                }
                complete(slot, result);
                *///?}
                if (filling && getStackInSlot(slot).is(Items.NAUTILUS_SHELL) && tank.getLevel() instanceof ServerLevel level) {
                    DfmAdvancements.awardNearby(level, tank.getBlockPos(), DfmAdvancements.NAUTILUS_SHELL_CREATED);
                }
                continue;
            }
            //? if >=26.1 {
            // A temporary container permits item replacement (a bucket becoming an empty bucket) atomically.
            SimpleContainer container = new SimpleContainer(input.copy());
            var access = ItemAccess.forHandlerIndexStrict(VanillaContainerWrapper.ofUncached(container), 0);
            var item = access.getCapability(Capabilities.Fluid.ITEM);
            if (item == null) {
            //?} else {
            /*if (input.getItem() instanceof DimensionalTankItem) {
            *///?}
                continue;
            }
            //? if >=26.1 {
            var fluid = Transfer.exposeFluids(tank.chamberHandler(0), tank.journal());
            try (Transaction transaction = Transaction.openRoot()) {
                var moved = filling ? ResourceHandlerUtil.moveFirst(fluid, item, resource -> true, Integer.MAX_VALUE, transaction)
                        : ResourceHandlerUtil.moveFirst(item, fluid, resource -> true, Integer.MAX_VALUE, transaction);
                if (moved != null) {
                    complete(slot, container.getItem(0).copy());
                    transaction.commit();
                }
            //?} else {
            /*ItemStack one = input.copyWithCount(1);
            FluidActionResult moved = filling ? FluidUtil.tryFillContainer(one, handler, Integer.MAX_VALUE, null, true)
                    : FluidUtil.tryEmptyContainer(one, handler, Integer.MAX_VALUE, null, true);
            if (moved.isSuccess()) {
                complete(slot, moved.getResult().copy());
            *///?}
            }
        }
    }

    private void complete(int slot, ItemStack result) {
        setStackInSlot(slot, result);
        finished[slot] = true;
    }
}
