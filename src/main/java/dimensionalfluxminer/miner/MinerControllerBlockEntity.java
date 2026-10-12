package dimensionalfluxminer.miner;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.machine.LogicalTickCounter;
import dimensionalfluxminer.machine.NeighborTransfer;
import dimensionalfluxminer.machine.ReadOnlyViews;
import dimensionalfluxminer.machine.math.DynamicEnergyBuffer;
import dimensionalfluxminer.machine.math.MinerProductionCurve;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.machine.math.WorkAccumulator;
import dimensionalfluxminer.miner.MinerLayout.PortRole;
import dimensionalfluxminer.miner.MinerVisualStage.CoreKind;
import dimensionalfluxminer.miner.corruption.CorruptionService;
import dimensionalfluxminer.miner.profile.ResourceFamily;
import dimensionalfluxminer.miner.profile.ResourceProfile;
import dimensionalfluxminer.miner.profile.ResourceProfiles;
import dimensionalfluxminer.miner.survey.MinerSurvey;
import dimensionalfluxminer.miner.survey.SurveyManager;
import dimensionalfluxminer.network.MinerFormationPayload;
import dimensionalfluxminer.registry.DfmBlockEntities;
import dimensionalfluxminer.registry.DfmItems;
import dimensionalfluxminer.registry.DfmSounds;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Logical controller of the Dimensional Flux Miner, living in the terminal. It owns every buffer of the formed
 * machine and drives production, the Basic environmental drain and the Stabilized Matter Injection.
 */
