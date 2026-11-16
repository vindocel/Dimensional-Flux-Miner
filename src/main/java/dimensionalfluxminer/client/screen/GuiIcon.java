package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * One source image per icon. The selected palette colors neutral pixels during drawing and preserves colored details.
 * Furnace display accents are composed from separate layers in the same pixel coordinates.
 */
enum GuiIcon {
    AUTO_EJECT,
    AUTO_INJECT,
    CLEAR_SIDES,
    CLOSE,
    ENERGY,
    FILTER,
    FLUIDS,
    ITEMS,
    LOCK,
    LOCK_OPEN,
    MINUS,
    PLUS,
    POWER_OFF,
    POWER_ON,
    REDSTONE_LOW,
    REDSTONE_OFF,
    REDSTONE_ON,
    /** Drawn 1 px lower: the drawing sits high in its image. */
    SETTINGS(1),
    SIDE_CONFIG,
    SMELTING,
    SMELTING_ALLOY,
    SMELTING_ALLOY_FURNACE,
    SORTING,
    STATS,
    /** Drawn 1 px lower: the drawing sits high in its image. */
    UPGRADE(1);

    /** Pixels the icon is drawn below the center. */
    private final int nudgeY;

    GuiIcon() {
        this(0);
    }

    GuiIcon(int nudgeY) {
        this.nudgeY = nudgeY;
    }

    //? if >=26.1 {
    private final Identifier sprite = DimensionalFluxMiner.id("icon_white/" + name().toLowerCase(java.util.Locale.ROOT));
    private static final Identifier TERMINAL = DimensionalFluxMiner.id("icon_white/smelting_alloy_terminal");
    private static final Identifier SEPARATION = DimensionalFluxMiner.id("icon_white/smelting_alloy_furnace_separation");
    //?} else {
    /*private final ResourceLocation sprite = DimensionalFluxMiner.id("icon_white/" + name().toLowerCase(java.util.Locale.ROOT));
    private static final ResourceLocation TERMINAL = DimensionalFluxMiner.id("icon_white/smelting_alloy_terminal");
    private static final ResourceLocation SEPARATION = DimensionalFluxMiner.id("icon_white/smelting_alloy_furnace_separation");
    *///?}

    /** Draws the icon centered in a {@code w} x {@code h} area at (x, y), shrunk only when it does not fit. */
    //? if >=26.1 {
    void drawCentered(GuiGraphicsExtractor g, int x, int y, int w, int h) {
    //?} else {
    /*void drawCentered(GuiGraphics g, int x, int y, int w, int h) {
    *///?}
        drawCentered(g, x, y, w, h, false);
    }

    /** Selected buttons use a darker foreground; colored artwork details retain their native colors. */
    //? if >=26.1 {
    void drawCentered(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected) {
        TextureAtlasSprite atlasSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.GUI).getSprite(sprite);
    //?} else {
    /*void drawCentered(GuiGraphics g, int x, int y, int w, int h, boolean selected) {
        TextureAtlasSprite atlasSprite = Minecraft.getInstance().getGuiSprites().getSprite(sprite);
    *///?}
        int width = atlasSprite.contents().width();
        int height = atlasSprite.contents().height();
        float scale = Math.min(1.0F, Math.min(w / (float) width, h / (float) height));
        //? if >=26.1 {
        g.pose().pushMatrix();
        g.pose().translate(x + (w - width * scale) / 2.0F, y + nudgeY + (h - height * scale) / 2.0F);
        g.pose().scale(scale, scale);
        //?} else {
        /*g.pose().pushPose();
        g.pose().translate(x + (w - width * scale) / 2.0F, y + nudgeY + (h - height * scale) / 2.0F, 0);
        g.pose().scale(scale, scale, 1.0F);
        *///?}
        IconColor color = GuiTheme.icons();
        int tint = selected ? IconPalette.selectedColor(color.iconTint()) : color.iconTint();
        int accent = IconPalette.accentColor(color);
        //? if >=26.1 {
        if (selected) accent = IconPalette.selectedColor(accent);
        var pipeline = selected && this == POWER_OFF ? IconPalette.MONOCHROME_PIPELINE : IconPalette.pipeline(color);
        g.blitSprite(pipeline, sprite, 0, 0, width, height, tint);
        //?} else {
        /*if (selected) {
            accent = IconPalette.selectedColor(accent);
        }
        IconPalette.draw(g, tint, selected && this == POWER_OFF, () -> g.blitSprite(sprite, 0, 0, width, height));
        *///?}
        if (this == SMELTING_ALLOY) {
            //? if >=26.1 {
            g.blitSprite(IconPalette.accent(color), TERMINAL, 0, 0, width, height, accent);
            //?} else {
            /*IconPalette.draw(g, accent, false, () -> g.blitSprite(TERMINAL, 0, 0, width, height));
            *///?}
        } else if (this == SMELTING_ALLOY_FURNACE) {
            // Only the right half belongs to the alloy furnace; the left half is the ordinary furnace.
            //? if >=26.1 {
            g.blitSprite(IconPalette.accent(color), TERMINAL, width, height, width / 2, 0,
                    width / 2, 0, width - width / 2, height, accent);
            g.blitSprite(IconPalette.accent(color), SEPARATION, 0, 0, width, height, accent);
        }
        g.pose().popMatrix();
            //?} else {
            /*IconPalette.draw(g, accent, false, () -> {
                g.blitSprite(TERMINAL, width, height, width / 2, 0, width / 2, 0, width - width / 2, height);
                g.blitSprite(SEPARATION, 0, 0, width, height);
            });
        }
        g.pose().popPose();
            *///?}
    }
}
