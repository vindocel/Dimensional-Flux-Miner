package dimensionalfluxminer.menu;

//? if >=26.1 {
import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.SlotsSlot;
//?}
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
*///?}

/**
 * Real container slot shown inside a floating screen window. The client screen moves it with the window and hides
 * it while the window is closed; the server always keeps it active.
 */
//? if >=26.1 {
public class WindowSlot extends SlotsSlot {
//?} else {
/*public class WindowSlot extends SlotItemHandler {
*///?}
    private final Predicate<ItemStack> filter;
    private final int maxStack;
    private boolean shown = true;

    //? if >=26.1 {
    public WindowSlot(ItemSlots handler, int index, Predicate<ItemStack> filter, int maxStack) {
    //?} else {
    /*public WindowSlot(IItemHandler handler, int index, Predicate<ItemStack> filter, int maxStack) {
    *///?}
        super(handler, index, 0, 0);
        this.filter = filter;
        this.maxStack = maxStack;
    }

    /** Client only: shows the slot at a menu-relative position, or hides it. */
    public void place(boolean shown, int x, int y) {
        this.shown = shown;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean isActive() {
        return shown;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return filter.test(stack) && super.mayPlace(stack);
    }

    @Override
    public int getMaxStackSize() {
        return Math.min(maxStack, super.getMaxStackSize());
    }
}
