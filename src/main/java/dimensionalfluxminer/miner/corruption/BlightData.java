package dimensionalfluxminer.miner.corruption;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=26.1 {
import net.minecraft.world.level.saveddata.SavedDataType;
import dimensionalfluxminer.content.TagSavedData;
//?}

/**
 * Per-level cache of corrupted surface columns, grouped by chunk. The Blight Spawn Manager reads it instead of
 * rescanning terrain; stale entries are dropped lazily when a candidate is found no longer corrupted.
 */
public final class BlightData extends SavedData {
    //? if >=26.1 {
    private static final String LEGACY_NAME = "dimensionalfluxminer_blight";
    private static final SavedDataType<BlightData> TYPE = TagSavedData.type(dimensionalfluxminer.DimensionalFluxMiner.id("blight"),
            BlightData::new, BlightData::load, (data, registries) -> data.save(new CompoundTag(), registries));
    //?} else {
    /*private static final String NAME = "dimensionalfluxminer_blight";
    *///?}
    private final Long2ObjectOpenHashMap<LongOpenHashSet> columnsByChunk = new Long2ObjectOpenHashMap<>();

    public static BlightData get(ServerLevel level) {
        //? if >=26.1 {
        return TagSavedData.get(level, TYPE, LEGACY_NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(new Factory<>(BlightData::new, BlightData::load, null), NAME);
        *///?}
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    public void markCorrupted(int x, int z) {
        //? if >=26.1 {
        long chunk = ChunkPos.pack(x >> 4, z >> 4);
        //?} else {
        /*long chunk = ChunkPos.asLong(x >> 4, z >> 4);
        *///?}
        if (columnsByChunk.computeIfAbsent(chunk, k -> new LongOpenHashSet()).add(pack(x, z))) {
            setDirty();
        }
    }

    public void remove(int x, int z) {
        //? if >=26.1 {
        long chunk = ChunkPos.pack(x >> 4, z >> 4);
        //?} else {
        /*long chunk = ChunkPos.asLong(x >> 4, z >> 4);
        *///?}
        LongOpenHashSet set = columnsByChunk.get(chunk);
        if (set != null && set.remove(pack(x, z))) {
            if (set.isEmpty()) {
                columnsByChunk.remove(chunk);
            }
            setDirty();
        }
    }

    public int count(long chunk) {
        LongOpenHashSet set = columnsByChunk.get(chunk);
        return set == null ? 0 : set.size();
    }

    public LongArrayList columns(long chunk) {
        LongOpenHashSet set = columnsByChunk.get(chunk);
        return set == null ? new LongArrayList() : new LongArrayList(set);
    }

    public List<Long> chunks() {
        return new ArrayList<>(columnsByChunk.keySet());
    }

    //? if >=26.1 {
    private CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    //?} else {
    /*@Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    *///?}
        ListTag list = new ListTag();
        columnsByChunk.forEach((chunk, set) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("chunk", chunk);
            entry.put("columns", new LongArrayTag(set.toLongArray()));
            list.add(entry);
        });
        tag.put("chunks", list);
        return tag;
    }

    private static BlightData load(CompoundTag tag, HolderLookup.Provider registries) {
        BlightData data = new BlightData();
        //? if >=26.1 {
        ListTag list = tag.getListOrEmpty("chunks");
        //?} else {
        /*ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
        *///?}
        for (int i = 0; i < list.size(); i++) {
            //? if >=26.1 {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            LongOpenHashSet set = new LongOpenHashSet(entry.getLongArray("columns").orElse(new long[0]));
            //?} else {
            /*CompoundTag entry = list.getCompound(i);
            LongOpenHashSet set = new LongOpenHashSet(entry.getLongArray("columns"));
            *///?}
            if (!set.isEmpty()) {
                //? if >=26.1 {
                data.columnsByChunk.put(entry.getLongOr("chunk", 0L), set);
                //?} else {
                /*data.columnsByChunk.put(entry.getLong("chunk"), set);
                *///?}
            }
        }
        return data;
    }
}
