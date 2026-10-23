package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * The color a liquid shows as a flat fill: the average of its still texture (first frame, weighted by alpha) times
 * its tint. Water (gray texture, blue tint) is blue, lava (orange texture, white tint) orange. Kept per texture and
 * dropped with it when the textures reload.
 */
public final class FluidColors {
    private static final Map<SpriteContents, Integer> AVERAGE = new WeakHashMap<>();

    private FluidColors() {
    }

    /** Opaque ARGB color of {@code sprite} tinted with {@code tint} (ARGB). */
    public static int color(TextureAtlasSprite sprite, int tint) {
        int average = AVERAGE.computeIfAbsent(sprite.contents(), FluidColors::average);
        int r = ((average >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
        int g = ((average >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
        int b = (average & 0xFF) * (tint & 0xFF) / 255;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int average(SpriteContents contents) {
        try {
            NativeImage image = contents.getOriginalImage();
            long r = 0;
            long g = 0;
            long b = 0;
            long weight = 0;
            for (int y = 0; y < contents.height(); y++) {
                for (int x = 0; x < contents.width(); x++) {
                    int argb = image.getPixel(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    r += (long) ((argb >> 16) & 0xFF) * a;
                    g += (long) ((argb >> 8) & 0xFF) * a;
                    b += (long) (argb & 0xFF) * a;
                    weight += a;
                }
            }
            if (weight == 0) {
                return 0xFFFFFF;
            }
            return (int) (r / weight) << 16 | (int) (g / weight) << 8 | (int) (b / weight);
        } catch (RuntimeException e) {
            // Texture data not available: the tint alone.
            return 0xFFFFFF;
        }
    }
}
