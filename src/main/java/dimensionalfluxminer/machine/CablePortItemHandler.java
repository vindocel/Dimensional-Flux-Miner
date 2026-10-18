package dimensionalfluxminer.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Item handler of a configurable face of a single-block machine, as on the Miner ports: every such face has one, whatever
 * its route, so a cable or pipe can be noticed. The face's own route works as before through the first slots; after them
 * come probe slots for what the route lacks (the outputs on an input face, the inputs on an output face, both on a face
 * with no route). Probe slots never give or take items: a cable that pulls from them or pushes into them is recorded,
 * and the machine's next scan gives the face that route.
 */
public final class CablePortItemHandler implements IItemHandler {
    private final IItemHandler route;
    /** Null when the route already inserts and extracts. */
    private final IItemHandler probe;
    private final Runnable onPull;
    private final Runnable onPush;

    public CablePortItemHandler(IItemHandler route, IItemHandler probe, Runnable onPull, Runnable onPush) {
        this.route = route;
        this.probe = probe;
        this.onPull = onPull;
        this.onPush = onPush;
    }

    private int routeSlots() {
        return route.getSlots();
    }

    @Override
    public int getSlots() {
        return routeSlots() + (probe != null ? probe.getSlots() : 0);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int own = routeSlots();
        if (slot < own) {
            return route.getStackInSlot(slot);
        }
        return probe != null ? probe.getStackInSlot(slot - own) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return stack;
        }
        int own = routeSlots();
        if (slot < own) {
            ItemStack remainder = route.insertItem(slot, stack, simulate);
            if (remainder.getCount() < stack.getCount()) {
                onPush.run();
            }
            return remainder;
        }
        if (probe != null && probe.insertItem(slot - own, stack, true).getCount() < stack.getCount()) {
            onPush.run();
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        int own = routeSlots();
        if (slot < own) {
            ItemStack taken = route.extractItem(slot, amount, simulate);
            if (!taken.isEmpty()) {
                onPull.run();
            }
            return taken;
        }
        if (probe != null && !probe.extractItem(slot - own, amount, true).isEmpty()) {
            onPull.run();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        int own = routeSlots();
        if (slot < own) {
            return route.getSlotLimit(slot);
        }
        return probe != null ? probe.getSlotLimit(slot - own) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int own = routeSlots();
        if (slot < own) {
            return route.isItemValid(slot, stack);
        }
        // Yes to a valid input even before the face takes it, so a pushing cable tries.
        return probe != null && probe.isItemValid(slot - own, stack);
    }
}
