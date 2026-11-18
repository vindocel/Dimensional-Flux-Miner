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
 * The effective configuration values and the keys owned by server scripts ({@link DfmConfig#overrides()}), as JSON with exact numeric decoding. Sent on login and after every
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
        return new ConfigOverridesPayload(snapshot());
    }

    private static String snapshot() {
        JsonObject root = GSON.toJsonTree(DfmConfig.effectiveValues()).getAsJsonObject();
        root.add("__scripted", GSON.toJsonTree(DfmConfig.overrides().keySet()));
        return GSON.toJson(root);
    }

    private static Object decode(JsonElement value) {
        if (value.isJsonArray()) {
            java.util.List<Object> items = new java.util.ArrayList<>();
            value.getAsJsonArray().forEach(item -> items.add(decode(item)));
            return java.util.List.copyOf(items);
        }
        var primitive = value.getAsJsonPrimitive();
        return primitive.isBoolean() ? primitive.getAsBoolean()
                : primitive.isNumber() ? primitive.getAsBigDecimal() : primitive.getAsString();
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
        java.util.Set<String> scripted = new java.util.HashSet<>();
        if (root.has("__scripted")) root.getAsJsonArray("__scripted").forEach(key -> scripted.add(key.getAsString()));
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getKey().equals("__scripted")) continue;
            try {
                values.put(entry.getKey(), DfmConfig.checkedOverride(entry.getKey(), decode(entry.getValue())));
            } catch (IllegalArgumentException e) {
                DimensionalFluxMiner.LOGGER.warn("Ignoring configuration value from the server: {}", e.getMessage());
            }
        }
        if (!root.has("__scripted")) {
            // Older senders supplied only script values; native config synchronization supplies the file values.
            scripted.addAll(values.keySet());
            Map<String, Object> complete = new HashMap<>(DfmConfig.fileValues());
            complete.putAll(values);
            values = complete;
        }
        DfmConfig.acceptRemoteValues(values, scripted);
    }
}
