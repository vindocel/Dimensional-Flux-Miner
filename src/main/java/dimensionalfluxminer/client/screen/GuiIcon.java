package dimensionalfluxminer.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

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
    private final Identifier[] sprites = java.util.Arrays.stream(IconColor.values())
            .map(color -> DimensionalFluxMiner.id(color.folder() + "/" + name().toLowerCase(java.util.Locale.ROOT)))
            .toArray(Identifier[]::new);

    /** Draws the icon centered in a {@code w} x {@code h} area at (x, y), shrunk only when it does not fit. */
    void drawCentered(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        Identifier sprite = sprites[GuiTheme.icons().ordinal()];
        TextureAtlasSprite atlasSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.GUI).getSprite(sprite);
        int width = atlasSprite.contents().width();
        int height = atlasSprite.contents().height();
        float scale = Math.min(1.0F, Math.min(w / (float) width, h / (float) height));
        g.pose().pushMatrix();
        g.pose().translate(x + (w - width * scale) / 2.0F, y + nudgeY + (h - height * scale) / 2.0F);
        g.pose().scale(scale, scale);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, width, height);
        g.pose().popMatrix();
    }
}
