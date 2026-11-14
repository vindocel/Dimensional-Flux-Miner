package dimensionalfluxminer.tank;

import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.registry.DfmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Tank menu with two container slots on the ordinary single tank and a snapshot of its liquid, code and sides. */
public class TankMenu extends MachineMenu<DimensionalTankBlockEntity> {
    public static final int INVENTORY_TOP = 99;
    public static final int CONTAINER_X = 151;
    public static final int DRAIN_Y = 23;
    public static final int FILL_Y = 56;
    private final boolean hasContainers;
    private final TankKind kind;

    /** Client constructor. */
    public TankMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null, buf.readEnum(TankKind.class));
    }

    /** Server constructor. */
    public TankMenu(int containerId, Inventory inventory, DimensionalTankBlockEntity be) {
        this(containerId, inventory, be.getBlockPos(), be, be.kind());
    }

    private TankMenu(int containerId, Inventory inventory, BlockPos pos, DimensionalTankBlockEntity be, TankKind kind) {
        super(DfmMenus.DIMENSIONAL_TANK.get(), containerId, inventory, pos, be);
        this.kind = kind;
        hasContainers = kind == TankKind.SINGLE;
        if (hasContainers) {
            var local = be == null ? new DimensionalTankBlockEntity(pos,
                    dimensionalfluxminer.registry.DfmBlocks.DIMENSIONAL_TANK.get().defaultBlockState()) : be;
            if (be == null) {
                local.setLevel(inventory.player.level());
            }
            var storage = local.containers();
            addSlot(new dimensionalfluxminer.machine.transfer.SlotsSlot(storage, 0, CONTAINER_X, DRAIN_Y));
            addSlot(new dimensionalfluxminer.machine.transfer.SlotsSlot(storage, 1, CONTAINER_X, FILL_Y));
        }
        addPlayerInventory(inventory, 8, INVENTORY_TOP);
    }

    public TankKind kind() {
        return kind;
    }

    @Override
    protected int playerSlotStart() {
        return hasContainers ? 2 : 0;
    }

    @Override
    protected ItemStack moveIntoMachine(ItemStack stack) {
        return blockEntity != null && hasContainers
                ? dimensionalfluxminer.machine.transfer.ItemSlots.insert(blockEntity.containers(), stack, false) : stack;
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
