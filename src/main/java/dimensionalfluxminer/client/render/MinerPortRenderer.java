package dimensionalfluxminer.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dimensionalfluxminer.DimensionalFluxMiner;
import dimensionalfluxminer.miner.MinerLayout;
import dimensionalfluxminer.miner.MinerPorts;
import dimensionalfluxminer.miner.MinerStructure;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Draws the configured ports of the Miner's lower layer: an 8 x 8 pixel socket in the middle of each port face, just
 * outside the casing. The sockets are painted in code, one texture per look: energy in the blue of the energy bar, Item
 * Output in orange, Matter Input in violet, and half orange half violet for a face that is both.
 */
public final class MinerPortRenderer {
    /** Socket pattern: frame corners, frame, then the darker inside from the rim to the center. */
    private static final String[] PATTERN = {
            "ABBBBBBA",
            "BCDDDDCB",
            "BDEFFEDB",
            "BDFGGFDB",
            "BDFGGFDB",
            "BDEFFEDB",
            "BCDDDDCB",
            "ABBBBBBA"};
    private static final int ENERGY = dimensionalfluxminer.machine.side.SideRoute.ENERGY_INPUT.color() & 0xFFFFFF;
    private static final int OUTPUT = 0xF08A24;
    private static final int MATTER = 0xA05CF0;
    /** The socket sits 1/64 of a block outside the casing so it never fights with it. */
    private static final float OFFSET = 1.0F / 64.0F;
    private static final float HALF = 0.25F;
    private static final float PIXEL = 1.0F / 16.0F;
    /** Half size of the network node frame: 10 x 10 pixels, a 1-pixel ring right against the socket. */
    private static final float FRAME = 5.0F / 16.0F;
    private static Identifier white;

    private static final Map<MinerPorts.Look, Identifier> TEXTURES = new EnumMap<>(MinerPorts.Look.class);

    private MinerPortRenderer() {
    }

    /**
     * @param pose  posed at the corner of the lower middle block (the block under the core), world axes
     * @param looks      the faces' looks packed by {@link MinerPorts#packLooks}
     * @param nodeColors per face, the ARGB color of the network node frame (0 = no node)
     */
    public static void render(PoseStack pose, SubmitNodeCollector collector, int looks, int[] nodeColors, Direction front, int light) {
        MinerLayout.Front f = MinerStructure.toFront(front);
        for (MinerPorts.Face face : MinerPorts.Face.VALUES) {
            MinerPorts.Look look = MinerPorts.unpackLook(looks, face);
            int node = face.ordinal() < nodeColors.length ? nodeColors[face.ordinal()] : 0;
            if (look == MinerPorts.Look.NONE && node == 0) {
                continue;
            }
            int[] base = face.baseOffset();
            int[] owner = MinerLayout.rotate(base[0], base[2], f);
            Direction out = face.outward(f);
            float nx = out.getStepX();
            float nz = out.getStepZ();
            float cx = owner[0] + 0.5F + nx * (0.5F + OFFSET);
            float cy = 0.5F;
            float cz = owner[1] + 0.5F + nz * (0.5F + OFFSET);
            // Right-hand direction of someone looking at the face from outside.
            float rx = nz;
            float rz = -nx;
            if (look != MinerPorts.Look.NONE) {
                collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture(look)), (last, consumer) ->
                        quad(consumer, last, cx, cy, cz, rx, rz, -HALF, HALF, -HALF, HALF, nx, nz, 0xFFFFFFFF, light));
            }
            if (node != 0) {
                // A network node on this face: a 1-pixel frame in the network's color around the socket.
                float in = FRAME - PIXEL;
                collector.submitCustomGeometry(pose, RenderTypes.entityCutout(white()), (last, consumer) -> {
                    quad(consumer, last, cx, cy, cz, rx, rz, -FRAME, FRAME, in, FRAME, nx, nz, node, light);
                    quad(consumer, last, cx, cy, cz, rx, rz, -FRAME, FRAME, -FRAME, -in, nx, nz, node, light);
                    quad(consumer, last, cx, cy, cz, rx, rz, -FRAME, -in, -in, in, nx, nz, node, light);
                    quad(consumer, last, cx, cy, cz, rx, rz, in, FRAME, -in, in, nx, nz, node, light);
                });
            }
        }
    }

    /** A rectangle on the face: {@code r0..r1} along the right-hand direction, {@code y0..y1} up, from the center. */
    private static void quad(VertexConsumer consumer, PoseStack.Pose last, float cx, float cy, float cz, float rx, float rz,
                             float r0, float r1, float y0, float y1, float nx, float nz, int argb, int light) {
        vertex(consumer, last, cx + rx * r0, cy + y1, cz + rz * r0, 0, 0, nx, nz, argb, light);
        vertex(consumer, last, cx + rx * r0, cy + y0, cz + rz * r0, 0, 1, nx, nz, argb, light);
        vertex(consumer, last, cx + rx * r1, cy + y0, cz + rz * r1, 1, 1, nx, nz, argb, light);
        vertex(consumer, last, cx + rx * r1, cy + y1, cz + rz * r1, 1, 0, nx, nz, argb, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float nx, float nz, int argb, int light) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, nx, 0, nz);
    }

    /** A plain white texture, tinted by the vertex color. */
    private static Identifier white() {
        if (white == null) {
            NativeImage image = new NativeImage(1, 1, true);
            image.setPixelABGR(0, 0, 0xFFFFFFFF);
            white = DimensionalFluxMiner.id("dynamic/miner_port_frame");
            Minecraft.getInstance().getTextureManager().register(white, new DynamicTexture(white::toString, image));
        }
        return white;
    }

    /** The texture of a look, painted the first time it is needed. */
    private static Identifier texture(MinerPorts.Look look) {
        return TEXTURES.computeIfAbsent(look, l -> {
            Identifier id = DimensionalFluxMiner.id("dynamic/miner_port_" + l.name().toLowerCase(Locale.ROOT));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, paint(l)));
            return id;
        });
    }

    private static NativeImage paint(MinerPorts.Look look) {
        NativeImage image = new NativeImage(8, 8, true);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int accent = switch (look) {
                    case ENERGY -> ENERGY;
                    case OUTPUT -> OUTPUT;
                    case MATTER -> MATTER;
                    // Item Output on the left half, Matter Input on the right half, seen from outside.
                    default -> x < 4 ? OUTPUT : MATTER;
                };
                image.setPixelABGR(x, y, abgr(shade(PATTERN[y].charAt(x), accent)));
            }
        }
        return image;
    }

    /** Color of one pattern cell for an accent color (0xRRGGBB). */
    private static int shade(char cell, int accent) {
        return switch (cell) {
            case 'A' -> mix(accent, 0xFFFFFF, 0.25F);
            case 'B' -> mix(0x000000, accent, 0.75F);
            case 'C' -> mix(0x081018, accent, 0.10F);
            case 'D' -> mix(0x081018, accent, 0.18F);
            case 'E' -> mix(0x081018, accent, 0.04F);
            case 'F' -> mix(0x081018, accent, 0.10F);
            default -> mix(0x081018, accent, 0.22F);
        };
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** NativeImage stores ABGR. */
    private static int abgr(int rgb) {
        return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }
}
