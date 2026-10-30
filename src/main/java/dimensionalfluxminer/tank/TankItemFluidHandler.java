package dimensionalfluxminer.tank;

import dimensionalfluxminer.machine.transfer.Transfer;
import dimensionalfluxminer.registry.DfmDataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * A single or creative tank item as a fluid container. The single tank's liquid is its channel's
 * (the code on the item, white, white, white for a new one), so filling or draining the item changes the channel; it
 * only answers on the server thread. The creative tank's liquid is on the item and never runs out. The item itself
 * never changes, except an empty creative tank picking its liquid.
 */
public final class TankItemFluidHandler {
    private TankItemFluidHandler() {
    }

    /** The liquid of a creative tank item (full), or empty. */
    public static FluidStack creativeFluid(ItemStack stack) {
        SimpleFluidContent content = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        return content == null || content.isEmpty() ? FluidStack.EMPTY : content.copy().copyWithAmount(TankKind.CREATIVE.capacity());
    }

    /** The item reached through {@code access} (a hand, a slot): the fluid capability of single and creative tanks. */
    public static @Nullable ResourceHandler<FluidResource> forAccess(ItemAccess access, TankKind kind) {
        if (kind.creative()) {
            return new Creative(access);
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return null;
        }
        ItemStack stack = access.getResource().toStack();
        TankChannels.Channel channel = TankChannels.get(server).channel(kind, stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT));
        Transfer.Journal<FluidStack> journal = new Transfer.Journal<>(() -> channel.get(0).copy(), saved -> channel.set(0, saved));
        return Transfer.exposeFluids(new ChamberFluidHandler(() -> channel, 0, true, true), journal);
    }

    /**
     * The creative tank item: drains give as much as asked and never lower it; while empty, the first liquid poured in
     * becomes its endless liquid (the item is exchanged for one that carries it).
     */
    private record Creative(ItemAccess access) implements ResourceHandler<FluidResource> {
        private FluidStack stored() {
            return creativeFluid(access.getResource().toStack());
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.of(stored());
        }

        @Override
        public long getAmountAsLong(int index) {
            return stored().getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return TankKind.CREATIVE.capacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return true;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (index != 0 || resource.isEmpty() || amount <= 0 || !stored().isEmpty()) {
                return 0;
            }
            ItemResource filled = access.getResource().with(DfmDataComponents.CREATIVE_TANK_FLUID.get(),
                    SimpleFluidContent.copyOf(resource.toStack(1)));
            return access.exchange(filled, 1, transaction) == 1 ? amount : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 && amount > 0 && !resource.isEmpty() && resource.matches(stored()) ? amount : 0;
        }
    }
}
