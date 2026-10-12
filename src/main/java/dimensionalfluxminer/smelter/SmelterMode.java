package dimensionalfluxminer.smelter;

import dimensionalfluxminer.machine.side.SideRules;
import java.util.Locale;

/**
 * Operating mode of the Alloy Smelter; also selects the ITEM side-route profile.
 * <ul>
 *     <li>{@link #NORMAL}: furnace only, the three lanes smelt independently;</li>
 *     <li>{@link #ALL}: alloys first (when the lanes hold an alloy recipe), otherwise the lanes smelt;</li>
 *     <li>{@link #ALLOY}: alloys only.</li>
 * </ul>
 */
public enum SmelterMode {
    NORMAL(SideRules.PROFILE_NORMAL, false, true),
    ALL(SideRules.PROFILE_ALLOY, true, true),
    ALLOY(SideRules.PROFILE_ALLOY, true, false);

    public static final SmelterMode[] VALUES = values();

    private final int profile;
    private final boolean canAlloy;
    private final boolean canSmelt;

    SmelterMode(int profile, boolean canAlloy, boolean canSmelt) {
        this.profile = profile;
        this.canAlloy = canAlloy;
        this.canSmelt = canSmelt;
    }

    public int profile() {
        return profile;
    }

    public boolean canAlloy() {
        return canAlloy;
    }

    public boolean canSmelt() {
        return canSmelt;
    }

    /** Next (or previous) mode: furnace only -> alloy + furnace -> alloy only -> furnace only. */
    public SmelterMode cycle(boolean forward) {
        return VALUES[Math.floorMod(ordinal() + (forward ? 1 : -1), VALUES.length)];
    }

    public String translationKey() {
        return "gui.dimensionalfluxminer.smelter.mode." + name().toLowerCase(Locale.ROOT);
    }
}
