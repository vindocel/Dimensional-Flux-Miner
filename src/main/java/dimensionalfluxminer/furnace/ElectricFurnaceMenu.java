package dimensionalfluxminer.furnace;

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

public class ElectricFurnaceMenu extends MachineMenu<ElectricFurnaceBlockEntity> {
    /** Player inventory right under the processing element (187 px panel). */
    public static final int INVENTORY_LEFT = 8;
    public static final int INVENTORY_TOP = 105;

    public ElectricFurnaceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null);
    }

    public ElectricFurnaceMenu(int containerId, Inventory inventory, ElectricFurnaceBlockEntity be) {
        this(containerId, inventory, be.getBlockPos(), be);
    }

    private ElectricFurnaceMenu(int containerId, Inventory inventory, BlockPos pos, ElectricFurnaceBlockEntity be) {
        super(DfmMenus.ELECTRIC_FURNACE.get(), containerId, inventory, pos, be);
        addSlot(new WindowSlot(be != null ? be.upgradeSlot() : new ItemStackHandler(1), 0,
                stack -> stack.is(DfmItems.EFFICIENCY_UPGRADE.get()), 1));
        addPlayerInventory(inventory, INVENTORY_LEFT, INVENTORY_TOP);
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
        return blockEntity.insertAggregate(stack, false);
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
