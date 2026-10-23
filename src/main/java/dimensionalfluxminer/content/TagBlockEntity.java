package dimensionalfluxminer.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Block entity whose data is written and read as one NBT compound, key by key at the top level, so saves keep exactly
 * the keys and layout of earlier versions. Subclasses implement {@link #saveTag} / {@link #loadTag}, and
 * {@link #readUpdateTag} for what clients receive.
 */
public abstract class TagBlockEntity extends BlockEntity {
    protected TagBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void saveTag(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
    }

    /** Chunk update tag and block entity packet, on the client. */
    protected void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadTag(tag, registries);
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = new CompoundTag();
        saveTag(tag, registries());
        write(output, tag);
    }

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadTag(read(input), input.lookup());
    }

    /** Breaking or unloading the block also drops what an optional energy network attached to it. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        dimensionalfluxminer.compat.Ae2Compat.release(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        dimensionalfluxminer.compat.Ae2Compat.release(this);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        readUpdateTag(read(input), input.lookup());
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        readUpdateTag(read(input), input.lookup());
    }

    protected HolderLookup.Provider registries() {
        if (level != null) {
            return level.registryAccess();
        }
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }

    /** Every top-level key of the input as one compound. */
    public static CompoundTag read(ValueInput input) {
        CompoundTag tag = new CompoundTag();
        for (String key : input.keySet()) {
            input.read(key, ExtraCodecs.NBT).ifPresent(value -> tag.put(key, value));
        }
        return tag;
    }

    /** Writes every key of {@code tag} at the top level of the output. */
    public static void write(ValueOutput output, CompoundTag tag) {
        for (String key : tag.keySet()) {
            output.store(key, ExtraCodecs.NBT, tag.get(key));
        }
    }
}
