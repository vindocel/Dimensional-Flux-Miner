package dimensionalfluxminer.network;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.menu.SyncedMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client snapshot of a machine menu's display data (values above 16 bits included). */
public record MenuSyncPayload(int containerId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<MenuSyncPayload> TYPE = new Type<>(DimensionalFluxMiner.id("menu_sync"));
    public static final StreamCodec<ByteBuf, MenuSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenuSyncPayload::containerId,
            ByteBufCodecs.COMPOUND_TAG, MenuSyncPayload::data,
            MenuSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MenuSyncPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof SyncedMenu menu && context.player().containerMenu.containerId == payload.containerId()) {
            menu.applySync(payload.data());
        }
    }
}
