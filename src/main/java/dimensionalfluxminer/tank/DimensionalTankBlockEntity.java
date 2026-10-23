package dimensionalfluxminer.tank;

import dimensionalfluxminer.machine.transfer.FluidTanks;
import dimensionalfluxminer.machine.transfer.Transfer;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A Dimensional Tank: an endpoint of the channel named by its kind and its three-color code. The liquid is kept in
 * {@link TankChannels}, never here, so every tank with the same kind and code (in any dimension) sees and changes the
 * same content. No energy, no processing. Each face carries the liquid of one chamber (Side Configuration) and the
 * liquids have their own Auto Input and Auto Eject (default OFF); capabilities stay available with both OFF.
 */
public class DimensionalTankBlockEntity extends dimensionalfluxminer.content.TagBlockEntity implements MenuProvider {
    /** {@code BUTTON_CODE_BASE + position * 16 + dye color id}: picks the color of one code position. */
    public static final int BUTTON_CODE_BASE = 3000;
    /** {@code BUTTON_CONTAINER + chamber}: the carried container is emptied into (or filled from) that chamber. */
    public static final int BUTTON_CONTAINER = 3100;
    /**
     * Personal code on / off: a public tank turns personal with the personal item on the cursor (one is used); a
     * personal one turns public again and gives the item back.
     */
    public static final int BUTTON_PERSONAL = 3200;
    /** Client updates of the liquids at most every this many ticks. */
    private static final int SYNC_INTERVAL = 2;

    private final TankKind kind;
    private final SideConfiguration sides;
    private TankCode code = TankCode.DEFAULT;
    private boolean autoInputFluid;
    private boolean autoEjectFluid;
    /** Server: the channel of the current code, looked up again when the code changes. */
    private TankChannels.Channel channel;
    private int syncedVersion = -1;
    /** Game time of the last client update; starts far enough back for the first tick to send one (no overflow). */
    private long lastSync = -SYNC_INTERVAL;
    /** Client: the chambers last received from the server. */
    private FluidStack[] clientChambers;
    private final Map<Direction, FluidTanks> handlers = new EnumMap<>(Direction.class);

    /** The chambers of the channel, put back if a transaction of another block that changed them is cancelled. */
    private final Transfer.Journal<Object[]> journal = new Transfer.Journal<>(() -> {
        TankChannels.Channel c = channel();
        FluidStack[] copy = new FluidStack[c == null ? 0 : c.chambers()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = c.get(i).copy();
        }
        return new Object[] {c, copy};
    }, snapshot -> {
        if (snapshot[0] instanceof TankChannels.Channel c) {
            FluidStack[] copy = (FluidStack[]) snapshot[1];
            for (int i = 0; i < copy.length; i++) {
                c.set(i, copy[i]);
            }
        }
    });

    public Transfer.Journal<Object[]> journal() {
        return journal;
    }

