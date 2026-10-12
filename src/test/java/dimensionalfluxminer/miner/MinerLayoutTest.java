package dimensionalfluxminer.miner;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dimensionalfluxminer.miner.MinerLayout.Front;
import dimensionalfluxminer.miner.MinerLayout.Part;
import dimensionalfluxminer.miner.MinerLayout.PortRole;
import dimensionalfluxminer.miner.MinerLayout.Slot;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MinerLayoutTest {
    @Test
    void partCountsMatchContract() {
        assertEquals(27, MinerLayout.BASE_SLOTS.size());
        assertEquals(8, MinerLayout.count(Part.STEEL_CHASSIS));
        assertEquals(9, MinerLayout.count(Part.VENTILATION_UNIT));
        assertEquals(5, MinerLayout.count(Part.REINFORCED_STRUCTURAL_GLASS));
        assertEquals(3, MinerLayout.count(Part.DIMENSIONAL_PORT));
        assertEquals(1, MinerLayout.count(Part.QUANTUM_CORE));
        assertEquals(1, MinerLayout.count(Part.MINER_TERMINAL));
        Set<String> unique = new HashSet<>();
        for (Slot s : MinerLayout.BASE_SLOTS) {
            unique.add(s.dx() + "," + s.dy() + "," + s.dz());
        }
        assertEquals(27, unique.size());
    }

    @Test
    void receiversMatchContract() {
        assertEquals(9, MinerLayout.BASE_SLOTS.stream().filter(Slot::redstoneReceiver).count());
        assertEquals(8, MinerLayout.BASE_SLOTS.stream().filter(Slot::accelerationReceiver).count());
        MinerLayout.BASE_SLOTS.stream().filter(Slot::accelerationReceiver).forEach(s -> {
            assertEquals(Part.STEEL_CHASSIS, s.part());
            assertEquals(-1, s.dy());
        });
    }

    /** Spec table: terminal front -> energy / output / matter outward faces. */
    @Test
    void portFacesFollowTerminalFront() {
        expectPorts(Front.SOUTH, "W", "E", "N");
        expectPorts(Front.WEST, "N", "S", "E");
        expectPorts(Front.NORTH, "E", "W", "S");
        expectPorts(Front.EAST, "S", "N", "W");
    }

    @Test
    void coreOffsetsFromTerminalMatchContract() {
        assertArrayEquals(new int[] {0, 1, -1}, MinerLayout.coreFromTerminal(Front.SOUTH));
        assertArrayEquals(new int[] {1, 1, 0}, MinerLayout.coreFromTerminal(Front.WEST));
        assertArrayEquals(new int[] {0, 1, 1}, MinerLayout.coreFromTerminal(Front.NORTH));
        assertArrayEquals(new int[] {-1, 1, 0}, MinerLayout.coreFromTerminal(Front.EAST));
    }

    private static void expectPorts(Front front, String energy, String output, String matter) {
        for (Slot s : MinerLayout.BASE_SLOTS) {
            if (s.role() == null) {
                continue;
            }
            int[] out = MinerLayout.rotate(s.outX(), s.outZ(), front);
            String dir = out[0] == 1 ? "E" : out[0] == -1 ? "W" : out[1] == 1 ? "S" : "N";
            String expected = s.role() == PortRole.ENERGY_INPUT ? energy : s.role() == PortRole.ITEM_OUTPUT ? output : matter;
            assertEquals(expected, dir, front + " " + s.role());
            int[] offset = MinerLayout.offsetFromCore(s, front);
            assertEquals(out[0], offset[0], "port sits on its outward side");
            assertEquals(out[1], offset[2], "port sits on its outward side");
        }
    }
}
