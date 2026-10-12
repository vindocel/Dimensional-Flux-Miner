package dimensionalfluxminer.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Read-only capability views returned for the {@code null} side (no face). Information tools such as Jade query
 * that side to show a machine's energy and items; nothing can be inserted or extracted through it, so the per-face
 * Side Configuration stays the only way in or out.
 */
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

    public static IEnergyStorage energy(IEnergyStorage source) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                return 0;
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return source.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return source.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return false;
            }
        };
    }
}
