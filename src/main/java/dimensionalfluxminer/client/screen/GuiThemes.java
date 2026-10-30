package dimensionalfluxminer.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dimensionalfluxminer.DimensionalFluxMiner;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The player's own settings of the machines, kept on this computer (never sent to the server, so every player has them
 * their way): {@code config/dimensionalfluxminer-themes.json}. The look of the screens, screen color (light / dark) and
 * icon color ({@link IconColor}: white by default, blue or purple):
 * <ul>
 *     <li>{@code applyToAll} (shared by every machine): every screen uses {@code allDark} and {@code allIcons};</li>
 *     <li>otherwise each machine uses its own choices, and a machine never chosen uses the shared ones, so turning
 *     {@code applyToAll} off changes nothing on screen until a machine gets its own choice.</li>
 * </ul>
 * The sound volume of each machine ({@code volumes}, 0 to 100 %, 100 when never set) is always its own.
 * Machines are told apart by world (folder of the save, or the server address), dimension and position.
 */
public final class GuiThemes {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "dimensionalfluxminer-themes.json";

    private static boolean loaded;
    private static boolean applyToAll = true;
    private static boolean allDark;
    private static IconColor allIcons = IconColor.WHITE;
    private static final Map<String, Boolean> MACHINES = new LinkedHashMap<>();
    private static final Map<String, IconColor> ACCENTS = new LinkedHashMap<>();
    private static final Map<String, Integer> VOLUMES = new LinkedHashMap<>();

    private GuiThemes() {
    }

    /** Sound volume of the machine at {@code pos} in the player's current world, 0 to 100 (%). */
    public static int volume(BlockPos pos) {
        load();
        return VOLUMES.getOrDefault(key(pos), 100);
    }

    static void setVolume(BlockPos pos, int percent) {
        load();
        int value = Math.max(0, Math.min(100, percent));
        if (value == 100) {
            VOLUMES.remove(key(pos));
        } else {
            VOLUMES.put(key(pos), value);
        }
        save();
    }

    static boolean applyToAll() {
        load();
        return applyToAll;
    }

    /** Look of the machine at {@code pos} in the player's current world and dimension. */
    static boolean dark(BlockPos pos) {
        load();
        if (applyToAll) {
            return allDark;
        }
        return MACHINES.getOrDefault(key(pos), allDark);
    }

    /** Chooses the look for this machine, or for every machine while {@link #applyToAll} is on. */
    static void choose(BlockPos pos, boolean dark) {
        load();
        if (applyToAll) {
            allDark = dark;
        } else {
            MACHINES.put(key(pos), dark);
        }
        save();
    }

    /** Icon color of the machine at {@code pos} (white by default). */
    public static IconColor icons(BlockPos pos) {
        load();
        if (applyToAll) {
            return allIcons;
        }
        return ACCENTS.getOrDefault(key(pos), allIcons);
    }

    /** Chooses the icon color for this machine, or for every machine while {@link #applyToAll} is on. */
    static void chooseAccent(BlockPos pos, IconColor color) {
        load();
        if (applyToAll) {
            allIcons = color;
        } else {
            ACCENTS.put(key(pos), color);
        }
        save();
    }

    /**
     * Turns "apply to all" on or off from the machine at {@code pos}. On: that machine's screen and icon colors become
     * those of every machine. Off: that machine keeps the colors it shows (no machine goes back to the defaults). The
     * volume is never shared.
     */
    static void setApplyToAll(BlockPos pos, boolean value) {
        load();
        boolean currentDark = dark(pos);
        IconColor currentIcons = icons(pos);
        applyToAll = value;
        if (value) {
            allDark = currentDark;
            allIcons = currentIcons;
        } else {
            MACHINES.put(key(pos), currentDark);
            ACCENTS.put(key(pos), currentIcons);
        }
        save();
    }

    private static String key(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        String world;
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) {
            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            world = "local:" + root.getFileName();
        } else if (mc.getCurrentServer() != null) {
            world = "server:" + mc.getCurrentServer().ip;
        } else {
            world = "unknown";
        }
        String dimension = mc.level != null ? mc.level.dimension().location().toString() : "unknown";
        return world + "|" + dimension + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            applyToAll = !json.has("applyToAll") || json.get("applyToAll").getAsBoolean();
            allDark = json.has("allDark") && json.get("allDark").getAsBoolean();
            if (json.has("allIcons")) {
                allIcons = IconColor.byId(json.get("allIcons").getAsString());
            } else if (json.has("allPurple") && json.get("allPurple").getAsBoolean()) {
                // Files from before the white icons kept only purple or not; not purple is the default (white) now.
                allIcons = IconColor.PURPLE;
            }
            if (json.has("machines")) {
                json.getAsJsonObject("machines").entrySet().forEach(e -> MACHINES.put(e.getKey(), "dark".equals(e.getValue().getAsString())));
            }
            if (json.has("accents")) {
                json.getAsJsonObject("accents").entrySet().forEach(e -> ACCENTS.put(e.getKey(), IconColor.byId(e.getValue().getAsString())));
            }
            if (json.has("volumes")) {
                json.getAsJsonObject("volumes").entrySet().forEach(e -> VOLUMES.put(e.getKey(), Math.max(0, Math.min(100, e.getValue().getAsInt()))));
            }
        } catch (IOException | RuntimeException e) {
            DimensionalFluxMiner.LOGGER.warn("Could not read {}; the machine screens use the light look", path, e);
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("applyToAll", applyToAll);
        json.addProperty("allDark", allDark);
        json.addProperty("allIcons", allIcons.id());
        JsonObject machines = new JsonObject();
        MACHINES.forEach((key, dark) -> machines.addProperty(key, dark ? "dark" : "light"));
        json.add("machines", machines);
        JsonObject accents = new JsonObject();
        ACCENTS.forEach((key, color) -> accents.addProperty(key, color.id()));
        json.add("accents", accents);
        JsonObject volumes = new JsonObject();
        VOLUMES.forEach(volumes::addProperty);
        json.add("volumes", volumes);
        Path path = path();
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            DimensionalFluxMiner.LOGGER.warn("Could not save {}", path, e);
        }
    }
}
