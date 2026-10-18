package dimensionalfluxminer.machine.side;

import java.util.List;

/** Which routes a machine offers for a face and transmission (an empty list means the face is locked). */
public interface SideRules {
    List<SideRoute> allowed(RelativeFace face, Transmission transmission, int itemProfile);

    /** Route a newly placed machine starts with on that face. */
    default SideRoute defaultRoute(RelativeFace face, Transmission transmission, int itemProfile) {
        return SideRoute.NONE;
    }

    List<SideRoute> ITEM_BASIC = List.of(SideRoute.NONE, SideRoute.ITEM_INPUT_ALL, SideRoute.ITEM_OUTPUT, SideRoute.ITEM_INPUT_OUTPUT_ALL);
    List<SideRoute> ITEM_ALLOY = List.of(SideRoute.NONE, SideRoute.ITEM_INPUT_ALL, SideRoute.ITEM_INPUT_1, SideRoute.ITEM_INPUT_2,
            SideRoute.ITEM_INPUT_3, SideRoute.ITEM_OUTPUT, SideRoute.ITEM_INPUT_OUTPUT_ALL);
    List<SideRoute> ENERGY = List.of(SideRoute.NONE, SideRoute.ENERGY_INPUT);
    List<SideRoute> EXPERIENCE = List.of(SideRoute.NONE, SideRoute.XP_OUTPUT);
    /** The Crusher top is a fixed input funnel: it never outputs items. */
    List<SideRoute> CRUSHER_TOP = List.of(SideRoute.ITEM_INPUT_ALL);

    int PROFILE_NORMAL = 0;
    int PROFILE_ALLOY = 1;

    /**
     * Crusher: FRONT locked; TOP fixed item input (in-world pickup face); LEFT/RIGHT/BACK/DOWN item + energy.
     * Redstone is a whole-block control, not a face route. Every face that can take energy starts as ENERGY_INPUT.
     */
    SideRules CRUSHER = new SideRules() {
        @Override
        public List<SideRoute> allowed(RelativeFace face, Transmission transmission, int profile) {
            if (face == RelativeFace.FRONT || transmission == Transmission.EXPERIENCE) {
                return List.of();
            }
            if (face == RelativeFace.UP) {
                return transmission == Transmission.ITEM ? CRUSHER_TOP : List.of();
            }
            return switch (transmission) {
                case ITEM -> ITEM_BASIC;
                case ENERGY -> ENERGY;
                case REDSTONE -> List.of();
                case EXPERIENCE -> List.of();
            };
        }

        @Override
        public SideRoute defaultRoute(RelativeFace face, Transmission transmission, int profile) {
            if (face == RelativeFace.UP && transmission == Transmission.ITEM) {
                return SideRoute.ITEM_INPUT_ALL;
            }
            return transmission == Transmission.ENERGY && allowed(face, transmission, profile).contains(SideRoute.ENERGY_INPUT)
                    ? SideRoute.ENERGY_INPUT : SideRoute.NONE;
        }
    };

    /** Electric Furnace: FRONT locked; UP/DOWN/LEFT/RIGHT/BACK configurable; every configurable face starts accepting energy. */
    SideRules ELECTRIC_FURNACE = new SideRules() {
        @Override
        public List<SideRoute> allowed(RelativeFace face, Transmission transmission, int profile) {
            if (face == RelativeFace.FRONT) {
                return List.of();
            }
            return switch (transmission) {
                case ITEM -> profile == PROFILE_ALLOY ? ITEM_ALLOY : ITEM_BASIC;
                case ENERGY -> ENERGY;
                case REDSTONE -> List.of();
                case EXPERIENCE -> EXPERIENCE;
            };
        }

        @Override
        public SideRoute defaultRoute(RelativeFace face, Transmission transmission, int profile) {
            return transmission == Transmission.ENERGY && face != RelativeFace.FRONT ? SideRoute.ENERGY_INPUT : SideRoute.NONE;
        }
    };
}
