package dimensionalfluxminer.menu;

import net.minecraft.nbt.CompoundTag;

/** A menu that receives display snapshots from the server. */
public interface SyncedMenu {
    void applySync(CompoundTag data);

    CompoundTag data();
}
