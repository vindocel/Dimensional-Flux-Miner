package dimensionalfluxminer.machine.transfer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Plain list of stacks (core and upgrade slots). Saved as {@code {Items: [{Slot, id, count, components}], Size}}, the
 * format worlds of earlier versions already hold.
 */
public class SlotStorage implements ItemSlots.Modifiable {
    protected NonNullList<ItemStack> stacks;

    public SlotStorage(int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        ItemStack existing = stacks.get(slot);
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }
        if (limit <= 0) {
            return stack;
        }
        boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }
        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (simulate) {
                return existing.copy();
            }
            stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
            return existing;
        }
        if (!simulate) {
            stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            onContentsChanged(slot);
        }
        return existing.copyWithCount(toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Item.ABSOLUTE_MAX_STACK_SIZE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    protected void onContentsChanged(int slot) {
    }

    /** Copy of every slot (transaction snapshots). */
    public ItemStack[] snapshot() {
        ItemStack[] copy = new ItemStack[stacks.size()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = stacks.get(i).copy();
        }
        return copy;
    }

    public void restore(ItemStack[] snapshot) {
        for (int i = 0; i < snapshot.length && i < stacks.size(); i++) {
            stacks.set(i, snapshot[i].copy());
        }
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        ListTag items = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            if (!stacks.get(i).isEmpty()) {
                CompoundTag itemTag = TagStacks.save(registries, stacks.get(i));
                itemTag.putInt("Slot", i);
                items.add(itemTag);
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        tag.putInt("Size", stacks.size());
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        stacks = NonNullList.withSize(tag.getIntOr("Size", stacks.size()), ItemStack.EMPTY);
        ListTag items = tag.getListOrEmpty("Items");
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompoundOrEmpty(i);
            int slot = itemTag.getIntOr("Slot", 0);
            if (slot >= 0 && slot < stacks.size()) {
                ItemStack stack = TagStacks.parse(registries, itemTag);
                if (!stack.isEmpty()) {
                    stacks.set(slot, stack);
                }
            }
        }
    }
}
