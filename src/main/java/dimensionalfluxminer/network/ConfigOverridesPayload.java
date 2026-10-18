package dimensionalfluxminer.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dimensionalfluxminer.DfmConfig;
import dimensionalfluxminer.DimensionalFluxMiner;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The configuration values set by server scripts ({@link DfmConfig#overrides()}), as JSON. Sent on login and after every
 * script reload, so the guide and screens of the clients show the numbers the server really uses.
 */
public record ConfigOverridesPayload(String json) implements CustomPacketPayload {
    public static final Type<ConfigOverridesPayload> TYPE = new Type<>(DimensionalFluxMiner.id("config_overrides"));
    public static final StreamCodec<ByteBuf, ConfigOverridesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1 << 18), ConfigOverridesPayload::json, ConfigOverridesPayload::new);
    private static final Gson GSON = new Gson();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ConfigOverridesPayload current() {
        return new ConfigOverridesPayload(GSON.toJson(DfmConfig.overrides()));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, current());
        }
    }

    /** Sends the current values to every connected player (after the scripts changed them). */
    public static void sendToAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            server.execute(() -> PacketDistributor.sendToAllPlayers(current()));
        }
    }

    public static void handle(ConfigOverridesPayload payload, IPayloadContext context) {
        Map<String, Object> values = new HashMap<>();
        JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            try {
                values.put(entry.getKey(), DfmConfig.checkedOverride(entry.getKey(), GSON.fromJson(entry.getValue(), Object.class)));
            } catch (IllegalArgumentException e) {
                DimensionalFluxMiner.LOGGER.warn("Ignoring configuration value from the server: {}", e.getMessage());
            }
        }
        DfmConfig.setOverrides(values);
    }
}
