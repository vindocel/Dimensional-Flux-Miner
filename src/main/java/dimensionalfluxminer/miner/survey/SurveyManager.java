package dimensionalfluxminer.miner.survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Real-tick survey scheduler: at most 256 columns per Miner and 2 048 columns per dimension per server tick,
 * shared fairly (round-robin) among the Basic Miners that asked for survey work this tick. External tick
 * acceleration never adds survey budget because it is only spent here, once per real tick.
 */
public final class SurveyManager {
    public static final int PER_MINER_BUDGET = 256;
    public static final int PER_LEVEL_BUDGET = 2048;

    private static final Map<ResourceKey<Level>, Map<BlockPos, MinerSurvey>> SURVEYS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new HashMap<>();
    private static final Map<ResourceKey<Level>, Integer> ROUND_ROBIN = new HashMap<>();

    private SurveyManager() {
    }

    /** Survey of the Miner whose core is at {@code core}; reused while the machine stays at that position. */
    public static MinerSurvey survey(ServerLevel level, BlockPos core) {
        return SURVEYS.computeIfAbsent(level.dimension(), k -> new HashMap<>()).computeIfAbsent(core.immutable(), MinerSurvey::new);
    }

    /** Requests survey work for this real tick (called by an armed, formed Basic Miner). */
    public static void request(ServerLevel level, BlockPos core) {
        ACTIVE.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(core.immutable());
    }

    public static void forget(ServerLevel level, BlockPos core) {
        Map<BlockPos, MinerSurvey> map = SURVEYS.get(level.dimension());
        if (map != null) {
            map.remove(core);
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Set<BlockPos> active = ACTIVE.get(level.dimension());
        if (active == null || active.isEmpty()) {
            return;
        }
        List<BlockPos> miners = new ArrayList<>(active);
        active.clear();
        int start = Math.floorMod(ROUND_ROBIN.getOrDefault(level.dimension(), 0), miners.size());
        ROUND_ROBIN.put(level.dimension(), start + 1);
        int remaining = PER_LEVEL_BUDGET;
        for (int i = 0; i < miners.size() && remaining > 0; i++) {
            BlockPos core = miners.get((start + i) % miners.size());
            int budget = Math.min(PER_MINER_BUDGET, remaining);
            remaining -= survey(level, core).step(level, budget);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SURVEYS.clear();
        ACTIVE.clear();
        ROUND_ROBIN.clear();
    }
}
