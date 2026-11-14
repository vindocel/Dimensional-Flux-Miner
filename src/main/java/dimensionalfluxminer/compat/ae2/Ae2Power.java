package dimensionalfluxminer.compat.ae2;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.parts.IPartHost;
import appeng.blockentity.networking.CreativeEnergyCellBlockEntity;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.parts.networking.QuartzFiberPart;
import dimensionalfluxminer.machine.transfer.EnergyStore;
import dimensionalfluxminer.registry.DfmBlockEntities;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * AE2 side of {@link dimensionalfluxminer.compat.Ae2Compat}. A block entity joins an AE2 network only through power
 * links: an energy cell (normal, dense or creative) touching a side that takes energy, or a quartz fiber mounted on the
 * face of a cable bus that points at that side. ME cables and other parts never connect, so a cable run can pass next
 * to the machines without touching them. The node needs no channel and uses no idle power; every tick it draws from
 * the network what the energy input still accepts.
 */
public final class Ae2Power {
    /** How often, in ticks, the neighbors are checked again for power links (staggered by position). */
    private static final int LINK_CHECK_TICKS = 10;
    private static final Map<BlockEntity, Node> NODES = new WeakHashMap<>();

    private Ae2Power() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(Ae2Power::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<?> type : java.util.List.of(DfmBlockEntities.MECHANICAL_CRUSHER.get(), DfmBlockEntities.ELECTRIC_FURNACE.get(),
                DfmBlockEntities.MINER_LINK.get())) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type, (be, context) -> NODES.get(be));
        }
    }

    public static void pull(BlockEntity be, Function<Direction, EnergyStore> input) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide() || be.isRemoved()) {
            return;
        }
        Set<Direction> inputs = EnumSet.noneOf(Direction.class);
        for (Direction side : Direction.values()) {
            EnergyStore s = input.apply(side);
            if (s != null && s.canReceive()) {
                inputs.add(side);
            }
        }
        BlockPos pos = be.getBlockPos();
        boolean check = Math.floorMod(level.getGameTime() + pos.asLong(), LINK_CHECK_TICKS) == 0;
        Node node = NODES.get(be);
        if (node == null) {
            if (inputs.isEmpty() || !check) {
                return;
            }
            Set<Direction> links = powerLinks(level, pos, inputs);
            if (links.isEmpty()) {
                return;
            }
            node = new Node(be);
            NODES.put(be, node);
            node.managed.setExposedOnSides(links);
            node.sides = links;
            node.managed.create(level, pos);
            be.invalidateCapabilities();
        } else if (check || !inputs.containsAll(node.sides)) {
            Set<Direction> links = powerLinks(level, pos, inputs);
            if (!node.sides.equals(links)) {
                node.managed.setExposedOnSides(links);
                node.sides = links;
            }
        }
        if (node.sides.isEmpty()) {
            return;
        }
        EnergyStore store = input.apply(node.sides.iterator().next());
        IGrid grid = node.managed.getGrid();
        if (store == null || grid == null) {
            return;
        }
        int room = store.receiveEnergy(Integer.MAX_VALUE, true);
        if (room <= 0) {
            return;
        }
        double wanted = PowerUnit.FE.convertTo(PowerUnit.AE, room);
        double taken = grid.getEnergyService().extractAEPower(wanted, Actionable.MODULATE, PowerMultiplier.ONE);
        int fe = (int) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, taken) + 1e-6);
        if (fe > 0) {
            store.receiveEnergy(fe, false);
        }
    }

    /** The sides in {@code inputs} whose neighbor is a power link (an energy cell or a quartz fiber facing us). */
    private static Set<Direction> powerLinks(Level level, BlockPos pos, Set<Direction> inputs) {
        Set<Direction> links = EnumSet.noneOf(Direction.class);
        for (Direction side : inputs) {
            BlockPos other = pos.relative(side);
            if (level.isLoaded(other) && isPowerLink(level.getBlockEntity(other), side.getOpposite())) {
                links.add(side);
            }
        }
        return links;
    }

    private static boolean isPowerLink(BlockEntity other, Direction towardUs) {
        if (other instanceof EnergyCellBlockEntity || other instanceof CreativeEnergyCellBlockEntity) {
            return true;
        }
        return other instanceof IPartHost host && host.getPart(towardUs) instanceof QuartzFiberPart;
    }

    public static void release(BlockEntity be) {
        Node node = NODES.remove(be);
        if (node != null) {
            node.managed.destroy();
        }
    }

    /** One block entity's node; the host AE2 asks for it through the capability. */
    private static final class Node implements IInWorldGridNodeHost {
        private static final IGridNodeListener<BlockEntity> LISTENER = (owner, gridNode) -> owner.setChanged();

        final IManagedGridNode managed;
        Set<Direction> sides = Set.of();

        Node(BlockEntity be) {
            managed = GridHelper.createManagedNode(be, LISTENER)
                    .setInWorldNode(true)
                    .setIdlePowerUsage(0)
                    .setTagName("dimensionalfluxminer_power")
                    .setVisualRepresentation(be.getBlockState().getBlock());
        }

        @Override
        public IGridNode getGridNode(Direction dir) {
            return sides.contains(dir) ? managed.getNode() : null;
        }
    }
}
