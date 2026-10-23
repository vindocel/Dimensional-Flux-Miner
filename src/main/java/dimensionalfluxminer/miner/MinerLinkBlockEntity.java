package dimensionalfluxminer.miner;

import dimensionalfluxminer.machine.transfer.EnergyStore;
import dimensionalfluxminer.machine.transfer.ItemSlots;
import dimensionalfluxminer.machine.transfer.Transfer;
import dimensionalfluxminer.machine.LogicalTickCounter;
import dimensionalfluxminer.registry.DfmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Link from a steel chassis or dimensional port to its formed controller. The controller answers the capability of
 * each configurable face ({@code MinerPorts}); the lower-layer ports and chassis act as lightweight tick-acceleration
 * receivers.
 */
public class MinerLinkBlockEntity extends dimensionalfluxminer.content.TagBlockEntity {
    private BlockPos controller;
    private final LogicalTickCounter tickCounter = new LogicalTickCounter();

    public MinerLinkBlockEntity(BlockPos pos, BlockState state) {
        super(DfmBlockEntities.MINER_LINK.get(), pos, state);
    }

    public void link(BlockPos controller) {
        this.controller = controller.immutable();
        setChanged();
    }

    public void unlink() {
        this.controller = null;
        setChanged();
    }

    public MinerControllerBlockEntity controller() {
        if (controller == null || level == null || !level.isLoaded(controller)) {
            return null;
        }
        return level.getBlockEntity(controller) instanceof MinerControllerBlockEntity c && c.formed() ? c : null;
    }

    /**
     * The first ticker call of a game time is the receiver's normal tick and adds no work; every additional
     * call in the same game time (external acceleration) asks the controller for one extra logical cycle.
     * Only the eight lower-layer blocks around the terminal (seven ports and the center chassis) are receivers.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, MinerLinkBlockEntity be) {
        if (be.tickCounter.register(level.getGameTime())) {
            dimensionalfluxminer.compat.Ae2Compat.pull(be, be::energyStorage);
            return;
        }
        MinerControllerBlockEntity controller = be.controller();
        if (controller == null) {
            return;
        }
        MinerStructure structure = controller.structure();
        if (structure != null && structure.member(pos).map(m -> m.slot().accelerationReceiver()).orElse(false)) {
            controller.acceleratedCycle();
        }
    }

    public ItemSlots itemHandler(Direction side) {
        MinerControllerBlockEntity c = controller();
        return c == null ? null : c.itemPort(worldPosition, side);
    }

    public EnergyStore energyStorage(Direction side) {
        MinerControllerBlockEntity c = controller();
        return c == null ? null : c.energyPort(worldPosition, side);
    }

    /** The controller's port of this face, as the NeoForge item capability. */
    public ResourceHandler<ItemResource> exposedItems(Direction side) {
        MinerControllerBlockEntity c = controller();
        return c == null ? null : Transfer.exposeItems(c.itemPort(worldPosition, side), c.journal());
    }

    public EnergyHandler exposedEnergy(Direction side) {
        MinerControllerBlockEntity c = controller();
        return c == null ? null : Transfer.exposeEnergy(c.energyPort(worldPosition, side), c.journal());
    }

    @Override
    protected void saveTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveTag(tag, registries);
        if (controller != null) {
            tag.store("controller", BlockPos.CODEC, controller);
        }
    }

    @Override
    protected void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        controller = tag.read("controller", BlockPos.CODEC).orElse(null);
    }
}
