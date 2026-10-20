package dimensionalfluxminer.machine.side;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Per-transmission side configuration of a single-block machine. ITEM routes are stored per item profile
 * (Electric Furnace NORMAL / ALLOY); ENERGY, REDSTONE, EXPERIENCE and FLUID are shared between profiles.
 * Every face starts as {@link SideRoute#NONE}.
 */
public final class SideConfiguration {
    private static final int PROFILES = 2;
    private static final int FACES = RelativeFace.VALUES.length;

    private final SideRules rules;
    private final SideRoute[][] item = new SideRoute[PROFILES][FACES];
    private final SideRoute[] energy = new SideRoute[FACES];
    private final SideRoute[] redstone = new SideRoute[FACES];
    private final SideRoute[] experience = new SideRoute[FACES];
    private final SideRoute[] fluid = new SideRoute[FACES];
    /**
     * Faces (bit per {@link RelativeFace}) whose route an external network node sets, per transmission: they do not
     * change by hand, batch or clear while the node is there. Not saved: read again from the nodes.
     */
    private final int[] locks = new int[Transmission.VALUES.length];

    public SideConfiguration(SideRules rules) {
        this.rules = rules;
        resetToDefaults();
    }

    /** Applies the machine's default routes (for example energy accepted on every allowed face). */
    public void resetToDefaults() {
        for (Transmission t : Transmission.VALUES) {
            for (int p = 0; p < PROFILES; p++) {
                SideRoute[] target = array(t, p);
                for (RelativeFace face : RelativeFace.VALUES) {
                    target[face.ordinal()] = rules.defaultRoute(face, t, p);
                }
            }
        }
    }

    public SideRules rules() {
        return rules;
    }

    private SideRoute[] array(Transmission transmission, int profile) {
        return switch (transmission) {
            case ITEM -> item[Math.floorMod(profile, PROFILES)];
            case ENERGY -> energy;
            case REDSTONE -> redstone;
            case EXPERIENCE -> experience;
            case FLUID -> fluid;
        };
    }

    public SideRoute get(RelativeFace face, Transmission transmission, int profile) {
        SideRoute route = array(transmission, profile)[face.ordinal()];
        List<SideRoute> allowed = rules.allowed(face, transmission, profile);
        if (allowed.contains(route)) {
            return route;
        }
        SideRoute fallback = rules.defaultRoute(face, transmission, profile);
        return allowed.contains(fallback) ? fallback : SideRoute.NONE;
    }

    public boolean set(RelativeFace face, Transmission transmission, int profile, SideRoute route) {
        if (!rules.allowed(face, transmission, profile).contains(route)) {
            return false;
        }
        array(transmission, profile)[face.ordinal()] = route;
        return true;
    }

    public boolean locked(RelativeFace face, Transmission transmission) {
        return (locks[transmission.ordinal()] & (1 << face.ordinal())) != 0;
    }

    public int locks(Transmission transmission) {
        return locks[transmission.ordinal()];
    }

    /** Replaces the faces locked by network nodes for a transmission; returns whether they changed. */
    public boolean setLocks(Transmission transmission, int mask) {
        boolean changed = locks[transmission.ordinal()] != mask;
        locks[transmission.ordinal()] = mask;
        return changed;
    }

    /** Moves to the next (or previous) supported route of that face/transmission. */
    public boolean cycle(RelativeFace face, Transmission transmission, int profile, boolean forward) {
        List<SideRoute> allowed = rules.allowed(face, transmission, profile);
        if (allowed.size() < 2 || locked(face, transmission)) {
            return false;
        }
        int index = allowed.indexOf(get(face, transmission, profile));
        int next = Math.floorMod(index + (forward ? 1 : -1), allowed.size());
        return set(face, transmission, profile, allowed.get(next));
    }

    /** Sets every configurable face of the transmission back to NONE (fixed routes are kept). */
    public void clear(Transmission transmission, int profile) {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (rules.allowed(face, transmission, profile).contains(SideRoute.NONE) && !locked(face, transmission)) {
                array(transmission, profile)[face.ordinal()] = SideRoute.NONE;
            }
        }
    }

    /**
     * Route the batch button moves every configurable face (two or more allowed routes) to: the next (or previous)
     * route after the one they all share, or NONE when they differ.
     */
    public SideRoute batchTarget(Transmission transmission, int profile, boolean forward) {
        SideRoute common = null;
        boolean[] supported = new boolean[SideRoute.VALUES.length];
        for (RelativeFace face : RelativeFace.VALUES) {
            List<SideRoute> allowed = rules.allowed(face, transmission, profile);
            if (allowed.size() < 2) {
                continue;
            }
            for (SideRoute route : allowed) {
                supported[route.ordinal()] = true;
            }
            SideRoute current = get(face, transmission, profile);
            if (common == null) {
                common = current;
            } else if (common != current) {
                return SideRoute.NONE;
            }
        }
        if (common == null) {
            return SideRoute.NONE;
        }
        int step = forward ? 1 : -1;
        for (int i = 1; i <= SideRoute.VALUES.length; i++) {
            SideRoute candidate = SideRoute.VALUES[Math.floorMod(common.ordinal() + i * step, SideRoute.VALUES.length)];
            if (supported[candidate.ordinal()]) {
                return candidate;
            }
        }
        return common;
    }

    /** Sets {@code route} on every configurable face that allows it; returns whether anything changed. */
    public boolean applyAll(Transmission transmission, int profile, SideRoute route) {
        boolean changed = false;
        for (RelativeFace face : RelativeFace.VALUES) {
            List<SideRoute> allowed = rules.allowed(face, transmission, profile);
            if (allowed.size() >= 2 && allowed.contains(route) && get(face, transmission, profile) != route
                    && !locked(face, transmission)) {
                array(transmission, profile)[face.ordinal()] = route;
                changed = true;
            }
        }
        return changed;
    }

    public boolean anyFace(Transmission transmission, int profile, SideRoute route) {
        for (RelativeFace face : RelativeFace.VALUES) {
            if (get(face, transmission, profile) == route) {
                return true;
            }
        }
        return false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (int p = 0; p < PROFILES; p++) {
            tag.put("item_" + p, write(item[p]));
        }
        tag.put("energy", write(energy));
        tag.put("redstone", write(redstone));
        tag.put("experience", write(experience));
        tag.put("fluid", write(fluid));
        return tag;
    }

    /** Loads saved routes; transmissions missing from the tag keep the machine defaults. */
    public void load(CompoundTag tag) {
        resetToDefaults();
        for (int p = 0; p < PROFILES; p++) {
            read(tag.getList("item_" + p, Tag.TAG_STRING), item[p]);
        }
        read(tag.getList("energy", Tag.TAG_STRING), energy);
        read(tag.getList("redstone", Tag.TAG_STRING), redstone);
        read(tag.getList("experience", Tag.TAG_STRING), experience);
        read(tag.getList("fluid", Tag.TAG_STRING), fluid);
    }

    private static ListTag write(SideRoute[] routes) {
        ListTag list = new ListTag();
        for (SideRoute route : routes) {
            list.add(StringTag.valueOf(route.name()));
        }
        return list;
    }

    private static void read(ListTag list, SideRoute[] target) {
        for (int i = 0; i < target.length && i < list.size(); i++) {
            try {
                target[i] = SideRoute.valueOf(list.getString(i));
            } catch (IllegalArgumentException ignored) {
                // Unknown route name: keep the default for that face.
            }
        }
    }
}
