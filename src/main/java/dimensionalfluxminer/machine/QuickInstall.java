package dimensionalfluxminer.machine;

import dimensionalfluxminer.machine.transfer.SlotStorage;
import dimensionalfluxminer.miner.MinerControllerBlockEntity;
import dimensionalfluxminer.miner.MinerLinkBlockEntity;
import dimensionalfluxminer.miner.block.MinerPartBlock;
import dimensionalfluxminer.registry.DfmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Sneak + right click with an Efficiency Upgrade on a machine (or any block of a formed Miner) installs it directly in
 * the upgrade slot; with a Dimensional Core on the Miner, in the core slot. Vanilla does not call the block when the
 * player sneaks with an item, so this listens to the right click event instead.
 */
public final class QuickInstall {
    private QuickInstall() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!player.isSecondaryUseActive() || !installable(stack)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        boolean dfmBlock = level.getBlockState(pos).getBlock() instanceof MinerPartBlock
                || (level.getBlockEntity(pos) instanceof AbstractMachineBlockEntity && stack.is(DfmItems.EFFICIENCY_UPGRADE.get()));
        if (!dfmBlock) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!level.isClientSide()) {
            Component message = install(player, level, pos, stack);
            if (message != null) {
                player.sendOverlayMessage(message);
            }
        }
    }

    private static boolean installable(ItemStack stack) {
        return stack.is(DfmItems.EFFICIENCY_UPGRADE.get()) || DfmItems.isCore(stack.getItem());
    }

    /**
     * Installs one item of {@code stack} in the slot of the machine at {@code pos} (taken from the stack unless the
     * player is in Creative). Returns the message for the player, or null when the block takes no such item.
     */
    public static Component install(Player player, Level level, BlockPos pos, ItemStack stack) {
        boolean upgrade = stack.is(DfmItems.EFFICIENCY_UPGRADE.get());
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MinerLinkBlockEntity link) {
            be = link.controller();
            if (be == null) {
                return Component.translatable("message.dimensionalfluxminer.quick_install.not_formed");
            }
        }
        SlotStorage slot;
        if (be instanceof MinerControllerBlockEntity miner) {
            slot = upgrade ? miner.upgradeSlot() : miner.coreSlot();
        } else if (be instanceof AbstractMachineBlockEntity machine && upgrade) {
            slot = machine.upgradeSlot();
        } else {
            return null;
        }
        ItemStack installed = slot.getStackInSlot(0);
        if (!installed.isEmpty()) {
            return Component.translatable("message.dimensionalfluxminer.quick_install.occupied", installed.getHoverName());
        }
        if (!slot.insertItem(0, stack.copyWithCount(1), false).isEmpty()) {
            return null;
        }
        Component name = stack.getHoverName();
        stack.consume(1, player);
        be.setChanged();
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        return Component.translatable("message.dimensionalfluxminer.quick_install.installed", name);
    }
}
