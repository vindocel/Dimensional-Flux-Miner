package dimensionalfluxminer.machine;

import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.content.MachineState;
import dimensionalfluxminer.machine.math.DynamicEnergyBuffer;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideConfiguration;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.registry.DfmItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared behaviour of the single-block machines (Crusher and Smelter): dynamic energy buffer, Rate 1..10,
 * Efficiency Upgrade slot, Auto-Input/Auto-Eject toggles (default OFF), per-transmission side configuration,
 * whole-block redstone control (ignored / high / low) and external tick acceleration (each extra ticker call in the same game
 * time runs one more complete logical cycle).
 */
public abstract class AbstractMachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BUTTON_RATE_DOWN = 0;
    public static final int BUTTON_RATE_UP = 1;
    public static final int BUTTON_AUTO_INPUT = 2;
    public static final int BUTTON_AUTO_EJECT = 3;
    /** Redstone control: next mode / previous mode (IGNORED, HIGH, LOW). */
    public static final int BUTTON_REDSTONE_NEXT = 6;
    public static final int BUTTON_REDSTONE_PREVIOUS = 7;
    /** {@code BUTTON_SIDE_BASE + face * 16 + transmission * 2 + (forward ? 0 : 1)} */
    public static final int BUTTON_SIDE_BASE = 1000;
    /** Rate selector: next level (x10 wraps to x1) and previous level (x1 wraps to x10). */
    public static final int BUTTON_RATE_CYCLE = 8;
    public static final int BUTTON_RATE_CYCLE_BACK = 9;
    /** {@code BUTTON_SIDE_CLEAR + transmission} */
    public static final int BUTTON_SIDE_CLEAR = 2000;
    /** {@code BUTTON_SIDE_BATCH + transmission * 2 + (forward ? 0 : 1)}: every configurable face to the batch target. */
    public static final int BUTTON_SIDE_BATCH = 2100;
    /** Every configurable face of every transmission back to NONE. */
    public static final int BUTTON_SIDE_CLEAR_ALL = 2200;
    /** Automation mode cycle: OFF -> Auto Input -> Auto Eject -> Input + Eject (and back). */
    public static final int BUTTON_AUTO_IO_NEXT = 2300;
    public static final int BUTTON_AUTO_IO_PREVIOUS = 2301;

    protected final DynamicEnergyBuffer energyBuffer = new DynamicEnergyBuffer(0);
    protected final MachineEnergyStorage energy = new MachineEnergyStorage(energyBuffer, this::setChanged);
    protected final SideConfiguration sides;
    protected final LogicalTickCounter tickCounter = new LogicalTickCounter();
    private final AccelerationDemand accelerationDemand = new AccelerationDemand();
    protected final ItemStackHandler upgradeSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(DfmItems.EFFICIENCY_UPGRADE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    protected int rate = RateScale.MIN_LEVEL;
    protected boolean autoInput;
    protected boolean autoEject;
    /** Whether the last logical cycle advanced work (drives ACTIVE). */
    protected boolean workedThisTick;
    /** The machine stays ACTIVE for half a second after the work pauses, so short gaps do not flicker it. */
    private static final int ACTIVE_LINGER_TICKS = 10;
    private int activeLinger;
    protected boolean redstoneHalt;
    protected RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    protected String status = "idle";

    private final Map<Direction, IItemHandler> itemHandlers = new EnumMap<>(Direction.class);

    protected AbstractMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, SideRules rules) {
        super(type, pos, state);
        this.sides = new SideConfiguration(rules);
    }

    // ---------------------------------------------------------------- configuration

    public Direction facing() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public int rate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = RateScale.clampLevel(rate);
        setChanged();
    }

    public void setAutoInput(boolean autoInput) {
        this.autoInput = autoInput;
        setChanged();
    }

    public void setAutoEject(boolean autoEject) {
        this.autoEject = autoEject;
        setChanged();
    }

    /** Sets a route directly (used by tests and tools); returns false if the route is not allowed there. */
    public boolean configureSide(RelativeFace face, Transmission transmission, SideRoute route) {
        boolean ok = sides.set(face, transmission, itemProfile(), route);
        if (ok) {
            onSidesChanged();
        }
        return ok;
    }

    public DynamicEnergyBuffer energyBuffer() {
        return energyBuffer;
    }

    public String status() {
        return status;
    }

    public boolean hasEfficiencyUpgrade() {
        return upgradeSlot.getStackInSlot(0).is(DfmItems.EFFICIENCY_UPGRADE.get());
    }

    public ItemStackHandler upgradeSlot() {
        return upgradeSlot;
    }

    public SideConfiguration sides() {
        return sides;
    }

    /** Item profile used for ITEM routes (Smelter NORMAL/ALLOY). */
    public int itemProfile() {
        return SideRules.PROFILE_NORMAL;
    }

    public SideRoute route(RelativeFace face, Transmission transmission) {
        return sides.get(face, transmission, itemProfile());
    }

    public SideRoute route(Direction side, Transmission transmission) {
        return route(RelativeFace.of(facing(), side), transmission);
    }

    /** Effective FE/t power limit of the selected Rate (Efficiency Upgrade applied). */
    public abstract long effectivePowerLimit();

    public MachineEnergyStorage energy() {
        return energy;
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractMachineBlockEntity be) {
        boolean normalCall = be.tickCounter.register(level.getGameTime());
        be.logicalCycle(normalCall);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, AbstractMachineBlockEntity be) {
        be.clientEffects();
        ClientHooks.machineSound.accept(be);
    }

    /**
     * Runs one complete logical cycle. Called once per server tick and again for every extra accelerator
     * call within the same game time; every requirement is re-validated each time.
     */
    protected void logicalCycle(boolean normalCall) {
        long gameTime = level != null ? level.getGameTime() : 0;
        if (!normalCall) {
            accelerationDemand.recordExtra(gameTime);
        }
        energyBuffer.setTarget(RateScale.energyBufferTarget(effectivePowerLimit()));
        // The input also accepts what the accelerated cycles of one game time use, so they run at full power.
        energyBuffer.setHeadroom(accelerationDemand.extraPerTick(gameTime) * effectivePowerLimit());
        redstoneHalt = !redstoneAllows();
        workedThisTick = false;
        if (autoInput) {
            autoInputCycle();
        }
        if (!redstoneHalt) {
            workedThisTick = processCycle();
        } else {
            status = "redstone_halt";
        }
        if (autoEject) {
            autoEjectCycle();
        }
        updateMachineState();
    }

    /** Machine work for one logical cycle; returns true if progress advanced. */
    protected abstract boolean processCycle();

    protected abstract void autoInputCycle();

    protected abstract void autoEjectCycle();

    protected void clientEffects() {
    }

    /** Redstone gate of the whole block: a signal from any side counts. */
    protected boolean redstoneAllows() {
        if (level == null || redstoneMode == RedstoneMode.IGNORED) {
            return true;
        }
        return redstoneMode.allows(level.hasNeighborSignal(worldPosition));
    }

    public RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        redstoneMode = mode;
        setChanged();
    }

    protected void updateMachineState() {
        if (level == null) {
            return;
        }
        // Short gaps in the work keep the machine ACTIVE (light, sound, animation); a redstone halt ends it at once.
        if (workedThisTick) {
            activeLinger = ACTIVE_LINGER_TICKS;
        } else if (redstoneHalt) {
            activeLinger = 0;
        } else if (activeLinger > 0) {
            activeLinger--;
        }
        boolean active = workedThisTick || activeLinger > 0;
        MachineState next;
        if (energyBuffer.stored() <= 0 && !active) {
            next = MachineState.OFF;
        } else if (active) {
            next = MachineState.ACTIVE;
        } else {
            next = MachineState.IDLE;
        }
        BlockState state = getBlockState();
        if (state.getValue(DfmStateProperties.MACHINE_STATE) != next) {
            level.setBlock(worldPosition, state.setValue(DfmStateProperties.MACHINE_STATE, next), Block.UPDATE_ALL);
        }
    }

    public MachineState machineState() {
        return getBlockState().getValue(DfmStateProperties.MACHINE_STATE);
    }

    // ---------------------------------------------------------------- capabilities

    /**
     * Item handler exposed on a world side, or null when that side has no item route. The {@code null} side is a
     * read-only view of every input and output (information tools such as Jade).
     */
    public IItemHandler itemHandlerFor(Direction side) {
        if (side == null) {
            return ReadOnlyViews.items(createItemHandler(SideRoute.ITEM_INPUT_OUTPUT_ALL));
        }
        SideRoute route = route(side, Transmission.ITEM);
        if (route == SideRoute.NONE) {
            return null;
        }
        return itemHandlers.computeIfAbsent(side, s -> createItemHandler(route(s, Transmission.ITEM)));
    }

    protected abstract IItemHandler createItemHandler(SideRoute route);

    /** Energy input on configured faces; the {@code null} side is a read-only view of the buffer. */
    public IEnergyStorage energyFor(Direction side) {
        if (side == null) {
            return ReadOnlyViews.energy(energy);
        }
        if (route(side, Transmission.ENERGY) != SideRoute.ENERGY_INPUT) {
            return null;
        }
        return energy;
    }

    /** Clears cached handlers and tells NeoForge that capabilities changed. */
    protected void invalidateSides() {
        itemHandlers.clear();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    /** World directions whose ITEM route matches the predicate. */
    protected List<Direction> itemSides(java.util.function.Predicate<SideRoute> predicate) {
        List<Direction> result = new ArrayList<>();
        for (RelativeFace face : RelativeFace.VALUES) {
            SideRoute route = route(face, Transmission.ITEM);
            if (route != SideRoute.NONE && predicate.test(route)) {
                result.add(face.toDirection(facing()));
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- menu actions

    /** Applies a validated GUI button press on the server. */
    public boolean handleButton(Player player, int id) {
        if (id == BUTTON_RATE_DOWN || id == BUTTON_RATE_UP) {
            int next = RateScale.clampLevel(rate + (id == BUTTON_RATE_UP ? 1 : -1));
            if (next == rate) {
                return false;
            }
            rate = next;
            setChanged();
            return true;
        }
        if (id == BUTTON_RATE_CYCLE || id == BUTTON_RATE_CYCLE_BACK) {
            rate = RateScale.cycleLevel(rate, id == BUTTON_RATE_CYCLE);
            setChanged();
            return true;
        }
        if (id == BUTTON_REDSTONE_NEXT || id == BUTTON_REDSTONE_PREVIOUS) {
            redstoneMode = redstoneMode.cycle(id == BUTTON_REDSTONE_NEXT);
            setChanged();
            return true;
        }
        if (id == BUTTON_AUTO_IO_NEXT || id == BUTTON_AUTO_IO_PREVIOUS) {
            // Bit 0 = Auto Input, bit 1 = Auto Eject: 0 OFF, 1 input, 2 eject, 3 both.
            int mode = (autoInput ? 1 : 0) | (autoEject ? 2 : 0);
            int next = Math.floorMod(mode + (id == BUTTON_AUTO_IO_NEXT ? 1 : -1), 4);
            autoInput = (next & 1) != 0;
            autoEject = (next & 2) != 0;
            setChanged();
            return true;
        }
        if (id == BUTTON_AUTO_INPUT) {
            autoInput = !autoInput;
            setChanged();
            return true;
        }
        if (id == BUTTON_AUTO_EJECT) {
            autoEject = !autoEject;
            setChanged();
            return true;
        }
        if (id >= BUTTON_SIDE_BASE && id < BUTTON_SIDE_BASE + RelativeFace.VALUES.length * 16) {
            int local = id - BUTTON_SIDE_BASE;
            int face = local / 16;
            int transmission = (local % 16) / 2;
            boolean forward = local % 2 == 0;
            if (transmission >= Transmission.VALUES.length) {
                return false;
            }
            if (sides.cycle(RelativeFace.VALUES[face], Transmission.VALUES[transmission], itemProfile(), forward)) {
                onSidesChanged();
                return true;
            }
            return false;
        }
        if (id >= BUTTON_SIDE_CLEAR && id < BUTTON_SIDE_CLEAR + Transmission.VALUES.length) {
            sides.clear(Transmission.VALUES[id - BUTTON_SIDE_CLEAR], itemProfile());
            onSidesChanged();
            return true;
        }
        if (id >= BUTTON_SIDE_BATCH && id < BUTTON_SIDE_BATCH + Transmission.VALUES.length * 2) {
            Transmission transmission = Transmission.VALUES[(id - BUTTON_SIDE_BATCH) / 2];
            boolean forward = (id - BUTTON_SIDE_BATCH) % 2 == 0;
            SideRoute target = sides.batchTarget(transmission, itemProfile(), forward);
            if (sides.applyAll(transmission, itemProfile(), target)) {
                onSidesChanged();
                return true;
            }
            return false;
        }
        if (id == BUTTON_SIDE_CLEAR_ALL) {
            for (Transmission transmission : Transmission.VALUES) {
                sides.clear(transmission, itemProfile());
            }
            onSidesChanged();
            return true;
        }
        return false;
    }

    protected void onSidesChanged() {
        invalidateSides();
        setChanged();
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    /** Snapshot sent to open menus (server -> client). */
    public void writeMenuData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("rate", rate);
        tag.putBoolean("auto_input", autoInput);
        tag.putBoolean("auto_eject", autoEject);
        tag.putLong("fe", energyBuffer.stored());
        tag.putLong("fe_capacity", energyBuffer.capacity());
        tag.putLong("power", effectivePowerLimit());
        tag.putBoolean("redstone_halt", redstoneHalt);
        tag.putString("redstone_mode", redstoneMode.name());
        tag.putString("status", status);
        tag.putString("state", machineState().getSerializedName());
        tag.putInt("profile", itemProfile());
        tag.put("sides", sides.save());
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", energyBuffer.stored());
        tag.putInt("rate", rate);
        tag.putBoolean("auto_input", autoInput);
        tag.putBoolean("auto_eject", autoEject);
        tag.putString("redstone_mode", redstoneMode.name());
        tag.put("sides", sides.save());
        tag.put("upgrade", upgradeSlot.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyBuffer.setStored(tag.getLong("energy"));
        rate = RateScale.clampLevel(tag.contains("rate") ? tag.getInt("rate") : RateScale.MIN_LEVEL);
        autoInput = tag.getBoolean("auto_input");
        autoEject = tag.getBoolean("auto_eject");
        redstoneMode = RedstoneMode.byName(tag.getString("redstone_mode"));
        sides.load(tag.getCompound("sides"));
        upgradeSlot.deserializeNBT(registries, tag.getCompound("upgrade"));
        itemHandlers.clear();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClientData(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientData(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            readClientData(tag, registries);
        }
    }

    /** Minimal render data for clients tracking the chunk (visual descriptors only). */
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void syncToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Physical ItemStacks dropped when the machine is broken (FE, progress and configuration are lost). */
    public void dropContents() {
        if (level == null) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        collectDrops(drops);
        ItemStack upgrade = upgradeSlot.getStackInSlot(0);
        if (!upgrade.isEmpty()) {
            drops.add(upgrade.copy());
        }
        for (ItemStack stack : drops) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
    }

    protected abstract void collectDrops(List<ItemStack> drops);
}
