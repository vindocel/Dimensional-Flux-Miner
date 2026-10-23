package dimensionalfluxminer.content;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Saved data kept as one NBT compound, as in earlier versions. Files of earlier versions sit at
 * {@code data/<legacy name>.dat}; the first time a level asks for its data, such a file is moved to the current place
 * ({@code data/<namespace>/<path>.dat}).
 */
public final class TagSavedData {
    private TagSavedData() {
    }

    public static <T extends SavedData> SavedDataType<T> type(Identifier id, Supplier<T> constructor,
                                                              BiFunction<CompoundTag, HolderLookup.Provider, T> load,
                                                              BiFunction<T, HolderLookup.Provider, CompoundTag> save) {
        return new SavedDataType<>(id, level -> constructor.get(),
                level -> CompoundTag.CODEC.xmap(tag -> load.apply(tag, registries(level)), data -> save.apply(data, registries(level))));
    }

    private static HolderLookup.Provider registries(ServerLevel level) {
        if (level == null) {
            throw new IllegalStateException("level saved data read outside a level");
        }
        return level.registryAccess();
    }

    /** Storages (one per level and save) already checked for files of earlier versions, by data id. */
    private static final Map<SavedDataStorage, Set<Identifier>> CHECKED = new WeakHashMap<>();

    public static <T extends SavedData> T get(ServerLevel level, SavedDataType<T> type, String legacyName) {
        SavedDataStorage storage = level.getDataStorage();
        boolean first;
        synchronized (CHECKED) {
            first = CHECKED.computeIfAbsent(storage, s -> new HashSet<>()).add(type.id());
        }
        if (first) {
            migrate(level, type.id(), legacyName);
        }
        return storage.computeIfAbsent(type);
    }

    private static void migrate(ServerLevel level, Identifier id, String legacyName) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path folder = DimensionType.getStorageFolder(level.dimension(), root).resolve("data");
        Path target = id.withSuffix(".dat").resolveAgainst(folder);
        if (Files.exists(target)) {
            return;
        }
        List<Path> candidates = level.dimension() == Level.OVERWORLD
                ? List.of(folder.resolve(legacyName + ".dat"), root.resolve("data").resolve(legacyName + ".dat"))
                : List.of(folder.resolve(legacyName + ".dat"));
        for (Path legacy : candidates) {
            if (Files.exists(legacy)) {
                try {
                    Files.createDirectories(target.getParent());
                    Files.move(legacy, target);
                } catch (IOException e) {
                    DimensionalFluxMiner.LOGGER.error("Could not move {} to {}", legacy, target, e);
                }
                return;
            }
        }
    }
}
