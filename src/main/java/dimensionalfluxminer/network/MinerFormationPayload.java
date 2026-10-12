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

/** One-shot visual event sent on the UNFORMED -> FORMED transition, listing the 27 participant positions. */
public record MinerFormationPayload(List<BlockPos> positions) implements CustomPacketPayload {
    public static final Type<MinerFormationPayload> TYPE = new Type<>(DimensionalFluxMiner.id("miner_formation"));
    public static final StreamCodec<ByteBuf, MinerFormationPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(27)), MinerFormationPayload::positions,
            MinerFormationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MinerFormationPayload payload, IPayloadContext context) {
        ClientHooks.formationEffect.accept(payload.positions());
    }
}
