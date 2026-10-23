package dimensionalfluxminer.machine;

import dimensionalfluxminer.machine.transfer.ItemSlots;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Sided item handler over logical queues. Input queues accept insertion only (through the machine's own
 * insertion strategy) and output queues allow extraction only, so a combined handler can serve
 * {@code ITEM_INPUT_OUTPUT_ALL} without ever inserting into outputs or extracting unprocessed inputs.
 */
public final class QueueItemHandler implements ItemSlots {
    private final List<ItemQueue> inputs;
    private final Inserter inserter;
    private final List<ItemQueue> outputs;
    private final Runnable onExtract;
    private final int inputSlots;

    public QueueItemHandler(List<ItemQueue> inputs, Inserter inserter, List<ItemQueue> outputs, Runnable onExtract) {
        this.inputs = inputs;
        this.inserter = inserter;
        this.outputs = outputs;
        this.onExtract = onExtract;
        int slots = 0;
        for (ItemQueue q : inputs) {
            slots += q.maxKinds();
        }
        this.inputSlots = slots;
    }

    @Override
    public int getSlots() {
        int slots = inputSlots;
        for (ItemQueue q : outputs) {
            slots += q.maxKinds();
        }
        return slots;
    }

    /** Resolves a slot to (queue, kind index). */
    private ItemQueue queueFor(int slot, int[] kindOut) {
        int index = slot;
        for (ItemQueue q : inputs) {
            if (index < q.maxKinds()) {
                kindOut[0] = index;
                return q;
            }
            index -= q.maxKinds();
        }
        for (ItemQueue q : outputs) {
            if (index < q.maxKinds()) {
                kindOut[0] = index;
                return q;
            }
            index -= q.maxKinds();
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int[] kind = new int[1];
        ItemQueue queue = queueFor(slot, kind);
        if (queue == null || kind[0] >= queue.kinds()) {
            return ItemStack.EMPTY;
        }
        ItemStack prototype = queue.prototype(kind[0]);
        int shown = (int) Math.min(queue.count(kind[0]), prototype.getMaxStackSize());
        return prototype.copyWithCount(shown);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= inputSlots) {
            return stack;
        }
        return inserter.insert(stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < inputSlots) {
            return ItemStack.EMPTY;
        }
        int[] kind = new int[1];
        ItemQueue queue = queueFor(slot, kind);
        if (queue == null || kind[0] >= queue.kinds()) {
            return ItemStack.EMPTY;
        }
        ItemStack prototype = queue.prototype(kind[0]);
        int wanted = Math.min(amount, prototype.getMaxStackSize());
        long removed = queue.remove(kind[0], wanted, simulate);
        if (removed <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            onExtract.run();
        }
        return prototype.copyWithCount((int) removed);
    }

    @Override
    public int getSlotLimit(int slot) {
        int[] kind = new int[1];
        ItemQueue queue = queueFor(slot, kind);
        return queue == null ? 0 : ItemQueue.SLOT_LIMIT;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < inputSlots && !stack.isEmpty() && inserter.insert(stack.copyWithCount(1), true).isEmpty();
    }

    /** Machine-specific insertion strategy (validation, lane choice, Auto-Split). */
    @FunctionalInterface
    public interface Inserter {
        /** @return the remainder that could not be inserted */
        ItemStack insert(ItemStack stack, boolean simulate);
    }
}
