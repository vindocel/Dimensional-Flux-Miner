package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.machine.ItemQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Client-side read-only view of a synced logical queue. */
record QueueView(List<ItemStack> prototypes, List<Long> counts) {
    static QueueView read(ListTag list, HolderLookup.Provider registries) {
        List<ItemStack> prototypes = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            ItemStack stack = dimensionalfluxminer.machine.transfer.TagStacks.parse(registries, tag.getCompoundOrEmpty("item"));
            if (!stack.isEmpty()) {
                prototypes.add(stack);
                counts.add(tag.getLongOr("count", 0L));
            }
        }
        return new QueueView(prototypes, counts);
    }

    boolean isEmpty() {
        return prototypes.isEmpty();
    }

    int size() {
        return prototypes.size();
    }

    ItemStack prototype(int i) {
        return prototypes.get(i);
    }

    long count(int i) {
        return counts.get(i);
    }

    long total() {
        return counts.stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Tooltip of one machine slot: {@code key} formatted with {@code prefix} followed by count and slot limit,
     * then the item name when the slot is not empty.
     */
    List<Component> slotTooltip(int kind, String key, Object... prefix) {
        boolean filled = kind < size();
        long count = filled ? count(kind) : 0;
        long limit = filled ? ItemQueue.slotLimit(prototype(kind)) : ItemQueue.SLOT_LIMIT;
        Object[] args = Arrays.copyOf(prefix, prefix.length + 2);
        args[prefix.length] = DfmScreen.compact(count);
        args[prefix.length + 1] = limit;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(key, args));
        if (filled) {
            lines.add(prototype(kind).getHoverName());
        }
        return lines;
    }

    /** Header line (translated key with total/capacity) followed by one line per kind. */
    List<Component> tooltip(Component header) {
        List<Component> lines = new ArrayList<>();
        lines.add(header);
        for (int i = 0; i < size(); i++) {
            lines.add(Component.literal(DfmScreen.compact(count(i)) + " x ").append(prototype(i).getHoverName()));
        }
        return lines;
    }
}
