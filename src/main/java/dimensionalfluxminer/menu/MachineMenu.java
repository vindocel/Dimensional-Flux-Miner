package dimensionalfluxminer.menu;

import dimensionalfluxminer.machine.ItemQueue;
import dimensionalfluxminer.network.MenuSyncPayload;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Base menu for DFM machines. Display values travel in a {@link MenuSyncPayload} snapshot built from the
 * block entity; every player action is a menu button id validated and applied on the server.
 */
public abstract class MachineMenu<T extends BlockEntity> extends AbstractContainerMenu implements SyncedMenu {
    public static final int PLAYER_INVENTORY_SLOTS = 36;

    protected final BlockPos pos;
    protected final T blockEntity;
    protected final Player player;
    private CompoundTag data = new CompoundTag();
    private CompoundTag lastSent;

    protected MachineMenu(MenuType<?> type, int containerId, Inventory inventory, BlockPos pos, T blockEntity) {
        super(type, containerId);
        this.pos = pos;
        this.blockEntity = blockEntity;
        this.player = inventory.player;
    }

    public BlockPos pos() {
        return pos;
    }

    public Player player() {
        return player;
    }

    /** Server-side block entity; null on the client. */
    public T blockEntity() {
        return blockEntity;
    }

    protected void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, left + col * 18, top + 58));
        }
    }

    /** Fills the snapshot sent to the client. Only called on the server. */
    protected abstract void writeData(CompoundTag tag);

    /** Server-side handling of a button id. */
    protected abstract boolean onButton(Player player, int id);

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null && player instanceof ServerPlayer serverPlayer) {
            CompoundTag snapshot = new CompoundTag();
            writeData(snapshot);
            if (!snapshot.equals(lastSent)) {
                lastSent = snapshot;
                PacketDistributor.sendToPlayer(serverPlayer, new MenuSyncPayload(containerId, snapshot));
            }
        }
    }

    @Override
    public void applySync(CompoundTag data) {
        this.data = data;
    }

    @Override
    public CompoundTag data() {
        return data;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return false;
        }
        boolean changed = onButton(player, id);
        if (changed) {
            blockEntity.setChanged();
        }
        return changed;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity != null && blockEntity.isRemoved()) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    /** Index of the first player-inventory slot in this menu. */
    protected abstract int playerSlotStart();

    /** Shift-click from the player inventory into the machine; returns the remainder. */
    protected abstract ItemStack moveIntoMachine(ItemStack stack);

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int start = playerSlotStart();
        if (index >= start) {
            ItemStack remainder = moveIntoMachine(stack.copy());
            if (remainder.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.set(remainder);
        } else {
            if (!moveItemStackTo(stack, start, start + PLAYER_INVENTORY_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
            slot.setChanged();
        }
        return original;
    }

    /**
     * Takes items of a machine slot the way a vanilla slot click does: a full stack, or half of it (rounded up) for
     * a right click; to the inventory with Shift, otherwise to the cursor.
     */
    public static boolean take(Player player, ItemQueue queue, int kind, boolean half, boolean toInventory) {
        if (kind >= queue.kinds()) {
            return false;
        }
        ItemStack prototype = queue.prototype(kind);
        long available = Math.min(queue.count(kind), prototype.getMaxStackSize());
        long wanted = half ? (available + 1) / 2 : available;
        long removed = queue.remove(kind, wanted, false);
        if (removed <= 0) {
            return false;
        }
        giveToPlayer(player, prototype.copyWithCount((int) removed), toInventory);
        return true;
    }

    /**
     * Creative middle click, as the vanilla clone click: a full stack of the slot's item goes to the empty cursor and
     * the machine keeps its items.
     */
    public static boolean pick(Player player, ItemQueue queue, int kind) {
        if (!player.hasInfiniteMaterials() || kind >= queue.kinds() || !player.containerMenu.getCarried().isEmpty()) {
            return false;
        }
        ItemStack prototype = queue.prototype(kind);
        player.containerMenu.setCarried(prototype.copyWithCount(prototype.getMaxStackSize()));
        return true;
    }

    /** Inserts the carried stack, or a single item of it ({@code one}), through the machine's insertion strategy. */
    public static boolean insertCarried(AbstractContainerMenu menu, boolean one, UnaryOperator<ItemStack> inserter) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            return false;
        }
        ItemStack offered = one ? carried.copyWithCount(1) : carried.copy();
        ItemStack remainder = inserter.apply(offered);
        int inserted = offered.getCount() - remainder.getCount();
        if (inserted <= 0) {
            return false;
        }
        carried.shrink(inserted);
        menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        return true;
    }

    /** Gives a stack to the player: carried slot first, then inventory, finally dropped. */
    public static void giveToPlayer(Player player, ItemStack stack, boolean toInventory) {
        if (stack.isEmpty()) {
            return;
        }
        if (!toInventory) {
            ItemStack carried = player.containerMenu.getCarried();
            if (carried.isEmpty()) {
                player.containerMenu.setCarried(stack);
                return;
            }
            if (ItemStack.isSameItemSameComponents(carried, stack) && carried.getCount() + stack.getCount() <= carried.getMaxStackSize()) {
                carried.grow(stack.getCount());
                return;
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
