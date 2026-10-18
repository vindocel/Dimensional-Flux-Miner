package dimensionalfluxminer.furnace;

import dimensionalfluxminer.machine.ItemQueue;
import net.minecraft.world.item.ItemStack;

/**
 * Output choice of the furnace lanes for one cycle. A lane first uses the output under it; when that slot holds another
 * item, it may use a slot that already holds its result, then an empty slot whose own lane is idle or makes the same
 * item. An empty slot stays reserved for the lane above it when that lane makes something else. What a lane reserves
 * counts against the slot for the lanes after it, so inputs are only consumed when their results have room.
 */
final class ElectricFurnaceOutputPlanner {
    private final ItemQueue[] outputs;
    private final ItemStack[] laneUnits;
    private final ItemStack[] claimed;
    private final long[] planned;

    /**
     * @param outputs   the output slots, one per lane
     * @param laneUnits what each lane would make in one operation (empty for an idle lane)
     */
    ElectricFurnaceOutputPlanner(ItemQueue[] outputs, ItemStack[] laneUnits) {
        this.outputs = outputs;
        this.laneUnits = laneUnits;
        this.claimed = new ItemStack[outputs.length];
        java.util.Arrays.fill(claimed, ItemStack.EMPTY);
        this.planned = new long[outputs.length];
    }

    /** Items of {@code unit}'s kind the slot can still take this cycle. */
    long room(int slot, ItemStack unit) {
        if (!claimed[slot].isEmpty() && !ItemStack.isSameItemSameComponents(claimed[slot], unit)) {
            return 0;
        }
        return Math.max(0, outputs[slot].acceptable(unit) - planned[slot]);
    }

    void reserve(int slot, ItemStack unit, long items) {
        claimed[slot] = unit;
        planned[slot] += items;
    }

    /** The output slot for {@code lane}'s results, or -1 when none can take one unit. */
    int choose(int lane, ItemStack unit) {
        if (room(lane, unit) >= unit.getCount()) {
            return lane;
        }
        for (int slot = 0; slot < outputs.length; slot++) {
            if (slot != lane && !outputs[slot].isEmpty() && room(slot, unit) >= unit.getCount()) {
                return slot;
            }
        }
        for (int slot = 0; slot < outputs.length; slot++) {
            if (slot == lane || !outputs[slot].isEmpty() || room(slot, unit) < unit.getCount()) {
                continue;
            }
            ItemStack owner = laneUnits[slot];
            if (owner.isEmpty() || ItemStack.isSameItemSameComponents(owner, unit)) {
                return slot;
            }
        }
        return -1;
    }
}
