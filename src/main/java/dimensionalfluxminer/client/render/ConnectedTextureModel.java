package dimensionalfluxminer.client.render;

import dimensionalfluxminer.content.DfmStateProperties;
//? if <26.1 {
/*import java.util.ArrayList;
*///?}
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
*///?}
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if >=26.1 {
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
//?} else {
/*import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.util.RandomSource;
//? if <26.1 {
/*import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
*///?}
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
//?} else {
/*import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
*///?}
import org.joml.Vector3f;
//? if >=26.1 {
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
//?}

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
//? if >=26.1 {
public final class ConnectedTextureModel extends DelegateBlockStateModel {
//?} else {
/*public final class ConnectedTextureModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<byte[]> VARIANTS = new ModelProperty<>();
*///?}
    private static final String[] SUFFIXES = {"", "_h", "_v", "_center", "_corner"};
    private static final int PLAIN = 0;
    private static final int OPEN_H = 1;
    private static final int OPEN_V = 2;
    private static final int CENTER = 3;
    private static final int CORNER = 4;

    private final Block block;
    /** Base texture of each connected face; faces not listed keep the original quads. */
    //? if >=26.1 {
    private final Map<Direction, Identifier> textures;
    /** Quads by face, quarter and variant: {@code [face][quarter][variant]}, built from the original quads on first use. */
    //?} else {
    /*private final Map<Direction, ResourceLocation> textures;
    /^* Quads by face, quarter and variant: {@code [face][quarter][variant]}, built on first use. ^/
    *///?}
    private volatile BakedQuad[][][] quads;
    //? if >=26.1 {
    private volatile BlockStateModelPart quadsFrom;
    //?}

    /** False: only neighbors to the left and right connect (a row of vents makes one long grille, rows stay apart). */
    private final boolean vertical;

    //? if >=26.1 {
    public ConnectedTextureModel(BlockStateModel original, Block block, Map<Direction, Identifier> textures, boolean vertical) {
    //?} else {
    /*public ConnectedTextureModel(BakedModel original, Block block, Map<Direction, ResourceLocation> textures, boolean vertical) {
    *///?}
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
    //? if >=26.1 {
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        int start = parts.size();
        super.collectParts(level, pos, state, random, parts);
        if (state.hasProperty(DfmStateProperties.FORMED) && state.getValue(DfmStateProperties.FORMED) || parts.size() - start != 1) {
            return;
        }
        BlockStateModelPart original = parts.get(start);
        BakedQuad[][][] all = quads(original);
        @SuppressWarnings("unchecked")
        List<BakedQuad>[] faces = new List[Direction.values().length];
    //?} else {
    /*public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (state.hasProperty(DfmStateProperties.FORMED) && state.getValue(DfmStateProperties.FORMED)) {
            return modelData;
        }
        byte[] variants = new byte[24];
    *///?}
        for (Direction face : Direction.values()) {
            //? if >=26.1 {
            if (!textures.containsKey(face) || all[face.ordinal()][0][PLAIN] == null) {
            //?} else {
            /*if (!textures.containsKey(face)) {
            *///?}
                continue;
            }
            Direction u = uDirection(face);
            Direction v = vDirection(face);
            //? if >=26.1 {
            BakedQuad[] quarters = new BakedQuad[4];
            //?}
            for (int quarter = 0; quarter < 4; quarter++) {
                Direction horizontal = (quarter & 1) == 0 ? u.getOpposite() : u;
                //? if >=26.1 {
                Direction verticalDir = (quarter & 2) == 0 ? v.getOpposite() : v;
                //?} else {
                /*Direction vertical = (quarter & 2) == 0 ? v.getOpposite() : v;
                *///?}
                boolean h = connects(level, pos, pos.relative(horizontal), face);
                //? if >=26.1 {
                boolean vv = this.vertical && connects(level, pos, pos.relative(verticalDir), face);
                //?} else {
                /*boolean vv = this.vertical && connects(level, pos, pos.relative(vertical), face);
                *///?}
                int variant;
                if (h && vv) {
                    //? if >=26.1 {
                    variant = connects(level, pos, pos.relative(horizontal).relative(verticalDir), face) ? CENTER : CORNER;
                    //?} else {
                    /*variant = connects(level, pos, pos.relative(horizontal).relative(vertical), face) ? CENTER : CORNER;
                    *///?}
                } else {
                    variant = h ? OPEN_H : vv ? OPEN_V : PLAIN;
                }
                //? if >=26.1 {
                quarters[quarter] = all[face.ordinal()][quarter][variant];
            }
            faces[face.ordinal()] = List.of(quarters);
        }
        parts.set(start, new Part(original, faces));
                //?} else {
                /*variants[face.ordinal() * 4 + quarter] = (byte) variant;
            }
        }
        return modelData.derive().with(VARIANTS, variants).build();
                *///?}
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

    //? if >=26.1 {
    /** The original part with the connected faces replaced by their four quarters. */
    private record Part(BlockStateModelPart original, @Nullable List<BakedQuad>[] faces) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction != null && faces[direction.ordinal()] != null) {
                return faces[direction.ordinal()];
            }
            return original.getQuads(direction);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return original.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return original.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return original.materialFlags();
        }
    //?} else {
    /*// ---------------------------------------------------------------- quads

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
    *///?}
    }

    //? if >=26.1 {
    // ---------------------------------------------------------------- quads

    private BakedQuad[][][] quads(BlockStateModelPart original) {
    //?} else {
    /*private BakedQuad[][][] quads() {
    *///?}
        BakedQuad[][][] built = quads;
        //? if >=26.1 {
        if (built == null || quadsFrom != original) {
        //?} else {
        /*if (built == null) {
        *///?}
            synchronized (this) {
                built = quads;
                //? if >=26.1 {
                if (built == null || quadsFrom != original) {
                    built = bake(original);
                    quads = built;
                    quadsFrom = original;
                //?} else {
                /*if (built == null) {
                    quads = built = bake();
                *///?}
                }
            }
        }
        return built;
    }

    //? if >=26.1 {
    /** Quarters of each connected face, cut from the original full-face quad (same shading, only the texture changes). */
    private BakedQuad[][][] bake(BlockStateModelPart original) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS);
    //?} else {
    /*private BakedQuad[][][] bake() {
        FaceBakery bakery = new FaceBakery();
        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
    *///?}
        BakedQuad[][][] result = new BakedQuad[6][4][SUFFIXES.length];
        //? if >=26.1 {
        for (Map.Entry<Direction, Identifier> entry : textures.entrySet()) {
        //?} else {
        /*for (Map.Entry<Direction, ResourceLocation> entry : textures.entrySet()) {
        *///?}
            Direction face = entry.getKey();
            //? if >=26.1 {
            List<BakedQuad> faceQuads = original.getQuads(face);
            if (faceQuads.size() != 1) {
                continue;
            }
            BakedQuad full = faceQuads.getFirst();
            //?}
            // Without vertical connections only the plain and the left/right-open textures exist.
            //? if >=26.1 {
            for (int variant = 0; variant < SUFFIXES.length; variant++) {
                int texture = vertical || variant <= OPEN_H ? variant : PLAIN;
                Identifier id = entry.getValue();
                if (texture > 0) {
            //?} else {
            /*for (int variant = 0; variant < (vertical ? SUFFIXES.length : OPEN_H + 1); variant++) {
                ResourceLocation id = entry.getValue();
                if (variant > 0) {
            *///?}
                    String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
                    //? if >=26.1 {
                    id = id.withPath("block/parts/connected/" + name + SUFFIXES[texture]);
                    //?} else {
                    /*id = id.withPath("block/parts/connected/" + name + SUFFIXES[variant]);
                    *///?}
                }
                TextureAtlasSprite sprite = atlas.getSprite(id);
                for (int quarter = 0; quarter < 4; quarter++) {
                    //? if >=26.1 {
                    result[face.ordinal()][quarter][variant] = quarter(full, sprite, (quarter & 1) * 8, (quarter >> 1) * 8);
                    //?} else {
                    /*float u0 = (quarter & 1) * 8;
                    float v0 = (quarter >> 1) * 8;
                    result[face.ordinal()][quarter][variant] = quarter(bakery, face, sprite, u0, v0);
                    *///?}
                }
            }
        }
        return result;
    }

    //? if =26.1.2 {
    /**
     * The quarter of {@code full} whose texture area is [u0, u0 + 8) x [v0, v0 + 8) (texels of a 16 x 16 face),
     * textured with that area of {@code sprite}. Positions are interpolated over the original quad by its texture
     * coordinates, so the winding, normal and shading stay those of the model.
     */
    private static BakedQuad quarter(BakedQuad full, TextureAtlasSprite sprite, float u0, float v0) {
        TextureAtlasSprite base = full.materialInfo().sprite();
        float[] s = new float[4];
        float[] t = new float[4];
        for (int i = 0; i < 4; i++) {
            s[i] = local(UVPair.unpackU(full.packedUV(i)), base.getU0(), base.getU1());
            t[i] = local(UVPair.unpackV(full.packedUV(i)), base.getV0(), base.getV1());
        }
        // Corners of the face in texture space: (0,0), (16,0) and (0,16) span it.
        Vector3fc p00 = corner(full, s, t, 0, 0);
        Vector3fc p10 = corner(full, s, t, 16, 0);
        Vector3fc p01 = corner(full, s, t, 0, 16);
        Vector3f[] positions = new Vector3f[4];
        long[] uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            float su = u0 + s[i] / 2;
            float tv = v0 + t[i] / 2;
            positions[i] = new Vector3f(p10).sub(p00).mul(su / 16).add(new Vector3f(p01).sub(p00).mul(tv / 16)).add(p00);
            uvs[i] = UVPair.pack(sprite.getU(su / 16), sprite.getV(tv / 16));
        }
        BakedQuad.MaterialInfo info = full.materialInfo();
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(sprite, info.layer(), info.itemRenderType(),
                info.tintIndex(), info.shade(), info.lightEmission(), info.ambientOcclusion());
        return new BakedQuad(positions[0], positions[1], positions[2], positions[3], uvs[0], uvs[1], uvs[2], uvs[3],
                full.direction(), material, full.bakedNormals(), full.bakedColors());
    //?} else if >=26.3 {
    /*/^*
     * The quarter of {@code full} whose texture area is [u0, u0 + 8) x [v0, v0 + 8) (texels of a 16 x 16 face),
     * textured with that area of {@code sprite}. Positions are interpolated over the original quad by its texture
     * coordinates, so the winding, normal and shading stay those of the model.
     ^/
    private static BakedQuad quarter(BakedQuad full, TextureAtlasSprite sprite, float u0, float v0) {
        TextureAtlasSprite base = full.materialInfo().sprite();
        float[] s = new float[4];
        float[] t = new float[4];
        for (int i = 0; i < 4; i++) {
            s[i] = local(UVPair.unpackU(full.packedUV(i)), base.getU0(), base.getU1());
            t[i] = local(UVPair.unpackV(full.packedUV(i)), base.getV0(), base.getV1());
        }
        // Corners of the face in texture space: (0,0), (16,0) and (0,16) span it.
        Vector3fc p00 = corner(full, s, t, 0, 0);
        Vector3fc p10 = corner(full, s, t, 16, 0);
        Vector3fc p01 = corner(full, s, t, 0, 16);
        Vector3f[] positions = new Vector3f[4];
        long[] uvs = new long[4];
        for (int i = 0; i < 4; i++) {
            float su = u0 + s[i] / 2;
            float tv = v0 + t[i] / 2;
            positions[i] = new Vector3f(p10).sub(p00).mul(su / 16).add(new Vector3f(p01).sub(p00).mul(tv / 16)).add(p00);
            uvs[i] = UVPair.pack(sprite.getU(su / 16), sprite.getV(tv / 16));
        }
        BakedQuad.MaterialInfo info = full.materialInfo();
        BakedQuad.MaterialInfo material = new BakedQuad.MaterialInfo(sprite, info.layer(), info.itemRenderType(), info.itemGlintRenderType(),
                info.itemGlintSpecialRenderType(), info.tintIndex(), info.shadeDirectionOverride(), info.lightEmission(), info.ambientOcclusion());
        return new BakedQuad(positions[0], positions[1], positions[2], positions[3], uvs[0], uvs[1], uvs[2], uvs[3],
                full.direction(), material, full.bakedNormals(), full.bakedColors());
    *///?} else {
    /*/^* The quarter of a full face whose texture area is [u0, u0 + 8) x [v0, v0 + 8). ^/
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
    *///?}
    }

    //? if >=26.1 {
    /** Atlas coordinate as texels (0..16) of the sprite, rounded to the nearest corner. */
    private static float local(float value, float min, float max) {
        return Math.round((value - min) / (max - min) * 16);
    }

    /** Position of the vertex at texture corner (cs, ct). */
    private static Vector3fc corner(BakedQuad quad, float[] s, float[] t, float cs, float ct) {
        for (int i = 0; i < 4; i++) {
            if (s[i] == cs && t[i] == ct) {
                return quad.position(i);
            }
        }
        // The opposite corner of the other two (a quad whose texture is mirrored).
        return quad.position(0);
    //?} else {
    /*private static void range(float[] from, float[] to, int axisA, float a0, float a1, int axisB, float b0, float b1) {
        from[axisA] = a0;
        to[axisA] = a1;
        from[axisB] = b0;
        to[axisB] = b1;
    *///?}
    }
}
