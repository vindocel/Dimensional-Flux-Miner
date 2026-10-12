package dimensionalfluxminer.machine.side;

import net.minecraft.core.Direction;

/** Machine-relative face. LEFT/RIGHT are seen from a player looking at the FRONT of the machine. */
public enum RelativeFace {
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    UP,
    DOWN;

    public static final RelativeFace[] VALUES = values();

    public Direction toDirection(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            // Looking at the front means looking along -facing; the player's left is the machine's right side.
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    public static RelativeFace of(Direction facing, Direction side) {
        for (RelativeFace face : VALUES) {
            if (face.toDirection(facing) == side) {
                return face;
            }
        }
        throw new IllegalStateException("Unreachable side " + side);
    }

    public String translationKey() {
        return "gui.dimensionalfluxminer.face." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
