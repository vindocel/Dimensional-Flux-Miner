package dimensionalfluxminer.tank;

//? if >=26.1 {
import dimensionalfluxminer.machine.transfer.FluidTanks;
import dimensionalfluxminer.machine.transfer.TagStacks;
import dimensionalfluxminer.machine.transfer.Transfer;
//?}
import dimensionalfluxminer.machine.AbstractMachineBlockEntity;
import dimensionalfluxminer.machine.side.RelativeFace;
import dimensionalfluxminer.machine.side.SideConfiguration;
import dimensionalfluxminer.machine.side.SideRoute;
import dimensionalfluxminer.machine.side.SideRules;
import dimensionalfluxminer.machine.side.Transmission;
import dimensionalfluxminer.menu.MachineMenu;
import dimensionalfluxminer.registry.DfmBlockEntities;
import dimensionalfluxminer.registry.DfmDataComponents;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
//? if <26.1 {
/*import net.neoforged.neoforge.fluids.FluidActionResult;
*///?}
import net.neoforged.neoforge.fluids.FluidStack;
//? if <26.1 {
/*import net.neoforged.neoforge.fluids.FluidUtil;
*///?}
import net.neoforged.neoforge.fluids.SimpleFluidContent;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;
//?} else {
/*import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
*///?}

/**
 * A Dimensional Tank: an endpoint of the channel named by its kind and its three-color code. The liquid is kept in
 * {@link TankChannels} while connected, so tanks with the same kind and code share content across dimensions.
 * Closing the channel lock selects independent chambers persisted with this block and its item.
 * No energy. The ordinary single tank also converts containers in two local slots.
 * Each face carries the liquid of one chamber (Side Configuration) and the
 * liquids have their own Auto Input and Auto Eject (default OFF); capabilities stay available with both OFF.
 * <p>
 * The creative tank has no channel and no code: it keeps one liquid in the block and gives it without end
 * ({@link CreativeFluidHandler}).
 */
//? if >=26.1 {
public class DimensionalTankBlockEntity extends dimensionalfluxminer.content.TagBlockEntity implements MenuProvider {
//?} else {
/*public class DimensionalTankBlockEntity extends BlockEntity implements MenuProvider {
*///?}
    /** {@code BUTTON_CODE_BASE + position * 16 + dye color id}: picks the color of one code position. */
    public static final int BUTTON_CODE_BASE = 3000;
    /** {@code BUTTON_CONTAINER + chamber}: the carried container is emptied into (or filled from) that chamber. */
    public static final int BUTTON_CONTAINER = 3100;
    /**
     * Personal code on / off: a public tank turns personal with the personal item on the cursor (one is used); a
     * personal one turns public again and gives the item back.
     */
    public static final int BUTTON_PERSONAL = 3200;
    /**
     * {@code BUTTON_CLEAR + chamber}: empties that chamber. On a single or dual tank the liquid of the channel is
     * deleted for every tank with the code; the creative tank forgets its liquid.
     */
    public static final int BUTTON_CLEAR = 3300;
    public static final int BUTTON_CHANNEL_LOCK = 3400;
    /** Client updates of the liquids at most every this many ticks. */
    private static final int SYNC_INTERVAL = 2;

    private final TankKind kind;
    private final SideConfiguration sides;
    private final TankContainerSlots containers = new TankContainerSlots(this);
    private boolean autoInputItems;
    private boolean autoEjectItems;

    public TankContainerSlots containers() {
        return containers;
    }

    //? if >=26.1 {
    public dimensionalfluxminer.machine.transfer.ItemSlots itemFor(Direction side) {
    //?} else {
    /*/^* Item handler of a world side: the container slot of the face's route (single tank only); read-only without a face. ^/
    public IItemHandler itemFor(Direction side) {
    *///?}
        if (kind != TankKind.SINGLE) {
            return null;
        }
        return side == null ? containers.readOnly()
                : containers.face(sides.get(RelativeFace.of(facing(), side), Transmission.ITEM, 0));
    }

    public boolean configureItems(RelativeFace face, SideRoute route) {
        if (kind != TankKind.SINGLE || !sides.set(face, Transmission.ITEM, 0, route)) {
            return false;
        }
        onSidesChanged();
        return true;
    }
    //? if <26.1 {

