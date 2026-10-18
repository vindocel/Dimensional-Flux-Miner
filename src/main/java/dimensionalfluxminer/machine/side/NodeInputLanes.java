package dimensionalfluxminer.machine.side;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Item input faces that network nodes set on a machine with dedicated input lanes (the Smelter in the Alloy profile).
 * Works on the ITEM routes of one profile, indexed by {@link RelativeFace} ordinal.
 * <ul>
 * <li>A face keeps the lane it already has (set by hand before the node) when no other node face uses it.</li>
 * <li>A lone face feeds every lane ({@link SideRoute#ITEM_INPUT_ALL}). A second face means single lanes: whenever a
 * face is added, every node face on "all" takes the next free lane (1, 2, 3) in arrival order; with the three
 * lanes taken, the rest feed every lane.</li>
 * <li>A click cycles a face through all, lane 1, lane 2, lane 3 and all again. Taking the lane of another face gives
 * that face the old lane; leaving "all" moves the other faces on "all" to the free lanes in arrival order.</li>
 * <li>Removing a face frees its lane, and the remaining individual faces close the gap in lane order.</li>
 * </ul>
 * The arrival order lives only in memory: after a load the faces come back in lane order.
 */
public final class NodeInputLanes {
    private static final SideRoute[] CYCLE = {SideRoute.ITEM_INPUT_ALL, SideRoute.ITEM_INPUT_1, SideRoute.ITEM_INPUT_2,
            SideRoute.ITEM_INPUT_3};
    private static final int LANES = CYCLE.length - 1;

    /** Node input faces in the order they appeared. */
    private final List<Integer> order = new ArrayList<>();

    /** Faces (bit per face ordinal) currently handled here. */
    public int mask() {
        int mask = 0;
        for (int face : order) {
            mask |= 1 << face;
        }
        return mask;
    }

    public boolean contains(int face) {
        return order.contains(face);
    }

    /** Forgets every face (the item profile changed); the next update restores them from their routes. */
    public void reset() {
        order.clear();
    }

    /**
     * Applies the node input faces found by a scan ({@code mask}, bit per face) to {@code routes}; returns whether a
     * route changed.
     */
    public boolean update(SideRoute[] routes, int mask) {
        SideRoute[] before = routes.clone();
        boolean restore = order.isEmpty();
        boolean removed = order.removeIf(face -> (mask & (1 << face)) == 0);
        List<Integer> added = new ArrayList<>();
        for (int face = 0; face < routes.length; face++) {
            if ((mask & (1 << face)) != 0 && !order.contains(face)) {
                added.add(face);
            }
        }
        // Faces already on a lane go first so they keep it.
        added.sort(Comparator.comparingInt(face -> routes[face].dedicatedLane() >= 0 ? routes[face].dedicatedLane() : LANES + face));
        // A reloaded face on "all" keeps it, unless more than one is (older saves); any other face waits for a lane.
        long onAll = added.stream().filter(face -> routes[face] == SideRoute.ITEM_INPUT_ALL).count();
        List<Integer> pending = new ArrayList<>();
        for (int face : added) {
            SideRoute own = routes[face];
            int lane = own.dedicatedLane();
            if (lane < 0 || laneUsed(routes, lane)) {
                routes[face] = SideRoute.ITEM_INPUT_ALL;
                if (!restore || own != SideRoute.ITEM_INPUT_ALL || onAll > 1) {
                    pending.add(face);
                }
            }
            order.add(face);
        }
        if (!added.isEmpty() && order.size() > 1) {
            // A second node input means single lanes: the faces on "all" take the free lanes in arrival order.
            for (int face : order) {
                if (routes[face].dedicatedLane() < 0 && (!restore || pending.contains(face))) {
                    routes[face] = freeLane(routes);
                }
            }
        }
        if (removed) {
            compact(routes);
        }
        return !Arrays.equals(before, routes);
    }

    /** Next ({@code forward}) or previous route of the cycle all -> 1 -> 2 -> 3 for a node face; false if not handled. */
    public boolean cycle(SideRoute[] routes, int face, boolean forward) {
        if (!order.contains(face)) {
            return false;
        }
        SideRoute previous = routes[face];
        boolean wasLane = previous.dedicatedLane() >= 0;
        int index = wasLane ? previous.dedicatedLane() + 1 : 0;
        SideRoute next = CYCLE[Math.floorMod(index + (forward ? 1 : -1), CYCLE.length)];
        routes[face] = next;
        int lane = next.dedicatedLane();
        if (lane >= 0) {
            for (int other : order) {
                if (other != face && routes[other].dedicatedLane() == lane) {
                    routes[other] = wasLane ? previous : freeLane(routes);
                }
            }
            if (!wasLane) {
                // The face left "all": the faces that were waiting on "all" take the free lanes in order.
                for (int other : order) {
                    if (other != face && routes[other].dedicatedLane() < 0) {
                        routes[other] = freeLane(routes);
                    }
                }
            }
        }
        return true;
    }

    /** Removed faces leave gaps: the individual faces take lanes 1, 2, 3 again, keeping their lane order. */
    private void compact(SideRoute[] routes) {
        List<Integer> individual = new ArrayList<>();
        for (int face : order) {
            if (routes[face].dedicatedLane() >= 0) {
                individual.add(face);
            }
        }
        individual.sort(Comparator.comparingInt(face -> routes[face].dedicatedLane()));
        for (int i = 0; i < individual.size(); i++) {
            routes[individual.get(i)] = CYCLE[i + 1];
        }
    }

    private boolean laneUsed(SideRoute[] routes, int lane) {
        for (int face : order) {
            if (routes[face].dedicatedLane() == lane) {
                return true;
            }
        }
        return false;
    }

    /** First lane no node face uses, or "all" when the three are taken. */
    private SideRoute freeLane(SideRoute[] routes) {
        for (int lane = 0; lane < LANES; lane++) {
            if (!laneUsed(routes, lane)) {
                return CYCLE[lane + 1];
            }
        }
        return SideRoute.ITEM_INPUT_ALL;
    }
}
