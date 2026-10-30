package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmDataComponents;
import dimensionalfluxminer.registry.DfmItems;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Creative tank items: one already holding each liquid of the game, for the creative tab and the recipe viewers. */
public final class CreativeTanks {
    private CreativeTanks() {
    }

    /** A creative tank item holding {@code fluid}. */
    public static ItemStack filled(Fluid fluid) {
        ItemStack stack = new ItemStack(DfmItems.CREATIVE_DIMENSIONAL_TANK.get());
        stack.set(DfmDataComponents.CREATIVE_TANK_FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(fluid, 1)));
        return stack;
    }

    /** One filled tank per source liquid, leaving out the ones hidden from recipe viewers. */
    public static List<ItemStack> filledForEveryFluid(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.FLUID).listElements()
                .filter(holder -> holder.value() != Fluids.EMPTY && holder.value().isSource(holder.value().defaultFluidState()))
                .filter(holder -> !holder.is(Tags.Fluids.HIDDEN_FROM_RECIPE_VIEWERS))
                .map(Holder::value)
                .map(CreativeTanks::filled)
                .toList();
    }
}
