package dimensionalfluxminer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dimensionalfluxminer.client.screen.ScreenPaletteGuard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class ScreenPaletteMixin {
    @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
    private void dimensionalfluxminer$preservePalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                     float partialTick, Operation<Void> original) {
        ScreenPaletteGuard.draw((Screen) (Object) this, () -> original.call(graphics, mouseX, mouseY, partialTick));
    }
}
