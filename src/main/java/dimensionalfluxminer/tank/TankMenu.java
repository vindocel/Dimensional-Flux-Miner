package dimensionalfluxminer.tank;

import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.registry.DfmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Dimensional Tank screen: no machine slots; liquids, code and sides travel in the snapshot, actions as button ids. */
public class TankMenu extends MachineMenu<DimensionalTankBlockEntity> {
    public static final int INVENTORY_TOP = 99;

    /** Client constructor. */
    public TankMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null);
    }

    /** Server constructor. */
    public TankMenu(int containerId, Inventory inventory, DimensionalTankBlockEntity be) {
        this(containerId, inventory, be.getBlockPos(), be);
    }

    private TankMenu(int containerId, Inventory inventory, BlockPos pos, DimensionalTankBlockEntity be) {
        super(DfmMenus.DIMENSIONAL_TANK.get(), containerId, inventory, pos, be);
        addPlayerInventory(inventory, 8, INVENTORY_TOP);
    }

    @Override
    protected int playerSlotStart() {
        return 0;
    }

    @Override
    protected ItemStack moveIntoMachine(ItemStack stack) {
        return stack;
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
