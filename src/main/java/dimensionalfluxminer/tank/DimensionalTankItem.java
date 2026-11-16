package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmDataComponents;
//? if >=26.1 {
import java.util.function.Consumer;
//?} else {
/*import java.util.List;
*///?}
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <26.1 {
/*import net.minecraft.world.InteractionResultHolder;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
//? if <26.1 {
/*import net.neoforged.neoforge.fluids.FluidActionResult;
*///?}
import net.neoforged.neoforge.fluids.FluidStack;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?}

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
    //? if >=26.1 {
    public static ResourceHandler<FluidResource> fluidHandler(ItemAccess access) {
        return access.getResource().getItem() instanceof DimensionalTankItem item && item.kind() != TankKind.DUAL
                ? TankItemFluidHandler.forAccess(access, item.kind()) : null;
    //?} else {
    /*public static IFluidHandlerItem fluidHandler(ItemStack stack) {
        return stack.getItem() instanceof DimensionalTankItem item && item.kind() != TankKind.DUAL
                ? new TankItemFluidHandler(stack, item.kind()) : null;
    *///?}
    }

    @Override
    //? if >=26.1 {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //?} else {
    /*public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    *///?}
        ItemStack stack = player.getItemInHand(hand);
        if (kind() == TankKind.DUAL) {
            return super.use(level, player, hand);
        }
        if (player.isSecondaryUseActive() && getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() == HitResult.Type.MISS) {
            //? if >=26.1 {
            if (!level.isClientSide()) {
            //?} else {
            /*if (!level.isClientSide) {
            *///?}
                boolean on = !bucketMode(stack);
                if (on) {
                    stack.set(DfmDataComponents.TANK_BUCKET_MODE.get(), true);
                } else {
                    stack.remove(DfmDataComponents.TANK_BUCKET_MODE.get());
                }
                //? if >=26.1 {
                player.sendOverlayMessage(Component.translatable(on ? "message.dimensionalfluxminer.tank.bucket_mode.on"
                        : "message.dimensionalfluxminer.tank.bucket_mode.off"));
            }
            return InteractionResult.SUCCESS;
                //?} else {
                /*player.displayClientMessage(Component.translatable(on ? "message.dimensionalfluxminer.tank.bucket_mode.on"
                        : "message.dimensionalfluxminer.tank.bucket_mode.off"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                *///?}
        }
        if (bucketMode(stack)) {
            //? if >=26.1 {
            return bucket(level, player, hand, stack);
            //?} else {
            /*return new InteractionResultHolder<>(bucket(level, player, hand, stack), stack);
            *///?}
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
        //? if >=26.1 {
        if (level.isClientSide()) {
        //?} else {
        /*if (level.isClientSide) {
        *///?}
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getDirection();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(side), side, stack)) {
            return InteractionResult.FAIL;
        }
        //? if >=26.1 {
        // The item in the hand: an empty creative tank picking up a liquid is exchanged for one that carries it.
        ResourceHandler<FluidResource> handler = TankItemFluidHandler.forAccess(ItemAccess.forPlayerInteraction(player, hand), kind());
        if (handler == null) {
        //?} else {
        /*if (!player.isSecondaryUseActive() && level.getFluidState(pos).isSource()) {
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
        *///?}
            return InteractionResult.FAIL;
        }
        //? if >=26.1 {
        if (!player.isSecondaryUseActive() && level.getFluidState(pos).isSource()
                && !FluidUtil.tryPickupFluid(handler, player, level, pos, side, null).isEmpty()) {
        //?} else {
        /*FluidStack bucket = held.copyWithAmount(FluidType.BUCKET_VOLUME);
        // Into the clicked block when it can hold the liquid (waterlogging), otherwise beside it.
        if (level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer
                && FluidUtil.tryPlaceFluid(player, level, hand, pos, handler, bucket)) {
        *///?}
            return InteractionResult.CONSUME;
        }
        //? if >=26.1 {
        if (FluidUtil.getStack(handler, 0).isEmpty()) {
            return InteractionResult.FAIL;
        }
        // Into the clicked block when it can hold the liquid (waterlogging), otherwise beside it.
        if (level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer
                && !FluidUtil.tryPlaceFluid(handler, player, level, pos, true, null).isEmpty()) {
            return InteractionResult.CONSUME;
        }
        return !FluidUtil.tryPlaceFluid(handler, player, level, pos.relative(side), true, null).isEmpty()
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
        //?} else {
        /*return FluidUtil.tryPlaceFluid(player, level, hand, pos.relative(side), handler, bucket) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        *///?}
    }

    @Override
    public Component getName(ItemStack stack) {
        net.neoforged.neoforge.fluids.SimpleFluidContent fluid = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
        if (fluid != null && !fluid.isEmpty()) {
            //? if >=26.1 {
            return Component.translatable(getDescriptionId() + ".filled", fluid.copy().getHoverName());
            //?} else {
            /*return Component.translatable(getDescriptionId(stack) + ".filled", fluid.copy().getHoverName());
            *///?}
        }
        return super.getName(stack);
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    *///?}
        if (!kind().creative()) {
            for (var component : java.util.List.of(DfmDataComponents.TANK_LOCAL_A.get(), DfmDataComponents.TANK_LOCAL_B.get())) {
                localFluidLine(stack, component, tooltip);
            }
        }
        if (kind() != TankKind.DUAL) {
            //? if >=26.1 {
            tooltip.accept(Component.translatable(bucketMode(stack) ? "tooltip.dimensionalfluxminer.tank.bucket_mode.on"
            //?} else {
            /*tooltip.add(Component.translatable(bucketMode(stack) ? "tooltip.dimensionalfluxminer.tank.bucket_mode.on"
            *///?}
                    : "tooltip.dimensionalfluxminer.tank.bucket_mode.off").withStyle(bucketMode(stack) ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            //? if >=26.1 {
            tooltip.accept(Component.translatable("tooltip.dimensionalfluxminer.tank.bucket_mode.hint").withStyle(ChatFormatting.DARK_GRAY));
            //?} else {
            /*tooltip.add(Component.translatable("tooltip.dimensionalfluxminer.tank.bucket_mode.hint").withStyle(ChatFormatting.DARK_GRAY));
            *///?}
        }
        if (getBlock() instanceof DimensionalTankBlock block && block.kind().creative()) {
            net.neoforged.neoforge.fluids.SimpleFluidContent fluid = stack.get(DfmDataComponents.CREATIVE_TANK_FLUID.get());
            //? if >=26.1 {
            tooltip.accept(fluid == null || fluid.isEmpty()
            //?} else {
            /*tooltip.add(fluid == null || fluid.isEmpty()
            *///?}
                    ? Component.translatable("tooltip.dimensionalfluxminer.creative_tank.empty").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("tooltip.dimensionalfluxminer.creative_tank.fluid", fluid.copy().getHoverName())
                    .withStyle(ChatFormatting.GRAY));
            //? if >=26.1 {
            tooltip.accept(Component.translatable("tooltip.dimensionalfluxminer.creative_tank.no_channels").withStyle(ChatFormatting.DARK_GRAY));
            //?} else {
            /*tooltip.add(Component.translatable("tooltip.dimensionalfluxminer.creative_tank.no_channels").withStyle(ChatFormatting.DARK_GRAY));
            *///?}
            return;
        }
        if (channelLocked(stack)) {
            //? if >=26.1 {
            tooltip.accept(Component.translatable("gui.dimensionalfluxminer.tank.channel_locked").withStyle(ChatFormatting.GRAY));
            //?} else {
            /*tooltip.add(Component.translatable("gui.dimensionalfluxminer.tank.channel_locked").withStyle(ChatFormatting.GRAY));
            *///?}
            return;
        }
        TankCode code = stack.getOrDefault(DfmDataComponents.TANK_CODE.get(), TankCode.DEFAULT);
        //? if >=26.1 {
        tooltip.accept(Component.translatable("gui.dimensionalfluxminer.tank.channel_open", Component.translatable(
        //?} else {
        /*tooltip.add(Component.translatable("gui.dimensionalfluxminer.tank.channel_open", Component.translatable(
        *///?}
                "gui.dimensionalfluxminer.tank.code.colors", name(code.first()), name(code.second()), name(code.third())))
                .withStyle(ChatFormatting.GRAY));
        //? if >=26.1 {
        code.ownerName().ifPresent(owner -> tooltip.accept(Component.translatable("gui.dimensionalfluxminer.tank.personal.owner", owner)
        //?} else {
        /*code.ownerName().ifPresent(owner -> tooltip.add(Component.translatable("gui.dimensionalfluxminer.tank.personal.owner", owner)
        *///?}
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    /**
     * Whether the tank item keeps its liquid to itself (channel locked). A new tank, never placed and so without a
     * code, starts locked; a tank broken from the world carries its code and its own state.
     */
    public static boolean channelLocked(ItemStack stack) {
        Boolean locked = stack.get(DfmDataComponents.TANK_CHANNEL_LOCKED.get());
        return locked != null ? locked : !stack.has(DfmDataComponents.TANK_CODE.get());
    }

    private static void localFluidLine(ItemStack stack,
            //? if >=26.1 {
            net.minecraft.core.component.DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent> component,
            Consumer<Component> tooltip) {
            //?} else {
            /*net.minecraft.core.component.DataComponentType<SimpleFluidContent> component, List<Component> tooltip) {
            *///?}
        var content = stack.get(component);
        if (content != null && !content.isEmpty()) {
            //? if >=26.1 {
            tooltip.accept(Component.translatable("tooltip.dimensionalfluxminer.tank.local_fluid", content.copy().getHoverName(), content.copy().getAmount())
            //?} else {
            /*tooltip.add(Component.translatable("tooltip.dimensionalfluxminer.tank.local_fluid", content.copy().getHoverName(), content.copy().getAmount())
            *///?}
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component name(DyeColor color) {
        return Component.translatable("color.minecraft." + color.getName()).withColor(color.getTextureDiffuseColor() & 0xFFFFFF);
    }
}
