package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmDataComponents;
import dimensionalfluxminer.tank.DimensionalTankItem;
import dimensionalfluxminer.tank.TankCode;
import dimensionalfluxminer.tank.TankItemDisplay;
import dimensionalfluxminer.tank.TankKind;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Every tank item uses the block's dynamic contents, channel indicators and actual fill height.
 */
public class TankItemRenderer implements SpecialModelRenderer<TankItemRenderer.Data> {
    /** Retain the existing renderer ID for resource-pack compatibility. */
    public static final Identifier ID = DimensionalFluxMiner.id("creative_tank");

    public record Data(TankKind kind, FluidStack[] chambers, TankCode code, boolean locked) { }

    @Override
    public @Nullable Data extractArgument(ItemStack stack) {
        if (!(stack.getItem() instanceof DimensionalTankItem item)) return null;
        TankKind kind = item.kind();
        FluidStack[] fluids = new FluidStack[kind.chambers()];
        for (int i = 0; i < fluids.length; i++) fluids[i] = TankItemDisplay.fluid(stack, kind, i);
        return new Data(kind, fluids, kind.creative() ? null : stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT),
                !kind.creative() && dimensionalfluxminer.tank.DimensionalTankItem.channelLocked(stack));
    }

    @Override
    public void submit(@Nullable Data data, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                       int outlineColor) {
        if (data == null) return;
        TankRenderer.submitItem(data.kind(), data.chambers(), data.code(), data.locked(), pose, collector, light);
        var block = switch (data.kind()) {
            case SINGLE -> DfmBlocks.DIMENSIONAL_TANK.get();
            case DUAL -> DfmBlocks.DIMENSIONAL_DUAL_TANK.get();
            case CREATIVE -> DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get();
        };
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                .get(block.defaultBlockState());
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

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Data> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Data> bake(SpecialModelRenderer.BakingContext context) {
            return new TankItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
