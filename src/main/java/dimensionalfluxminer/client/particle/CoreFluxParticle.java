package dimensionalfluxminer.client.particle;

import dimensionalfluxminer.content.CoreFluxOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.LightCoordsUtil;

/**
 * A quantum mote that appears inside the Miner and is pulled into its core: it starts slowly, speeds up as it falls in,
 * fades in when it appears and shrinks as it reaches the core. The spawn velocity is the offset to the core; a higher\n * speed makes the trip shorter.
 */
public class CoreFluxParticle extends SingleQuadParticle {
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double toCoreX;
    private final double toCoreY;
    private final double toCoreZ;
    private final float baseSize;

    CoreFluxParticle(ClientLevel level, double x, double y, double z, double toCoreX, double toCoreY, double toCoreZ,
                     float speed, float red, float green, float blue, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(level.getRandom()));
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.toCoreX = toCoreX;
        this.toCoreY = toCoreY;
        this.toCoreZ = toCoreZ;
        this.lifetime = Math.max(8, Math.round((28 + random.nextInt(14)) / Math.max(0.1F, speed)));
        this.baseSize = 0.035F + random.nextFloat() * 0.03F;
        this.quadSize = baseSize;
        this.rCol = red;
        this.gCol = green;
        this.bCol = blue;
        this.alpha = 0;
        this.hasPhysics = false;
        this.gravity = 0;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float t = age / (float) lifetime;
        float pull = t * t;
        setPos(startX + toCoreX * pull, startY + toCoreY * pull, startZ + toCoreZ * pull);
        alpha = Math.min(1.0F, t * 5.0F) * 0.85F;
        quadSize = baseSize * (1.0F - 0.7F * pull);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    /** Cyan-blue motes of the Basic Core (#62DEDD). */
    public static ParticleProvider<CoreFluxOptions> basic(SpriteSet sprites) {
        return (options, level, x, y, z, dx, dy, dz, random) -> new CoreFluxParticle(level, x, y, z, dx, dy, dz, options.speed(), 0x62 / 255F, 0xDE / 255F, 0xDD / 255F, sprites);
    }

    /** Violet motes of the Stabilized Core. */
    public static ParticleProvider<CoreFluxOptions> stabilized(SpriteSet sprites) {
        return (options, level, x, y, z, dx, dy, dz, random) -> new CoreFluxParticle(level, x, y, z, dx, dy, dz, options.speed(), 0.72F, 0.4F, 1.0F, sprites);
    }
}
