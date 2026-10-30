package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Tank item: a broken tank keeps its code, shown in the tooltip; placing it reconnects to the same channel. A creative
 * tank item carries its liquid instead, in its name and tooltip.
 * <p>
 * Single and creative tank items are fluid containers of their liquid (the channel's, or the creative one): used on a
 * tank, in any mode, they fill it with as much as fits (or are filled from it). Bucket mode (sneak and use in the air
 * to switch it): the item is not placed, it pours one bucket out, or picks up the liquid source looked at; sneaking
 * always pours.
 */
public class DimensionalTankItem extends BlockItem {
    public DimensionalTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    public TankKind kind() {
        return getBlock() instanceof DimensionalTankBlock block ? block.kind() : TankKind.SINGLE;
    }

    public static boolean bucketMode(ItemStack stack) {
        return stack.getOrDefault(DfmDataComponents.TANK_BUCKET_MODE.get(), false);
    }

    /**
     * The item's fluid container (single and creative tanks, whatever the mode): used on a tank it fills it. Only
     * pouring on the ground and picking up sources need bucket mode.
     */
    public static IFluidHandlerItem fluidHandler(ItemStack stack) {
        return stack.getItem() instanceof DimensionalTankItem item && item.kind() != TankKind.DUAL
                ? new TankItemFluidHandler(stack, item.kind()) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind() == TankKind.DUAL) {
            return super.use(level, player, hand);
        }
        if (player.isSecondaryUseActive() && getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() == HitResult.Type.MISS) {
            if (!level.isClientSide) {
                boolean on = !bucketMode(stack);
                if (on) {
                    stack.set(DfmDataComponents.TANK_BUCKET_MODE.get(), true);
                } else {
                    stack.remove(DfmDataComponents.TANK_BUCKET_MODE.get());
                }
                player.displayClientMessage(Component.translatable(on ? "message.dimensionalfluxminer.tank.bucket_mode.on"
                        : "message.dimensionalfluxminer.tank.bucket_mode.off"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (bucketMode(stack)) {
            return new InteractionResultHolder<>(bucket(level, player, hand, stack), stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (kind() != TankKind.DUAL && bucketMode(stack) && context.getPlayer() != null) {
            return bucket(context.getLevel(), context.getPlayer(), context.getHand(), stack);
        }
        return super.useOn(context);
    }

    /** Bucket mode: picks up the liquid source looked at (not while sneaking), else pours one bucket out. */
    private InteractionResult bucket(Level level, Player player, InteractionHand hand, ItemStack stack) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(side), side, stack)) {
            return InteractionResult.FAIL;
        }
        if (!player.isSecondaryUseActive() && level.getFluidState(pos).isSource()) {
            FluidActionResult picked = FluidUtil.tryPickUpFluid(stack.copyWithCount(1), player, level, pos, side);
            if (picked.isSuccess()) {
                // Only an empty creative tank changes: it keeps the liquid it picked up.
                SimpleFluidContent content = picked.getResult().get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
                if (content != null) {
                    stack.set(DfmDataComponents.CREATIVE_TANK_FLUID.get(), content);
                }
                return InteractionResult.CONSUME;
            }
        }
        IFluidHandlerItem handler = new TankItemFluidHandler(stack, kind());
        FluidStack held = handler.getFluidInTank(0);
        if (held.isEmpty()) {
            return InteractionResult.FAIL;
        }
        FluidStack bucket = held.copyWithAmount(FluidType.BUCKET_VOLUME);
        // Into the clicked block when it can hold the liquid (waterlogging), otherwise beside it.
        if (level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer
                && FluidUtil.tryPlaceFluid(player, level, hand, pos, handler, bucket)) {
            return InteractionResult.CONSUME;
        }
        return FluidUtil.tryPlaceFluid(player, level, hand, pos.relative(side), handler, bucket) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {
        net.neoforged.neoforge.fluids.SimpleFluidContent fluid = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        if (fluid != null && !fluid.isEmpty()) {
            return Component.translatable(getDescriptionId(stack) + ".filled", fluid.copy().getHoverName());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (kind() != TankKind.DUAL) {
            tooltip.add(Component.translatable(bucketMode(stack) ? "tooltip.dimensionalfluxminer.tank.bucket_mode.on"
                    : "tooltip.dimensionalfluxminer.tank.bucket_mode.off").withStyle(bucketMode(stack) ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.dimensionalfluxminer.tank.bucket_mode.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (getBlock() instanceof DimensionalTankBlock block && block.kind().creative()) {
            net.neoforged.neoforge.fluids.SimpleFluidContent fluid = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
            tooltip.add(fluid == null || fluid.isEmpty()
                    ? Component.translatable("tooltip.dimensionalfluxminer.creative_tank.empty").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("tooltip.dimensionalfluxminer.creative_tank.fluid", fluid.copy().getHoverName())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.dimensionalfluxminer.creative_tank.no_channels").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        TankCode code = stack.get(DfmDataComponents.TANK_CODE.get());
        if (code != null) {
            tooltip.add(Component.translatable("tooltip.dimensionalfluxminer.tank.code", Component.translatable(
                    "gui.dimensionalfluxminer.tank.code.colors", name(code.first()), name(code.second()), name(code.third())))
                    .withStyle(ChatFormatting.GRAY));
            code.ownerName().ifPresent(owner -> tooltip.add(Component.translatable("gui.dimensionalfluxminer.tank.personal.owner", owner)
                    .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    private static Component name(DyeColor color) {
        return Component.translatable("color.minecraft." + color.getName()).withColor(color.getTextureDiffuseColor() & 0xFFFFFF);
    }
}
