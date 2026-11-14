package dimensionalfluxminer.compat.ae2;

import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Places AE2 cable bus parts for the energy GameTests; only called when AE2 is installed. */
public final class Ae2TestParts {
    private Ae2TestParts() {
    }

    /** A plain glass ME cable at {@code pos}. */
    public static void cable(ServerLevel level, BlockPos pos) {
        PartHelper.setPart(level, pos, null, null, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
    }

    /** A glass ME cable at {@code pos} with a quartz fiber on its {@code side} face. */
    public static void cableWithFiber(ServerLevel level, BlockPos pos, Direction side) {
        cable(level, pos);
        PartHelper.setPart(level, pos, side, null, AEParts.QUARTZ_FIBER.get());
    }
}
