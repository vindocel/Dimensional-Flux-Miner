package dimensionalfluxminer.machine.transfer;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu slot over one slot of an {@link ItemSlots}: placing asks the slots, taking extracts from them. */
public class SlotsSlot extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final ItemSlots slots;
    protected final int index;

    public SlotsSlot(ItemSlots slots, int index, int x, int y) {
        super(EMPTY, index, x, y);
        this.slots = slots;
        this.index = index;
    }

    public ItemSlots getItemHandler() {
        return slots;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && slots.isItemValid(index, stack);
    }

    @Override
    public ItemStack getItem() {
        return slots.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        ((ItemSlots.Modifiable) slots).setStackInSlot(index, stack);
        setChanged();
    }

    @Override
    public void onQuickCraft(ItemStack picked, ItemStack original) {
    }

    @Override
    public int getMaxStackSize() {
        return slots.getSlotLimit(index);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), slots.getSlotLimit(index));
    }

    @Override
    public boolean mayPickup(Player player) {
        return !slots.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return slots.extractItem(index, amount, false);
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof SlotsSlot slot && slot.slots == slots;
    }
}
