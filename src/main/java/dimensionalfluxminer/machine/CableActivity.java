package dimensionalfluxminer.machine;

import java.util.Arrays;

/**
 * When a cable or pipe last pulled items from / pushed items into each face of a machine (Miner ports, Crusher and
 * Electric Furnace sides). A face used within {@link #ACTIVE_TICKS} still counts as used: its role follows the cable
 * and stays locked in the Side Configuration until the cable stops. Not saved.
 */
public final class CableActivity {
    /** A cable that pulled from / pushed into a face this recently still counts as using it. */
    public static final int ACTIVE_TICKS = 100;

    private final long[] pullSeen;
    private final long[] pushSeen;

    public CableActivity(int faces) {
        pullSeen = never(faces);
        pushSeen = never(faces);
    }

    private static long[] never(int faces) {
        long[] seen = new long[faces];
        Arrays.fill(seen, Long.MIN_VALUE / 2);
        return seen;
    }

    public void notePull(int face, long now) {
        pullSeen[face] = now;
    }

    public void notePush(int face, long now) {
        pushSeen[face] = now;
    }

    public boolean pulling(int face, long now) {
        return now - pullSeen[face] <= ACTIVE_TICKS;
    }

    public boolean pushing(int face, long now) {
        return now - pushSeen[face] <= ACTIVE_TICKS;
    }

    /**
     * The face a cable is using for a role that only one face can hold (pulls or pushes): the current holder while its
     * cable stays active, otherwise the face used most recently within {@link #ACTIVE_TICKS}, or -1.
     */
    public int activeFace(boolean pull, int holder, long now) {
        long[] seen = pull ? pullSeen : pushSeen;
        if (holder >= 0 && now - seen[holder] <= ACTIVE_TICKS) {
            return holder;
        }
        int best = -1;
        for (int i = 0; i < seen.length; i++) {
            if (now - seen[i] <= ACTIVE_TICKS && (best < 0 || seen[i] > seen[best])) {
                best = i;
            }
        }
        return best;
    }
}
