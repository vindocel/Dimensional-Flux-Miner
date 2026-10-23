package dimensionalfluxminer.miner.corruption;

import net.minecraft.world.entity.EntityTypes;
import dimensionalfluxminer.advancement.DfmAdvancements;
import dimensionalfluxminer.registry.DfmFluids;
import dimensionalfluxminer.registry.DfmTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Corrupted Water is hostile to players and passive mobs (2 HP every 40 ticks, Creative/Spectator immune) and heals eligible
 * MONSTER mobs (+1 HP every 20 ticks, up to max health, except the heal blacklist); mobs of the heart-heal tag
 * (the Warden) heal one heart (2 HP) every 10 ticks instead.
 */
public final class CorruptedWaterEffects {
    public static final int PLAYER_DAMAGE_INTERVAL = 40;
    public static final float PLAYER_DAMAGE = 2.0F;
    public static final int MONSTER_HEAL_INTERVAL = 20;
    public static final float MONSTER_HEAL = 1.0F;
    /** Mobs tagged {@code corrupted_water_heart_heal} (the Warden): one heart every 10 ticks. */
    public static final int HEART_HEAL_INTERVAL = 10;
    public static final float HEART_HEAL = 2.0F;
    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, dimensionalfluxminer.DimensionalFluxMiner.id("corrupted_water"));

    private CorruptedWaterEffects() {
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (!living.isInFluidType(DfmFluids.CORRUPTED_WATER_TYPE.get())) {
            return;
        }
        long time = living.level().getGameTime();
        if (living instanceof Player player) {
            if (!player.isCreative() && !player.isSpectator() && time % PLAYER_DAMAGE_INTERVAL == 0) {
                player.hurt(damageSource(living), PLAYER_DAMAGE);
            }
            return;
        }
        if (living.getType().getCategory() != MobCategory.MONSTER) {
            // Passive mobs (animals, fish, villagers, golems) take the same damage as players.
            if (living instanceof Mob && time % PLAYER_DAMAGE_INTERVAL == 0) {
                living.hurt(damageSource(living), PLAYER_DAMAGE);
            }
            return;
        }
        if (living.getType().builtInRegistryHolder().is(DfmTags.Entities.CORRUPTED_WATER_HEAL_BLACKLIST)
                || living.getHealth() >= living.getMaxHealth()) {
            return;
        }
        if (living.getType().builtInRegistryHolder().is(DfmTags.Entities.CORRUPTED_WATER_HEART_HEAL)) {
            if (time % HEART_HEAL_INTERVAL == 0) {
                living.heal(HEART_HEAL);
                if (living.level() instanceof ServerLevel level && living.getType() == EntityTypes.WARDEN) {
                    // Whoever watches a Warden being healed by the blight earns it a new best friend.
                    DfmAdvancements.awardNearby(level, living.position(), DfmAdvancements.NEARBY, DfmAdvancements.WARDEN_HEALED,
                            player -> player.hasLineOfSight(living));
                }
            }
        } else if (time % MONSTER_HEAL_INTERVAL == 0) {
            living.heal(MONSTER_HEAL);
        }
    }

    private static DamageSource damageSource(LivingEntity living) {
        return new DamageSource(living.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE_TYPE));
    }
}
