package dimensionalfluxminer.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Active Auto-Input/Auto-Eject against the directly adjacent inventory only. */
public final class NeighborTransfer {
    private NeighborTransfer() {
    }

    public static IItemHandler neighbor(Level level, BlockPos pos, Direction side) {
        BlockPos target = pos.relative(side);
        if (!level.isLoaded(target)) {
            return null;
        }
        return level.getCapability(Capabilities.ItemHandler.BLOCK, target, side.getOpposite());
    }

    /**
     * Pulls items that the inserter accepts. Extraction is simulated first and only the amount that fits is
     * taken; anything rejected after a real extraction is handed back to the source. The source is passed over again
     * while it keeps giving items: one extraction moves at most a stack, and a single-slot source (a creative or bulk
     * storage) would otherwise deliver only one stack per tick.
     *
     * @param budget maximum number of items to move
     * @return items moved
     */
    public static long pull(Level level, BlockPos pos, Direction side, QueueItemHandler.Inserter inserter, long budget) {
        IItemHandler source = neighbor(level, pos, side);
        if (source == null || budget <= 0) {
            return 0;
        }
        long moved = 0;
        while (moved < budget) {
            long pass = pullPass(source, inserter, budget - moved);
            if (pass <= 0) {
                break;
            }
            moved += pass;
        }
        return moved;
    }

    private static long pullPass(IItemHandler source, QueueItemHandler.Inserter inserter, long budget) {
        long moved = 0;
        for (int slot = 0; slot < source.getSlots() && moved < budget; slot++) {
            ItemStack available = source.extractItem(slot, (int) Math.min(Integer.MAX_VALUE, budget - moved), true);
            if (available.isEmpty()) {
                continue;
            }
            ItemStack remainder = inserter.insert(available, true);
            int fits = available.getCount() - remainder.getCount();
            if (fits <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, fits, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack rejected = inserter.insert(taken, false);
            moved += taken.getCount() - rejected.getCount();
            if (!rejected.isEmpty()) {
                ItemStack back = source.insertItem(slot, rejected, false);
                if (!back.isEmpty()) {
                    ItemHandlerHelper.insertItemStacked(source, back, false);
                }
            }
        }
        return moved;
    }

    /**
     * Pushes queue contents into the neighbor; only what the destination accepts leaves the queue.
     *
     * @return items moved
     */
    public static long push(Level level, BlockPos pos, Direction side, ItemQueue queue, long budget) {
        IItemHandler target = neighbor(level, pos, side);
        if (target == null || budget <= 0) {
            return 0;
        }
        long moved = 0;
        for (int kind = queue.kinds() - 1; kind >= 0 && moved < budget; kind--) {
            ItemStack prototype = queue.prototype(kind);
            int max = Math.max(1, prototype.getMaxStackSize());
            while (moved < budget && kind < queue.kinds()) {
                long count = queue.count(kind);
                int attempt = (int) Math.min(Math.min(count, max), budget - moved);
                if (attempt <= 0) {
                    break;
                }
                ItemStack offer = prototype.copyWithCount(attempt);
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, offer, true);
                int accepted = attempt - left.getCount();
                if (accepted <= 0) {
                    break;
                }
                ItemStack really = ItemHandlerHelper.insertItemStacked(target, prototype.copyWithCount(accepted), false);
                int inserted = accepted - really.getCount();
                queue.remove(kind, inserted, false);
                moved += inserted;
                if (inserted < attempt) {
                    break;
                }
            }
        }
        return moved;
    }
}
