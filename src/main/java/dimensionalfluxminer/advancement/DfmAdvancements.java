package dimensionalfluxminer.advancement;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Advancement events reported by the machines. Machine events go to the Miner's owner when online, otherwise to
 * the players near the machine; the Crusher and the Electric Furnace (which have no owner) report to the players near them.
 */
public final class DfmAdvancements {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, DimensionalFluxMiner.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, DfmTrigger> EVENT = TRIGGERS.register("event", DfmTrigger::new);

    public static final String MINER_FORMED = "miner_formed";
    public static final String FIRST_ORE = "first_ore";
    public static final String COLUMN_DRAINED = "column_drained";
    public static final String MATTER_INJECTED = "matter_injected";
    public static final String TRASH_TO_TREASURE = "trash_to_treasure";
    public static final String ORE_FILTERED = "ore_filtered";
    public static final String MAX_SPEED = "max_speed";
    public static final String THOUSAND_PER_SECOND = "thousand_per_second";
    public static final String WARDEN_HEALED = "warden_healed";
    public static final String WORLD_DRAINED = "world_drained";
    public static final String SLAG_RECOVERED = "slag_recovered";
    /** A Reinforced Eye of Ender flew towards a stronghold (the advancement also needs the player inside one). */
    public static final String REINFORCED_EYE_USED = "reinforced_eye_used";

    /** Players this close to a machine see what it does. */
    public static final double NEARBY = 32.0;
    /** A frequent machine event is reported at most once per this many game ticks. */
    private static final long REPEAT_TICKS = 100;

    private DfmAdvancements() {
    }

    public static void award(ServerPlayer player, String event) {
        if (player != null) {
            EVENT.get().trigger(player, event);
        }
    }

    /** Every player in the level within {@code radius} of {@code center} that also passes {@code filter}. */
    public static void awardNearby(ServerLevel level, Vec3 center, double radius, String event, Predicate<ServerPlayer> filter) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(center) <= radius * radius && filter.test(player)) {
                award(player, event);
            }
        }
    }

    public static void awardNearby(ServerLevel level, BlockPos pos, String event) {
        awardNearby(level, Vec3.atCenterOf(pos), NEARBY, event, p -> true);
    }

    /** The owner when online (wherever they are), otherwise the players near the machine. */
    public static void awardOwner(ServerLevel level, UUID owner, BlockPos pos, String event) {
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            award(player, event);
        } else {
            awardNearby(level, pos, event);
        }
    }

    /** Rate limiter for events a machine can report every tick. */
    public static final class Throttle {
        private final Map<String, Long> last = new HashMap<>();

        /** True when the event was not reported in the last {@link #REPEAT_TICKS} ticks (and records it). */
        public boolean ready(String event, long gameTime) {
            Long previous = last.get(event);
            if (previous != null && gameTime - previous < REPEAT_TICKS && gameTime >= previous) {
                return false;
            }
            last.put(event, gameTime);
            return true;
        }
    }
}
