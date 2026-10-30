package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Creative Dimensional Tank item ({@code items/creative_dimensional_tank.json}): the liquid it holds, then the tank model
 * over it, so the liquid shows through the glass.
 */
public class CreativeTankItemRenderer implements SpecialModelRenderer<FluidStack> {
    public static final Identifier ID = DimensionalFluxMiner.id("creative_tank");

    @Override
    public @Nullable FluidStack extractArgument(ItemStack stack) {
        SimpleFluidContent fluid = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        return fluid == null || fluid.isEmpty() ? null : fluid.copy();
    }

    @Override
    public void submit(@Nullable FluidStack fluid, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                       int outlineColor) {
        if (fluid != null) {
            TankRenderer.submitCreativeItem(fluid, pose, collector, light);
        }
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                .get(DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get().defaultBlockState());
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        collector.submitMultiLayerBlockModel(pose, parts, model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT),
                BlockModelRenderState.EMPTY_TINTS, light, overlay, outlineColor);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int corner = 0; corner < 8; corner++) {
            output.accept(new Vector3f(corner & 1, (corner >> 1) & 1, (corner >> 2) & 1));
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<FluidStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<FluidStack> bake(SpecialModelRenderer.BakingContext context) {
            return new CreativeTankItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
