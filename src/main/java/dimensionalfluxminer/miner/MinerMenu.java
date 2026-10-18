package dimensionalfluxminer.miner;

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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Main Miner screen: Core slot and the 27 output slots (9 x 3) always visible; the upgrade slot lives in a window. */
public class MinerMenu extends MachineMenu<MinerControllerBlockEntity> {
    /** Core slot right of the terminal, its bottom level with the terminal's. */
    public static final int CORE_X = 151;
    public static final int CORE_Y = 56;
    public static final int OUTPUT_X = 8;
    public static final int OUTPUT_Y = 92;
    public static final int INVENTORY_TOP = 160;

    public MinerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), null);
    }

    public MinerMenu(int containerId, Inventory inventory, MinerControllerBlockEntity be) {
        this(containerId, inventory, be.getBlockPos(), be);
    }

    private MinerMenu(int containerId, Inventory inventory, BlockPos pos, MinerControllerBlockEntity be) {
        super(DfmMenus.MINER.get(), containerId, inventory, pos, be);
        IItemHandler core = be != null ? be.coreSlot() : new ItemStackHandler(1);
        IItemHandler upgrade = be != null ? be.upgradeSlot() : new ItemStackHandler(1);
        IItemHandler output = be != null ? be.output() : new ItemStackHandler(MinerControllerBlockEntity.OUTPUT_SLOTS);
        addSlot(new SlotItemHandler(core, 0, CORE_X, CORE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DfmItems.isCore(stack.getItem());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        // Until a Core is inserted only the Core slot can be used: the upgrade and output slots are locked.
        addSlot(new WindowSlot(upgrade, 0, stack -> stack.is(DfmItems.EFFICIENCY_UPGRADE.get()), 1) {
            @Override
            public boolean mayPickup(Player player) {
                return !locked() && super.mayPickup(player);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !locked() && super.mayPlace(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(output, col + row * 9, OUTPUT_X + col * 18, OUTPUT_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return !locked() && super.mayPickup(player);
                    }
                });
            }
        }
        addPlayerInventory(inventory, 8, INVENTORY_TOP);
    }

    /** No Core in the Core slot: everything but that slot is locked (both sides read the synced slot). */
    public boolean locked() {
        return !slots.get(0).hasItem();
    }

    public WindowSlot upgradeSlot() {
        return (WindowSlot) slots.get(1);
    }

    @Override
    protected int playerSlotStart() {
        return 2 + MinerControllerBlockEntity.OUTPUT_SLOTS;
    }

    @Override
    protected ItemStack moveIntoMachine(ItemStack stack) {
        if (blockEntity == null) {
            return stack;
        }
        if (blockEntity.locked() && !DfmItems.isCore(stack.getItem())) {
            return stack;
        }
        if (DfmItems.isCore(stack.getItem()) && blockEntity.coreSlot().getStackInSlot(0).isEmpty()) {
            return blockEntity.coreSlot().insertItem(0, stack, false);
        }
        if (stack.is(DfmItems.EFFICIENCY_UPGRADE.get())) {
            return blockEntity.upgradeSlot().insertItem(0, stack, false);
        }
        return stack;
    }

    @Override
    protected void writeData(CompoundTag tag) {
        blockEntity.writeMenuData(tag);
    }

    @Override
    protected boolean onButton(Player player, int id) {
        return blockEntity.handleButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && (blockEntity == null || blockEntity.formed());
    }
}
