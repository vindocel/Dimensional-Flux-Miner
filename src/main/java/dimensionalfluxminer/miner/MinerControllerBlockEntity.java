package dimensionalfluxminer.miner;

import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.advancement.DfmAdvancements;
import dimensionalfluxminer.registry.DfmTags;
import dimensionalfluxminer.machine.CableActivity;
import dimensionalfluxminer.machine.LogicalTickCounter;
import dimensionalfluxminer.machine.ThroughputStreak;
import dimensionalfluxminer.machine.NeighborTransfer;
import dimensionalfluxminer.machine.ReadOnlyViews;
import dimensionalfluxminer.machine.math.DynamicEnergyBuffer;
import dimensionalfluxminer.machine.math.LevelTable;
import dimensionalfluxminer.machine.math.MinerProductionCurve;
import dimensionalfluxminer.machine.math.RateScale;
import dimensionalfluxminer.machine.math.WorkAccumulator;
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
import dimensionalfluxminer.compat.LogisticsNetworksCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Logical controller of the Dimensional Flux Miner, living in the terminal. It owns every buffer of the formed
 * machine and drives production, the Basic environmental drain and the Stabilized Matter Injection.
 */
public class MinerControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OUTPUT_SLOTS = MinerOutput.SLOTS;
    public static final int MAX_ROLLS_PER_CYCLE = 64;
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
    /** Automation cycle of the item ports: OFF -> Auto Input -> Auto Eject -> both (and back). */
    public static final int BUTTON_AUTO_IO_NEXT = 2300;
    public static final int BUTTON_AUTO_IO_PREVIOUS = 2301;
    /** {@code + face * 2 + (forward ? 0 : 1)}: next / previous item role of a port face. */
    public static final int BUTTON_PORT_ITEMS = 4000;
    /** {@code + face}: adds or removes a hand-picked energy face. */
    public static final int BUTTON_PORT_ENERGY = 4100;
    public static final int BUTTON_PORTS_CLEAR_ITEMS = 4200;
    /** Back to the default energy: every face accepts it. */
    public static final int BUTTON_PORTS_RESET_ENERGY = 4201;
    /** How often (ticks) the faces look for something with energy touching them. */
    private static final int ENERGY_SCAN_TICKS = 10;

    // ---------------------------------------------------------------- persistent state
    private boolean formed;
    private final DynamicEnergyBuffer energyBuffer = new DynamicEnergyBuffer(0);
    /** Dimensional Matter buffer; see {@link MatterBuffer}. */
    private final MatterBuffer matter = new MatterBuffer();
    private int rate = RateScale.MIN_LEVEL;
    private boolean runRequested;
    private boolean redstoneControl;
    private boolean autoInput;
    private boolean autoEject;
    /** The nine configurable faces of the lower layer; see {@link MinerPorts}. */
    private final MinerPorts ports = new MinerPorts();
    private final Set<ResourceLocation> filter = new LinkedHashSet<>();
    private UUID owner;
    /** Columns this Miner has drained in its life; drives the "half of the area drained" advancement. */
    private long columnsDrained;
    /** Cobblestone this Miner has turned into DM in its life; drives the "From Trash to Treasure" advancement. */
    private long cobblestoneInjected;
    public static final long TRASH_TO_TREASURE_COBBLESTONE = 10_000;
    private final DfmAdvancements.Throttle advancements = new DfmAdvancements.Throttle();
    private final ThroughputStreak throughput = new ThroughputStreak(THOUSAND_PER_SECOND, THOUSAND_PER_SECOND_SECONDS);

    /** "Thousand per second": at least this many rolls per second... */
    public static final double THOUSAND_PER_SECOND = 1000.0;
    /** ...kept for this many seconds in a row. */
    public static final int THOUSAND_PER_SECOND_SECONDS = 5;
    /** Half of every column inside the drain radius. */
    public static long worldDrainedColumns() {
        return dimensionalfluxminer.miner.survey.ColumnOrder.get().size() / 2;
    }

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
    /** Output Buffer (27 output-only slots); see {@link MinerOutput}. */
    private final MinerOutput output = new MinerOutput(this::setChanged);

    // ---------------------------------------------------------------- runtime state
    private final LogicalTickCounter tickCounter = new LogicalTickCounter();
    /** Client side: tick accelerators never speed up the effects (one effect tick per game time). */
    private final LogicalTickCounter clientTickCounter = new LogicalTickCounter();
    private final dimensionalfluxminer.machine.AccelerationDemand accelerationDemand = new dimensionalfluxminer.machine.AccelerationDemand();
    private final WorkAccumulator work = new WorkAccumulator();
    private CoreKind lastCore = CoreKind.NONE;
    private MinerStatus status = MinerStatus.MULTIBLOCK_INVALID;
    /** Pace shown to the player (load, potential and actual attempts, limiting factor). */
    private final MinerMetrics metrics = new MinerMetrics();
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
    /** Faces with something that holds energy touching them (bit per {@link MinerPorts.Face}). */
    private int energyConnected;
    private int[] nodeColors = new int[MinerPorts.Face.VALUES.length];
    /** A cable that pulled from / pushed into a face this recently still counts as using it. */
    public static final int CABLE_ACTIVE_TICKS = CableActivity.ACTIVE_TICKS;
    private final PortHandler[] portHandlers = new PortHandler[MinerPorts.Face.VALUES.length];
    private final CableActivity cableActivity = new CableActivity(MinerPorts.Face.VALUES.length);
    private int cableOutput = -1;
    private int cableMatter = -1;
    /** Faces whose role a cable sets (for the Side Configuration). */
    private int cableFaces;
    /** The structure was checked against the current layout since loading (a machine of an older layout comes apart). */
    private boolean layoutVerified;

    // ---------------------------------------------------------------- client render state
    /** How the nine faces look, 3 bits each ({@link MinerPorts#packLooks}); server and client. */
    private int portLooks;
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
            return level instanceof ServerLevel serverLevel && coreKind() == CoreKind.STABILIZED
                    && MatterValues.substrateUnits(stack, serverLevel).isPresent();
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
        return output.handler();
    }

    public DynamicEnergyBuffer energyBuffer() {
        return energyBuffer;
    }

    public long dmUnits() {
        return matter.units();
    }

    public void setDmUnits(long units) {
        matter.set(units);
        setChanged();
    }

    public int rate() {
        return rate;
    }

    public void setRate(int rate) {
        int level = RateScale.clampLevel(rate);
        boolean changed = level != this.rate;
        this.rate = level;
        setChanged();
        if (changed) {
            // The client speeds the core and its motes up with the Rate.
            syncToClients();
        }
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

    /** The curve of the last configured values seen; rebuilt only when they change. */
    private record CurveKey(double minRate, double maxRate, double energy, LevelTable speed, LevelTable power, MinerProductionCurve curve) {
    }

    private static volatile CurveKey cachedCurve;

    /** The production curve of the configuration: the default curve, or the per-level tables when they are set. */
    public static MinerProductionCurve curve() {
        double[] rates = DfmConfig.range(DfmConfig.MINER_MIN_RATE, DfmConfig.MINER_MAX_RATE);
        double energy = DfmConfig.energy(DfmConfig.MINER_ENERGY_PER_ATTEMPT);
        LevelTable speed = DfmConfig.levelTable(DfmConfig.MINER_SPEED_BY_LEVEL);
        LevelTable power = DfmConfig.levelTable(DfmConfig.MINER_POWER_BY_LEVEL);
        CurveKey cached = cachedCurve;
        if (cached != null && cached.minRate() == rates[0] && cached.maxRate() == rates[1] && cached.energy() == energy
                && cached.speed() == speed && cached.power() == power) {
            return cached.curve();
        }
        MinerProductionCurve curve = buildCurve(rates, energy, speed, power);
        cachedCurve = new CurveKey(rates[0], rates[1], energy, speed, power, curve);
        return curve;
    }

    private static MinerProductionCurve buildCurve(double[] rates, double energy, LevelTable speed, LevelTable power) {
        try {
            return new MinerProductionCurve(speed != null ? speed : LevelTable.geometric(rates[0], Math.min(rates[1],
                    MinerProductionCurve.HARD_CAP_PER_CYCLE)), power, energy);
        } catch (IllegalArgumentException e) {
            return new MinerProductionCurve(DfmConfig.MINER_MIN_RATE.getDefault(), DfmConfig.MINER_MAX_RATE.getDefault(),
                    DfmConfig.MINER_ENERGY_PER_ATTEMPT.getDefault());
        }
    }

    /** FE/t of the selected Flux Rate; the Efficiency Upgrade never changes the Miner's FE. */
    public long powerLimit() {
        return RateScale.powerLimit(curve().levelPower(rate));
    }

    /** Progress towards the next mining attempt (0..1). */
    public double workProgress() {
        return work.progress();
    }

    /**
     * Matter Input conversion: everything that fits in the DM buffer is accepted. There is no item limit per tick; the
     * energy available for the work (and for any acceleration) is what limits the machine.
     */
    public ItemStack insertSubstrate(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !(level instanceof ServerLevel serverLevel) || !formed || coreKind() != CoreKind.STABILIZED) {
            return stack;
        }
        OptionalInt units = MatterValues.matterInputUnits(stack, serverLevel, hasEfficiencyUpgrade());
        if (units.isEmpty() || units.getAsInt() <= 0) {
            return stack;
        }
        int perItem = units.getAsInt();
        long space = matter.room();
        int byMatter = (int) Math.min(Integer.MAX_VALUE, space / perItem);
        int accepted = Math.min(stack.getCount(), byMatter);
        if (accepted <= 0) {
            return stack;
        }
        if (!simulate) {
            matter.add((long) accepted * perItem);
            lastSubstrate = stack.getItem().getDescriptionId();
            lastSubstrateUnits = perItem;
            setChanged();
            report(DfmAdvancements.MATTER_INJECTED);
            if (stack.is(DfmTags.Items.common("cobblestones"))) {
                cobblestoneInjected += accepted;
                if (cobblestoneInjected >= TRASH_TO_TREASURE_COBBLESTONE) {
                    report(DfmAdvancements.TRASH_TO_TREASURE);
                }
            }
        }
        return stack.copyWithCount(stack.getCount() - accepted);
    }

    // ---------------------------------------------------------------- port capabilities

    public MinerPorts ports() {
        return ports;
    }

    /** The configurable face of the lower layer at {@code pos} facing {@code side}, or null. */
    private MinerPorts.Face face(BlockPos pos, Direction side) {
        MinerStructure s = structure();
        if (s == null || side == null) {
            return null;
        }
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            if (face.outward(front) == side && face.owner(s.core(), front).equals(pos)) {
                return face;
            }
        }
        return null;
    }

    /**
     * Item capability of a face: Item Output, Matter Input (Stabilized Core only) or both on the same face. The
     * {@code null} side of every miner block is a read-only view of the output buffer (Jade).
     */
    public IItemHandler itemPort(BlockPos portPos, Direction side) {
        MinerStructure s = structure();
        if (!formed || s == null) {
            return null;
        }
        if (side == null) {
            return ReadOnlyViews.items(output.handler());
        }
        MinerPorts.Face face = face(portPos, side);
        if (face == null || !ports.isOutput(face) && !ports.isMatter(face)) {
            // A network node on the block reaches its face from whatever side its channel is set to.
            MinerPorts.Face node = nodeFace(portPos, ports.nodeItemFaces());
            face = node != null ? node : face;
        }
        if (face == null) {
            return null;
        }
        if (portHandlers[face.ordinal()] == null) {
            portHandlers[face.ordinal()] = new PortHandler(face);
        }
        return portHandlers[face.ordinal()];
    }

    /**
     * The item handler of one face. Every outward face has one, whatever its role, so a cable can be noticed: a cable
     * that pulls from a face (extracts) asks for the Item Output, one that pushes Matter (inserts a substrate, Stabilized
     * Core) asks for the Matter Input. The face only gives or takes items once it has the role; the next scan moves
     * the role there (see {@link #scanAutomaticPorts}). Slots: the output slots, then one insertion-only matter slot.
     */
    private final class PortHandler implements IItemHandler {
        private final MinerPorts.Face face;
        private final IItemHandler view = output.extractionView();

        PortHandler(MinerPorts.Face face) {
            this.face = face;
        }

        private boolean matterOpen() {
            return ports.isMatter(face) && coreKind() == CoreKind.STABILIZED;
        }

        @Override
        public int getSlots() {
            return view.getSlots() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < view.getSlots() ? view.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !matterPort.isItemValid(0, stack)) {
                return stack;
            }
            notePush(face);
            // Any slot feeds the Matter Input (cables that only insert into the first slot work too).
            return matterOpen() ? matterPort.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= view.getSlots() || amount <= 0 || view.getStackInSlot(slot).isEmpty()) {
                return ItemStack.EMPTY;
            }
            notePull(face);
            return ports.isOutput(face) ? view.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < view.getSlots() ? view.getSlotLimit(slot) : matterPort.getSlotLimit(0);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Yes to a substrate even before the face has the role, so a pushing cable tries.
            return matterPort.isItemValid(0, stack);
        }
    }

    private void notePull(MinerPorts.Face face) {
        if (level != null) {
            cableActivity.notePull(face.ordinal(), level.getGameTime());
        }
    }

    private void notePush(MinerPorts.Face face) {
        if (level != null) {
            cableActivity.notePush(face.ordinal(), level.getGameTime());
        }
    }

    /**
     * Energy capability of a face: every face by default, only the hand-picked ones once the player chose some. The
     * {@code null} side of every miner block is a read-only view of the energy buffer (Jade).
     */
    public IEnergyStorage energyPort(BlockPos portPos, Direction side) {
        MinerStructure s = structure();
        if (!formed || s == null) {
            return null;
        }
        if (side == null) {
            return ReadOnlyViews.energy(energyPort);
        }
        MinerPorts.Face face = face(portPos, side);
        if (face != null && ports.acceptsEnergy(face)) {
            return energyPort;
        }
        return nodeFace(portPos, ports.nodeEnergyFaces()) != null ? energyPort : null;
    }

    /** A face of the block at {@code pos} among the network node faces {@code mask}, or null. */
    private MinerPorts.Face nodeFace(BlockPos pos, int mask) {
        MinerStructure s = structure();
        if (s == null || mask == 0) {
            return null;
        }
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            if ((mask & face.bit()) != 0 && face.owner(s.core(), front).equals(pos)) {
                return face;
            }
        }
        return null;
    }

    /**
     * Roles set from outside, every 10 ticks: first the Logistics Networks nodes on the port blocks (a node that pulls
     * items takes the Item Output, one that pushes items the Matter Input with the Stabilized Core, one that pushes
     * energy an energy face; their own box is hidden and the port draws a frame in the network's color instead), then
     * any cable that keeps pulling from or pushing Matter into a face ({@link PortHandler}). A face set this way is
     * locked in the Side Configuration while the node is there or the cable stays active.
     */
    private void scanAutomaticPorts() {
        MinerStructure s = structure();
        if (!(level instanceof ServerLevel serverLevel) || s == null) {
            return;
        }
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        boolean stabilized = coreKind() == CoreKind.STABILIZED;
        int out = -1;
        int matter = -1;
        int energy = 0;
        int[] colors = new int[MinerPorts.Face.VALUES.length];
        Map<BlockPos, List<MinerPorts.Face>> blocks = new LinkedHashMap<>();
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            blocks.computeIfAbsent(face.owner(s.core(), front), k -> new ArrayList<>()).add(face);
        }
        for (Map.Entry<BlockPos, List<MinerPorts.Face>> block : LogisticsNetworksCompat.loaded() ? blocks.entrySet()
                : java.util.Set.<Map.Entry<BlockPos, List<MinerPorts.Face>>>of()) {
            List<MinerPorts.Face> faces = block.getValue();
            for (LogisticsNetworksCompat.Node node : LogisticsNetworksCompat.nodesAt(serverLevel, block.getKey())) {
                LogisticsNetworksCompat.hide(node);
                MinerPorts.Face chosen = null;
                if (node.takesItems() && out < 0) {
                    chosen = faces.stream().filter(ports::isOutput).findFirst().orElse(faces.get(0));
                    out = chosen.ordinal();
                }
                if (node.givesItems() && stabilized && matter < 0) {
                    // One node that pulls and pushes items: both roles on its face (half orange, half violet).
                    if (chosen == null) {
                        chosen = faces.stream().filter(ports::isMatter).findFirst().orElse(faces.get(0));
                    }
                    matter = chosen.ordinal();
                }
                if (node.givesEnergy()) {
                    if (chosen == null) {
                        chosen = faces.get(0);
                    }
                    energy |= chosen.bit();
                }
                if (chosen != null) {
                    colors[chosen.ordinal()] = node.color() | 0xFF000000;
                }
            }
        }
        // Cables: the face that keeps being pulled from / pushed into, unless a node already holds that role.
        long now = level.getGameTime();
        cableOutput = cableActivity.activeFace(true, cableOutput, now);
        cableMatter = stabilized ? cableActivity.activeFace(false, cableMatter, now) : -1;
        int cables = 0;
        if (out < 0 && cableOutput >= 0) {
            out = cableOutput;
            cables |= 1 << out;
        }
        if (matter < 0 && cableMatter >= 0) {
            matter = cableMatter;
            cables |= 1 << matter;
        }
        cableFaces = cables;
        boolean changed = ports.applyNodes(out, matter, energy);
        if (changed) {
            invalidatePorts();
            setChanged();
        }
        if (changed || !java.util.Arrays.equals(colors, nodeColors)) {
            nodeColors = colors;
            syncToClients();
        }
    }

    /** Color (ARGB, 0 = none) of the network node frame drawn on each face. */
    public int[] nodeColors() {
        return nodeColors;
    }

    /** The port blocks of the lower layer changed what they expose: cables and pipes around them look again. */
    private void invalidatePorts() {
        if (level == null) {
            return;
        }
        MinerStructure s = MinerStructure.layout(worldPosition, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        Set<BlockPos> owners = new LinkedHashSet<>();
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            owners.add(face.owner(s.core(), front));
        }
        for (BlockPos owner : owners) {
            level.invalidateCapabilities(owner);
            level.updateNeighborsAt(owner, level.getBlockState(owner).getBlock());
        }
        updatePortLooks();
    }

    /** Looks again for something with energy touching each face, outside the machine. */
    private void scanEnergyNeighbors() {
        MinerStructure s = structure();
        if (s == null || level == null) {
            energyConnected = 0;
            return;
        }
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        int mask = 0;
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            Direction out = face.outward(front);
            BlockPos neighbor = face.owner(s.core(), front).relative(out);
            if (level.isLoaded(neighbor) && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    neighbor, out.getOpposite()) != null) {
                mask |= face.bit();
            }
        }
        energyConnected = mask;
    }

    /** Recomputes how the faces look and sends it to the clients when it changed. */
    private void updatePortLooks() {
        int looks = formed ? ports.packLooks(energyConnected) : 0;
        if (looks != portLooks) {
            portLooks = looks;
            syncToClients();
        }
    }

    public int portLooks() {
        return portLooks;
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
        layoutVerified = true;
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
        DfmAdvancements.awardOwner(serverLevel, owner, worldPosition, DfmAdvancements.MINER_FORMED);
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
        drops.addAll(output.takeAll());
        coreSlot.setStackInSlot(0, ItemStack.EMPTY);
        upgradeSlot.setStackInSlot(0, ItemStack.EMPTY);
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, drop.x, drop.y, drop.z, stack);
            }
        }
        energyBuffer.setStored(0);
        energyBuffer.setDebt(0);
        matter.set(0);
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

    /**
     * A machine saved as formed is checked once against the current layout, as soon as its 27 positions are loaded. A
     * machine assembled with an older layout comes apart (its items drop and every part returns to a loose block), so
     * the player rebuilds it with the current one.
     */
    private void verifyLayout() {
        MinerStructure expected = MinerStructure.layout(worldPosition, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING));
        for (MinerStructure.Member m : expected.members()) {
            if (!level.isLoaded(m.pos())) {
                return;
            }
        }
        layoutVerified = true;
        if (MinerStructure.check(level, worldPosition).structure().isPresent()) {
            return;
        }
        invalidate();
        // Parts of the older layout may stand where the current one expects other blocks: release every one of them.
        for (MinerStructure.Member m : expected.members()) {
            BlockState state = level.getBlockState(m.pos());
            if (state.hasProperty(dimensionalfluxminer.content.DfmStateProperties.FORMED)
                    && state.getValue(dimensionalfluxminer.content.DfmStateProperties.FORMED)) {
                level.setBlock(m.pos(), state.setValue(dimensionalfluxminer.content.DfmStateProperties.FORMED, false),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
            if (level.getBlockEntity(m.pos()) instanceof MinerLinkBlockEntity link) {
                link.unlink();
            }
        }
    }

    private void onCoreChanged() {
        CoreKind now = coreKind();
        if (now != lastCore) {
            // Removing or replacing the Core always forces STOPPED.
            runRequested = false;
            lastCore = now;
            work.reset();
            // The Matter Input only exists with the Stabilized Core.
            if (now != CoreKind.STABILIZED) {
                ports.clearMatter();
            }
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
        if (formed && !layoutVerified) {
            verifyLayout();
        }
        if (formed && level != null && level.getGameTime() % ENERGY_SCAN_TICKS == 0) {
            scanEnergyNeighbors();
            scanAutomaticPorts();
            updatePortLooks();
        }
        cycle(true);
        noteLimit(status.limitingFactor());
        // Work of every cycle since the previous normal tick, accelerated ones included.
        int cycles = 1 + (level != null ? accelerationDemand.extraPerTick(level.getGameTime()) : 0);
        metrics.closeRealTick(curve().levelRate(rate), cycles, formed, processing || workedRecently());
        if (level != null && throughput.record(level.getGameTime(), metrics.actualRate())) {
            report(DfmAdvancements.THOUSAND_PER_SECOND);
        }
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

    private void noteLimit(MinerStatus.LimitingFactor factor) {
        metrics.noteLimit(factor);
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
        long cyclePower = RateScale.powerLimit(curve.levelPower(rate));
        energyBuffer.setTarget(RateScale.energyBufferTarget(cyclePower));
        // The input also accepts what the accelerated cycles of one game time use, so they run at full power.
        energyBuffer.setHeadroom(RateScale.saturatedMultiply(accelerationDemand.extraPerTick(serverLevel.getGameTime()), cyclePower));
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
            if (!survey.ready() && !matter.canPayAttempt()) {
                status = MinerStatus.DIMENSIONAL_SURVEY;
                return;
            }
        }
        if (!output.anyKeptResultFits(profile, filter)) {
            status = MinerStatus.OUTPUT_BLOCKED;
            return;
        }
        // Every attempt costs the same FE: without the full FE/t of the level, it attempts proportionally less.
        double levelPower = curve.levelPower(rate);
        double available = Math.min(levelPower, energyBuffer.available());
        double rateNow = curve.rateForPower(rate, available);
        if (rateNow <= 0) {
            status = MinerStatus.WAITING_FOR_ENERGY;
            return;
        }
        if (available < levelPower) {
            noteLimit(MinerStatus.LimitingFactor.FLUX);
        }
        boolean canRefill = core == CoreKind.BASIC && survey.hasCandidates();
        if (!matter.canPayAttempt() && !canRefill) {
            status = core == CoreKind.BASIC ? MinerStatus.NO_DRAINABLE_MATTER : MinerStatus.WAITING_FOR_MATTER;
            noteLimit(matterLimit(core));
            return;
        }

        WorkAccumulator.Plan plan = work.plan(rateNow, MAX_ROLLS_PER_CYCLE);
        // Basic: corruption is the source of matter, performed only when the buffer needs it.
        MinerSurvey drainSurvey = survey;
        MinerAttempts.Result attempts = MinerAttempts.run(plan.operations(), profile, serverLevel.random, filter, hasEfficiencyUpgrade(), matter,
                core == CoreKind.BASIC ? () -> drainColumn(serverLevel, drainSurvey) : () -> false, output);
        long executed = attempts.executed();
        boolean missed = attempts.missed();
        MinerStatus stop = null;
        if (attempts.matterRanOut()) {
            stop = core == CoreKind.BASIC ? MinerStatus.NO_DRAINABLE_MATTER : MinerStatus.WAITING_FOR_MATTER;
            noteLimit(matterLimit(core));
        }
        plan = work.truncate(plan, executed);
        // FE follows the attempts really performed (filtered and lost ones included), at the same cost per attempt.
        energyBuffer.charge(curve.powerForRate(rate, plan.workDone()));
        work.commit(plan);
        metrics.addWork(plan.workDone());
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
        if (attempts.producedOre()) {
            report(DfmAdvancements.FIRST_ORE);
        }
        if (attempts.filteredOre()) {
            report(DfmAdvancements.ORE_FILTERED);
        }
        if (autoEject) {
            ejectOutput();
        }
        setChanged();
    }

    /** Reports an advancement event to the owner (or the players nearby), at most once every few seconds. */
    private void report(String event) {
        if (level instanceof ServerLevel serverLevel && advancements.ready(event, serverLevel.getGameTime())) {
            DfmAdvancements.awardOwner(serverLevel, owner, worldPosition, event);
        }
    }

    public long columnsDrained() {
        return columnsDrained;
    }

    public long cobblestoneInjected() {
        return cobblestoneInjected;
    }

    /** Drains one valid cached column (revalidated now); returns true when 64 DM were credited. */
    private boolean drainColumn(ServerLevel level, MinerSurvey survey) {
        while (survey.hasCandidates()) {
            long packed = survey.pollCandidate();
            int x = (int) (packed >> 32);
            int z = (int) packed;
            if (matter.room() < MatterValues.COLUMN_UNITS) {
                return false;
            }
            if (CorruptionService.corrupt(level, x, z, owner)) {
                matter.add(MatterValues.COLUMN_UNITS);
                columnsDrained++;
                report(DfmAdvancements.COLUMN_DRAINED);
                if (columnsDrained >= worldDrainedColumns()) {
                    report(DfmAdvancements.WORLD_DRAINED);
                }
                return true;
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
        if (s == null || level == null || ports.outputFace() < 0) {
            return;
        }
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        MinerPorts.Face face = MinerPorts.Face.VALUES[ports.outputFace()];
        IItemHandler target = NeighborTransfer.neighbor(level, face.owner(s.core(), front), face.outward(front));
        if (target != null) {
            output.ejectTo(target);
        }
    }

    private void pullSubstrate() {
        MinerStructure s = structure();
        if (s == null || level == null || ports.matterFace() < 0) {
            return;
        }
        MinerLayout.Front front = MinerStructure.toFront(s.front());
        MinerPorts.Face face = MinerPorts.Face.VALUES[ports.matterFace()];
        // No item limit: the pull stops when the DM buffer is full or the source is empty.
        NeighborTransfer.pull(level, face.owner(s.core(), front), face.outward(front), this::insertSubstrate, Long.MAX_VALUE);
    }

    // ---------------------------------------------------------------- visuals

    /** True when a cycle of this game time or the previous one did work (accelerated cycles included). */
    private boolean workedRecently() {
        // Never worked yet: the subtraction from Long.MIN_VALUE would overflow into "just worked".
        return level != null && lastWorkGameTime != Long.MIN_VALUE && level.getGameTime() - lastWorkGameTime <= 1;
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
        int percent = (int) Math.round(metrics.smoothedLoad() * 100);
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
        tag.putInt("rate", rate);
        tag.putLong("anim_start", animationStartTick);
        tag.putInt("port_looks", portLooks);
        tag.putIntArray("node_colors", nodeColors);
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
        rate = RateScale.clampLevel(tag.getInt("rate"));
        animationStartTick = tag.getLong("anim_start");
        portLooks = tag.getInt("port_looks");
        int[] colors = tag.getIntArray("node_colors");
        nodeColors = colors.length == MinerPorts.Face.VALUES.length ? colors : new int[MinerPorts.Face.VALUES.length];
        clientFront = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public AABB renderBounds() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        MinerStructure s = MinerStructure.layout(worldPosition, facing);
        return new AABB(s.core()).inflate(1.6);
    }

    /** True for the first client tick of this game time; extra calls made by tick accelerators return false. */
    public boolean firstClientTick() {
        return level != null && clientTickCounter.register(level.getGameTime());
    }

    public BlockPos corePos() {
        return MinerStructure.layout(worldPosition, getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)).core();
    }

    // ---------------------------------------------------------------- menu

    /** Until a Core is inserted the machine is locked: only the Core slot can be used. */
    public boolean locked() {
        return coreKind() == CoreKind.NONE;
    }

    public boolean handleButton(Player player, int id) {
        if (locked()) {
            return false;
        }
        if (handlePortButton(id)) {
            invalidatePorts();
            setChanged();
            return true;
        }
        switch (id) {
            case BUTTON_AUTO_IO_NEXT, BUTTON_AUTO_IO_PREVIOUS -> {
                // Bit 0 = Auto Input, bit 1 = Auto Eject: 0 OFF, 1 input, 2 eject, 3 both.
                int mode = (autoInput ? 1 : 0) | (autoEject ? 2 : 0);
                int next = Math.floorMod(mode + (id == BUTTON_AUTO_IO_NEXT ? 1 : -1), 4);
                autoInput = (next & 1) != 0;
                autoEject = (next & 2) != 0;
                return true;
            }
            default -> {
            }
        }
        switch (id) {
            case BUTTON_RATE_DOWN, BUTTON_RATE_UP -> {
                int next = RateScale.clampLevel(rate + (id == BUTTON_RATE_UP ? 1 : -1));
                if (next == rate) {
                    return false;
                }
                setRate(next);
                return true;
            }
            case BUTTON_RATE_CYCLE, BUTTON_RATE_CYCLE_BACK -> {
                setRate(RateScale.cycleLevel(rate, id == BUTTON_RATE_CYCLE));
                if (rate == RateScale.MAX_LEVEL && player instanceof ServerPlayer serverPlayer) {
                    DfmAdvancements.award(serverPlayer, DfmAdvancements.MAX_SPEED);
                }
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

    /** Port configuration buttons; returns true when the configuration changed. */
    private boolean handlePortButton(int id) {
        int faces = MinerPorts.Face.VALUES.length;
        if (id >= BUTTON_PORT_ITEMS && id < BUTTON_PORT_ITEMS + faces * 2) {
            int local = id - BUTTON_PORT_ITEMS;
            ports.cycleItems(MinerPorts.Face.VALUES[local / 2], coreKind() == CoreKind.STABILIZED, local % 2 == 0);
            return true;
        }
        if (id >= BUTTON_PORT_ENERGY && id < BUTTON_PORT_ENERGY + faces) {
            ports.toggleEnergy(MinerPorts.Face.VALUES[id - BUTTON_PORT_ENERGY]);
            return true;
        }
        if (id == BUTTON_PORTS_CLEAR_ITEMS) {
            ports.clearItems();
            return true;
        }
        if (id == BUTTON_PORTS_RESET_ENERGY) {
            ports.resetEnergy();
            return true;
        }
        return false;
    }

    /** Drops filter selections whose family no longer exists after a reload; an empty filter means OFF. */
    private void pruneFilter(ResourceProfile profile) {
        filter.removeIf(key -> profile.family(key).isEmpty());
    }

    public void writeMenuData(CompoundTag tag) {
        tag.putBoolean("formed", formed);
        tag.putString("status", status.name());
        tag.putString("limit", metrics.shownLimit().name());
        tag.putString("core", coreKind().name());
        tag.putInt("rate", rate);
        tag.putLong("power", powerLimit());
        tag.putLong("fe", energyBuffer.stored());
        tag.putLong("fe_capacity", energyBuffer.capacity());
        tag.putLong("dm", matter.units());
        tag.putBoolean("run", runRequested);
        tag.putBoolean("redstone", redstoneControl);
        tag.putBoolean("redstone_signal", formed && redstoneSignal());
        tag.putBoolean("auto_input", autoInput);
        tag.putBoolean("auto_eject", autoEject);
        tag.putBoolean("locked", locked());
        tag.put("ports", ports.save());
        tag.putInt("energy_connected", energyConnected);
        tag.putInt("node_item_faces", ports.nodeItemFaces());
        tag.putInt("node_energy_faces", ports.nodeEnergyFaces());
        tag.putInt("cable_faces", cableFaces);
        tag.putDouble("potential", metrics.potentialRate());
        tag.putDouble("actual", metrics.actualRate());
        tag.putDouble("load", metrics.smoothedLoad());
        tag.putDouble("nominal", curve().levelRate(rate));
        if (level != null) {
            tag.putString("dimension", level.dimension().location().toString());
        }
        tag.putInt("output_used", output.usedSlots());
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
            tag.putDouble("empty_share", profile.emptyShare());
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
        tag.putDouble("energy_debt", energyBuffer.debt());
        // Progress already paid for survives a save and reload.
        tag.putDouble("work_progress", work.progress());
        tag.putLong("dm", matter.units());
        tag.putInt("rate", rate);
        tag.putBoolean("run_requested", runRequested);
        tag.putBoolean("redstone_control", redstoneControl);
        tag.putBoolean("auto_input", autoInput);
        tag.putBoolean("auto_eject", autoEject);
        tag.put("ports", ports.save());
        ListTag filterTag = new ListTag();
        filter.forEach(key -> filterTag.add(StringTag.valueOf(key.toString())));
        tag.put("filter", filterTag);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putLong("columns_drained", columnsDrained);
        tag.putLong("cobblestone_injected", cobblestoneInjected);
        tag.put("core", coreSlot.serializeNBT(registries));
        tag.put("upgrade", upgradeSlot.serializeNBT(registries));
        tag.put("output", output.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("formed");
        energyBuffer.setStored(tag.getLong("energy"));
        energyBuffer.setDebt(tag.getDouble("energy_debt"));
        work.setProgress(tag.getDouble("work_progress"));
        matter.set(tag.getLong("dm"));
        rate = RateScale.clampLevel(tag.contains("rate") ? tag.getInt("rate") : 1);
        // A normal save/restart with the structure intact keeps the manual START state.
        runRequested = tag.getBoolean("run_requested") && formed;
        redstoneControl = tag.getBoolean("redstone_control");
        autoInput = tag.getBoolean("auto_input");
        autoEject = tag.getBoolean("auto_eject");
        ports.load(tag.getCompound("ports"));
        filter.clear();
        ListTag filterTag = tag.getList("filter", Tag.TAG_STRING);
        for (int i = 0; i < filterTag.size(); i++) {
            ResourceLocation key = ResourceLocation.tryParse(filterTag.getString(i));
            if (key != null) {
                filter.add(key);
            }
        }
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        columnsDrained = tag.getLong("columns_drained");
        cobblestoneInjected = tag.getLong("cobblestone_injected");
        coreSlot.deserializeNBT(registries, tag.getCompound("core"));
        upgradeSlot.deserializeNBT(registries, tag.getCompound("upgrade"));
        output.load(registries, tag.getCompound("output"));
        lastCore = coreKind();
        structure = null;
        wasPowered = energyBuffer.stored() > 0;
    }
}
