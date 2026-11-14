package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmDataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Server-owned channel readings for item visuals. Snapshots never supply fluid to a handler or placed tank. */
public final class TankItemDisplay {
    private TankItemDisplay() { }

    public static FluidStack fluid(ItemStack stack, TankKind kind, int chamber) {
        if (kind.creative()) return TankItemFluidHandler.creativeFluid(stack);
        boolean local = DimensionalTankItem.channelLocked(stack);
        var component = local ? (chamber == 0 ? DfmDataComponents.TANK_LOCAL_A : DfmDataComponents.TANK_LOCAL_B)
                : (chamber == 0 ? DfmDataComponents.TANK_DISPLAY_A : DfmDataComponents.TANK_DISPLAY_B);
        return stack.getOrDefault(component.get(), SimpleFluidContent.EMPTY).copy();
    }

    /** Refresh only changed readings, letting normal inventory synchronization deliver them to the client. */
    public static void update(ItemStack stack, MinecraftServer server) {
        if (!(stack.getItem() instanceof DimensionalTankItem item) || item.kind().creative()) return;
        if (DimensionalTankItem.channelLocked(stack)) {
            stack.remove(DfmDataComponents.TANK_DISPLAY_A.get());
            stack.remove(DfmDataComponents.TANK_DISPLAY_B.get());
            return;
        }
        TankChannels.Channel channel = TankChannels.get(server).channel(item.kind(),
                stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT));
        for (int i = 0; i < item.kind().chambers(); i++) {
            var component = i == 0 ? DfmDataComponents.TANK_DISPLAY_A.get() : DfmDataComponents.TANK_DISPLAY_B.get();
            SimpleFluidContent next = SimpleFluidContent.copyOf(channel.get(i));
            if (!next.equals(stack.getOrDefault(component, SimpleFluidContent.EMPTY))) {
                if (next.isEmpty()) stack.remove(component);
                else stack.set(component, next);
            }
        }
    }

    /** Includes backpack slots, offhand, the cursor and any open container, even without a placed endpoint. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var server = player.level().getServer();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) update(player.getInventory().getItem(i), server);
        if (player.containerMenu != player.inventoryMenu) {
            for (var slot : player.containerMenu.slots) update(slot.getItem(), server);
        }
        update(player.containerMenu.getCarried(), server);
    }
}
