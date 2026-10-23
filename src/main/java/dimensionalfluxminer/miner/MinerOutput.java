package dimensionalfluxminer.miner;

import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.SlotStorage;
import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The Miner's Output Buffer: 27 physical output-only slots (3 x 9) that only the machine fills. Automation sees an
 * extraction-only view with no item limit per tick; Auto Eject pushes into the inventory in front of the output port.
 * While a cycle inserts its results ({@link #beginBatch()}), the machine is marked changed once at the end instead of
 * once per item.
 */
public final class MinerOutput {
    public static final int SLOTS = 27;

    private final Runnable onChange;
    private boolean batching;
    private final SlotStorage handler = new SlotStorage(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!batching) {
                onChange.run();
            }
        }
    };

    /** Extraction-only view for the Item Output Port. */
    private final ItemSlots extractionView = new ItemSlots() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    MinerOutput(Runnable onChange) {
        this.onChange = onChange;
    }

    public SlotStorage handler() {
        return handler;
    }

    ItemSlots extractionView() {
        return extractionView;
    }

    void beginBatch() {
        batching = true;
    }

    /** Ends a batch; the caller marks the machine changed. */
    void endBatch() {
        batching = false;
    }

    /**
     * The slot {@code ItemSlots.insert} would put a single {@code stack} in (the first empty slot or the
     * first slot of the same item with room, in slot order), or -1 when none takes it. No copies, no simulation.
     */
    public static int insertionSlot(ItemSlots handler, ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            int limit = Math.min(handler.getSlotLimit(slot), stack.getMaxStackSize());
            ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || (existing.getCount() < limit && ItemStack.isSameItemSameComponents(existing, stack))) {
                return slot;
            }
        }
        return -1;
    }

    int slotFor(ItemStack stack) {
        return insertionSlot(handler, stack);
    }

    void insert(int slot, ItemStack stack) {
        handler.insertItem(slot, stack, false);
    }

    /**
     * True when at least one result the machine keeps (all families, or the filtered ones) still fits. Only then is the
     * output really blocked: a single roll that does not fit is just a discarded attempt.
     */
    boolean anyKeptResultFits(ResourceProfile profile, Set<Identifier> filter) {
        for (int i = 0; i < SLOTS; i++) {
            if (handler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        for (ResourceFamily family : profile.families()) {
            if (!filter.isEmpty() && !filter.contains(family.key())) {
                continue;
            }
            for (ResourceFamily.Variant variant : family.variants()) {
                if (slotFor(new ItemStack(variant.item())) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Auto Eject: moves everything the target accepts, with no item limit. */
    void ejectTo(ItemSlots target) {
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack left = ItemSlots.insertStacked(target, stack.copy(), false);
            int moved = stack.getCount() - left.getCount();
            if (moved > 0) {
                handler.extractItem(slot, moved, false);
            }
        }
    }

    int usedSlots() {
        int used = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }

    /** Empties the buffer and returns its stacks (for the drops when the machine is invalidated). */
    List<ItemStack> takeAll() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            stacks.add(handler.getStackInSlot(i).copy());
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
        return stacks;
    }

    CompoundTag save(HolderLookup.Provider registries) {
        return handler.serializeNBT(registries);
    }

    void load(HolderLookup.Provider registries, CompoundTag tag) {
        handler.deserializeNBT(registries, tag);
    }
}
