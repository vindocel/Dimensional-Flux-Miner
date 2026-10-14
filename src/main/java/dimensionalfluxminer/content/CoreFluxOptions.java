package dimensionalfluxminer.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A mote pulled into the Miner's core; {@code speed} scales how fast it travels (1 = Rate 1). */
public record CoreFluxOptions(ParticleType<CoreFluxOptions> type, float speed) implements ParticleOptions {
    @Override
    public ParticleType<CoreFluxOptions> getType() {
        return type;
    }

    public static MapCodec<CoreFluxOptions> codec(ParticleType<CoreFluxOptions> type) {
        return Codec.FLOAT.fieldOf("speed").xmap(speed -> new CoreFluxOptions(type, speed), CoreFluxOptions::speed);
    }

    public static StreamCodec<RegistryFriendlyByteBuf, CoreFluxOptions> streamCodec(ParticleType<CoreFluxOptions> type) {
        return ByteBufCodecs.FLOAT.<RegistryFriendlyByteBuf>cast().map(speed -> new CoreFluxOptions(type, speed), CoreFluxOptions::speed);
    }
}
