package dimensionalfluxminer.compat;

import dimensionalfluxminer.machine.transfer.EnergyStore;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Optional Applied Energistics 2 support. Power: a machine (and each Miner port) takes AE only from an energy cell
 * touching a side that accepts energy or from a quartz fiber on that face, converted to FE at the AE2 rate; ME cables
 * never connect. Side screens: a cable bus shows the part facing the machine. Only {@code compat.ae2} touches AE2
 * classes; without the mod every call here does nothing.
 */
public final class Ae2Compat {
    public static final String MOD_ID = "ae2";
    private static Boolean loaded;

    private Ae2Compat() {
    }

    public static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    public static void register(IEventBus modBus) {
        if (loaded()) {
            dimensionalfluxminer.compat.ae2.Ae2Power.register(modBus);
        }
    }

    /**
     * Once per game tick on the server: joins the network through the power links on the sides where {@code input}
     * takes energy and fills that input from the network.
     */
    public static void pull(BlockEntity be, Function<Direction, EnergyStore> input) {
        if (loaded()) {
            dimensionalfluxminer.compat.ae2.Ae2Power.pull(be, input);
        }
    }

    /** Item of the AE2 part (or cable) at {@code be} facing the machine; empty when it is not a cable bus. */
    public static ItemStack partFacing(BlockEntity be, Direction towardMachine) {
        if (loaded() && be != null) {
            return dimensionalfluxminer.compat.ae2.Ae2Parts.facing(be, towardMachine);
        }
        return ItemStack.EMPTY;
    }

    /** The block entity left the world (broken or unloaded): its network node goes too. */
    public static void release(BlockEntity be) {
        if (loaded()) {
            dimensionalfluxminer.compat.ae2.Ae2Power.release(be);
        }
    }
}
