package dimensionalfluxminer.client.render;

import dimensionalfluxminer.content.DfmStateProperties;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.joml.Vector3f;

/**
 * Connected textures for the loose structural blocks (Steel Chassis, glass, ventilation sides): side by side, blocks of
 * the same kind lose the frame where they touch and read as one surface, keeping it only around the outside.
 * <p>
 * Each face is cut in four quarters. A quarter looks at its two neighbors in the face's plane (the one beside it and
 * the one above or below it) and at the diagonal one, and takes the same quarter of one of five textures: the plain
 * one (no neighbor), the frame open left/right ({@code _h}), open up/down ({@code _v}), open all around
 * ({@code _center}) or open with only the corner texel kept ({@code _corner}, both neighbors but not the diagonal).
 * Faces without connected textures (the ventilation cap) keep the original model's quads. Formed Miner parts are
 * invisible and never connect.
 */
public final class ConnectedTextureModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<byte[]> VARIANTS = new ModelProperty<>();
    private static final String[] SUFFIXES = {"", "_h", "_v", "_center", "_corner"};
    private static final int PLAIN = 0;
    private static final int OPEN_H = 1;
    private static final int OPEN_V = 2;
    private static final int CENTER = 3;
    private static final int CORNER = 4;

    private final Block block;
    /** Base texture of each connected face; faces not listed keep the original quads. */
    private final Map<Direction, ResourceLocation> textures;
    /** Quads by face, quarter and variant: {@code [face][quarter][variant]}, built on first use. */
    private volatile BakedQuad[][][] quads;

    /** False: only neighbors to the left and right connect (a row of vents makes one long grille, rows stay apart). */
    private final boolean vertical;

    public ConnectedTextureModel(BakedModel original, Block block, Map<Direction, ResourceLocation> textures, boolean vertical) {
        super(original);
        this.block = block;
        this.textures = new EnumMap<>(textures);
        this.vertical = vertical;
    }

    // ---------------------------------------------------------------- face geometry

    /** Direction in which a face's texture U grows (vanilla full-face UVs). */
    private static Direction uDirection(Direction face) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case SOUTH, UP, DOWN -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
        };
    }

    /** Direction in which a face's texture V grows. */
    private static Direction vDirection(Direction face) {
        return switch (face) {
            case UP -> Direction.SOUTH;
            case DOWN -> Direction.NORTH;
            default -> Direction.DOWN;
        };
    }

    // ---------------------------------------------------------------- connections

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (state.hasProperty(DfmStateProperties.FORMED) && state.getValue(DfmStateProperties.FORMED)) {
            return modelData;
        }
        byte[] variants = new byte[24];
        for (Direction face : Direction.values()) {
            if (!textures.containsKey(face)) {
                continue;
            }
            Direction u = uDirection(face);
            Direction v = vDirection(face);
            for (int quarter = 0; quarter < 4; quarter++) {
                Direction horizontal = (quarter & 1) == 0 ? u.getOpposite() : u;
                Direction vertical = (quarter & 2) == 0 ? v.getOpposite() : v;
                boolean h = connects(level, pos, pos.relative(horizontal), face);
                boolean vv = this.vertical && connects(level, pos, pos.relative(vertical), face);
                int variant;
                if (h && vv) {
                    variant = connects(level, pos, pos.relative(horizontal).relative(vertical), face) ? CENTER : CORNER;
                } else {
                    variant = h ? OPEN_H : vv ? OPEN_V : PLAIN;
                }
                variants[face.ordinal() * 4 + quarter] = (byte) variant;
            }
        }
        return modelData.derive().with(VARIANTS, variants).build();
    }

    /**
     * Whether the face continues into the block at {@code other}: the same kind, loose, with its own face uncovered
     * (a block in front of it would hide the seam anyway).
     */
    private boolean connects(BlockAndTintGetter level, BlockPos self, BlockPos other, Direction face) {
        BlockState state = level.getBlockState(other);
        if (!state.is(block) || state.hasProperty(DfmStateProperties.FORMED) && state.getValue(DfmStateProperties.FORMED)) {
            return false;
        }
        return !level.getBlockState(other.relative(face)).is(block);
    }

    // ---------------------------------------------------------------- quads

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
        byte[] variants = extraData.get(VARIANTS);
        if (side == null || variants == null || !textures.containsKey(side)) {
            return super.getQuads(state, side, rand, extraData, renderType);
        }
        BakedQuad[][][] all = quads();
        List<BakedQuad> result = new ArrayList<>(4);
        for (int quarter = 0; quarter < 4; quarter++) {
            result.add(all[side.ordinal()][quarter][variants[side.ordinal() * 4 + quarter]]);
        }
        return result;
    }

    private BakedQuad[][][] quads() {
        BakedQuad[][][] built = quads;
        if (built == null) {
            synchronized (this) {
                built = quads;
                if (built == null) {
                    quads = built = bake();
                }
            }
        }
        return built;
    }

    private BakedQuad[][][] bake() {
        FaceBakery bakery = new FaceBakery();
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        BakedQuad[][][] result = new BakedQuad[6][4][SUFFIXES.length];
        for (Map.Entry<Direction, ResourceLocation> entry : textures.entrySet()) {
            Direction face = entry.getKey();
            // Without vertical connections only the plain and the left/right-open textures exist.
            for (int variant = 0; variant < (vertical ? SUFFIXES.length : OPEN_H + 1); variant++) {
                ResourceLocation id = entry.getValue();
                if (variant > 0) {
                    String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
                    id = id.withPath("block/parts/connected/" + name + SUFFIXES[variant]);
                }
                TextureAtlasSprite sprite = atlas.getSprite(id);
                for (int quarter = 0; quarter < 4; quarter++) {
                    float u0 = (quarter & 1) * 8;
                    float v0 = (quarter >> 1) * 8;
                    result[face.ordinal()][quarter][variant] = quarter(bakery, face, sprite, u0, v0);
                }
            }
        }
        return result;
    }

    /** The quarter of a full face whose texture area is [u0, u0 + 8) x [v0, v0 + 8). */
    private static BakedQuad quarter(FaceBakery bakery, Direction face, TextureAtlasSprite sprite, float u0, float v0) {
        float u1 = u0 + 8;
        float v1 = v0 + 8;
        // Inverse of the vanilla full-face UVs: the part of the face this texture area covers.
        float[] from = {0, 0, 0};
        float[] to = {16, 16, 16};
        switch (face) {
            case NORTH -> range(from, to, 0, 16 - u1, 16 - u0, 1, 16 - v1, 16 - v0);
            case SOUTH -> range(from, to, 0, u0, u1, 1, 16 - v1, 16 - v0);
            case WEST -> range(from, to, 2, u0, u1, 1, 16 - v1, 16 - v0);
            case EAST -> range(from, to, 2, 16 - u1, 16 - u0, 1, 16 - v1, 16 - v0);
            case UP -> range(from, to, 0, u0, u1, 2, v0, v1);
            case DOWN -> range(from, to, 0, u0, u1, 2, 16 - v1, 16 - v0);
        }
        BlockElementFace element = new BlockElementFace(face, -1, "", new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0));
        return bakery.bakeQuad(new Vector3f(from[0], from[1], from[2]), new Vector3f(to[0], to[1], to[2]), element, sprite, face,
                BlockModelRotation.X0_Y0, null, true);
    }

    private static void range(float[] from, float[] to, int axisA, float a0, float a1, int axisB, float b0, float b1) {
        from[axisA] = a0;
        to[axisA] = a1;
        from[axisB] = b0;
        to[axisB] = b1;
    }
}
