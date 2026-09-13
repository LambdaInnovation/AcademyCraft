package cn.li.mc262.client.font.msdf;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

/**
 * FreeType glyph provider for the MSDF shadow font.
 *
 * <p>Copy of vanilla {@link com.mojang.blaze3d.font.TrueTypeGlyphProvider}
 * with hinting disabled ({@code FT_LOAD_NO_HINTING} instead of the vanilla
 * LCD target). The 1.20.1 reference rasterized with STB, which never hints:
 * a glyph's bitmap top equals its outline top, so x-height letters land on a
 * common baseline. FreeType's hinted LCD bitmaps push the bitmap top up by
 * per-glyph amounts (visible for t/r/h vs a/m/n), and the vertical placement
 * conversion cannot compensate because it has no way to know the hinted
 * blank rows. Rendering unhinted makes the bitmaps match the STB reference
 * exactly, so the 1.20.1 placement formula is precise.</p>
 */
public final class MsdfFreeTypeGlyphProvider implements GlyphProvider {

    /** FT_LOAD_RENDER | FT_LOAD_NO_HINTING — unhinted bitmaps like STB. */
    private static final int GLYPH_LOAD_FLAGS = FreeType.FT_LOAD_RENDER | FreeType.FT_LOAD_NO_HINTING;

    private @Nullable ByteBuffer fontMemory;
    private @Nullable FT_Face face;
    private final float oversample;
    private final CodepointMap<MsdfFreeTypeGlyphProvider.GlyphEntry> glyphs = new CodepointMap<>(
        MsdfFreeTypeGlyphProvider.GlyphEntry[]::new, MsdfFreeTypeGlyphProvider.GlyphEntry[][]::new
    );

