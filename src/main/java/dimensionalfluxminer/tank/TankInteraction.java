package dimensionalfluxminer.tank;

import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;

/**
 * Buckets and fluid containers also work on a tank while sneaking. Vanilla does not call the block when the player
 * sneaks with an item, so the click is let through to the block here.
 */
public final class TankInteraction {
    private TankInteraction() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSecondaryUseActive()
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof DimensionalTankBlock
                && !event.getItemStack().isEmpty() && ItemAccess.forStack(event.getItemStack()).getCapability(Capabilities.Fluid.ITEM) != null) {
            event.setUseBlock(TriState.TRUE);
        }
    }
}
