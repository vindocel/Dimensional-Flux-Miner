package dimensionalfluxminer.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Small helpers shared by the GameTests. */
final class TestSupport {
    private TestSupport() {
    }

    /** The block entity at a position relative to the test, of the type the caller expects. */
    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> T blockEntity(GameTestHelper helper, BlockPos pos) {
        return (T) helper.getBlockEntity(pos, BlockEntity.class);
    }

    /**
     * Grass over dirt under the test and around it ({@code radius} blocks past its corner), one block below the test
     * floor: the ground a Miner drains. The test world of this line is sandstone, which the corruption never takes.
     */
    static void naturalGround(GameTestHelper helper, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                helper.setBlock(new BlockPos(x, -1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
                helper.setBlock(new BlockPos(x, -2, z), net.minecraft.world.level.block.Blocks.DIRT);
            }
        }
    }

    /** Loads saved data into a block entity, as when its chunk is read again. */
    static void load(BlockEntity be, CompoundTag tag, HolderLookup.Provider registries) {
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }

    /** The data an entity saves. */
    static CompoundTag save(Entity entity, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        entity.saveWithoutId(output);
        return output.buildResult();
    }
}