    /*public boolean autoInputItems() {
        return autoInputItems;
    }

    public boolean autoEjectItems() {
        return autoEjectItems;
    }

    *///?}
    private TankCode code = TankCode.DEFAULT;
    private boolean autoInputFluid;
    private boolean autoEjectFluid;
    /** Server: the channel of the current code, looked up again when the code changes. */
    private TankChannels.Channel channel;
    private TankChannels.Channel local;
    private boolean channelLocked;
    private int syncedVersion = -1;
    /** Game time of the last client update; starts far enough back for the first tick to send one (no overflow). */
    private long lastSync = -SYNC_INTERVAL;
    /** Client: the chambers last received from the server. */
    private FluidStack[] clientChambers;
    //? if >=26.1 {
    private final Map<Direction, FluidTanks> handlers = new EnumMap<>(Direction.class);
    //?} else {
    /*private final Map<Direction, IFluidHandler> handlers = new EnumMap<>(Direction.class);
    *///?}
    /** Creative tank: its endless liquid (amount = capacity), and a version bumped on every change. */
    private FluidStack creativeFluid = FluidStack.EMPTY;
    private int creativeVersion;
    //? if >=26.1 {

    /** The chambers of the channel (and the creative liquid), put back if a transaction of another block that changed them is cancelled. */
    private final Transfer.Journal<Object[]> journal = new Transfer.Journal<>(() -> {
        TankChannels.Channel c = channel();
        FluidStack[] copy = new FluidStack[c == null ? 0 : c.chambers()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = c.get(i).copy();
        }
        return new Object[] {c, copy, creativeFluid.copy(), containers.snapshot(), containers.completionSnapshot(), containers.progressSnapshot()};
    }, snapshot -> {
        if (snapshot[0] instanceof TankChannels.Channel c) {
            FluidStack[] copy = (FluidStack[]) snapshot[1];
            for (int i = 0; i < copy.length; i++) {
                c.set(i, copy[i]);
            }
        }
        containers.restore((ItemStack[]) snapshot[3]);
        containers.restoreCompletion((boolean[]) snapshot[4]);
        containers.restoreProgress((int[]) snapshot[5]);
        FluidStack creative = (FluidStack) snapshot[2];
        if (!FluidStack.matches(creative, creativeFluid)) {
            creativeFluid = creative;
            creativeVersion++;
        }
    });

    public Transfer.Journal<Object[]> journal() {
        return journal;
    }
    //?}

    public DimensionalTankBlockEntity(BlockPos pos, BlockState state) {
        super(DfmBlockEntities.DIMENSIONAL_TANK.get(), pos, state);
        this.kind = state.getBlock() instanceof DimensionalTankBlock block ? block.kind() : TankKind.SINGLE;
        this.local = TankChannels.local(kind, this::setChanged);
        this.sides = new SideConfiguration(kind == TankKind.DUAL ? SideRules.TANK_DUAL
                : kind == TankKind.SINGLE ? SideRules.TANK_CONTAINERS : SideRules.TANK_SINGLE);
        this.clientChambers = emptyChambers(kind);
    }

    private static FluidStack[] emptyChambers(TankKind kind) {
        FluidStack[] result = new FluidStack[kind.chambers()];
        java.util.Arrays.fill(result, FluidStack.EMPTY);
        return result;
    }

    // ---------------------------------------------------------------- state

    public TankKind kind() {
        return kind;
    }

    public TankCode code() {
        return code;
    }

    public boolean channelLocked() {
        return channelLocked;
    }

    /** Switches storage without moving fluid between the shared channel and this block. */
    public boolean setChannelLocked(boolean locked) {
        if (kind.creative() || channelLocked == locked) {
            return false;
        }
        channelLocked = locked;
        syncedVersion = -1;
        handlers.clear();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
        setChanged();
        updateLight();
        syncToClients();
        return true;
    }

    public Direction facing() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public SideConfiguration sides() {
        return sides;
    }

    public boolean autoInputFluid() {
        return autoInputFluid;
    }

    public boolean autoEjectFluid() {
        return autoEjectFluid;
    }

    public void setAutoInputFluid(boolean on) {
        autoInputFluid = on;
        setChanged();
    }

    public void setAutoEjectFluid(boolean on) {
        autoEjectFluid = on;
        setChanged();
    }

    /** Points the tank at another channel. The content of both channels stays where it is. The creative tank has no channel. */
    public void setCode(TankCode code) {
        if (kind.creative() || channelLocked || code.equals(this.code)) {
            return;
        }
        boolean sameChannel = code.sameChannel(this.code);
        this.code = code;
        if (!sameChannel) {
            channel = null;
        }
        syncedVersion = -1;
        setChanged();
        syncToClients();
    }

    /**
     * Makes the code personal for {@code player} (a channel of its own, apart from the public one with the same colors),
     * using one personal item of {@code source} unless the player is in creative. False if already personal or the
     * stack is not the personal item.
     */
    public boolean makePersonal(Player player, ItemStack source) {
        if (kind.creative() || channelLocked || code.personal() || !source.is(dimensionalfluxminer.DfmConfig.tankPersonalItem())) {
            return false;
        }
        setCode(code.withOwner(player));
        if (!player.hasInfiniteMaterials()) {
            source.shrink(1);
        }
        return true;
    }

