package dimensionalfluxminer.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DfmNetwork {
    private DfmNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(MenuSyncPayload.TYPE, MenuSyncPayload.STREAM_CODEC, MenuSyncPayload::handle);
        registrar.playToClient(MinerFormationPayload.TYPE, MinerFormationPayload.STREAM_CODEC, MinerFormationPayload::handle);
        registrar.playToClient(MinerAssemblyPayload.TYPE, MinerAssemblyPayload.STREAM_CODEC, MinerAssemblyPayload::handle);
    }
}
