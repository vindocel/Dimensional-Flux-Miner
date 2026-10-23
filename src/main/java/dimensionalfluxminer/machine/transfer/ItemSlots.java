package dimensionalfluxminer.machine.transfer;

import net.minecraft.world.item.ItemStack;

/**
 * Slot view of an inventory used inside the mod (machine queues, ports, upgrade slots). Simulated calls never change
 * anything. {@link Transfer} turns it into the NeoForge item capability and wraps the capabilities of other blocks
 * back into this shape.
 */
public interface ItemSlots {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    /** Returns what did not fit. */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /** Returns what was taken (at most {@code amount}). */
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);

    /** Slots whose content can also be set directly (menus). */
    interface Modifiable extends ItemSlots {
        void setStackInSlot(int slot, ItemStack stack);
    }

    /** Offers {@code stack} to every slot in order; returns what did not fit. */
    static ItemStack insert(ItemSlots slots, ItemStack stack, boolean simulate) {
        if (slots == null || stack.isEmpty()) {
            return stack;
        }
        for (int i = 0; i < slots.getSlots(); i++) {
            stack = slots.insertItem(i, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /** Fills the slots that already hold the same item first, then the empty ones; returns what did not fit. */
    static ItemStack insertStacked(ItemSlots slots, ItemStack stack, boolean simulate) {
        if (slots == null || stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insert(slots, stack, simulate);
        }
        int size = slots.getSlots();
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (ItemStack.isSameItemSameComponents(slots.getStackInSlot(i), stack)) {
                stack = slots.insertItem(i, stack, simulate);
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (slots.getStackInSlot(i).isEmpty()) {
                stack = slots.insertItem(i, stack, simulate);
            }
        }
        return stack;
    }
}
