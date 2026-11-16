package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
//? if >=26.1 {
import com.mojang.serialization.MapCodec;
import dimensionalfluxminer.DimensionalFluxMiner;
//?}
import dimensionalfluxminer.registry.DfmBlocks;
import dimensionalfluxminer.registry.DfmDataComponents;
import dimensionalfluxminer.tank.DimensionalTankItem;
import dimensionalfluxminer.tank.TankCode;
import dimensionalfluxminer.tank.TankItemDisplay;
import dimensionalfluxminer.tank.TankKind;
//? if >=26.1 {
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
//?}
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
//?} else {
/*import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
*///?}
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
/*import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.data.ModelData;
*///?}
import net.neoforged.neoforge.fluids.FluidStack;
//? if >=26.1 {
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
//?} else {

/*/^* Every tank item uses the block's dynamic contents, channel indicators and actual fill height, then the tank model over them. ^/
public class TankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public TankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
*///?}
    }

    @Override
    //? if >=26.1 {
    public void submit(@Nullable Data data, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil,
                       int outlineColor) {
        if (data == null) return;
        TankRenderer.submitItem(data.kind(), data.chambers(), data.code(), data.locked(), pose, collector, light);
        var block = switch (data.kind()) {
    //?} else {
    /*public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof DimensionalTankItem item)) {
            return;
        }
        TankKind kind = item.kind();
        FluidStack[] fluids = new FluidStack[kind.chambers()];
        for (int i = 0; i < fluids.length; i++) {
            fluids[i] = TankItemDisplay.fluid(stack, kind, i);
        }
        TankCode code = kind.creative() ? null : stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT);
        // The liquid first: the glass is drawn after it in the same translucent buffer.
        TankRenderer.renderItem(kind, fluids, code, !kind.creative() && DimensionalTankItem.channelLocked(stack), pose, buffers, light);
        Block block = switch (kind) {
    *///?}
            case SINGLE -> DfmBlocks.DIMENSIONAL_TANK.get();
            case DUAL -> DfmBlocks.DIMENSIONAL_DUAL_TANK.get();
            case CREATIVE -> DfmBlocks.CREATIVE_DIMENSIONAL_TANK.get();
        };
        //? if >=26.1 {
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
        //?} else {
        /*Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), pose, buffers, light, overlay,
                ModelData.EMPTY, null);
        *///?}
    }
}
