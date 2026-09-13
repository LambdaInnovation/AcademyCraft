package cn.li.fabric1211.client.render.item;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Minimal Wavefront OBJ reader that bakes straight to vanilla {@link BakedQuad}s.
 *
 * <p>Forge and NeoForge ship an OBJ model loader; Fabric does not, which is why the portable
 * developer used to stay a flat icon on this target. Only what the mod's own models need is
 * supported: positions, texture coordinates, normals, {@code mtllib} / {@code usemtl}, and
 * triangle or quad faces (larger polygons are fan-triangulated). Vertex colours, smoothing
 * groups and per-group visibility are ignored.
 *
 * <p>{@code map_Kd #slot} resolves through the model JSON's texture slots, like the
 * Forge/NeoForge loaders; a material with no usable {@code map_Kd} falls back to the particle
 * sprite. UVs are flipped vertically to match upstream {@code ObjLegacyRender}, which emitted
 * {@code glTexCoord2f(u, 1 - v)}.
 */
public final class ObjMeshLoader {

    /** Vertex layout of DefaultVertexFormat.BLOCK, in ints: xyz, colour, uv, light, normal. */
    private static final int VERTEX_INTS = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
    private static final int WHITE = 0xFFFFFFFF;

    private ObjMeshLoader() {
    }

    /**
     * @param spriteForSlot resolves an MTL texture slot name (without {@code #}) to a stitched sprite;
     *                      may return null, in which case {@code fallbackSprite} is used
     */
    public static List<BakedQuad> load(ResourceManager resources, ResourceLocation objLocation,
                                       Function<String, TextureAtlasSprite> spriteForSlot,
                                       TextureAtlasSprite fallbackSprite) throws IOException {
        List<Vector3f> positions = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();

        Map<String, String> materialTextures = new HashMap<>();
        TextureAtlasSprite currentSprite = fallbackSprite;

        for (String[] line : readLines(resources, objLocation)) {
            switch (line[0]) {
                case "v" -> positions.add(vector3(line));
                case "vt" -> texCoords.add(new float[]{floatAt(line, 1), floatAt(line, 2)});
                case "vn" -> normals.add(vector3(line));
                case "mtllib" -> materialTextures.putAll(
                        readMaterialLibrary(resources, sibling(objLocation, line[1])));
                case "usemtl" -> currentSprite = resolveSprite(
                        materialTextures.get(join(line)), spriteForSlot, fallbackSprite);
                case "f" -> addFace(line, positions, texCoords, normals, currentSprite, quads);
                default -> {
                    // v/vt/vn/f/mtllib/usemtl are the whole supported grammar; skip g, o, s, comments.
                }
            }
        }
        return List.copyOf(quads);
    }

    private static void addFace(String[] line, List<Vector3f> positions, List<float[]> texCoords,
                                List<Vector3f> normals, TextureAtlasSprite sprite, List<BakedQuad> out) {
        int corners = line.length - 1;
        if (corners < 3) {
            return;
        }
        int[][] face = new int[corners][];
        for (int i = 0; i < corners; i++) {
            face[i] = parseVertexRef(line[i + 1], positions.size(), texCoords.size(), normals.size());
        }
        // A BakedQuad always holds 4 corners; triangles repeat the last one, larger polygons fan out.
        for (int i = 2; i < corners; i++) {
            out.add(buildQuad(new int[][]{face[0], face[i - 1], face[i], face[i]},
                    positions, texCoords, normals, sprite));
        }
    }

    private static BakedQuad buildQuad(int[][] corners, List<Vector3f> positions, List<float[]> texCoords,
                                       List<Vector3f> normals, TextureAtlasSprite sprite) {
        Vector3f faceNormal = faceNormal(corners, positions, normals);
        int[] vertexData = new int[VERTEX_INTS * 4];

        for (int i = 0; i < 4; i++) {
            int[] corner = corners[i];
            Vector3f position = positions.get(corner[0]);
            float[] uv = corner[1] >= 0 && corner[1] < texCoords.size()
                    ? texCoords.get(corner[1])
                    : new float[]{0.0F, 0.0F};

            int offset = i * VERTEX_INTS;
            vertexData[offset] = Float.floatToRawIntBits(position.x());
            vertexData[offset + 1] = Float.floatToRawIntBits(position.y());
            vertexData[offset + 2] = Float.floatToRawIntBits(position.z());
            vertexData[offset + 3] = WHITE;
            vertexData[offset + 4] = Float.floatToRawIntBits(sprite.getU(uv[0] * 16.0F));
            vertexData[offset + 5] = Float.floatToRawIntBits(sprite.getV((1.0F - uv[1]) * 16.0F));
            // offset + 6 is the lightmap and offset + 7 the packed normal; vanilla item rendering
            // fills both from the quad's Direction, so FaceBakery leaves them at zero too.
        }

        Direction direction = Direction.getNearest(faceNormal.x(), faceNormal.y(), faceNormal.z());
        return new BakedQuad(vertexData, -1, direction, sprite, true);
    }

    private static Vector3f faceNormal(int[][] corners, List<Vector3f> positions, List<Vector3f> normals) {
        int normalIndex = corners[0][2];
        if (normalIndex >= 0 && normalIndex < normals.size()) {
            return normals.get(normalIndex);
        }
        Vector3f a = positions.get(corners[0][0]);
        Vector3f edge1 = new Vector3f(positions.get(corners[1][0])).sub(a);
        Vector3f edge2 = new Vector3f(positions.get(corners[2][0])).sub(a);
        Vector3f normal = edge1.cross(edge2);
        return normal.lengthSquared() > 0.0F ? normal.normalize() : new Vector3f(0.0F, 1.0F, 0.0F);
    }

    /** {@code position/texCoord/normal}, 1-based, any part optional, negatives count back from the end. */
    private static int[] parseVertexRef(String ref, int positionCount, int texCoordCount, int normalCount) {
        String[] parts = ref.split("/", -1);
        return new int[]{
                resolveIndex(parts, 0, positionCount),
                resolveIndex(parts, 1, texCoordCount),
                resolveIndex(parts, 2, normalCount)
        };
    }

    private static int resolveIndex(String[] parts, int part, int count) {
        if (part >= parts.length || parts[part].isEmpty()) {
            return -1;
        }
        int index = Integer.parseInt(parts[part]);
        return index < 0 ? count + index : index - 1;
    }

    private static Map<String, String> readMaterialLibrary(ResourceManager resources, ResourceLocation location) {
        Map<String, String> textureByMaterial = new HashMap<>();
        String current = null;
        try {
            for (String[] line : readLines(resources, location)) {
                switch (line[0]) {
                    case "newmtl" -> current = join(line);
                    case "map_Kd" -> {
                        if (current != null) {
                            // Trailing token, so any leading option flags are dropped.
                            textureByMaterial.put(current, line[line.length - 1]);
                        }
                    }
                    default -> {
                        // Colours and other material properties do not survive into a BakedQuad.
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read OBJ material library " + location, e);
        }
        return textureByMaterial;
    }

    private static TextureAtlasSprite resolveSprite(String textureRef,
                                                    Function<String, TextureAtlasSprite> spriteForSlot,
                                                    TextureAtlasSprite fallbackSprite) {
        if (textureRef == null || !textureRef.startsWith("#")) {
            return fallbackSprite;
        }
        TextureAtlasSprite sprite = spriteForSlot.apply(textureRef.substring(1));
        return sprite != null ? sprite : fallbackSprite;
    }

    private static List<String[]> readLines(ResourceManager resources, ResourceLocation location) throws IOException {
        Resource resource = resources.getResource(location)
                .orElseThrow(() -> new IOException("Missing OBJ resource " + location));
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader reader = resource.openAsReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                String content = line.trim();
                // Only a leading '#' is a comment — MTL texture-slot references start with one too.
                if (!content.isEmpty() && !content.startsWith("#")) {
                    lines.add(content.split("\\s+"));
                }
            }
        }
        return lines;
    }

    private static ResourceLocation sibling(ResourceLocation location, String fileName) {
        String path = location.getPath();
        int lastSlash = path.lastIndexOf('/');
        String directory = lastSlash < 0 ? "" : path.substring(0, lastSlash + 1);
        return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), directory + fileName);
    }

    private static String join(String[] line) {
        return String.join(" ", Arrays.copyOfRange(line, 1, line.length));
    }

    private static Vector3f vector3(String[] line) {
        return new Vector3f(floatAt(line, 1), floatAt(line, 2), floatAt(line, 3));
    }

    private static float floatAt(String[] line, int index) {
        return index < line.length ? Float.parseFloat(line[index]) : 0.0F;
    }
}
