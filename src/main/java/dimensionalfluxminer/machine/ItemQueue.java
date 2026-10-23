package dimensionalfluxminer.machine;

import dimensionalfluxminer.machine.transfer.TagStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

/**
 * Machine item slots: {@code maxKinds} individual slots, one item kind per slot, each holding at most
 * {@link #SLOT_LIMIT} items (fewer for items with a smaller max stack size). As in a vanilla inventory, a kind whose
 * slots are full continues in the next free slot; the machine stops only when no slot can take the item.
 */
public final class ItemQueue {
    public static final int SLOT_LIMIT = 64;

    private final int maxKinds;
    private final List<Entry> entries = new ArrayList<>();
    private Runnable onChange = () -> { };

    public ItemQueue(int slots) {
        this.maxKinds = slots;
    }

    /** Items one slot can hold for this kind. */
    public static long slotLimit(ItemStack stack) {
        return Math.min(SLOT_LIMIT, Math.max(1, stack.getMaxStackSize()));
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Nominal capacity: every slot full of 64-stackable items. */
    public long capacity() {
        return (long) SLOT_LIMIT * maxKinds;
    }

    public int maxKinds() {
        return maxKinds;
    }

    public long total() {
        long sum = 0;
        for (Entry e : entries) {
            sum += e.count;
        }
        return sum;
    }

    /** Free room over all slots (empty slots count as a full {@link #SLOT_LIMIT}). */
    public long free() {
        long free = (long) Math.max(0, maxKinds - entries.size()) * SLOT_LIMIT;
        for (Entry e : entries) {
            free += Math.max(0, slotLimit(e.prototype) - e.count);
        }
        return free;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int kinds() {
        return entries.size();
    }

    /** Prototype (count 1) of a kind. */
    public ItemStack prototype(int kind) {
        return entries.get(kind).prototype;
    }

    public long count(int kind) {
        return entries.get(kind).count;
    }

    public ItemStack firstPrototype() {
        return entries.isEmpty() ? ItemStack.EMPTY : entries.getFirst().prototype;
    }

    public long firstCount() {
        return entries.isEmpty() ? 0 : entries.getFirst().count;
    }

    private static boolean same(Entry entry, ItemStack stack) {
        return ItemStack.isSameItemSameComponents(entry.prototype, stack);
    }

    /** Total of {@code stack}'s kind over all its slots. */
    public long countOf(ItemStack stack) {
        long count = 0;
        for (Entry e : entries) {
            if (same(e, stack)) {
                count += e.count;
            }
        }
        return count;
    }

    /** Room left in the slots already holding {@code stack}'s kind. */
    private long roomInOwnSlots(ItemStack stack) {
        long limit = slotLimit(stack);
        long room = 0;
        for (Entry e : entries) {
            if (same(e, stack)) {
                room += Math.max(0, limit - e.count);
            }
        }
        return room;
    }

    private int freeSlots() {
        return Math.max(0, maxKinds - entries.size());
    }

    /** How many of {@code stack}'s kind could be inserted right now (ignoring the stack's own count). */
    public long acceptable(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return roomInOwnSlots(stack) + freeSlots() * slotLimit(stack);
    }

    /**
     * Inserts up to {@code amount} items of the kind of {@code stack}, topping up its slots first and then using free
     * slots; returns the amount inserted.
     */
    public long insert(ItemStack stack, long amount, boolean simulate) {
        long accepted = Math.min(amount, acceptable(stack));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            long limit = slotLimit(stack);
            long left = accepted;
            for (Entry e : entries) {
                if (left > 0 && same(e, stack)) {
                    long put = Math.min(left, Math.max(0, limit - e.count));
                    e.count += put;
                    left -= put;
                }
            }
            while (left > 0) {
                long put = Math.min(left, limit);
                entries.add(new Entry(stack.copyWithCount(1), put));
                left -= put;
            }
            onChange.run();
        }
        return accepted;
    }

    /** Inserts a whole stack and returns what did not fit. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        long inserted = insert(stack, stack.getCount(), simulate);
        return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount((int) (stack.getCount() - inserted));
    }

    /** Removes up to {@code amount} of a kind; returns the amount removed. */
    public long remove(int kind, long amount, boolean simulate) {
        if (kind < 0 || kind >= entries.size() || amount <= 0) {
            return 0;
        }
        Entry entry = entries.get(kind);
        long removed = Math.min(amount, entry.count);
        if (!simulate && removed > 0) {
            entry.count -= removed;
            if (entry.count <= 0) {
                entries.remove(kind);
            }
            onChange.run();
        }
        return removed;
    }

    /** Removes up to {@code amount} of a kind over all its slots, the last slot first; returns the amount removed. */
    public long remove(ItemStack kind, long amount, boolean simulate) {
        long removed = 0;
        for (int i = entries.size() - 1; i >= 0 && removed < amount; i--) {
            if (same(entries.get(i), kind)) {
                removed += remove(i, amount - removed, simulate);
            }
        }
        return removed;
    }

    /**
     * Simulates inserting several result stacks at once (atomic reservation).
     * @return true if all of them fit together
     */
    public boolean canFitAll(List<ItemStack> results) {
        List<ItemStack> kinds = new ArrayList<>();
        List<Long> needed = new ArrayList<>();
        for (ItemStack stack : results) {
            if (stack.isEmpty()) {
                continue;
            }
            int k = 0;
            while (k < kinds.size() && !ItemStack.isSameItemSameComponents(kinds.get(k), stack)) {
                k++;
            }
            if (k == kinds.size()) {
                kinds.add(stack);
                needed.add(0L);
            }
            needed.set(k, needed.get(k) + stack.getCount());
        }
        // Each kind tops up its own slots, then shares the free slots with the other kinds.
        long slotsNeeded = 0;
        for (int k = 0; k < kinds.size(); k++) {
            long rest = needed.get(k) - roomInOwnSlots(kinds.get(k));
            if (rest > 0) {
                long limit = slotLimit(kinds.get(k));
                slotsNeeded += (rest + limit - 1) / limit;
            }
        }
        return slotsNeeded <= freeSlots();
    }

    /** Largest number of operations (each producing {@code perOperation}) whose output fits. */
    public long operationsThatFit(ItemStack perOperation) {
        if (perOperation.isEmpty()) {
            return Long.MAX_VALUE;
        }
        return acceptable(perOperation) / perOperation.getCount();
    }

    public void clear() {
        if (!entries.isEmpty()) {
            entries.clear();
            onChange.run();
        }
    }

    /** Splits everything into normal stacks (used for drops). */
    public List<ItemStack> toStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Entry e : entries) {
            long left = e.count;
            int max = Math.max(1, e.prototype.getMaxStackSize());
            while (left > 0) {
                int n = (int) Math.min(left, max);
                out.add(e.prototype.copyWithCount(n));
                left -= n;
            }
        }
        return out;
    }

    public boolean anyMatch(Predicate<ItemStack> predicate) {
        return entries.stream().anyMatch(e -> predicate.test(e.prototype));
    }

    /** Copy of the contents, put back by {@link #restore} (transactions that are cancelled). */
    public Object snapshot() {
        List<Entry> copy = new ArrayList<>(entries.size());
        for (Entry e : entries) {
            copy.add(new Entry(e.prototype.copy(), e.count));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    public void restore(Object snapshot) {
        entries.clear();
        for (Entry e : (List<Entry>) snapshot) {
            entries.add(new Entry(e.prototype.copy(), e.count));
        }
        onChange.run();
    }

    public ListTag save(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag tag = new CompoundTag();
            tag.put("item", TagStacks.save(registries, e.prototype));
            tag.putLong("count", e.count);
            list.add(tag);
        }
        return list;
    }

    public void load(ListTag list, HolderLookup.Provider registries) {
        entries.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            ItemStack prototype = TagStacks.parse(registries, tag.getCompoundOrEmpty("item"));
            long count = tag.getLongOr("count", 0L);
            if (!prototype.isEmpty() && count > 0 && entries.size() < maxKinds) {
                entries.add(new Entry(prototype.copyWithCount(1), count));
            }
        }
    }

    private static final class Entry {
        final ItemStack prototype;
        long count;

        Entry(ItemStack prototype, long count) {
            this.prototype = prototype;
            this.count = count;
        }
    }
}
