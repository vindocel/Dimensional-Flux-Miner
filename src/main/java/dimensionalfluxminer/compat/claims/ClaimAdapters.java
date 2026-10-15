package dimensionalfluxminer.compat.claims;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.corruption.ProtectionService;
import net.neoforged.fml.ModList;

/**
 * Registers the claim adapters of the claim mods that are installed. The adapters reach those mods only through
 * reflection on their public APIs, so nothing of them is loaded when they are absent and a changed API never crashes
 * the game: the adapter reports itself unavailable instead.
 */
public final class ClaimAdapters {
    private ClaimAdapters() {
    }

    public static void registerInstalled() {
        if (ModList.get().isLoaded(FtbChunksAdapter.MOD_ID)) {
            ProtectionService.register(new FtbChunksAdapter());
        }
        if (ModList.get().isLoaded(OpenPacAdapter.MOD_ID)) {
            ProtectionService.register(new OpenPacAdapter());
        }
        if (!ProtectionService.adaptersPresent()) {
            DimensionalFluxMiner.LOGGER.info("No claim mod installed: Dimensional Death follows the server rules only");
        }
    }
}