public class MinerControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OUTPUT_SLOTS = 27;
    public static final int MAX_ROLLS_PER_CYCLE = 64;
    private static final double LOAD_EMA_ALPHA = 0.0952380952;
    /** The animation and the sound go on for 1 s after the work pauses, so short gaps do not flicker them. */
    private static final int VISUAL_LINGER_TICKS = 20;

    public static final int BUTTON_RATE_DOWN = 0;
    public static final int BUTTON_RATE_UP = 1;
    public static final int BUTTON_AUTO_INPUT = 2;
    public static final int BUTTON_AUTO_EJECT = 3;
    public static final int BUTTON_REDSTONE = 6;
    public static final int BUTTON_RUN = 7;
    /** Flux Rate selector: next level (x10 wraps to x1) and previous level (x1 wraps to x10). */
    public static final int BUTTON_RATE_CYCLE = 8;
    public static final int BUTTON_RATE_CYCLE_BACK = 9;
    public static final int BUTTON_FILTER_CLEAR = 2999;
    /** {@code + index} in the synced family list. */
    public static final int BUTTON_FILTER_TOGGLE = 3000;

    // ---------------------------------------------------------------- persistent state
    private boolean formed;
    private final DynamicEnergyBuffer energyBuffer = new DynamicEnergyBuffer(0);
    private long dmUnits;
    private int rate = RateScale.MIN_LEVEL;
    private boolean runRequested;
    private boolean redstoneControl;
    private boolean autoInput;
    private boolean autoEject;
    private final Set<ResourceLocation> filter = new LinkedHashSet<>();
    private UUID owner;

    private final ItemStackHandler coreSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return DfmItems.isCore(stack.getItem());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            onCoreChanged();
        }
    };
    private final ItemStackHandler upgradeSlot = new ItemStackHandler(1) {
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
    /** 27 physical output-only slots (3 x 9). Only the machine inserts here. */
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // ---------------------------------------------------------------- runtime state
    private final LogicalTickCounter tickCounter = new LogicalTickCounter();
    private final dimensionalfluxminer.machine.AccelerationDemand accelerationDemand = new dimensionalfluxminer.machine.AccelerationDemand();
    private final WorkAccumulator work = new WorkAccumulator();
    private CoreKind lastCore = CoreKind.NONE;
    private MinerStatus status = MinerStatus.MULTIBLOCK_INVALID;
    private double workThisRealTick;
    private double smoothedLoad;
    /** Results per game tick, accelerated cycles included: what it could roll and what it really rolled. */
    private double potentialRate;
    private double actualRate;
    /** Strongest limit met by any cycle since the previous normal tick, and the one shown for the last tick. */
    private MinerStatus.LimitingFactor limitSinceRealTick = MinerStatus.LimitingFactor.NONE;
    private MinerStatus.LimitingFactor shownLimit = MinerStatus.LimitingFactor.NONE;
    private boolean processing;
    /** Ticks the processing visuals still last after the work paused (see {@link #VISUAL_LINGER_TICKS}). */
    private int visualLinger;
    /**
     * Game time of the last cycle that did work, normal or accelerated. Accelerated cycles may drain the buffer before
     * the normal tick of the same game time; the machine still counts as powered and working then.
     */
    private long lastWorkGameTime = Long.MIN_VALUE;
    private boolean wasPowered;
    private long animationStartTick;
    private String lastSubstrate = "";
    private long lastSubstrateUnits;
    private MinerStructure structure;

    // ---------------------------------------------------------------- client render state
    private MinerVisualStage visualStage = MinerVisualStage.NO_ENERGY;
    private int loadPercent;
    private Direction clientFront = Direction.SOUTH;

    private final IEnergyStorage energyPort = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!formed) {
                return 0;
            }
            long accepted = energyBuffer.receive(toReceive, simulate);
            if (!simulate && accepted > 0) {
                setChanged();
            }
            return (int) accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, energyBuffer.stored());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, energyBuffer.capacity());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return formed;
        }
    };

    /** Extraction-only view of the Output Buffer for the Item Output Port (no item limit per tick). */
    private final IItemHandler outputPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return OUTPUT_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return output.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return output.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return output.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    /** Insertion-only Matter Input (Stabilized Core): accepted substrate becomes DM immediately. */
    private final IItemHandler matterPort = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insertSubstrate(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return level != null && coreKind() == CoreKind.STABILIZED
                    && MatterValues.substrateUnits(stack, level.dimension()).isPresent();
        }
    };

    public MinerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(DfmBlockEntities.MINER_CONTROLLER.get(), pos, state);
    }

    // ---------------------------------------------------------------- accessors

    public boolean formed() {
        return formed;
    }

    public MinerStructure structure() {
        if (structure == null && formed) {
            structure = MinerStructure.layout(worldPosition, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return structure;
    }

    public ItemStackHandler coreSlot() {
        return coreSlot;
    }

    public ItemStackHandler upgradeSlot() {
        return upgradeSlot;
    }

    public ItemStackHandler output() {
        return output;
    }

    public DynamicEnergyBuffer energyBuffer() {
        return energyBuffer;
    }

    public long dmUnits() {
        return dmUnits;
    }

    public void setDmUnits(long units) {
        dmUnits = Math.max(0, Math.min(MatterValues.BUFFER_UNITS, units));
        setChanged();
    }

    public int rate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = RateScale.clampLevel(rate);
        setChanged();
    }

    public boolean runRequested() {
        return runRequested;
    }

    public void setRunRequested(boolean run) {
        runRequested = run && formed;
        setChanged();
    }

    public void setRedstoneControl(boolean on) {
        redstoneControl = on;
        setChanged();
    }

    public void setAutoInput(boolean on) {
        autoInput = on;
        setChanged();
    }

    public void setAutoEject(boolean on) {
        autoEject = on;
        setChanged();
    }

    public Set<ResourceLocation> filter() {
        return filter;
    }

    public MinerStatus status() {
        return status;
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public MinerVisualStage visualStage() {
        return visualStage;
    }

    public int loadPercent() {
        return loadPercent;
    }

    public long animationStartTick() {
        return animationStartTick;
    }

    public Direction clientFront() {
        return clientFront;
    }

    public boolean hasEfficiencyUpgrade() {
        return upgradeSlot.getStackInSlot(0).is(DfmItems.EFFICIENCY_UPGRADE.get());
    }

    public CoreKind coreKind() {
        ItemStack core = coreSlot.getStackInSlot(0);
        if (core.is(DfmItems.BASIC_DIMENSIONAL_CORE.get())) {
            return CoreKind.BASIC;
        }
        if (core.is(DfmItems.STABILIZED_DIMENSIONAL_CORE.get())) {
            return CoreKind.STABILIZED;
        }
        return CoreKind.NONE;
    }

    public static MinerProductionCurve curve() {
        double[] power = DfmConfig.range(DfmConfig.MINER_MIN_FE, DfmConfig.MINER_MAX_FE);
        double minRate = DfmConfig.SPEC.isLoaded() ? DfmConfig.MINER_MIN_RATE.get() : DfmConfig.MINER_MIN_RATE.getDefault();
        double maxRate = DfmConfig.SPEC.isLoaded() ? DfmConfig.MINER_MAX_RATE.get() : DfmConfig.MINER_MAX_RATE.getDefault();
        try {
            return new MinerProductionCurve(power[0], power[1], minRate, maxRate);
        } catch (IllegalArgumentException e) {
            return new MinerProductionCurve(500, 1_500_000, 0.0125, 64);
        }
    }

    /** Power limit of the selected Flux Rate; the Efficiency Upgrade never changes the Miner's FE. */
    public long powerLimit() {
        return Math.round(curve().levelPower(rate));
    }

    /**
     * Matter Input conversion: everything that fits in the DM buffer is accepted. There is no item limit per tick; the
     * energy available for the work (and for any acceleration) is what limits the machine.
     */
    public ItemStack insertSubstrate(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || level == null || !formed || coreKind() != CoreKind.STABILIZED) {
            return stack;
        }
        OptionalInt base = MatterValues.substrateUnits(stack, level.dimension());
        if (base.isEmpty() || base.getAsInt() <= 0) {
            return stack;
        }
        int perItem = MatterValues.matterInputUnits(base.getAsInt(), hasEfficiencyUpgrade());
        long space = MatterValues.BUFFER_UNITS - dmUnits;
        int byMatter = (int) Math.min(Integer.MAX_VALUE, space / perItem);
        int accepted = Math.min(stack.getCount(), byMatter);
        if (accepted <= 0) {
            return stack;
        }
        if (!simulate) {
            dmUnits += (long) accepted * perItem;
            lastSubstrate = stack.getItem().getDescriptionId();
            lastSubstrateUnits = perItem;
            setChanged();
        }
        return stack.copyWithCount(stack.getCount() - accepted);
    }

    // ---------------------------------------------------------------- port capabilities

    /** The {@code null} side of every miner block is a read-only view of the output buffer (Jade). */
    public IItemHandler itemPort(BlockPos portPos, Direction side) {
        MinerStructure s = structure();
        if (!formed || s == null) {
            return null;
        }
        if (side == null) {
            return ReadOnlyViews.items(output);
        }
        return s.member(portPos).filter(m -> m.outward() == side).map(m -> switch (m.slot().role()) {
            case ITEM_OUTPUT -> outputPort;
            case MATTER_INPUT -> coreKind() == CoreKind.STABILIZED ? matterPort : null;
            default -> null;
        }).orElse(null);
    }

    /** The {@code null} side of every miner block is a read-only view of the energy buffer (Jade). */
    public IEnergyStorage energyPort(BlockPos portPos, Direction side) {
        MinerStructure s = structure();
        if (!formed || s == null) {
            return null;
        }
        if (side == null) {
            return ReadOnlyViews.energy(energyPort);
        }
        return s.member(portPos).filter(m -> m.outward() == side && m.slot().role() == PortRole.ENERGY_INPUT)
                .map(m -> energyPort).orElse(null);
    }

    private void invalidatePorts() {
        if (level == null) {
            return;
        }
        MinerStructure s = MinerStructure.layout(worldPosition, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        for (MinerStructure.Member m : s.members()) {
            if (m.slot().role() != null) {
                level.invalidateCapabilities(m.pos());
            }
        }
    }

    // ---------------------------------------------------------------- formation

    /** Tries to form the assembly; returns a message for the player (null on success). */
    public Component tryForm() {
        if (formed || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        MinerStructure.Check check = MinerStructure.check(level, worldPosition);
        if (check.structure().isEmpty()) {
            return Component.translatable("message.dimensionalfluxminer.structure_incomplete",
                    Component.translatable(MinerStructure.blockFor(check.expected()).getDescriptionId()),
                    check.mismatch().getX(), check.mismatch().getY(), check.mismatch().getZ());
        }
        MinerStructure s = check.structure().get();
        if (s.anyMemberFormed(level)) {
            return Component.translatable("message.dimensionalfluxminer.structure_shared");
        }
        structure = s;
        formed = true;
        runRequested = false;
        work.reset();
        wasPowered = false;
        s.setFormed(level, true);
        for (MinerStructure.Member m : s.members()) {
            if (level.getBlockEntity(m.pos()) instanceof MinerLinkBlockEntity link) {
                link.link(worldPosition);
            }
        }
        invalidatePorts();
        setChanged();
        syncToClients();
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new net.minecraft.world.level.ChunkPos(worldPosition),
                new MinerFormationPayload(s.members().stream().map(MinerStructure.Member::pos).toList()));
        return null;
    }

    /**
     * Invalidates the formed machine: stops immediately, drops every physical ItemStack once (Core, upgrade,
     * Output Buffer), loses FE and DM, and restores the individual renders of the remaining parts.
     */
    public void invalidate() {
        if (!formed || level == null) {
            return;
        }
        MinerStructure s = structure();
        formed = false;
        runRequested = false;
        structure = null;
        Vec3 drop = s != null ? Vec3.atCenterOf(s.core()) : Vec3.atCenterOf(worldPosition);
        List<ItemStack> drops = new ArrayList<>();
        drops.add(coreSlot.getStackInSlot(0).copy());
        drops.add(upgradeSlot.getStackInSlot(0).copy());
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            drops.add(output.getStackInSlot(i).copy());
            output.setStackInSlot(i, ItemStack.EMPTY);
        }
        coreSlot.setStackInSlot(0, ItemStack.EMPTY);
        upgradeSlot.setStackInSlot(0, ItemStack.EMPTY);
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, drop.x, drop.y, drop.z, stack);
            }
        }
        energyBuffer.setStored(0);
        dmUnits = 0;
        work.reset();
        processing = false;
        status = MinerStatus.MULTIBLOCK_INVALID;
        if (s != null) {
            for (MinerStructure.Member m : s.members()) {
                if (level.getBlockEntity(m.pos()) instanceof MinerLinkBlockEntity link) {
                    link.unlink();
                }
            }
            s.setFormed(level, false);
        }
        invalidatePorts();
        updateVisual();
        setChanged();
        syncToClients();
    }

    private void onCoreChanged() {
        CoreKind now = coreKind();
        if (now != lastCore) {
            // Removing or replacing the Core always forces STOPPED.
            runRequested = false;
            lastCore = now;
            work.reset();
            invalidatePorts();
        }
        setChanged();
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, MinerControllerBlockEntity be) {
        // The terminal is not a tick-acceleration receiver: only the normal call of a game time does work.
        if (be.tickCounter.register(level.getGameTime())) {
            be.realTick();
        }
    }

    private void realTick() {
        cycle(true);
        noteLimit(status.limitingFactor());
        // Work of every cycle since the previous normal tick, accelerated ones included.
        double nominal = curve().levelRate(rate);
        double load = nominal > 0 ? Math.max(0, Math.min(1, workThisRealTick / nominal)) : 0;
        if (!processing && !workedRecently()) {
            load = 0;
        }
        int cycles = 1 + (level != null ? accelerationDemand.extraPerTick(level.getGameTime()) : 0);
        potentialRate = formed ? nominal * cycles : 0;
        actualRate = workThisRealTick;
        workThisRealTick = 0;
        shownLimit = limitSinceRealTick;
        limitSinceRealTick = MinerStatus.LimitingFactor.NONE;
        smoothedLoad += LOAD_EMA_ALPHA * (load - smoothedLoad);
        boolean powered = formed && (energyBuffer.stored() > 0 || workedRecently());
        if (powered && !wasPowered && level instanceof ServerLevel serverLevel) {
            BlockPos core = structure() != null ? structure().core() : worldPosition;
            serverLevel.playSound(null, core, DfmSounds.MINER_POWER_ON.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        wasPowered = powered;
        // Accelerated cycles already used this game time's energy: the machine is running, not waiting for energy.
        if (status == MinerStatus.WAITING_FOR_ENERGY && workedRecently()) {
            status = MinerStatus.RUNNING;
        }
        updateVisual();
    }

    /** Extra logical cycle requested by an accelerated lower steel chassis in the same game time. */
    public void acceleratedCycle() {
        if (level != null) {
            accelerationDemand.recordExtra(level.getGameTime());
        }
        cycle(false);
        noteLimit(status.limitingFactor());
    }

    /** Keeps the strongest limit met since the previous normal tick (enum order is the priority). */
    private void noteLimit(MinerStatus.LimitingFactor factor) {
        if (factor.ordinal() < limitSinceRealTick.ordinal()) {
            limitSinceRealTick = factor;
        }
    }

    /** Why matter ran out: nothing left to drain (Basic) or no substrate coming in (Stabilized). */
    private MinerStatus.LimitingFactor matterLimit(CoreKind core) {
        return core == CoreKind.BASIC ? MinerStatus.LimitingFactor.DIMENSIONAL_INTEGRITY : MinerStatus.LimitingFactor.DIMENSIONAL_MATTER;
    }

    private void cycle(boolean realTick) {
        processing = false;
        if (!formed || !(level instanceof ServerLevel serverLevel)) {
            status = MinerStatus.MULTIBLOCK_INVALID;
            return;
        }
        MinerProductionCurve curve = curve();
        long cyclePower = Math.round(curve.levelPower(rate));
        energyBuffer.setTarget(RateScale.energyBufferTarget(cyclePower));
        // The input also accepts what the accelerated cycles of one game time use, so they run at full power.
        energyBuffer.setHeadroom(accelerationDemand.extraPerTick(serverLevel.getGameTime()) * cyclePower);
        CoreKind core = coreKind();
        // Auto Eject / Auto Input work whenever they are switched on, also while the machine is off (so the DM buffer
        // fills before it starts); only a Redstone Control halt freezes them.
        boolean redstoneHalt = redstoneControl && !redstoneSignal();
        if (!redstoneHalt) {
            if (autoEject) {
                ejectOutput();
            }
            if (core == CoreKind.STABILIZED && autoInput) {
                pullSubstrate();
            }
        }
        if (core == CoreKind.NONE) {
            status = MinerStatus.CORE_MISSING;
            return;
        }
        if (!runRequested) {
            status = MinerStatus.MANUAL_STOP;
            return;
        }
        if (redstoneHalt) {
            status = MinerStatus.REDSTONE_HALT;
            return;
        }
        ResourceProfile profile = ResourceProfiles.get(serverLevel);
        if (profile.isEmpty()) {
            status = MinerStatus.NO_RESOURCES;
            return;
        }
        MinerSurvey survey = null;
        if (core == CoreKind.BASIC) {
            survey = SurveyManager.survey(serverLevel, structure().core());
            if (realTick) {
                SurveyManager.request(serverLevel, structure().core());
            }
            if (!survey.ready() && dmUnits < MatterValues.ROLL_COST_UNITS) {
                status = MinerStatus.DIMENSIONAL_SURVEY;
                return;
            }
        }
        if (!anyKeptResultFits(profile)) {
            status = MinerStatus.OUTPUT_BLOCKED;
            return;
        }
        double levelPower = curve.levelPower(rate);
        double available = Math.min(levelPower, energyBuffer.stored());
        double rateNow = curve.rateForPower(available);
        if (rateNow <= 0) {
            status = MinerStatus.WAITING_FOR_ENERGY;
            return;
        }
        if (available < levelPower) {
            noteLimit(MinerStatus.LimitingFactor.FLUX);
        }
        boolean canRefill = core == CoreKind.BASIC && survey.hasCandidates();
        if (dmUnits < MatterValues.ROLL_COST_UNITS && !canRefill) {
            status = core == CoreKind.BASIC ? MinerStatus.NO_DRAINABLE_MATTER : MinerStatus.WAITING_FOR_MATTER;
            noteLimit(matterLimit(core));
            return;
        }

        WorkAccumulator.Plan plan = work.plan(rateNow, MAX_ROLLS_PER_CYCLE);
        long executed = 0;
        MinerStatus stop = null;
        boolean missed = false;
        boolean upgrade = hasEfficiencyUpgrade();
        for (long i = 0; i < plan.operations(); i++) {
            if (dmUnits < MatterValues.ROLL_COST_UNITS) {
                // Basic: corruption is the source of matter, performed only when the buffer needs it.
                if (core == CoreKind.BASIC) {
                    drainColumn(serverLevel, survey);
                }
                if (dmUnits < MatterValues.ROLL_COST_UNITS) {
                    stop = core == CoreKind.BASIC ? MinerStatus.NO_DRAINABLE_MATTER : MinerStatus.WAITING_FOR_MATTER;
                    noteLimit(matterLimit(core));
                    break;
                }
            }
            ResourceFamily family = profile.rollFamily(serverLevel.random);
            ItemStack result = new ItemStack(family.pickVariant(serverLevel.random));
            boolean kept = filter.isEmpty() || filter.contains(family.key());
            if (kept) {
                if (!ItemHandlerHelper.insertItem(output, result, true).isEmpty()) {
                    // No room for this result: the roll is lost. It still takes its time and FE like any roll (only
                    // the DM is kept), so every result keeps its own chance and pace: a nearly full output never
                    // turns into fast production of the one ore that still fits.
                    missed = true;
                    executed++;
                    continue;
                }
                dmUnits -= MatterValues.ROLL_COST_UNITS;
                ItemHandlerHelper.insertItem(output, result, false);
            } else {
                dmUnits -= MatterValues.ROLL_COST_UNITS;
                if (upgrade) {
                    OptionalInt base = MatterValues.baseUnits(result);
                    int recovered = MatterValues.filterRecoveryUnits(base.orElse(5));
                    dmUnits = Math.min(MatterValues.BUFFER_UNITS, dmUnits + recovered);
                }
            }
            executed++;
        }
        plan = work.truncate(plan, executed);
        // FE follows the work really performed through the inverse curve P(R), never a linear scaling.
        double fePerTick = curve.powerForRate(plan.workDone());
        energyBuffer.consume(Math.round(Math.min(fePerTick, energyBuffer.stored())));
        work.commit(plan);
        workThisRealTick += plan.workDone();
        if (missed) {
            noteLimit(MinerStatus.LimitingFactor.OUTPUT);
        }
        processing = plan.workDone() > 0 || missed;
        if (processing) {
            lastWorkGameTime = serverLevel.getGameTime();
        }
        if (processing && animationStartTick == 0) {
            animationStartTick = serverLevel.getGameTime();
        }
        status = stop != null && executed == 0 ? stop : MinerStatus.RUNNING;
        if (autoEject) {
            ejectOutput();
        }
        setChanged();
    }

    /** Drains one valid cached column (revalidated now); returns true when 64 DM were credited. */
    private boolean drainColumn(ServerLevel level, MinerSurvey survey) {
        while (survey.hasCandidates()) {
            long packed = survey.pollCandidate();
            int x = (int) (packed >> 32);
            int z = (int) packed;
            if (dmUnits + MatterValues.COLUMN_UNITS > MatterValues.BUFFER_UNITS) {
                return false;
            }
            if (CorruptionService.corrupt(level, x, z, owner)) {
                dmUnits += MatterValues.COLUMN_UNITS;
                return true;
            }
        }
        return false;
    }

    /**
     * True when at least one result the machine keeps (all families, or the filtered ones) still fits in the output.
     * Only then is the output really blocked: a single roll that does not fit is just a discarded attempt.
     */
    private boolean anyKeptResultFits(ResourceProfile profile) {
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (output.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        for (ResourceFamily family : profile.families()) {
            if (!filter.isEmpty() && !filter.contains(family.key())) {
                continue;
            }
            for (ResourceFamily.Variant variant : family.variants()) {
                if (ItemHandlerHelper.insertItem(output, new ItemStack(variant.item()), true).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Redstone receivers: the nine lower-layer positions (8 chassis + terminal). */
    private boolean redstoneSignal() {
        MinerStructure s = structure();
        if (s == null || level == null) {
            return false;
        }
        for (MinerStructure.Member m : s.members()) {
            if (m.slot().redstoneReceiver() && level.hasNeighborSignal(m.pos())) {
                return true;
            }
        }
        return false;
    }

    private void ejectOutput() {
        MinerStructure s = structure();
        if (s == null || level == null) {
            return;
        }
        s.port(PortRole.ITEM_OUTPUT).ifPresent(port -> {
            IItemHandler target = NeighborTransfer.neighbor(level, port.pos(), port.outward());
            if (target == null) {
                return;
            }
            for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
                ItemStack stack = output.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack left = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                int moved = stack.getCount() - left.getCount();
                if (moved > 0) {
                    output.extractItem(slot, moved, false);
                }
            }
        });
    }

    private void pullSubstrate() {
        MinerStructure s = structure();
        if (s == null || level == null) {
            return;
        }
        // No item limit: the pull stops when the DM buffer is full or the source is empty.
        s.port(PortRole.MATTER_INPUT).ifPresent(port ->
                NeighborTransfer.pull(level, port.pos(), port.outward(), this::insertSubstrate, Long.MAX_VALUE));
    }

    // ---------------------------------------------------------------- visuals

    /** True when a cycle of this game time or the previous one did work (accelerated cycles included). */
    private boolean workedRecently() {
        return level != null && level.getGameTime() - lastWorkGameTime <= 1;
    }

    private void updateVisual() {
        CoreKind core = formed ? coreKind() : CoreKind.NONE;
        boolean worked = formed && (processing || workedRecently());
        boolean hasEnergy = formed && (energyBuffer.stored() >= curve().minPower() || worked);
        // Short gaps in the work keep the animation and the sound running; a real stop ends them at once.
        boolean halted = !formed || !runRequested || status == MinerStatus.MANUAL_STOP || status == MinerStatus.REDSTONE_HALT
                || status == MinerStatus.CORE_MISSING;
        if (worked && !halted) {
            visualLinger = VISUAL_LINGER_TICKS;
        } else if (halted) {
            visualLinger = 0;
        } else if (visualLinger > 0) {
            visualLinger--;
        }
        MinerVisualStage next = MinerVisualStage.select(core, hasEnergy, formed && (worked || visualLinger > 0));
        int percent = (int) Math.round(smoothedLoad * 100);
        boolean changed = next != visualStage || Math.abs(percent - loadPercent) >= 1;
        if (next.processing() && !visualStage.processing() && level != null) {
            animationStartTick = level.getGameTime();
        }
        visualStage = next;
        loadPercent = percent;
        if (changed) {
            syncToClients();
        }
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", formed);
        tag.putString("stage", visualStage.key());
        tag.putInt("load", loadPercent);
        tag.putLong("anim_start", animationStartTick);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClient(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        if (pkt.getTag() != null) {
            readClient(pkt.getTag());
        }
    }

    private void readClient(CompoundTag tag) {
        formed = tag.getBoolean("formed");
        visualStage = MinerVisualStage.byKey(tag.getString("stage"));
        loadPercent = tag.getInt("load");
        animationStartTick = tag.getLong("anim_start");
        clientFront = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public AABB renderBounds() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        MinerStructure s = MinerStructure.layout(worldPosition, facing);
        return new AABB(s.core()).inflate(1.6);
    }

    public BlockPos corePos() {
        return MinerStructure.layout(worldPosition, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)).core();
    }

    // ---------------------------------------------------------------- menu

    public boolean handleButton(Player player, int id) {
        switch (id) {
            case BUTTON_RATE_DOWN, BUTTON_RATE_UP -> {
                int next = RateScale.clampLevel(rate + (id == BUTTON_RATE_UP ? 1 : -1));
                if (next == rate) {
                    return false;
                }
                rate = next;
                return true;
            }
            case BUTTON_RATE_CYCLE, BUTTON_RATE_CYCLE_BACK -> {
                rate = RateScale.cycleLevel(rate, id == BUTTON_RATE_CYCLE);
                return true;
            }
            case BUTTON_AUTO_INPUT -> {
                autoInput = !autoInput;
                return true;
            }
            case BUTTON_AUTO_EJECT -> {
                autoEject = !autoEject;
                return true;
            }
            case BUTTON_REDSTONE -> {
                redstoneControl = !redstoneControl;
                return true;
            }
            case BUTTON_RUN -> {
                if (!formed) {
                    return false;
                }
                runRequested = !runRequested;
                return true;
            }
            case BUTTON_FILTER_CLEAR -> {
                filter.clear();
                return true;
            }
            default -> {
                if (id >= BUTTON_FILTER_TOGGLE && level instanceof ServerLevel serverLevel) {
                    List<ResourceFamily> families = ResourceProfiles.get(serverLevel).families();
                    int index = id - BUTTON_FILTER_TOGGLE;
                    if (index < families.size()) {
                        ResourceLocation key = families.get(index).key();
                        if (!filter.remove(key)) {
                            filter.add(key);
                        }
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /** Drops filter selections whose family no longer exists after a reload; an empty filter means OFF. */
    private void pruneFilter(ResourceProfile profile) {
        filter.removeIf(key -> profile.family(key).isEmpty());
    }

    public void writeMenuData(CompoundTag tag) {
        tag.putBoolean("formed", formed);
        tag.putString("status", status.name());
        tag.putString("limit", shownLimit.name());
        tag.putString("core", coreKind().name());
        tag.putInt("rate", rate);
        tag.putLong("power", powerLimit());
        tag.putLong("fe", energyBuffer.stored());
        tag.putLong("fe_capacity", energyBuffer.capacity());
        tag.putLong("dm", dmUnits);
        tag.putBoolean("run", runRequested);
        tag.putBoolean("redstone", redstoneControl);
        tag.putBoolean("redstone_signal", formed && redstoneSignal());
        tag.putBoolean("auto_input", autoInput);
        tag.putBoolean("auto_eject", autoEject);
        tag.putDouble("potential", potentialRate);
        tag.putDouble("actual", actualRate);
        tag.putDouble("load", smoothedLoad);
        tag.putDouble("nominal", curve().levelRate(rate));
        if (level != null) {
            tag.putString("dimension", level.dimension().location().toString());
        }
        int used = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (!output.getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        tag.putInt("output_used", used);
        if (level instanceof ServerLevel serverLevel) {
            ResourceProfile profile = ResourceProfiles.get(serverLevel);
            pruneFilter(profile);
            ListTag families = new ListTag();
            for (ResourceFamily f : profile.families()) {
                CompoundTag ft = new CompoundTag();
                ft.putString("key", f.key().toString());
                ft.putString("icon", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(f.icon()).toString());
                ft.putInt("weight", f.weight());
                ft.putBoolean("selected", filter.contains(f.key()));
                families.add(ft);
            }
            tag.put("families", families);
            if (coreKind() == CoreKind.BASIC && structure() != null) {
                MinerSurvey survey = SurveyManager.survey(serverLevel, structure().core());
                tag.putDouble("survey_coverage", survey.coverage());
                tag.putInt("known_drainable", survey.knownDrainable());
                tag.putBoolean("survey_ready", survey.ready());
                tag.putDouble("blight_threat", dimensionalfluxminer.miner.corruption.BlightSpawnManager.threatAround(serverLevel, structure().core()));
            }
        }
        tag.putString("substrate", lastSubstrate);
        tag.putLong("substrate_units", lastSubstrateUnits);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dimensionalfluxminer.miner_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MinerMenu(containerId, inventory, this);
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("formed", formed);
        tag.putLong("energy", energyBuffer.stored());
        tag.putLong("dm", dmUnits);
        tag.putInt("rate", rate);
        tag.putBoolean("run_requested", runRequested);
        tag.putBoolean("redstone_control", redstoneControl);
        tag.putBoolean("auto_input", autoInput);
        tag.putBoolean("auto_eject", autoEject);
        ListTag filterTag = new ListTag();
        filter.forEach(key -> filterTag.add(StringTag.valueOf(key.toString())));
        tag.put("filter", filterTag);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.put("core", coreSlot.serializeNBT(registries));
        tag.put("upgrade", upgradeSlot.serializeNBT(registries));
        tag.put("output", output.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("formed");
        energyBuffer.setStored(tag.getLong("energy"));
        dmUnits = Math.max(0, Math.min(MatterValues.BUFFER_UNITS, tag.getLong("dm")));
        rate = RateScale.clampLevel(tag.contains("rate") ? tag.getInt("rate") : 1);
        // A normal save/restart with the structure intact keeps the manual START state.
        runRequested = tag.getBoolean("run_requested") && formed;
        redstoneControl = tag.getBoolean("redstone_control");
        autoInput = tag.getBoolean("auto_input");
        autoEject = tag.getBoolean("auto_eject");
        filter.clear();
        ListTag filterTag = tag.getList("filter", Tag.TAG_STRING);
        for (int i = 0; i < filterTag.size(); i++) {
            ResourceLocation key = ResourceLocation.tryParse(filterTag.getString(i));
            if (key != null) {
                filter.add(key);
            }
        }
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        coreSlot.deserializeNBT(registries, tag.getCompound("core"));
        upgradeSlot.deserializeNBT(registries, tag.getCompound("upgrade"));
        output.deserializeNBT(registries, tag.getCompound("output"));
        lastCore = coreKind();
        structure = null;
        wasPowered = energyBuffer.stored() > 0;
    }
}
