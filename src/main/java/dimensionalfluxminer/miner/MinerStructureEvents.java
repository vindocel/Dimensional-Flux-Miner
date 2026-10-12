package dimensionalfluxminer.miner;

import dimensionalfluxminer.content.DfmStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Structural change notifications coming from the assembly blocks. */
public final class MinerStructureEvents {
    /** A terminal is at most two blocks away (horizontally and vertically) from any participant. */
    private static final int SEARCH = 2;

    private MinerStructureEvents() {
    }

    /** A part was placed: unformed terminals nearby try to form (neighbor-update formation). */
    public static void onPartPlaced(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SEARCH, -SEARCH, -SEARCH), pos.offset(SEARCH, SEARCH, SEARCH))) {
            if (level.getBlockEntity(p) instanceof MinerControllerBlockEntity controller && !controller.formed()) {
                controller.tryForm();
            }
        }
    }

    /** A formed participant was removed or replaced: its controller invalidates the machine once. */
    public static void onPartRemoved(ServerLevel level, BlockPos pos, BlockState oldState) {
        if (!oldState.hasProperty(DfmStateProperties.FORMED) || !oldState.getValue(DfmStateProperties.FORMED)) {
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SEARCH, -SEARCH, -SEARCH), pos.offset(SEARCH, SEARCH, SEARCH))) {
            if (level.getBlockEntity(p) instanceof MinerControllerBlockEntity controller && controller.formed()) {
                MinerStructure structure = controller.structure();
                if (structure != null && structure.contains(pos)) {
                    controller.invalidate();
                    return;
                }
            }
        }
    }
}