    public MsdfFreeTypeGlyphProvider(ByteBuffer fontMemory, FT_Face face, float size, float oversample, float shiftX, float shiftY, String skip) {
        this.fontMemory = fontMemory;
        this.face = face;
        this.oversample = oversample;
        IntSet skipSet = new IntArraySet();
        skip.codePoints().forEach(skipSet::add);
        int pixelsPerEm = Math.round(size * oversample);
        FreeType.FT_Set_Pixel_Sizes(face, pixelsPerEm, pixelsPerEm);
        float transformX = shiftX * oversample;
        float transformY = -shiftY * oversample;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FT_Vector vector = FreeTypeUtil.setVector(FT_Vector.malloc(stack), transformX, transformY);
            FreeType.FT_Set_Transform(face, null, vector);
            IntBuffer indexPtr = stack.mallocInt(1);
            int codepoint = (int)FreeType.FT_Get_First_Char(face, indexPtr);

            while (true) {
                int index = indexPtr.get(0);
                if (index == 0) {
                    return;
                }

                if (!skipSet.contains(codepoint)) {
                    this.glyphs.put(codepoint, new MsdfFreeTypeGlyphProvider.GlyphEntry(index));
                }

                codepoint = (int)FreeType.FT_Get_Next_Char(face, codepoint, indexPtr);
            }
        }
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        MsdfFreeTypeGlyphProvider.GlyphEntry entry = this.glyphs.get(codepoint);
        return entry != null ? this.getOrLoadGlyphInfo(codepoint, entry) : null;
    }

    private UnbakedGlyph getOrLoadGlyphInfo(int codepoint, MsdfFreeTypeGlyphProvider.GlyphEntry entry) {
        UnbakedGlyph result = entry.glyph;
        if (result == null) {
            FT_Face face = this.validateFontOpen();
            synchronized (face) {
                result = entry.glyph;
                if (result == null) {
                    result = this.loadGlyph(codepoint, face, entry.index);
                    entry.glyph = result;
                }
            }
        }

        return result;
    }

    private UnbakedGlyph loadGlyph(int codepoint, FT_Face face, int index) {
        int errorCode = FreeType.FT_Load_Glyph(face, index, GLYPH_LOAD_FLAGS);
        if (errorCode != 0) {
            FreeTypeUtil.assertError(errorCode, String.format(Locale.ROOT, "Loading glyph U+%06X", codepoint));
        }

        FT_GlyphSlot glyph = face.glyph();
        if (glyph == null) {
            throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", codepoint));
        }

        float scaledAdvance = FreeTypeUtil.x(glyph.advance());
        FT_Bitmap bitmap = glyph.bitmap();
        int left = glyph.bitmap_left();
        int top = glyph.bitmap_top();
        int width = bitmap.width();
        int height = bitmap.rows();
        return width > 0 && height > 0
            ? new MsdfFreeTypeGlyphProvider.Glyph(left, top, width, height, scaledAdvance, index)
            : new EmptyGlyph(scaledAdvance / this.oversample);
    }

    private FT_Face validateFontOpen() {
        if (this.fontMemory != null && this.face != null) {
            return this.face;
        } else {
            throw new IllegalStateException("Provider already closed");
        }
    }

    @Override
    public void close() {
        if (this.face != null) {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                FreeTypeUtil.checkError(FreeType.FT_Done_Face(this.face), "Deleting face");
            }

            this.face = null;
        }

        MemoryUtil.memFree(this.fontMemory);
        this.fontMemory = null;
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.glyphs.keySet();
    }

    private class Glyph implements UnbakedGlyph {
        private final int width;
        private final int height;
        private final float bearingX;
        private final float bearingY;
        private final float advance;
        private final GlyphInfo info;
        private final int index;

        private Glyph(float left, float top, int width, int height, float advance, int index) {
            this.width = width;
            this.height = height;
            this.advance = advance / MsdfFreeTypeGlyphProvider.this.oversample;
            this.info = GlyphInfo.simple(this.advance);
            this.bearingX = left / MsdfFreeTypeGlyphProvider.this.oversample;
            this.bearingY = top / MsdfFreeTypeGlyphProvider.this.oversample;
            this.index = index;
        }

        @Override
        public GlyphInfo info() {
            return this.info;
        }

        @Override
        public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
            return stitcher.stitch(this.info, new GlyphBitmap() {
                @Override
                public int getPixelWidth() {
                    return Glyph.this.width;
                }

                @Override
                public int getPixelHeight() {
                    return Glyph.this.height;
                }

                @Override
                public float getOversample() {
                    return MsdfFreeTypeGlyphProvider.this.oversample;
                }

                @Override
                public float getBearingLeft() {
                    return Glyph.this.bearingX;
                }

                @Override
                public float getBearingTop() {
                    return Glyph.this.bearingY;
                }

                @Override
                public void upload(int x, int y, GpuTexture texture) {
                    FT_Face face = MsdfFreeTypeGlyphProvider.this.validateFontOpen();
                    // Re-load with OUR unhinted flags: NativeImage.copyFromFont
                    // re-loads with vanilla's hinted LCD flags, whose bitmap
                    // rows differ, and its size check would throw.
                    int errorCode = FreeType.FT_Load_Glyph(face, Glyph.this.index, GLYPH_LOAD_FLAGS);
                    if (errorCode != 0) {
                        return;
                    }
                    FT_Bitmap bitmap = face.glyph().bitmap();
                    if (bitmap.width() != Glyph.this.width || bitmap.rows() != Glyph.this.height) {
                        return;
                    }
                    // Pack rows (FreeType rows may have a padded pitch) into a
                    // tight buffer and upload it directly — NativeImage is
                    // 1-component-unfriendly for per-pixel writes.
                    ByteBuffer src = bitmap.buffer(bitmap.pitch() * bitmap.rows());
                    long srcBase = MemoryUtil.memAddress(src);
                    int pitch = bitmap.pitch();
                    ByteBuffer compact = MemoryUtil.memAlloc(Glyph.this.width * Glyph.this.height);
                    long dstBase = MemoryUtil.memAddress(compact);
                    for (int row = 0; row < Glyph.this.height; row++) {
                        MemoryUtil.memCopy(
                                srcBase + (long) row * pitch,
                                dstBase + (long) row * Glyph.this.width,
                                Glyph.this.width);
                    }
                    RenderSystem.getDevice().createCommandEncoder()
                            .writeToTexture(texture, compact, 0, 0, x, y,
                                    Glyph.this.width, Glyph.this.height);
                    MemoryUtil.memFree(compact);
                }

                @Override
                public boolean isColored() {
                    return false;
                }
            });
        }
    }

    private static class GlyphEntry {
        private final int index;
        private volatile @Nullable UnbakedGlyph glyph;

        private GlyphEntry(int index) {
            this.index = index;
        }
    }
}
