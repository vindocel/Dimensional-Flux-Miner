package dimensionalfluxminer.tank;

import dimensionalfluxminer.registry.DfmDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Tank item: a broken tank keeps its code, shown in the tooltip; placing it reconnects to the same channel. */
public class DimensionalTankItem extends BlockItem {
    public DimensionalTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        TankCode code = stack.get(DfmDataComponents.TANK_CODE.get());
        if (code != null) {
            tooltip.accept(Component.translatable("tooltip.dimensionalfluxminer.tank.code", Component.translatable(
                    "gui.dimensionalfluxminer.tank.code.colors", name(code.first()), name(code.second()), name(code.third())))
                    .withStyle(ChatFormatting.GRAY));
            code.ownerName().ifPresent(owner -> tooltip.accept(Component.translatable("gui.dimensionalfluxminer.tank.personal.owner", owner)
                    .withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    private static Component name(DyeColor color) {
        return Component.translatable("color.minecraft." + color.getName()).withColor(color.getTextureDiffuseColor() & 0xFFFFFF);
    }
}
