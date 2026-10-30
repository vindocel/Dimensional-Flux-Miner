package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Icons of the machine screens, in the icon color the player picked ({@link IconColor}: {@code icon/<name>},
 * {@code icon_purple/<name>} or {@code icon_white/<name>}); each reads on both the light and the dark screens. Drawn at its own pixel size (read from
 * the GUI atlas).
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
    POWER_OFF,
    POWER_ON,
    REDSTONE_LOW,
    REDSTONE_OFF,
    REDSTONE_ON,
    REUSE,
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

    /** Sprite of each icon color, in {@link IconColor} order. */
    private final ResourceLocation[] sprites = java.util.Arrays.stream(IconColor.values())
            .map(color -> DimensionalFluxMiner.id(color.folder() + "/" + name().toLowerCase(java.util.Locale.ROOT)))
            .toArray(ResourceLocation[]::new);

    /** Draws the icon centered in a {@code w} x {@code h} area at (x, y), shrunk only when it does not fit. */
    void drawCentered(GuiGraphics g, int x, int y, int w, int h) {
        ResourceLocation sprite = sprites[GuiTheme.icons().ordinal()];
        TextureAtlasSprite atlasSprite = Minecraft.getInstance().getGuiSprites().getSprite(sprite);
        int width = atlasSprite.contents().width();
        int height = atlasSprite.contents().height();
        float scale = Math.min(1.0F, Math.min(w / (float) width, h / (float) height));
        g.pose().pushPose();
        g.pose().translate(x + (w - width * scale) / 2.0F, y + nudgeY + (h - height * scale) / 2.0F, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.blitSprite(sprite, 0, 0, width, height);
        g.pose().popPose();
    }
}
