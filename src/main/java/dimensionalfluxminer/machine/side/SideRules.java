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
                case FLUID -> List.of();
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
                case FLUID -> List.of();
            };
        }

        @Override
        public SideRoute defaultRoute(RelativeFace face, Transmission transmission, int profile) {
            return transmission == Transmission.ENERGY && face != RelativeFace.FRONT ? SideRoute.ENERGY_INPUT : SideRoute.NONE;
        }
    };

    List<SideRoute> FLUID_SINGLE = List.of(SideRoute.NONE, SideRoute.FLUID_INPUT, SideRoute.FLUID_OUTPUT, SideRoute.FLUID_INPUT_OUTPUT);
    List<SideRoute> FLUID_DUAL = List.of(SideRoute.NONE, SideRoute.FLUID_A_INPUT, SideRoute.FLUID_A_OUTPUT, SideRoute.FLUID_A_INPUT_OUTPUT,
            SideRoute.FLUID_B_INPUT, SideRoute.FLUID_B_OUTPUT, SideRoute.FLUID_B_INPUT_OUTPUT);

    /**
     * Dimensional Tank: FRONT locked (panel and gauge); the other faces only carry liquids. Every configurable face
     * starts as input/output, so pipes connect without configuration.
     */
    SideRules TANK_SINGLE = new SideRules() {
        @Override
        public List<SideRoute> allowed(RelativeFace face, Transmission transmission, int profile) {
            return face != RelativeFace.FRONT && transmission == Transmission.FLUID ? FLUID_SINGLE : List.of();
        }

        @Override
        public SideRoute defaultRoute(RelativeFace face, Transmission transmission, int profile) {
            return face != RelativeFace.FRONT && transmission == Transmission.FLUID ? SideRoute.FLUID_INPUT_OUTPUT : SideRoute.NONE;
        }
    };

    /**
     * Dual Dimensional Tank: like the single tank, but each face serves chamber A or chamber B. The right side (seen
     * from the front, where chamber A is) starts with A, the left side with B; the other faces start without a route.
     */
    SideRules TANK_DUAL = new SideRules() {
        @Override
        public List<SideRoute> allowed(RelativeFace face, Transmission transmission, int profile) {
            return face != RelativeFace.FRONT && transmission == Transmission.FLUID ? FLUID_DUAL : List.of();
        }

        @Override
        public SideRoute defaultRoute(RelativeFace face, Transmission transmission, int profile) {
            if (transmission != Transmission.FLUID) {
                return SideRoute.NONE;
            }
            return switch (face) {
                case RIGHT -> SideRoute.FLUID_A_INPUT_OUTPUT;
                case LEFT -> SideRoute.FLUID_B_INPUT_OUTPUT;
                default -> SideRoute.NONE;
            };
        }
    };
}
