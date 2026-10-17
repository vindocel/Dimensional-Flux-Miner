package dimensionalfluxminer.miner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The 27-position assembly of the Dimensional Flux Miner, expressed as offsets from the structural quantum core
 * for the base orientation (terminal facing south). Other fronts rotate these offsets around the Y axis;
 * no mirroring and no vertical orientation exist.
 */
public final class MinerLayout {
    public enum Part {
        STEEL_CHASSIS, VENTILATION_UNIT, REINFORCED_STRUCTURAL_GLASS, DIMENSIONAL_PORT, QUANTUM_CORE, MINER_TERMINAL
    }

    public enum Front {
        SOUTH(0), WEST(1), NORTH(2), EAST(3);

        /** Clockwise quarter turns (seen from above) from the base SOUTH orientation. */
        private final int quarterTurns;

        Front(int quarterTurns) {
            this.quarterTurns = quarterTurns;
        }

        public int quarterTurns() {
            return quarterTurns;
        }
    }

    public record Slot(int dx, int dy, int dz, Part part, boolean redstoneReceiver, boolean accelerationReceiver) {
    }

    public static final List<Slot> BASE_SLOTS;

    static {
        List<Slot> s = new ArrayList<>();
        // Lower layer: 7 dimensional ports around (the nine configurable faces, see MinerPorts), a steel chassis in the
        // center and the terminal in front; all nine receive redstone, all but the terminal receive tick acceleration.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 1) {
                    s.add(new Slot(0, -1, 1, Part.MINER_TERMINAL, true, false));
                } else if (x == 0 && z == 0) {
                    s.add(new Slot(0, -1, 0, Part.STEEL_CHASSIS, true, true));
                } else {
                    s.add(new Slot(x, -1, z, Part.DIMENSIONAL_PORT, true, true));
                }
            }
        }
        // Middle layer: 4 vents in the corners, glass in front and on both sides, steel chassis at the back, core.
        s.add(new Slot(-1, 0, -1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(-1, 0, 1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(1, 0, -1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(1, 0, 1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(0, 0, 1, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        s.add(new Slot(-1, 0, 0, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        s.add(new Slot(1, 0, 0, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        s.add(new Slot(0, 0, -1, Part.STEEL_CHASSIS, false, false));
        s.add(new Slot(0, 0, 0, Part.QUANTUM_CORE, false, false));
        // Upper layer: 5 vents (corners + back middle) and 4 glass.
        s.add(new Slot(1, 1, 1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(1, 1, -1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(-1, 1, -1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(-1, 1, 1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(0, 1, -1, Part.VENTILATION_UNIT, false, false));
        s.add(new Slot(0, 1, 1, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        s.add(new Slot(0, 1, 0, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        s.add(new Slot(-1, 1, 0, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        s.add(new Slot(1, 1, 0, Part.REINFORCED_STRUCTURAL_GLASS, false, false));
        BASE_SLOTS = Collections.unmodifiableList(s);
    }

    private MinerLayout() {
    }

    /** Rotates a horizontal base vector clockwise (seen from above) by the front's quarter turns. */
    public static int[] rotate(int x, int z, Front front) {
        int rx = x;
        int rz = z;
        for (int i = 0; i < front.quarterTurns(); i++) {
            int t = rx;
            rx = -rz;
            rz = t;
        }
        return new int[] {rx, rz};
    }

    /** Offset from the core of a slot for the given front. */
    public static int[] offsetFromCore(Slot slot, Front front) {
        int[] r = rotate(slot.dx(), slot.dz(), front);
        return new int[] {r[0], slot.dy(), r[1]};
    }

    /** Offset of the core relative to the terminal for the given front. */
    public static int[] coreFromTerminal(Front front) {
        int[] r = rotate(0, -1, front);
        return new int[] {r[0], 1, r[1]};
    }

    public static long count(Part part) {
        return BASE_SLOTS.stream().filter(s -> s.part() == part).count();
    }
}
