package dimensionalfluxminer.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** The still texture and tint of a fluid, from its fluid model, for screens and renderers. */
public final class FluidSprites {
    private FluidSprites() {
    }

    private static FluidModel model(Fluid fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    }

    public static TextureAtlasSprite still(Fluid fluid) {
        return model(fluid).stillMaterial().sprite();
    }

    /** ARGB tint of the fluid (white when it has none). */
    public static int tint(FluidStack stack) {
        FluidModel model = model(stack.getFluid());
        return model.fluidTintSource() != null ? model.fluidTintSource().colorAsStack(stack) : 0xFFFFFFFF;
    }

    public static int tint(Fluid fluid) {
        return tint(new FluidStack(fluid, 1000));
    }
}
