package dimensionalfluxminer.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

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

    public record Instance(Optional<ContextAwarePredicate> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event)
        ).apply(i, Instance::new));
    }
}