    //? if >=26.1 {
    /** Anarchy mode: the drop is public (see the components), so the personal item drops apart. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide() && kind == TankKind.SINGLE) {
            for (int slot = 0; slot < containers.getSlots(); slot++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), containers.getStackInSlot(slot));
                containers.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (level != null && dimensionalfluxminer.DfmConfig.tankAnarchyMode() && code().personal()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem()));
        }
    }

    //?}
    /** Makes a personal code public again and gives the personal item back (none in creative). */
    public boolean makePublic(Player player) {
        if (channelLocked || !code.personal()) {
            return false;
        }
        setCode(code.withoutOwner());
        if (!player.hasInfiniteMaterials()) {
            MachineMenu.giveToPlayer(player, new ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem()), true);
        }
        return true;
    }

    /** Server: the active shared or local storage (null on the client or for the creative tank). */
    public TankChannels.Channel channel() {
        //? if >=26.1 {
        if (channelLocked && !kind.creative() && level != null && !level.isClientSide()) {
        //?} else {
        /*if (channelLocked && !kind.creative() && level != null && !level.isClientSide) {
        *///?}
            return local;
        }
        //? if >=26.1 {
        if (channel == null && !kind.creative() && level != null && !level.isClientSide() && level.getServer() != null) {
        //?} else {
        /*if (channel == null && !kind.creative() && level != null && !level.isClientSide && level.getServer() != null) {
        *///?}
            channel = TankChannels.get(level.getServer()).channel(kind, code);
        }
        return channel;
    }

    /** Liquid of a chamber: the channel on the server, the last update on the client. Do not modify it. */
    public FluidStack chamber(int index) {
        //? if >=26.1 {
        if (level != null && level.isClientSide()) {
        //?} else {
        /*if (level != null && level.isClientSide) {
        *///?}
            return index < clientChambers.length ? clientChambers[index] : FluidStack.EMPTY;
        }
        if (kind.creative()) {
            return index == 0 ? creativeFluid : FluidStack.EMPTY;
        }
        TankChannels.Channel c = channel();
        return c == null ? FluidStack.EMPTY : c.get(index);
    }

    /** Full access to one chamber (both directions), as used by buckets and the screen. */
    //? if >=26.1 {
    public FluidTanks chamberHandler(int index) {
    //?} else {
    /*public IFluidHandler chamberHandler(int index) {
    *///?}
        return kind.creative() ? CreativeFluidHandler.forPlayer(this) : new ChamberFluidHandler(this::channel, index, true, true);
    }

    /** Creative tank: makes {@code fluid} its endless liquid (empty forgets it). */
    public void setCreativeFluid(FluidStack fluid) {
        if (!kind.creative()) {
            return;
        }
        FluidStack next = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(kind.capacity());
        if (FluidStack.matches(next, creativeFluid)) {
            return;
        }
        creativeFluid = next;
        creativeVersion++;
        setChanged();
    }

    /** Version of the content: the channel's, or the creative tank's own; -1 when unknown. */
    private int contentVersion() {
        if (kind.creative()) {
            return creativeVersion;
        }
        TankChannels.Channel c = channel();
        return c == null ? -1 : c.version();
    }

    /** Handler of a face or of the automation: on the creative tank, liquid from outside is voided while it is empty. */
    //? if >=26.1 {
    private FluidTanks handler(int chamber, java.util.function.BooleanSupplier fills, boolean drains) {
    //?} else {
    /*private IFluidHandler handler(int chamber, java.util.function.BooleanSupplier fills, boolean drains) {
    *///?}
        return kind.creative() ? new CreativeFluidHandler(() -> chamber(0), null, fills, drains)
                : new ChamberFluidHandler(this::channel, chamber, fills, drains);
    }

    public SideRoute route(RelativeFace face) {
        return sides.get(face, Transmission.FLUID, 0);
    }

    public boolean configureSide(RelativeFace face, SideRoute route) {
        boolean ok = sides.set(face, Transmission.FLUID, 0, route);
        if (ok) {
            onSidesChanged();
        }
        return ok;
    }

    // ---------------------------------------------------------------- ticking

    /** Light the block gives: the brightest liquid in its chambers (lava 15), 0 when empty or for liquids that do not glow. */
    public int lightLevel() {
        int light = 0;
        for (int i = 0; i < kind.chambers(); i++) {
            FluidStack stack = chamber(i);
            if (!stack.isEmpty()) {
                light = Math.max(light, stack.getFluid().getFluidType().getLightLevel(stack));
            }
        }
        return Math.min(15, light);
    }

    /** Light last given to the light engine on this side (-1 before the first check). */
    private int litLevel = -1;

    /** Tells this side's light engine when the light of the liquids inside changed. */
    private void updateLight() {
        int light = lightLevel();
        if (light != litLevel && level != null) {
            litLevel = light;
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
    }

    /** Channel version last sent to the clients (-1 before the first update). */
    public int syncedVersion() {
        return syncedVersion;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalTankBlockEntity be) {
        if (be.autoInputFluid || be.autoEjectFluid) {
            be.automation();
        }
        if (be.kind == TankKind.SINGLE) {
            be.itemAutomation();
            be.containers.tick();
        }
        int version = be.contentVersion();
        long now = level.getGameTime();
        if (version >= 0 && version != be.syncedVersion && now - be.lastSync >= SYNC_INTERVAL) {
            be.syncedVersion = version;
            be.lastSync = now;
            be.syncToClients();
            be.updateLight();
        }
    }

    //? if >=26.1 {
    private void itemAutomation() {
    //?} else {
    /*/^* Auto Input pulls a new container into, and Auto Eject pushes a finished one out of, the slot of each face. ^/
    private void itemAutomation() {
    *///?}
        if (!autoInputItems && !autoEjectItems) {
            return;
        }
        for (RelativeFace face : RelativeFace.VALUES) {
            SideRoute route = sides.get(face, Transmission.ITEM, 0);
            if (route.containerSlot() < 0) {
                continue;
            }
            //? if >=26.1 {
            var own = Transfer.exposeItems(containers.face(route), journal);
            //?} else {
            /*IItemHandler own = containers.face(route);
            *///?}
            Direction direction = face.toDirection(facing());
            //? if <26.1 {
            /*BlockPos target = worldPosition.relative(direction);
            if (!level.isLoaded(target)) {
                continue;
            }
            IItemHandler neighbor = level.getCapability(Capabilities.ItemHandler.BLOCK, target, direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            *///?}
            if (autoEjectItems && route.extractsContainers()) {
                //? if >=26.1 {
                Transfer.moveContainer(level, worldPosition, direction, own, false);
                //?} else {
                /*moveOne(own, neighbor);
                *///?}
            }
            if (autoInputItems && route.insertsContainers()) {
                //? if >=26.1 {
                Transfer.moveContainer(level, worldPosition, direction, own, true);
            }
                //?} else {
                /*moveOne(neighbor, own);
            }
        }
    }

    /^* Moves one item from the first slot of {@code from} that {@code to} fully accepts. ^/
    private static void moveOne(IItemHandler from, IItemHandler to) {
        for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack one = from.extractItem(slot, 1, true);
            if (one.isEmpty() || !ItemHandlerHelper.insertItem(to, one, true).isEmpty()) {
                continue;
            }
            ItemStack taken = from.extractItem(slot, 1, false);
            ItemStack left = ItemHandlerHelper.insertItem(to, taken, false);
            if (!left.isEmpty()) {
                // Should not happen after the simulation; put it back where it came from.
                from.insertItem(slot, left, false);
            }
            return;
                *///?}
        }
    }

    /** Auto Input pulls into, and Auto Eject pushes out of, the chamber of each face, without a per-tick limit. */
    private void automation() {
        for (RelativeFace face : RelativeFace.VALUES) {
            SideRoute route = route(face);
            int chamber = route.fluidChamber();
            if (chamber < 0) {
                continue;
            }
            boolean in = autoInputFluid && route.fillsFluid();
            boolean out = autoEjectFluid && route.drainsFluid();
            if (!in && !out) {
                continue;
            }
            Direction side = face.toDirection(facing());
            BlockPos target = worldPosition.relative(side);
            if (!level.isLoaded(target)) {
                continue;
            }
            //? if >=26.1 {
            FluidTanks neighbor = Transfer.fluids(level, target, side.getOpposite());
            //?} else {
            /*IFluidHandler neighbor = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side.getOpposite());
            *///?}
            if (neighbor == null) {
                continue;
            }
            //? if >=26.1 {
            FluidTanks own = handler(chamber, () -> true, true);
            //?} else {
            /*IFluidHandler own = handler(chamber, () -> true, true);
            *///?}
            if (in) {
                //? if >=26.1 {
                FluidTanks.move(own, neighbor, Integer.MAX_VALUE);
                //?} else {
                /*FluidUtil.tryFluidTransfer(own, neighbor, Integer.MAX_VALUE, true);
                *///?}
            }
            if (out) {
                //? if >=26.1 {
                FluidTanks.move(neighbor, own, Integer.MAX_VALUE);
                //?} else {
                /*FluidUtil.tryFluidTransfer(neighbor, own, Integer.MAX_VALUE, true);
                *///?}
            }
        }
    }

    // ---------------------------------------------------------------- capabilities

    /**
     * Fluid handler of a world side: the chamber of the face's route, filling and/or draining as the route allows; no
     * handler without a route. While Auto Eject is on, a face that ejects takes nothing from outside: a pipe network
     * would otherwise send part of the ejected liquid straight back (the last mB bouncing between tank and pipe). The
     * {@code null} side is a read-only view of every chamber (information tools).
     */
    //? if >=26.1 {
    public FluidTanks fluidFor(Direction side) {
    //?} else {
    /*public IFluidHandler fluidFor(Direction side) {
    *///?}
        if (side == null) {
            return new ReadOnlyChambers();
        }
        SideRoute route = route(RelativeFace.of(facing(), side));
        if (route.fluidChamber() < 0) {
            return null;
        }
        boolean fills = route.fillsFluid();
        boolean drains = route.drainsFluid();
        return handlers.computeIfAbsent(side, s -> handler(route.fluidChamber(), () -> fills && !(drains && autoEjectFluid), drains));
    }

    private void onSidesChanged() {
        handlers.clear();
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    /** Every chamber, nothing can be inserted or extracted. */
    //? if >=26.1 {
    private final class ReadOnlyChambers implements FluidTanks {
    //?} else {
    /*private final class ReadOnlyChambers implements IFluidHandler {
    *///?}
        @Override
        public int getTanks() {
            return kind.chambers();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return chamber(tank).copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            return kind.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    // ---------------------------------------------------------------- menu

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TankMenu(containerId, inventory, this);
    }

    /** Applies a validated GUI button press on the server. */
    public boolean handleButton(Player player, int id) {
        if (id == BUTTON_CHANNEL_LOCK) {
            return setChannelLocked(!channelLocked);
        }
        if (channelLocked && (id == BUTTON_PERSONAL || id >= BUTTON_CODE_BASE && id < BUTTON_CODE_BASE + TankCode.POSITIONS * 16)) {
            return false;
        }
        if (kind.creative() && (id == BUTTON_PERSONAL || id >= BUTTON_CODE_BASE && id < BUTTON_CODE_BASE + TankCode.POSITIONS * 16)) {
            // No channels on the creative tank.
            return false;
        }
        if (id >= BUTTON_CLEAR && id < BUTTON_CLEAR + kind.chambers()) {
            int chamber = id - BUTTON_CLEAR;
            if (chamber(chamber).isEmpty()) {
                return false;
            }
            if (kind.creative()) {
                setCreativeFluid(FluidStack.EMPTY);
            } else {
                channel().set(chamber, FluidStack.EMPTY);
            }
            return true;
        }
        if (id >= BUTTON_CODE_BASE && id < BUTTON_CODE_BASE + TankCode.POSITIONS * 16) {
            int position = (id - BUTTON_CODE_BASE) / 16;
            DyeColor color = DyeColor.byId((id - BUTTON_CODE_BASE) % 16);
            TankCode next = code.with(position, color);
            if (next.equals(code)) {
                return false;
            }
            setCode(next);
            return true;
        }
        if (id == BUTTON_PERSONAL && player != null) {
            if (code.personal()) {
                return makePublic(player);
            }
            ItemStack carried = player.containerMenu.getCarried();
            boolean done = makePersonal(player, carried);
            if (done && carried.isEmpty()) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            return done;
        }
        if (id >= BUTTON_CONTAINER && id < BUTTON_CONTAINER + kind.chambers()) {
            return useCarried(player, id - BUTTON_CONTAINER);
        }
        if (id == AbstractMachineBlockEntity.BUTTON_AUTO_FLUID_NEXT || id == AbstractMachineBlockEntity.BUTTON_AUTO_FLUID_PREVIOUS) {
            // Bit 0 = Auto Input, bit 1 = Auto Eject: 0 OFF, 1 input, 2 eject, 3 both.
            int mode = (autoInputFluid ? 1 : 0) | (autoEjectFluid ? 2 : 0);
            int next = Math.floorMod(mode + (id == AbstractMachineBlockEntity.BUTTON_AUTO_FLUID_NEXT ? 1 : -1), 4);
            autoInputFluid = (next & 1) != 0;
            autoEjectFluid = (next & 2) != 0;
            setChanged();
            return true;
        }
        if (id == AbstractMachineBlockEntity.BUTTON_AUTO_INPUT_FLUID) {
            autoInputFluid = !autoInputFluid;
            setChanged();
            return true;
        }
        if (id == AbstractMachineBlockEntity.BUTTON_AUTO_EJECT_FLUID) {
            autoEjectFluid = !autoEjectFluid;
            setChanged();
            return true;
        }
        if (kind == TankKind.SINGLE && id == AbstractMachineBlockEntity.BUTTON_AUTO_INPUT) {
            autoInputItems = !autoInputItems;
            setChanged();
            return true;
        }
        if (kind == TankKind.SINGLE && id == AbstractMachineBlockEntity.BUTTON_AUTO_EJECT) {
            autoEjectItems = !autoEjectItems;
            setChanged();
            return true;
        }
        int sideBase = AbstractMachineBlockEntity.BUTTON_SIDE_BASE;
        if (id >= sideBase && id < sideBase + RelativeFace.VALUES.length * 16) {
            int local = id - sideBase;
            int transmission = (local % 16) / 2;
            if (transmission != Transmission.FLUID.ordinal() && !(kind == TankKind.SINGLE && transmission == Transmission.ITEM.ordinal())) {
                return false;
            }
            if (sides.cycle(RelativeFace.VALUES[local / 16], Transmission.VALUES[transmission], 0, local % 2 == 0)) {
                onSidesChanged();
                return true;
            }
            return false;
        }
        if (id == AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL) {
            sides.clear(Transmission.FLUID, 0);
            sides.clear(Transmission.ITEM, 0);
            onSidesChanged();
            return true;
        }
        for (Transmission transmission : new Transmission[] {Transmission.ITEM, Transmission.FLUID}) {
            if (transmission == Transmission.ITEM && kind != TankKind.SINGLE) {
                continue;
            }
            if (id == AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR + transmission.ordinal()) {
                sides.clear(transmission, 0);
                onSidesChanged();
                return true;
            }
            int batch = AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + transmission.ordinal() * 2;
            if (id == batch || id == batch + 1) {
                SideRoute target = sides.batchTarget(transmission, 0, id == batch);
                if (sides.applyAll(transmission, 0, target)) {
                    onSidesChanged();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The carried container (bucket or any fluid item) is emptied into the chamber, or, if it cannot be emptied, filled
     * from it. One container at a time; the result goes back to the cursor (or the inventory).
     */
    private boolean useCarried(Player player, int chamber) {
        //? if >=26.1 {
        if (player.containerMenu.getCarried().isEmpty() || level == null) {
        //?} else {
        /*AbstractContainerMenu menu = player.containerMenu;
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
        *///?}
            return false;
        }
        //? if >=26.1 {
        ResourceHandler<FluidResource> item = ItemAccess.forPlayerCursor(player, player.containerMenu).oneByOne()
                .getCapability(Capabilities.Fluid.ITEM);
        if (item == null) {
        //?} else {
        /*ItemStack one = carried.copyWithCount(1);
        IFluidHandler handler = chamberHandler(chamber);
        FluidActionResult result = FluidUtil.tryEmptyContainer(one, handler, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) {
            result = FluidUtil.tryFillContainer(one, handler, Integer.MAX_VALUE, player, true);
        }
        if (!result.isSuccess()) {
        *///?}
            return false;
        }
        //? if >=26.1 {
        ResourceHandler<FluidResource> tank = Transfer.exposeFluids(chamberHandler(chamber), journal);
        try (Transaction transaction = Transaction.openRoot()) {
            boolean pickup = false;
            ResourceStack<FluidResource> moved = ResourceHandlerUtil.moveFirst(item, tank, resource -> true, Integer.MAX_VALUE, transaction);
            if (moved == null) {
                pickup = true;
                moved = ResourceHandlerUtil.moveFirst(tank, item, resource -> true, Integer.MAX_VALUE, transaction);
            }
            if (moved == null) {
                return false;
            }
            transaction.commit();
            FluidUtil.triggerSoundAndGameEvent(moved.resource(), level, player.position(), player, pickup);
            return true;
        }
    }
        //?} else {
        /*carried.shrink(1);
        if (carried.isEmpty()) {
            menu.setCarried(result.getResult());
        } else {
            MachineMenu.giveToPlayer(player, result.getResult(), true);
        }
        return true;
    }

        *///?}
    /** Snapshot sent to open menus (server -> client). */
    public void writeMenuData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("channel_locked", channelLocked);
        tag.putString("kind", kind.id());
        tag.put("code", code.save());
        tag.put("chambers", writeChambers(registries));
        tag.putInt("capacity", kind.capacity());
        tag.putBoolean("auto_input_fluid", autoInputFluid);
        tag.putBoolean("auto_eject_fluid", autoEjectFluid);
        tag.put("sides", sides.save());
        tag.putBoolean("auto_input", autoInputItems);
        tag.putBoolean("auto_eject", autoEjectItems);
    }

    private ListTag writeChambers(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < kind.chambers(); i++) {
            //? if >=26.1 {
            list.add(dimensionalfluxminer.machine.transfer.TagStacks.save(registries, chamber(i)));
            //?} else {
            /*list.add(chamber(i).saveOptional(registries));
            *///?}
        }
        return list;
    }

    public static FluidStack[] readChambers(ListTag list, int count, HolderLookup.Provider registries) {
        FluidStack[] result = new FluidStack[count];
        for (int i = 0; i < count; i++) {
            //? if >=26.1 {
            result[i] = i < list.size() ? dimensionalfluxminer.machine.transfer.TagStacks.parseFluid(registries, list.getCompoundOrEmpty(i)) : FluidStack.EMPTY;
            //?} else {
            /*result[i] = i < list.size() ? FluidStack.parseOptional(registries, list.getCompound(i)) : FluidStack.EMPTY;
            *///?}
        }
        return result;
    }

    // ---------------------------------------------------------------- persistence

    @Override
    //? if >=26.1 {
    protected void saveTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveTag(tag, registries);
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    *///?}
        tag.putBoolean("channel_locked", channelLocked);
        ListTag localChambers = new ListTag();
        for (int i = 0; i < kind.chambers(); i++) {
            //? if >=26.1 {
            localChambers.add(TagStacks.save(registries, local.get(i)));
            //?} else {
            /*localChambers.add(local.get(i).saveOptional(registries));
            *///?}
        }
        tag.put("local_chambers", localChambers);
        tag.put("code", code.save());
        tag.put("sides", sides.save());
        tag.putBoolean("auto_input", autoInputItems);
        tag.putBoolean("auto_eject", autoEjectItems);
        tag.putBoolean("auto_input_fluid", autoInputFluid);
        tag.putBoolean("auto_eject_fluid", autoEjectFluid);
        if (kind == TankKind.SINGLE) {
            tag.put("containers", containers.serializeNBT(registries));
        }
        if (!creativeFluid.isEmpty()) {
            //? if >=26.1 {
            tag.put("creative_fluid", TagStacks.save(registries, creativeFluid));
            //?} else {
            /*tag.put("creative_fluid", creativeFluid.save(registries));
            *///?}
        }
    }

    @Override
    //? if >=26.1 {
    protected void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        channelLocked = !kind.creative() && tag.getBooleanOr("channel_locked", false);
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channelLocked = !kind.creative() && tag.getBoolean("channel_locked");
    *///?}
        local = TankChannels.local(kind, this::setChanged);
        //? if >=26.1 {
        FluidStack[] savedLocal = readChambers(tag.getListOrEmpty("local_chambers"), kind.chambers(), registries);
        //?} else {
        /*FluidStack[] savedLocal = readChambers(tag.getList("local_chambers", Tag.TAG_COMPOUND), kind.chambers(), registries);
        *///?}
        for (int i = 0; i < savedLocal.length; i++) {
            local.set(i, savedLocal[i].copyWithAmount(Math.min(kind.capacity(), savedLocal[i].getAmount())));
        }
        syncedVersion = -1;
        //? if >=26.1 {
        code = tag.getCompound("code").map(TankCode::load).orElse(TankCode.DEFAULT);
        sides.load(tag.getCompoundOrEmpty("sides"));
        autoInputItems = tag.getBooleanOr("auto_input", false);
        autoEjectItems = tag.getBooleanOr("auto_eject", false);
        //?} else {
        /*code = tag.contains("code", Tag.TAG_COMPOUND) ? TankCode.load(tag.getCompound("code")) : TankCode.DEFAULT;
        sides.load(tag.getCompound("sides"));
        autoInputItems = tag.getBoolean("auto_input");
        autoEjectItems = tag.getBoolean("auto_eject");
        *///?}
        if (kind == TankKind.SINGLE) {
            //? if >=26.1 {
            containers.load(registries, tag.getCompoundOrEmpty("containers"));
        }
        autoInputFluid = tag.getBooleanOr("auto_input_fluid", false);
        autoEjectFluid = tag.getBooleanOr("auto_eject_fluid", false);
            //?} else {
            /*containers.load(registries, tag.getCompound("containers"));
        }
        autoInputFluid = tag.getBoolean("auto_input_fluid");
        autoEjectFluid = tag.getBoolean("auto_eject_fluid");
            *///?}
        creativeFluid = FluidStack.EMPTY;
        //? if >=26.1 {
        if (kind.creative()) {
            creativeFluid = tag.getCompound("creative_fluid").map(t -> TagStacks.parseFluid(registries, t)).orElse(FluidStack.EMPTY);
        //?} else {
        /*if (kind.creative() && tag.contains("creative_fluid", Tag.TAG_COMPOUND)) {
            creativeFluid = FluidStack.parseOptional(registries, tag.getCompound("creative_fluid"));
        *///?}
            if (!creativeFluid.isEmpty()) {
                creativeFluid = creativeFluid.copyWithAmount(kind.capacity());
            }
        }
        creativeVersion++;
        channel = null;
        handlers.clear();
    }

    @Override
    //? if >=26.1 {
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
    //?} else {
    /*protected void applyImplicitComponents(DataComponentInput input) {
    *///?}
        super.applyImplicitComponents(input);
        if (kind.creative()) {
            SimpleFluidContent fluid = input.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
            if (fluid != null) {
                setCreativeFluid(fluid.copy());
            }
            return;
        }
        TankCode stored = input.get(DfmDataComponents.TANK_CODE.get());
        // A new tank item (no code yet) is placed locked; a broken tank keeps its state.
        Boolean locked = input.get(DfmDataComponents.TANK_CHANNEL_LOCKED.get());
        channelLocked = locked != null ? locked : stored == null;
        for (int i = 0; i < kind.chambers(); i++) {
            SimpleFluidContent fluid = input.get(i == 0 ? DfmDataComponents.TANK_LOCAL_A.get() : DfmDataComponents.TANK_LOCAL_B.get());
            local.set(i, fluid == null ? FluidStack.EMPTY : fluid.copy().copyWithAmount(Math.min(kind.capacity(), fluid.getAmount())));
        }
        if (stored != null) {
            code = stored;
            channel = null;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (kind.creative()) {
            if (!creativeFluid.isEmpty()) {
                components.set(DfmDataComponents.CREATIVE_TANK_FLUID.get(), SimpleFluidContent.copyOf(creativeFluid.copyWithAmount(1)));
            }
            return;
        }
        // Anarchy mode: a broken personal tank comes back public (the block drops the personal item apart).
        components.set(DfmDataComponents.TANK_CODE.get(), dimensionalfluxminer.DfmConfig.tankAnarchyMode() ? code.withoutOwner() : code);
        if (channelLocked) {
            components.set(DfmDataComponents.TANK_CHANNEL_LOCKED.get(), true);
        }
        if (!channelLocked) {
            for (int i = 0; i < kind.chambers(); i++) {
                components.set(i == 0 ? DfmDataComponents.TANK_DISPLAY_A.get() : DfmDataComponents.TANK_DISPLAY_B.get(),
                        SimpleFluidContent.copyOf(chamber(i)));
            }
        }
        for (int i = 0; i < kind.chambers(); i++) {
            if (!local.get(i).isEmpty()) {
                components.set(i == 0 ? DfmDataComponents.TANK_LOCAL_A.get() : DfmDataComponents.TANK_LOCAL_B.get(), SimpleFluidContent.copyOf(local.get(i)));
            }
        }
    }

    @Override
    //? if >=26.1 {
    public void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput output) {
        output.discard("code");
        output.discard("creative_fluid");
        output.discard("channel_locked");
        output.discard("local_chambers");
    //?} else {
    /*@SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("code");
        tag.remove("creative_fluid");
        tag.remove("channel_locked");
        tag.remove("local_chambers");
    *///?}
    }

    // ---------------------------------------------------------------- client sync

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("channel_locked", channelLocked);
        tag.put("code", code.save());
        tag.put("chambers", writeChambers(registries));
        return tag;
    }

    @Override
    //? if >=26.1 {
    protected void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
    //?} else {
    /*public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
    *///?}
        readClientData(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if <26.1 {
    /*@Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            readClientData(tag, registries);
        }
    }
    *///?}

    private void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        //? if >=26.1 {
        channelLocked = !kind.creative() && tag.getBooleanOr("channel_locked", false);
        code = TankCode.load(tag.getCompoundOrEmpty("code"));
        clientChambers = readChambers(tag.getListOrEmpty("chambers"), kind.chambers(), registries);
        //?} else {
        /*channelLocked = !kind.creative() && tag.getBoolean("channel_locked");
        code = TankCode.load(tag.getCompound("code"));
        clientChambers = readChambers(tag.getList("chambers", Tag.TAG_COMPOUND), kind.chambers(), registries);
        *///?}
        updateLight();
    }

    private void syncToClients() {
        //? if >=26.1 {
        if (level != null && !level.isClientSide()) {
        //?} else {
        /*if (level != null && !level.isClientSide) {
        *///?}
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
