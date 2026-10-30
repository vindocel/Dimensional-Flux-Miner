package dimensionalfluxminer.tank;

import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidUtil;

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
                && FluidUtil.getFluidHandler(event.getItemStack()).isPresent()
                // Sneaking with a tank item places it (against the tank) instead.
                && !(event.getItemStack().getItem() instanceof DimensionalTankItem)) {
            event.setUseBlock(TriState.TRUE);
        }
    }
}