    public DimensionalTankBlockEntity(BlockPos pos, BlockState state) {
        super(DfmBlockEntities.DIMENSIONAL_TANK.get(), pos, state);
        this.kind = state.getBlock() instanceof DimensionalTankBlock block ? block.kind() : TankKind.SINGLE;
        this.sides = new SideConfiguration(kind == TankKind.DUAL ? SideRules.TANK_DUAL : SideRules.TANK_SINGLE);
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

    /** Points the tank at another channel. The content of both channels stays where it is. */
    public void setCode(TankCode code) {
        if (code.equals(this.code)) {
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
        if (code.personal() || !source.is(dimensionalfluxminer.DfmConfig.tankPersonalItem())) {
            return false;
        }
        setCode(code.withOwner(player));
        if (!player.hasInfiniteMaterials()) {
            source.shrink(1);
        }
        return true;
    }

    /** Anarchy mode: the drop is public (see the components), so the personal item drops apart. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && dimensionalfluxminer.DfmConfig.tankAnarchyMode() && code().personal()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem()));
        }
    }

    /** Makes a personal code public again and gives the personal item back (none in creative). */
    public boolean makePublic(Player player) {
        if (!code.personal()) {
            return false;
        }
        setCode(code.withoutOwner());
        if (!player.hasInfiniteMaterials()) {
            MachineMenu.giveToPlayer(player, new ItemStack(dimensionalfluxminer.DfmConfig.tankPersonalItem()), true);
        }
        return true;
    }

    /** Server: the channel of this tank (null on the client or outside a server). */
    public TankChannels.Channel channel() {
        if (channel == null && level != null && !level.isClientSide() && level.getServer() != null) {
            channel = TankChannels.get(level.getServer()).channel(kind, code);
        }
        return channel;
    }

    /** Liquid of a chamber: the channel on the server, the last update on the client. Do not modify it. */
    public FluidStack chamber(int index) {
        if (level != null && level.isClientSide()) {
            return index < clientChambers.length ? clientChambers[index] : FluidStack.EMPTY;
        }
        TankChannels.Channel c = channel();
        return c == null ? FluidStack.EMPTY : c.get(index);
    }

    /** Full access to one chamber (both directions), as used by buckets and the screen. */
    public FluidTanks chamberHandler(int index) {
        return new ChamberFluidHandler(this::channel, index, true, true);
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
        TankChannels.Channel c = be.channel();
        long now = level.getGameTime();
        if (c != null && c.version() != be.syncedVersion && now - be.lastSync >= SYNC_INTERVAL) {
            be.syncedVersion = c.version();
            be.lastSync = now;
            be.syncToClients();
            be.updateLight();
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
            FluidTanks neighbor = Transfer.fluids(level, target, side.getOpposite());
            if (neighbor == null) {
                continue;
            }
            FluidTanks own = new ChamberFluidHandler(this::channel, chamber, true, true);
            if (in) {
                FluidTanks.move(own, neighbor, Integer.MAX_VALUE);
            }
            if (out) {
                FluidTanks.move(neighbor, own, Integer.MAX_VALUE);
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
    public FluidTanks fluidFor(Direction side) {
        if (side == null) {
            return new ReadOnlyChambers();
        }
        SideRoute route = route(RelativeFace.of(facing(), side));
        if (route.fluidChamber() < 0) {
            return null;
        }
        boolean fills = route.fillsFluid();
        boolean drains = route.drainsFluid();
        return handlers.computeIfAbsent(side, s -> new ChamberFluidHandler(this::channel, route.fluidChamber(),
                () -> fills && !(drains && autoEjectFluid), drains));
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
    private final class ReadOnlyChambers implements FluidTanks {
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
            return TankKind.CHAMBER_CAPACITY;
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
        int sideBase = AbstractMachineBlockEntity.BUTTON_SIDE_BASE;
        if (id >= sideBase && id < sideBase + RelativeFace.VALUES.length * 16) {
            int local = id - sideBase;
            int transmission = (local % 16) / 2;
            if (transmission != Transmission.FLUID.ordinal()) {
                return false;
            }
            if (sides.cycle(RelativeFace.VALUES[local / 16], Transmission.FLUID, 0, local % 2 == 0)) {
                onSidesChanged();
                return true;
            }
            return false;
        }
        if (id == AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR + Transmission.FLUID.ordinal()
                || id == AbstractMachineBlockEntity.BUTTON_SIDE_CLEAR_ALL) {
            sides.clear(Transmission.FLUID, 0);
            onSidesChanged();
            return true;
        }
        int batch = AbstractMachineBlockEntity.BUTTON_SIDE_BATCH + Transmission.FLUID.ordinal() * 2;
        if (id == batch || id == batch + 1) {
            SideRoute target = sides.batchTarget(Transmission.FLUID, 0, id == batch);
            if (sides.applyAll(Transmission.FLUID, 0, target)) {
                onSidesChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * The carried container (bucket or any fluid item) is emptied into the chamber, or, if it cannot be emptied, filled
     * from it. One container at a time; the result goes back to the cursor (or the inventory).
     */
    private boolean useCarried(Player player, int chamber) {
        if (player.containerMenu.getCarried().isEmpty() || level == null) {
            return false;
        }
        ResourceHandler<FluidResource> item = ItemAccess.forPlayerCursor(player, player.containerMenu).oneByOne()
                .getCapability(Capabilities.Fluid.ITEM);
        if (item == null) {
            return false;
        }
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
    /** Snapshot sent to open menus (server -> client). */
    public void writeMenuData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("kind", kind.id());
        tag.put("code", code.save());
        tag.put("chambers", writeChambers(registries));
        tag.putInt("capacity", TankKind.CHAMBER_CAPACITY);
        tag.putBoolean("auto_input_fluid", autoInputFluid);
        tag.putBoolean("auto_eject_fluid", autoEjectFluid);
        tag.put("sides", sides.save());
    }

    private ListTag writeChambers(HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < kind.chambers(); i++) {
            list.add(dimensionalfluxminer.machine.transfer.TagStacks.save(registries, chamber(i)));
        }
        return list;
    }

    public static FluidStack[] readChambers(ListTag list, int count, HolderLookup.Provider registries) {
        FluidStack[] result = new FluidStack[count];
        for (int i = 0; i < count; i++) {
            result[i] = i < list.size() ? dimensionalfluxminer.machine.transfer.TagStacks.parseFluid(registries, list.getCompoundOrEmpty(i)) : FluidStack.EMPTY;
        }
        return result;
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void saveTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveTag(tag, registries);
        tag.put("code", code.save());
        tag.put("sides", sides.save());
        tag.putBoolean("auto_input_fluid", autoInputFluid);
        tag.putBoolean("auto_eject_fluid", autoEjectFluid);
    }

    @Override
    protected void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        code = tag.getCompound("code").map(TankCode::load).orElse(TankCode.DEFAULT);
        sides.load(tag.getCompoundOrEmpty("sides"));
        autoInputFluid = tag.getBooleanOr("auto_input_fluid", false);
        autoEjectFluid = tag.getBooleanOr("auto_eject_fluid", false);
        channel = null;
        handlers.clear();
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter input) {
        super.applyImplicitComponents(input);
        TankCode stored = input.get(DfmDataComponents.TANK_CODE.get());
        if (stored != null) {
            code = stored;
            channel = null;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        // Anarchy mode: a broken personal tank comes back public (the block drops the personal item apart).
        components.set(DfmDataComponents.TANK_CODE.get(), dimensionalfluxminer.DfmConfig.tankAnarchyMode() ? code.withoutOwner() : code);
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput output) {
        output.discard("code");
    }

    // ---------------------------------------------------------------- client sync

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("code", code.save());
        tag.put("chambers", writeChambers(registries));
        return tag;
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientData(tag, registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    private void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        code = TankCode.load(tag.getCompoundOrEmpty("code"));
        clientChambers = readChambers(tag.getListOrEmpty("chambers"), kind.chambers(), registries);
        updateLight();
    }

    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
