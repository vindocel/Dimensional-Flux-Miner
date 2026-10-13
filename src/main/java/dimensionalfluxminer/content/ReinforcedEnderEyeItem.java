package dimensionalfluxminer.content;

import dimensionalfluxminer.advancement.DfmAdvancements;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Locates strongholds like the vanilla eye but always survives its flight.
 * It intentionally does not override {@code useOn}, so it can never be inserted into an End Portal Frame.
 */
public class ReinforcedEnderEyeItem extends Item {
    public ReinforcedEnderEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (level instanceof ServerLevel serverLevel) {
            BlockPos target = serverLevel.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, player.blockPosition(), 100, false);
            if (target != null) {
                EyeOfEnder eye = new EyeOfEnder(level, player.getX(), player.getY(0.5), player.getZ());
                eye.setItem(stack);
                eye.signalTo(target);
                eye.surviveAfterDeath = true;
                level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
                level.addFreshEntity(eye);
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.USED_ENDER_EYE.trigger(serverPlayer, target);
                    DfmAdvancements.award(serverPlayer, DfmAdvancements.REINFORCED_EYE_USED);
                }
                float pitch = Mth.lerp(level.random.nextFloat(), 0.33F, 0.5F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, pitch);
                stack.consume(1, player);
                player.awardStat(Stats.ITEM_USED.get(this));
                player.swing(hand, true);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Re-applies the survival flag after a reload, since vanilla does not persist it. */
    public static void keepAlive(EyeOfEnder eye) {
        if (eye.getItem().getItem() instanceof ReinforcedEnderEyeItem) {
            eye.surviveAfterDeath = true;
        }
    }
}
