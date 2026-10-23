package dimensionalfluxminer.client.render;

import dimensionalfluxminer.client.screen.GuideScreen;
import dimensionalfluxminer.registry.DfmItems;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Open/close animation of the guide in the local player's hand, from the book model's own "abrir" and "fechar"
 * keyframes. Using the book opens the guide screen at once while the book opens in the hand behind it; closing the
 * screen closes the book again.
 */
public final class GuideBookAnimation {
    private static final long DURATION_MS = 550;
    /** Held open the book stays half open (like the model's V), not flat: every part stops at this share of its turn. */
    private static final float HOLD_OPEN = 0.62F;

    private enum Phase { CLOSED, OPENING, OPEN, CLOSING }

    /** Same length as the page turn of the guide screen. */
    private static final long PAGE_TURN_MS = 380;

    private static Phase phase = Phase.CLOSED;
    private static long phaseStart;
    /** Page turn in the hand while the screen turns a page; -1 when none. */
    private static long pageTurnStart = -1;
    private static boolean pageTurnForward;

    private GuideBookAnimation() {
    }

    /** The guide was used: the screen opens at once while the book opens in the hand behind it. */
    public static void requestOpen() {
        if (phase != Phase.OPENING && phase != Phase.OPEN) {
            // A book still closing starts opening from where it is.
            phaseStart = Util.getMillis() - (phase == Phase.CLOSING ? (long) ((1 - progress()) * DURATION_MS) : 0);
            phase = Phase.OPENING;
        }
        Minecraft.getInstance().setScreenAndShow(new GuideScreen());
    }

    /** The guide screen turned a page: a leaf of the book in the hand turns too, as if the player were reading it. */
    public static void pageTurned(boolean forward) {
        if (phase == Phase.OPEN || phase == Phase.OPENING) {
            pageTurnStart = Util.getMillis();
            pageTurnForward = forward;
        }
    }

    /** The guide screen closed: the book closes. */
    public static void screenClosed() {
        startClosing();
    }

    private static void startClosing() {
        if (phase == Phase.OPEN || phase == Phase.OPENING) {
            long elapsed = phase == Phase.OPENING ? (long) ((1 - progress()) * DURATION_MS) : 0;
            phase = Phase.CLOSING;
            phaseStart = Util.getMillis() - elapsed;
        }
    }

    private static float progress() {
        return Math.min(1.0F, (Util.getMillis() - phaseStart) / (float) DURATION_MS);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean holding = player != null && (player.getMainHandItem().is(DfmItems.GUIDE_BOOK.get())
                || player.getOffhandItem().is(DfmItems.GUIDE_BOOK.get()));
        if (!holding && (phase == Phase.OPENING || phase == Phase.OPEN)) {
            startClosing();
        }
        if (phase == Phase.OPENING && progress() >= 1.0F) {
            phase = Phase.OPEN;
        }
        if (phase == Phase.CLOSING && progress() >= 1.0F) {
            phase = Phase.CLOSED;
        }
    }

    /** Current pose of the book parts in degrees / pixels. */
    public static Pose pose() {
        return switch (phase) {
            case CLOSED -> Pose.CLOSED;
            case OPEN -> held();
            case OPENING -> Pose.opening(progress()).scaled(HOLD_OPEN);
            case CLOSING -> Pose.closing(progress()).scaled(HOLD_OPEN);
        };
    }

    /**
     * Held open: the first leaf lies with the cover (left), the second one on the back pages (right), so a turn has a
     * leaf on each side to use. Forward, the right leaf turns over to the left; backward, the left leaf comes back
     * to the right. When the turn ends the leaf is back on its own side, exactly over identical pages, so the reset is
     * invisible.
     */
    private static Pose held() {
        Pose open = Pose.OPEN.scaled(HOLD_OPEN);
        float left = open.leaf1();
        float leaf1 = left;
        float leaf2 = 0;
        if (pageTurnStart >= 0) {
            float t = (Util.getMillis() - pageTurnStart) / (float) PAGE_TURN_MS;
            if (t >= 1.0F) {
                pageTurnStart = -1;
            } else {
                float eased = (float) (0.5 - 0.5 * Math.cos(Math.PI * t));
                if (pageTurnForward) {
                    leaf2 = left * eased;
                } else {
                    leaf1 = left * (1.0F - eased);
                }
            }
        }
        return new Pose(open.cover(), leaf1, leaf2, open.spine(), open.spineDrop());
    }

    /** Cover, leaves and spine rotations (Z, degrees) and the spine drop (pixels). */
    public record Pose(float cover, float leaf1, float leaf2, float spine, float spineDrop) {
        public static final Pose CLOSED = new Pose(0, 0, 0, 0, 0);
        static final Pose OPEN = new Pose(180, 180, 180, 90, -0.97F);

        /** Keyframes of the "abrir" animation. */
        static Pose opening(float t) {
            return new Pose(
                    lerp(t, 0, 0, 0.2F, 15, 0.65F, 145, 1, 180),
                    lerp(t, 0, 0, 0.35F, 0, 0.7F, 120, 1, 180),
                    lerp(t, 0, 0, 0.25F, 0, 0.6F, 115, 0.95F, 180, 1, 180),
                    lerp(t, 0, 0, 0.2F, 7.5F, 0.65F, 72.5F, 1, 90),
                    lerp(t, 0, 0, 0.2F, -0.08083F, 0.65F, -0.78139F, 1, -0.97F));
        }

        /** Keyframes of the "fechar" animation. */
        static Pose closing(float t) {
            return new Pose(
                    lerp(t, 0, 180, 0.35F, 145, 0.8F, 15, 1, 0),
                    lerp(t, 0, 180, 0.3F, 120, 0.65F, 0, 1, 0),
                    lerp(t, 0, 180, 0.05F, 180, 0.4F, 115, 0.75F, 0, 1, 0),
                    lerp(t, 0, 90, 0.35F, 72.5F, 0.8F, 7.5F, 1, 0),
                    lerp(t, 0, -0.97F, 0.35F, -0.78139F, 0.8F, -0.08083F, 1, 0));
        }

        Pose scaled(float share) {
            return new Pose(cover * share, leaf1 * share, leaf2 * share, spine * share, spineDrop * share);
        }

        /** How far the book is open (0 closed, 1 open), by the cover. */
        public float openness() {
            return cover / 180.0F;
        }

        /** Linear interpolation over (time, value) pairs. */
        private static float lerp(float t, float... keys) {
            if (t <= keys[0]) {
                return keys[1];
            }
            for (int i = 2; i < keys.length; i += 2) {
                if (t <= keys[i]) {
                    float t0 = keys[i - 2];
                    float span = keys[i] - t0;
                    float f = span <= 0 ? 1 : (t - t0) / span;
                    return keys[i - 1] + (keys[i + 1] - keys[i - 1]) * f;
                }
            }
            return keys[keys.length - 1];
        }
    }
}
