package dimensionalfluxminer.miner;

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
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Link from a steel chassis or dimensional port to its formed controller. The controller answers the capability of
 * each configurable face ({@code MinerPorts}); the lower-layer ports and chassis act as lightweight tick-acceleration
 * receivers.
 */
public class MinerLinkBlockEntity extends BlockEntity {
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

    public IItemHandler itemHandler(Direction side) {
        MinerControllerBlockEntity c = controller();
        return c == null ? null : c.itemPort(worldPosition, side);
    }

    public IEnergyStorage energyStorage(Direction side) {
        MinerControllerBlockEntity c = controller();
        return c == null ? null : c.energyPort(worldPosition, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controller != null) {
            tag.put("controller", NbtUtils.writeBlockPos(controller));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controller = NbtUtils.readBlockPos(tag, "controller").orElse(null);
    }
}
