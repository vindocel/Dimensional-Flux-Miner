package dimensionalfluxminer.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Item shown for the block touching one face of a machine in the side screens. */
final class NeighborIcon {
    private NeighborIcon() {
    }

    /**
     * Item of the block at {@code pos} as seen from the machine on its {@code towardMachine} side: a part mounted on a
     * cable bus shows the part on that face; any other block shows its pick-block item. Empty for air.
     */
    static ItemStack of(Level level, BlockPos pos, Direction towardMachine) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return ItemStack.EMPTY;
        }
        ItemStack part = dimensionalfluxminer.compat.Ae2Compat.partFacing(level.getBlockEntity(pos), towardMachine);
        if (!part.isEmpty()) {
            return part;
        }
        //? if >=26.1 {
        ItemStack stack = state.getCloneItemStack(pos, level, false, Minecraft.getInstance().player);
        //?} else {
        /*net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                pos.getCenter().relative(towardMachine, 0.5), towardMachine, pos, false);
        ItemStack stack = state.getCloneItemStack(hit, level, pos, Minecraft.getInstance().player);
        *///?}
        return stack.isEmpty() ? new ItemStack(state.getBlock()) : stack;
    }
}
