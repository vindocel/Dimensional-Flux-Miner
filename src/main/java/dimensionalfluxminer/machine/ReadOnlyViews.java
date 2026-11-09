package dimensionalfluxminer.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Read-only item capability views for information tools that query without a face. */
public final class ReadOnlyViews {
    private ReadOnlyViews() {
    }

    public static IItemHandler items(IItemHandler source) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return source.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return source.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return source.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }
}
