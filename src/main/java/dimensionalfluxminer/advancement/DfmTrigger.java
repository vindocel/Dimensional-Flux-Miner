package dimensionalfluxminer.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
//? if =26.1.2 {
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
//?} else if >=26.3 {
/*import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
*///?} else {
/*import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
*///?}
import net.minecraft.server.level.ServerPlayer;
//? if >=26.3 {
/*import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
*///?}

/**
 * Criterion {@code dimensionalfluxminer:event}: met when the mod reports the named event for the player (see
 * {@link DfmAdvancements} for the event names).
 */
public class DfmTrigger extends SimpleCriterionTrigger<DfmTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String event) {
        trigger(player, instance -> instance.event().equals(event));
    }

    //? if <26.3 {
    public record Instance(Optional<ContextAwarePredicate> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
    //?} else {
    /*public record Instance(Optional<Holder<LootItemCondition>> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
    *///?}
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                //? if =26.1.2 {
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                //?} else if >=26.3 {
                /*LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                *///?} else {
                /*EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                *///?}
                Codec.STRING.fieldOf("event").forGetter(Instance::event)
        ).apply(i, Instance::new));
    }
}
