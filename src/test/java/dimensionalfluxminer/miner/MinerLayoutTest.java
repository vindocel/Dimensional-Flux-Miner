package dimensionalfluxminer.miner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dimensionalfluxminer.miner.MinerLayout.Front;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerLayout.Slot;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MinerLayoutTest {
    @Test
    void partCountsMatchContract() {
        assertEquals(27, MinerLayout.BASE_SLOTS.size());
        assertEquals(6, MinerLayout.count(Part.STEEL_CHASSIS));
        assertEquals(5, MinerLayout.count(Part.VENTILATION_UNIT));
        assertEquals(7, MinerLayout.count(Part.REINFORCED_STRUCTURAL_GLASS));
        assertEquals(7, MinerLayout.count(Part.DIMENSIONAL_PORT));
        assertEquals(1, MinerLayout.count(Part.QUANTUM_CORE));
        assertEquals(1, MinerLayout.count(Part.MINER_TERMINAL));
        Set<String> unique = new HashSet<>();
        for (Slot s : MinerLayout.BASE_SLOTS) {
            unique.add(s.dx() + "," + s.dy() + "," + s.dz());
        }
        assertEquals(27, unique.size());
    }

    /** The seven ports surround the lower layer (every position but the terminal and the center chassis). */
    @Test
    void portsSurroundTheLowerLayer() {
        MinerLayout.BASE_SLOTS.stream().filter(s -> s.part() == Part.DIMENSIONAL_PORT).forEach(s -> {
            assertEquals(-1, s.dy());
            assertEquals(false, s.dx() == 0 && s.dz() >= 0, "never the terminal or the center");
        });
        Slot center = MinerLayout.BASE_SLOTS.stream().filter(s -> s.dx() == 0 && s.dy() == -1 && s.dz() == 0).findFirst().orElseThrow();
        assertEquals(Part.STEEL_CHASSIS, center.part());
        Slot back = MinerLayout.BASE_SLOTS.stream().filter(s -> s.dx() == 0 && s.dy() == 0 && s.dz() == -1).findFirst().orElseThrow();
        assertEquals(Part.STEEL_CHASSIS, back.part());
        MinerLayout.BASE_SLOTS.stream().filter(s -> s.dy() == 0 && s.dx() != 0 && s.dz() != 0)
                .forEach(s -> assertEquals(Part.STEEL_CHASSIS, s.part(), "steel in the middle corners"));
        MinerLayout.BASE_SLOTS.stream().filter(s -> s.part() == Part.VENTILATION_UNIT)
                .forEach(s -> assertEquals(1, s.dy(), "vents only on top"));
    }

    @Test
    void receiversMatchContract() {
        assertEquals(9, MinerLayout.BASE_SLOTS.stream().filter(Slot::redstoneReceiver).count());
        assertEquals(8, MinerLayout.BASE_SLOTS.stream().filter(Slot::accelerationReceiver).count());
        MinerLayout.BASE_SLOTS.stream().filter(Slot::accelerationReceiver).forEach(s -> {
            assertEquals(-1, s.dy());
            assertEquals(false, s.part() == Part.MINER_TERMINAL);
        });
    }

    /** Rotation of a base vector: the left side (-X) of a south-facing machine faces west, and so on clockwise. */
    @Test
    void sidesFollowTerminalFront() {
        assertArrayEquals(new int[] {-1, 0}, MinerLayout.rotate(-1, 0, Front.SOUTH));
        assertArrayEquals(new int[] {0, -1}, MinerLayout.rotate(-1, 0, Front.WEST));
        assertArrayEquals(new int[] {1, 0}, MinerLayout.rotate(-1, 0, Front.NORTH));
        assertArrayEquals(new int[] {0, 1}, MinerLayout.rotate(-1, 0, Front.EAST));
        assertArrayEquals(new int[] {0, -1}, MinerLayout.rotate(0, -1, Front.SOUTH));
        assertArrayEquals(new int[] {1, 0}, MinerLayout.rotate(0, -1, Front.WEST));
    }

    @Test
    void coreOffsetsFromTerminalMatchContract() {
        assertArrayEquals(new int[] {0, 1, -1}, MinerLayout.coreFromTerminal(Front.SOUTH));
        assertArrayEquals(new int[] {1, 1, 0}, MinerLayout.coreFromTerminal(Front.WEST));
        assertArrayEquals(new int[] {0, 1, 1}, MinerLayout.coreFromTerminal(Front.NORTH));
        assertArrayEquals(new int[] {-1, 1, 0}, MinerLayout.coreFromTerminal(Front.EAST));
    }
}
