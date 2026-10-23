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
import dimensionalfluxminer.machine.transfer.EnergyStore;
import dimensionalfluxminer.registry.DfmBlockEntities;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * AE2 side of {@link dimensionalfluxminer.compat.Ae2Compat}. Each block entity gets one in-world grid node, created
 * on its first server tick and exposed only on the sides where it takes energy; it needs no channel and uses no idle
 * power. Every tick the node draws from the network what the energy input still accepts.
 */
public final class Ae2Power {
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
        if (be.getLevel() == null || be.getLevel().isClientSide() || be.isRemoved()) {
            return;
        }
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        EnergyStore store = null;
        for (Direction side : Direction.values()) {
            EnergyStore s = input.apply(side);
            if (s != null && s.canReceive()) {
                sides.add(side);
                store = store == null ? s : store;
            }
        }
        Node node = NODES.get(be);
        if (node == null) {
            if (sides.isEmpty()) {
                return;
            }
            node = new Node(be);
            NODES.put(be, node);
            node.managed.setExposedOnSides(sides);
            node.sides = sides;
            node.managed.create(be.getLevel(), be.getBlockPos());
            be.invalidateCapabilities();
        } else if (!node.sides.equals(sides)) {
            node.managed.setExposedOnSides(sides);
            node.sides = sides;
        }
        if (store == null) {
            return;
        }
        int room = store.receiveEnergy(Integer.MAX_VALUE, true);
        IGrid grid = node.managed.getGrid();
        if (room <= 0 || grid == null) {
            return;
        }
        double wanted = PowerUnit.FE.convertTo(PowerUnit.AE, room);
        double taken = grid.getEnergyService().extractAEPower(wanted, Actionable.MODULATE, PowerMultiplier.ONE);
        int fe = (int) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, taken) + 1e-6);
        if (fe > 0) {
            store.receiveEnergy(fe, false);
        }
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
