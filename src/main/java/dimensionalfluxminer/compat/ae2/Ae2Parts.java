package dimensionalfluxminer.compat.ae2;

import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Item that stands for a cable bus in the side screens: the part mounted on the face toward the machine, otherwise the
 * cable itself, otherwise the facade. The cable bus block has no item of its own and its pick-block follows the
 * player's aim, so it cannot describe a face seen from a screen.
 */
public final class Ae2Parts {
    private Ae2Parts() {
    }

    public static ItemStack facing(BlockEntity be, Direction towardMachine) {
        if (!(be instanceof IPartHost host)) {
            return ItemStack.EMPTY;
        }
        IPart part = host.getPart(towardMachine);
        if (part == null) {
            part = host.getPart(null);
        }
        if (part != null) {
            return new ItemStack(part.getPartItem());
        }
        IFacadePart facade = host.getFacadeContainer().getFacade(towardMachine);
        return facade != null ? facade.getItemStack() : ItemStack.EMPTY;
    }
}
