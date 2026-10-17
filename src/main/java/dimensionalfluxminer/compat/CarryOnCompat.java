package dimensionalfluxminer.compat;

import dimensionalfluxminer.content.DfmStateProperties;
import dimensionalfluxminer.miner.block.MinerPartBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Carry On picks a block up with a sneaking right click and empty hands, unless the click was already cancelled. A
 * formed Miner part must not be carried away: that click is taken first (the block still does its own use, so the
 * terminal still opens), and Carry On leaves it alone. Loose parts can still be carried.
 */
public final class CarryOnCompat {
    private static Boolean loaded;

    private CarryOnCompat() {
    }

    /** Registered with the highest priority, so it runs before Carry On. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("carryon");
        }
        Player player = event.getEntity();
        if (!loaded || event.isCanceled() || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof MinerPartBlock) || !state.getValue(DfmStateProperties.FORMED)) {
            return;
        }
        InteractionResult result = state.useWithoutItem(event.getLevel(), player, event.getHitVec());
        event.setCancellationResult(result.consumesAction() ? result : InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
