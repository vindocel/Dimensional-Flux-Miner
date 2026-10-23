package dimensionalfluxminer.compat;

import dimensionalfluxminer.machine.transfer.EnergyStore;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Optional Applied Energistics 2 power: a machine (and each Miner port) next to an ME cable joins the network and draws
 * AE as its energy input, converted to FE at the AE2 rate. Only {@code compat.ae2} touches AE2 classes; without the mod
 * every call here does nothing.
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
     * Once per game tick on the server: joins the network on the sides where {@code input} takes energy and fills that
     * input from the network.
     */
    public static void pull(BlockEntity be, Function<Direction, EnergyStore> input) {
        if (loaded()) {
            dimensionalfluxminer.compat.ae2.Ae2Power.pull(be, input);
        }
    }

    /** The block entity left the world (broken or unloaded): its network node goes too. */
    public static void release(BlockEntity be) {
        if (loaded()) {
            dimensionalfluxminer.compat.ae2.Ae2Power.release(be);
        }
    }
}
