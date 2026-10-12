package dimensionalfluxminer.machine;

import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-only behaviour installed by the client setup, so common block entity code never links client classes.
 */
public final class ClientHooks {
    /** Starts/keeps the single looping sound of an active machine. */
    public static Consumer<BlockEntity> machineSound = be -> { };
    /** Running loop of a formed Dimensional Flux Miner, anchored to the structure center. */
    public static Consumer<BlockEntity> minerSound = be -> { };
    /** Short particle burst when a Miner assembly forms. */
    public static Consumer<java.util.List<net.minecraft.core.BlockPos>> formationEffect = positions -> { };
    /** Highlight of the parts still missing in a Miner assembly (an empty list clears it). */
    public static Consumer<java.util.List<dimensionalfluxminer.network.MinerAssemblyPayload.Entry>> assemblyHighlight = entries -> { };

    private ClientHooks() {
    }
}
