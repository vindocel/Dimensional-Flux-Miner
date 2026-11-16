package dimensionalfluxminer.content;

import dimensionalfluxminer.machine.ClientHooks;
import net.minecraft.world.InteractionHand;
//? if >=26.1 {
import net.minecraft.world.InteractionResult;
//?} else {
/*import net.minecraft.world.InteractionResultHolder;
*///?}
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
    //? if >=26.1 {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
    //?} else {
    /*public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
    *///?}
            ClientHooks.openGuide.run();
        }
        //? if >=26.1 {
        return InteractionResult.SUCCESS;
        //?} else {
        /*return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        *///?}
    }
}
