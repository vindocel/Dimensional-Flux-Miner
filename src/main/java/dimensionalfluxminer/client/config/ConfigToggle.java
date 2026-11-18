package dimensionalfluxminer.client.config;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}

import java.util.function.BooleanSupplier;

/** Keyboard-accessible toggle with a short sliding-knob animation. */
public final class ConfigToggle extends Button {
    private final BooleanSupplier value;
    private boolean last;
    private float from;
    private long changed;

    public ConfigToggle(int x, int y, Component label, BooleanSupplier value, Runnable toggle) {
        super(Button.builder(label, button -> toggle.run()).bounds(x, y, 28, 14));
        this.value = value;
        last = value.getAsBoolean();
        from = last ? 1 : 0;
    }

    @Override
    //? if >=26.1 {
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    *///?}
        boolean current = value.getAsBoolean();
        long now = Util.getMillis();
        float progress = Math.min(1, (now - changed) / 140f);
        float position = from + ((last ? 1 : 0) - from) * progress;
        if (current != last) { from = position; last = current; changed = now; }
        int x = getX(), y = getY();
        ConfigStyle.fill(g, x, y, x + 28, y + 14, 0xFF000000);
        ConfigStyle.fill(g, x + 1, y + 1, x + 27, y + 13, !active ? 0xFF3A3A3A : current ? 0xFF3FA34D : 0xFF5A5A5A);
        int knob = x + 1 + Math.round(16 * position);
        ConfigStyle.fill(g, knob, y + 2, knob + 10, y + 12, !active ? 0xFF8A8A8A : isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFE6E6E6);
    }

    @Override
    public Component getMessage() {
        return Component.empty().append(super.getMessage()).append(": ")
                .append(Component.translatable(value != null && value.getAsBoolean() ? "options.on" : "options.off"));
    }
}
