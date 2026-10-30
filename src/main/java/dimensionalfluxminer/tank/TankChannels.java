package dimensionalfluxminer.tank;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Every Dimensional Tank channel of the save, kept with the Overworld so all dimensions see the same one. The liquid
 * lives here, not in the tanks: a tank only points at its channel, so a channel keeps its content while no tank of it
 * is loaded, when a tank is broken or when a tank switches to another code.
 */
public final class TankChannels extends SavedData {
    private static final String NAME = "dimensionalfluxminer_tank_channels";

    private final Map<String, Channel> channels = new HashMap<>();

    public static TankChannels get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TankChannels::new, TankChannels::load, null), NAME);
    }

    /** The channel of {@code kind} and {@code code}, created empty the first time. */
    public Channel channel(TankKind kind, TankCode code) {
        return channels.computeIfAbsent(code.key(kind), key -> new Channel(this, kind, code));
    }

    /** Chambers of one channel: a single tank has one, a dual tank two (A = 0, B = 1). */
    public static final class Channel {
        private final TankChannels owner;
        private final TankKind kind;
        private final TankCode code;
        private final FluidStack[] chambers;
        /** Bumped on every change, so the tanks of the channel know when to update their clients. */
        private int version;

        private Channel(TankChannels owner, TankKind kind, TankCode code) {
            this.owner = owner;
            this.kind = kind;
            this.code = code;
            this.chambers = new FluidStack[kind.chambers()];
            java.util.Arrays.fill(chambers, FluidStack.EMPTY);
        }

        public TankKind kind() {
            return kind;
        }

        public TankCode code() {
            return code;
        }

        public int chambers() {
            return chambers.length;
        }

        /** The stored stack itself; never modify it, use {@link #set}. */
        public FluidStack get(int chamber) {
            return chambers[chamber];
        }

        public void set(int chamber, FluidStack stack) {
            chambers[chamber] = stack.isEmpty() ? FluidStack.EMPTY : stack;
            version++;
            owner.setDirty();
        }

        public int version() {
            return version;
        }

        boolean isEmpty() {
            for (FluidStack stack : chambers) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Channel channel : channels.values()) {
            // An empty channel is the same as one never used.
            if (channel.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("kind", channel.kind.id());
            entry.put("code", channel.code.save());
            ListTag chambers = new ListTag();
            for (FluidStack stack : channel.chambers) {
                chambers.add(stack.saveOptional(registries));
            }
            entry.put("chambers", chambers);
            list.add(entry);
        }
        tag.put("channels", list);
        return tag;
    }

    private static TankChannels load(CompoundTag tag, HolderLookup.Provider registries) {
        TankChannels data = new TankChannels();
        ListTag list = tag.getList("channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            TankKind kind = TankKind.byId(entry.getString("kind"));
            if (kind == null || kind.creative()) {
                continue;
            }
            Channel channel = data.channel(kind, TankCode.load(entry.getCompound("code")));
            ListTag chambers = entry.getList("chambers", Tag.TAG_COMPOUND);
            for (int c = 0; c < channel.chambers.length && c < chambers.size(); c++) {
                channel.chambers[c] = FluidStack.parseOptional(registries, chambers.getCompound(c));
            }
        }
        return data;
    }
}
