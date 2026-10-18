package dimensionalfluxminer.network;

import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.machine.ClientHooks;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Positions still missing in the Miner assembly of {@code terminal}, with the expected part, for the player's highlight
 * (an empty list hides it).
 */
public record MinerAssemblyPayload(BlockPos terminal, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<MinerAssemblyPayload> TYPE = new Type<>(DimensionalFluxMiner.id("miner_assembly"));

    /** {@code part} is the ordinal of {@link dimensionalfluxminer.miner.MinerLayout.Part}. */
    public record Entry(BlockPos pos, int part) {
        static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::pos, ByteBufCodecs.VAR_INT, Entry::part, Entry::new);
    }

    public static final StreamCodec<ByteBuf, MinerAssemblyPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MinerAssemblyPayload::terminal,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(27)), MinerAssemblyPayload::entries, MinerAssemblyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MinerAssemblyPayload payload, IPayloadContext context) {
        ClientHooks.assemblyHighlight.accept(payload);
    }
}
