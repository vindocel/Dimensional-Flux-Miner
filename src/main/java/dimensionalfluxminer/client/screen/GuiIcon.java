package dimensionalfluxminer.client.screen;

import net.minecraft.client.renderer.RenderPipelines;
import dimensionalfluxminer.DimensionalFluxMiner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * Icons of the machine screens, in the icon color the player picked: blue ({@code icon/<name>}) or purple
 * ({@code icon_purple/<name>}); each reads on both the light and the dark screens. Drawn at its own pixel size (read from
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
    SETTINGS,
    SIDE_CONFIG,
    SMELTING,
    SMELTING_ALLOY,
    SMELTING_ALLOY_FURNACE,
    SORTING,
    STATS,
    UPGRADE;

    private final Identifier blue = DimensionalFluxMiner.id("icon/" + name().toLowerCase(java.util.Locale.ROOT));
    private final Identifier purple = DimensionalFluxMiner.id("icon_purple/" + name().toLowerCase(java.util.Locale.ROOT));

    /** Draws the icon centered in a {@code w} x {@code h} area at (x, y), shrunk only when it does not fit. */
    void drawCentered(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        Identifier sprite = GuiTheme.purple() ? purple : blue;
        TextureAtlasSprite atlasSprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.GUI).getSprite(sprite);
        int width = atlasSprite.contents().width();
        int height = atlasSprite.contents().height();
        float scale = Math.min(1.0F, Math.min(w / (float) width, h / (float) height));
        g.pose().pushMatrix();
        g.pose().translate(x + (w - width * scale) / 2.0F, y + (h - height * scale) / 2.0F);
        g.pose().scale(scale, scale);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, width, height);
        g.pose().popMatrix();
    }
}
