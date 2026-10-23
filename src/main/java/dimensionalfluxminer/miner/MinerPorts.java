package dimensionalfluxminer.miner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * The nine configurable faces of the Miner's lower layer and their configuration: the outer faces of the seven
 * dimensional ports (three on each side, three at the back; the two back corners carry two faces each). Faces are
 * defined for the base orientation (terminal facing south) and rotate with the machine like {@link MinerLayout}.
 * <ul>
 *     <li>Item Output: at most one face.</li>
 *     <li>Matter Input: at most one face, Stabilized Core only; it may share the Item Output face.</li>
 *     <li>Energy: by default every face accepts energy (a port is drawn only where something with energy touches the
 *     face); once the player picks energy faces by hand, only those accept it and they are always drawn. Clearing them
 *     returns to the default.</li>
 * </ul>
 */
public final class MinerPorts {
    /**
     * One face: the port block that carries it (offset from the core, base orientation) and its outward direction.
     * "West"/"east" are the sides on the left/right of someone facing the terminal; "rear" is the back.
     */
    public enum Face {
        WEST_REAR(-1, -1, -1, 0),
        WEST_CENTER(-1, -1, 0, 0),
        WEST_FRONT(-1, -1, 1, 0),
        EAST_REAR(1, -1, -1, 1),
        EAST_CENTER(1, -1, 0, 1),
        EAST_FRONT(1, -1, 1, 1),
        REAR_WEST(-1, -1, -1, 2),
        REAR_CENTER(0, -1, -1, 2),
        REAR_EAST(1, -1, -1, 2);

        public static final Face[] VALUES = values();

        private final int dx;
        private final int dy;
        private final int dz;
        /** 0 = west (-X), 1 = east (+X), 2 = rear (-Z), in the base orientation. */
        private final int side;

        Face(int dx, int dy, int dz, int side) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.side = side;
        }

        public int bit() {
            return 1 << ordinal();
        }

        /** Offset of the owner block from the core for a machine whose terminal faces {@code front}. */
        public BlockPos owner(BlockPos core, MinerLayout.Front front) {
            int[] r = MinerLayout.rotate(dx, dz, front);
            return core.offset(r[0], dy, r[1]);
        }

        /** Outward direction of the face for a machine whose terminal faces {@code front}. */
        public Direction outward(MinerLayout.Front front) {
            int[] r = switch (side) {
                case 0 -> MinerLayout.rotate(-1, 0, front);
                case 1 -> MinerLayout.rotate(1, 0, front);
                default -> MinerLayout.rotate(0, -1, front);
            };
            return Direction.getNearest(r[0], 0, r[1], null);
        }

        /** Base-orientation offset of the owner block from the core (x, y, z). */
        public int[] baseOffset() {
            return new int[] {dx, dy, dz};
        }

