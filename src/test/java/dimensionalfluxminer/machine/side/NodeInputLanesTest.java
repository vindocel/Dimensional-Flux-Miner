package dimensionalfluxminer.machine.side;

import static dimensionalfluxminer.machine.side.SideRoute.ITEM_INPUT_1;
import static dimensionalfluxminer.machine.side.SideRoute.ITEM_INPUT_2;
import static dimensionalfluxminer.machine.side.SideRoute.ITEM_INPUT_3;
import static dimensionalfluxminer.machine.side.SideRoute.ITEM_INPUT_ALL;
import static dimensionalfluxminer.machine.side.SideRoute.NONE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class NodeInputLanesTest {
    private static final int A = 1;
    private static final int B = 2;
    private static final int C = 3;
    private static final int D = 4;

    private static SideRoute[] empty() {
        SideRoute[] routes = new SideRoute[6];
        Arrays.fill(routes, NONE);
        return routes;
    }

    private static int mask(int... faces) {
        int mask = 0;
        for (int face : faces) {
            mask |= 1 << face;
        }
        return mask;
    }

    @Test
    void theFirstFaceFeedsEveryLane() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        lanes.update(routes, mask(A));
        assertEquals(ITEM_INPUT_ALL, routes[A]);
    }

    @Test
    void clicksCycleAllThenEachLane() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        lanes.update(routes, mask(A));
        SideRoute[] expected = {ITEM_INPUT_1, ITEM_INPUT_2, ITEM_INPUT_3, ITEM_INPUT_ALL};
        for (SideRoute route : expected) {
            lanes.cycle(routes, A, true);
            assertEquals(route, routes[A]);
        }
        lanes.cycle(routes, A, false);
        assertEquals(ITEM_INPUT_3, routes[A], "right click goes back");
    }

    @Test
    void aSecondFaceSplitsTheLanes() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        lanes.update(routes, mask(A));
        assertEquals(ITEM_INPUT_ALL, routes[A]);
        lanes.update(routes, mask(A, B));
        assertEquals(ITEM_INPUT_1, routes[A], "the first face leaves all");
        assertEquals(ITEM_INPUT_2, routes[B]);

        lanes.update(routes, mask(A, B, C));
        assertEquals(ITEM_INPUT_3, routes[C]);
        lanes.update(routes, mask(A, B, C, D));
        assertEquals(ITEM_INPUT_ALL, routes[D], "no lane left");
    }

    @Test
    void takingAnotherFacesLaneSwapsThem() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        lanes.update(routes, mask(A));
        lanes.cycle(routes, A, true);
        lanes.update(routes, mask(A, B));
        assertEquals(ITEM_INPUT_2, routes[B]);
        lanes.cycle(routes, A, true);
        assertEquals(ITEM_INPUT_2, routes[A]);
        assertEquals(ITEM_INPUT_1, routes[B]);
    }

    @Test
    void addingAFaceSplitsAFaceSetToAllByClick() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        lanes.update(routes, mask(A, B));
        assertEquals(ITEM_INPUT_1, routes[A]);
        assertEquals(ITEM_INPUT_2, routes[B]);
        lanes.cycle(routes, B, false);
        assertEquals(ITEM_INPUT_1, routes[B], "B takes lane 1 back from A");
        assertEquals(ITEM_INPUT_2, routes[A]);
        lanes.cycle(routes, B, false);
        assertEquals(ITEM_INPUT_ALL, routes[B], "a click may still pick all");
        lanes.cycle(routes, B, true);
        assertEquals(ITEM_INPUT_1, routes[B]);
        lanes.cycle(routes, B, false);
        lanes.update(routes, mask(A, B, C));
        assertEquals(ITEM_INPUT_2, routes[A]);
        assertEquals(ITEM_INPUT_1, routes[B], "a new face takes B off all");
        assertEquals(ITEM_INPUT_3, routes[C]);
    }

    @Test
    void removingAFaceClosesTheGap() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        routes[A] = ITEM_INPUT_1;
        routes[B] = ITEM_INPUT_2;
        routes[C] = ITEM_INPUT_3;
        lanes.update(routes, mask(A, B, C));
        lanes.update(routes, mask(A, C));
        assertEquals(ITEM_INPUT_1, routes[A]);
        assertEquals(ITEM_INPUT_2, routes[C], "lane 3 moves to the freed lane 2");
        assertEquals(mask(A, C), lanes.mask());
    }

    @Test
    void lanesSetByHandAreKept() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        routes[A] = ITEM_INPUT_3;
        routes[B] = ITEM_INPUT_3;
        lanes.update(routes, mask(A, B));
        assertEquals(ITEM_INPUT_3, routes[A]);
        assertEquals(ITEM_INPUT_1, routes[B], "the repeated lane moves to the first free one");
    }

    @Test
    void reloadedFacesKeepTheirRoutes() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        routes[A] = ITEM_INPUT_ALL;
        routes[B] = ITEM_INPUT_1;
        routes[C] = ITEM_INPUT_3;
        SideRoute[] saved = routes.clone();
        assertFalse(lanes.update(routes, mask(A, B, C)));
        assertArrayEquals(saved, routes);
    }

    @Test
    void reloadedFacesNeverShareAll() {
        NodeInputLanes lanes = new NodeInputLanes();
        SideRoute[] routes = empty();
        routes[A] = ITEM_INPUT_ALL;
        routes[B] = ITEM_INPUT_ALL;
        lanes.update(routes, mask(A, B));
        assertEquals(ITEM_INPUT_1, routes[A]);
        assertEquals(ITEM_INPUT_2, routes[B]);
    }
}
