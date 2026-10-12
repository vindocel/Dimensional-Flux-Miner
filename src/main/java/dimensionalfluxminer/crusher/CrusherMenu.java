package dimensionalfluxminer.crusher;

import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.menu.WindowSlot;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class CrusherMenu extends MachineMenu<DimensionalCrusherBlockEntity> {
    /** Player inventory under the processing mechanism (181 px panel). */
    public static final int INVENTORY_TOP = 99;

    /** Client constructor. */
    public CrusherMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null);
    }

    /** Server constructor. */
    public CrusherMenu(int containerId, Inventory inventory, DimensionalCrusherBlockEntity be) {
        this(containerId, inventory, be.getBlockPos(), be);
    }

    private CrusherMenu(int containerId, Inventory inventory, BlockPos pos, DimensionalCrusherBlockEntity be) {
        super(DfmMenus.CRUSHER.get(), containerId, inventory, pos, be);
        addSlot(new WindowSlot(be != null ? be.upgradeSlot() : new ItemStackHandler(1), 0,
                stack -> stack.is(DfmItems.EFFICIENCY_UPGRADE.get()), 1));
        addPlayerInventory(inventory, 8, INVENTORY_TOP);
    }

    public WindowSlot upgradeSlot() {
        return (WindowSlot) slots.getFirst();
    }

    @Override
    protected int playerSlotStart() {
        return 1;
    }

    @Override
    protected ItemStack moveIntoMachine(ItemStack stack) {
        if (blockEntity == null) {
            return stack;
        }
        if (blockEntity.upgradeSlot().isItemValid(0, stack)) {
            return blockEntity.upgradeSlot().insertItem(0, stack, false);
        }
        return blockEntity.insertInput(stack, false);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        blockEntity.writeMenuData(tag, player.registryAccess());
    }

    @Override
    protected boolean onButton(Player player, int id) {
        return blockEntity.handleButton(player, id);
    }
}
