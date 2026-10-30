package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Creative Dimensional Tank item: the tank model with the liquid it holds showing through the glass. */
public class CreativeTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public CreativeTankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        SimpleFluidContent fluid = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        // The liquid first: the glass is drawn after it in the same translucent buffer.
        if (fluid != null && !fluid.isEmpty()) {
            TankRenderer.renderCreativeItem(fluid.copy(), pose, buffers, light);
        }
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get().defaultBlockState(), pose, buffers,
                light, overlay, ModelData.EMPTY, null);
    }
}
