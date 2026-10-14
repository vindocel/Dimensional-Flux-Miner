package dimensionalfluxminer.machine;

import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-only behaviour installed by the client setup, so common block entity code never links client classes.
 */
public final class ClientHooks {
    /** Starts/keeps the single looping sound of an active machine. */
    public static Consumer<BlockEntity> machineSound = be -> { };
    /** Client tick of a formed Dimensional Flux Miner: its running loop and the motes its core pulls in. */
    public static Consumer<BlockEntity> minerTick = be -> { };
    /** Short particle burst when a Miner assembly forms. */
    public static Consumer<java.util.List<net.minecraft.core.BlockPos>> formationEffect = positions -> { };
    /** Highlight of the parts still missing in a Miner assembly (an empty list clears it). */
    public static Consumer<java.util.List<dimensionalfluxminer.network.MinerAssemblyPayload.Entry>> assemblyHighlight = entries -> { };
    /** Opens the Dimensional Flux Guide screen. */
    public static Runnable openGuide = () -> { };

    private ClientHooks() {
    }
}
