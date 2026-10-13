package dimensionalfluxminer.content;

import dimensionalfluxminer.machine.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Dimensional Flux Guide: using it opens the interactive guide screen on the client. */
public class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            ClientHooks.openGuide.run();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
