package dimensionalfluxminer.machine.transfer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Item and fluid stacks as NBT ({@code {id, count, components}} / {@code {id, amount, components}}), the format the
 * saves already use. An empty stack is an empty tag.
 */
public final class TagStacks {
    private TagStacks() {
    }

    public static CompoundTag save(HolderLookup.Provider registries, ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        Tag tag = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    public static ItemStack parse(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }

    public static CompoundTag save(HolderLookup.Provider registries, FluidStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        Tag tag = FluidStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    public static FluidStack parseFluid(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return FluidStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(FluidStack.EMPTY);
    }
}