        /** Base-orientation outward vector (x, z). */
        public int[] baseOutward() {
            return switch (side) {
                case 0 -> new int[] {-1, 0};
                case 1 -> new int[] {1, 0};
                default -> new int[] {0, -1};
            };
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** What a face shows, as drawn on the casing and synced to clients. */
    public enum Look {
        NONE, ENERGY, OUTPUT, MATTER, OUTPUT_MATTER;

        public static final Look[] VALUES = values();
    }

    private int outputFace = -1;
    private int matterFace = -1;
    /** Faces chosen by hand for energy; 0 = every face accepts energy (the default). */
    private int energyMask;
    /**
     * Roles set by an external network node attached to a port (Logistics Networks): its face is locked in the Side
     * Configuration and that role cannot go to another face while the node is there. Not saved: read again from the
     * nodes.
     */
    private int nodeOutput = -1;
    private int nodeMatter = -1;
    private int nodeEnergy;

    public int outputFace() {
        return outputFace;
    }

    public int matterFace() {
        return matterFace;
    }

    public int energyMask() {
        return energyMask;
    }

    public boolean energyManual() {
        return energyMask != 0;
    }

    public boolean acceptsEnergy(Face face) {
        return energyMask == 0 || ((energyMask | nodeEnergy) & face.bit()) != 0;
    }

    /** Faces whose roles a network node sets (locked in the Side Configuration). */
    public int nodeFaces() {
        return nodeItemFaces() | nodeEnergy;
    }

    public int nodeItemFaces() {
        return (nodeOutput >= 0 ? 1 << nodeOutput : 0) | (nodeMatter >= 0 ? 1 << nodeMatter : 0);
    }

    public int nodeEnergyFaces() {
        return nodeEnergy;
    }

    public boolean nodeLocked(Face face) {
        return (nodeFaces() & face.bit()) != 0;
    }

    /**
     * Roles asked by the network nodes on the ports (-1 / 0 = none). The roles move to those faces; returns true when
     * anything changed.
     */
    public boolean applyNodes(int output, int matter, int energy) {
        boolean changed = output != nodeOutput || matter != nodeMatter || energy != nodeEnergy;
        nodeOutput = output;
        nodeMatter = matter;
        nodeEnergy = energy;
        if (output >= 0 && outputFace != output) {
            outputFace = output;
            changed = true;
        }
        if (matter >= 0 && matterFace != matter) {
            matterFace = matter;
            changed = true;
        }
        if (changed) {
            cycleOrder = null;
        }
        return changed;
    }

    public boolean isOutput(Face face) {
        return outputFace == face.ordinal();
    }

    public boolean isMatter(Face face) {
        return matterFace == face.ordinal();
    }

    /** Face whose clicks follow {@link #cycleOrder} (not saved: a new series of clicks starts a new order). */
    private int cycleFace = -1;
    private int[] cycleOrder;
    private boolean cycleMatter;

    /**
     * Next item role of a face (right click goes back). Without the Stabilized Core only none and Item Output exist. The
     * roles still free on other faces come first, then both together, then the roles taken from another face: with the
     * Item Output already on another face, a face cycles none, Matter Input, both, Item Output. The order is kept while
     * the player keeps clicking the same face. Taking a role keeps every role on one face.
     */
    public void cycleItems(Face face, boolean matterAllowed, boolean forward) {
        if ((nodeItemFaces() & face.bit()) != 0) {
            return;
        }
        int state = (isOutput(face) ? 1 : 0) | (isMatter(face) ? 2 : 0);
        if (cycleFace != face.ordinal() || cycleMatter != matterAllowed || cycleOrder == null || !contains(cycleOrder, state)) {
            cycleFace = face.ordinal();
            cycleMatter = matterAllowed;
            cycleOrder = order(face, matterAllowed);
        }
        int[] order = cycleOrder;
        int index = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == state) {
                index = i;
            }
        }
        int next = order[Math.floorMod(index + (forward ? 1 : -1), order.length)];
        if ((next & 1) != 0) {
            outputFace = face.ordinal();
        } else if (isOutput(face)) {
            outputFace = -1;
        }
        if ((next & 2) != 0) {
            matterFace = face.ordinal();
        } else if (isMatter(face)) {
            matterFace = -1;
        }
    }

    /** Click order of a face: none, the free roles, both, then the roles taken from another face. */
    private int[] order(Face face, boolean matterAllowed) {
        // A role held by a network node on another face is never offered.
        boolean outputHeld = nodeOutput >= 0 && nodeOutput != face.ordinal();
        boolean matterHeld = !matterAllowed || nodeMatter >= 0 && nodeMatter != face.ordinal();
        boolean outputElsewhere = outputFace >= 0 && outputFace != face.ordinal();
        boolean matterElsewhere = matterFace >= 0 && matterFace != face.ordinal();
        int[] order = new int[4];
        int n = 0;
        order[n++] = 0;
        if (!outputElsewhere && !outputHeld) {
            order[n++] = 1;
        }
        if (!matterElsewhere && !matterHeld) {
            order[n++] = 2;
        }
        if (!outputHeld && !matterHeld) {
            order[n++] = 3;
        }
        if (outputElsewhere && !outputHeld) {
            order[n++] = 1;
        }
        if (matterElsewhere && !matterHeld) {
            order[n++] = 2;
        }
        return java.util.Arrays.copyOf(order, n);
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    /** Adds or removes a face from the hand-picked energy faces. */
    public void toggleEnergy(Face face) {
        if ((nodeEnergy & face.bit()) == 0) {
            energyMask ^= face.bit();
        }
    }

    /** Clears the item roles, except the ones a network node holds. */
    public void clearItems() {
        outputFace = nodeOutput;
        matterFace = nodeMatter;
    }

    /** Back to the default: every face accepts energy. */
    public void resetEnergy() {
        energyMask = 0;
    }

    /** Matter Input only exists with the Stabilized Core. */
    public void clearMatter() {
        matterFace = -1;
        nodeMatter = -1;
    }

    /**
     * How every face looks: item roles first, then energy (hand-picked faces always; by default only where
     * {@code energyConnected} says something with energy touches the face).
     */
    public Look look(Face face, int energyConnected) {
        boolean output = isOutput(face);
        boolean matter = isMatter(face);
        if (output && matter) {
            return Look.OUTPUT_MATTER;
        }
        if (output) {
            return Look.OUTPUT;
        }
        if (matter) {
            return Look.MATTER;
        }
        boolean energy = (nodeEnergy & face.bit()) != 0
                || (energyManual() ? (energyMask & face.bit()) != 0 : (energyConnected & face.bit()) != 0);
        return energy ? Look.ENERGY : Look.NONE;
    }

    /** Looks of the nine faces packed 3 bits each (synced to clients). */
    public int packLooks(int energyConnected) {
        int packed = 0;
        for (Face face : Face.VALUES) {
            packed |= look(face, energyConnected).ordinal() << (3 * face.ordinal());
        }
        return packed;
    }

    public static Look unpackLook(int packed, Face face) {
        int index = (packed >> (3 * face.ordinal())) & 7;
        return index < Look.VALUES.length ? Look.VALUES[index] : Look.NONE;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("output", outputFace);
        tag.putInt("matter", matterFace);
        tag.putInt("energy", energyMask);
        return tag;
    }

    public void load(CompoundTag tag) {
        outputFace = valid(tag.contains("output") ? tag.getIntOr("output", 0) : -1);
        matterFace = valid(tag.contains("matter") ? tag.getIntOr("matter", 0) : -1);
        energyMask = tag.getIntOr("energy", 0) & ((1 << Face.VALUES.length) - 1);
    }

    private static int valid(int face) {
        return face >= 0 && face < Face.VALUES.length ? face : -1;
    }
}
