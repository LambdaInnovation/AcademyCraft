package cn.li.mc1211.client.font.msdf;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
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
 * common baseline, and the bitmap left equals the STB grid x0. FreeType's
 * hinted LCD bitmaps push the bitmap top up by per-glyph amounts (visible
 * for t/r/h vs a/m/n) and shift bitmap_left, which neither the vertical
 * placement conversion nor the horizontal bearing can compensate. Rendering
 * unhinted makes the bitmaps match the STB reference exactly.</p>
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
    public @Nullable GlyphInfo getGlyph(int codepoint) {
        MsdfFreeTypeGlyphProvider.GlyphEntry entry = this.glyphs.get(codepoint);
        return entry != null ? this.getOrLoadGlyphInfo(codepoint, entry) : null;
    }

    private GlyphInfo getOrLoadGlyphInfo(int codepoint, MsdfFreeTypeGlyphProvider.GlyphEntry entry) {
        GlyphInfo result = entry.glyph;
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

    private GlyphInfo loadGlyph(int codepoint, FT_Face face, int index) {
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
        if (width > 0 && height > 0) {
            return new MsdfFreeTypeGlyphProvider.Glyph(codepoint, left, top, width, height, scaledAdvance, index);
        }
        return (GlyphInfo.SpaceGlyphInfo) () -> scaledAdvance / this.oversample;
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

    private class Glyph implements GlyphInfo, SheetGlyphInfo {
        private final int codePoint;
        private final int width;
        private final int height;
        private final float bearingX;
        private final float bearingY;
        private final float advance;
        private final GlyphInfo info;
        private final int index;

        private Glyph(int codePoint, float left, float top, int width, int height, float advance, int index) {
            this.codePoint = codePoint;
            this.width = width;
            this.height = height;
            this.advance = advance / MsdfFreeTypeGlyphProvider.this.oversample;
            this.info = new GlyphInfo() {
                @Override
                public float getAdvance() {
                    return Glyph.this.advance;
                }

                @Override
                public float getBoldOffset() {
                    return 0.0F;
                }

                @Override
                public float getShadowOffset() {
                    return 1.0F;
                }

                @Override
                public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> baker) {
                    return baker.apply(Glyph.this);
                }
            };
            this.bearingX = left / MsdfFreeTypeGlyphProvider.this.oversample;
            this.bearingY = top / MsdfFreeTypeGlyphProvider.this.oversample;
            this.index = index;
        }

        @Override
        public float getAdvance() {
            return this.advance;
        }

        @Override
        public float getBoldOffset() {
            return this.info.getBoldOffset();
        }

        @Override
        public float getShadowOffset() {
            return this.info.getShadowOffset();
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> baker) {
            return baker.apply(this);
        }

        @Override
        public int getPixelWidth() {
            return this.width;
        }

        @Override
        public int getPixelHeight() {
            return this.height;
        }

        @Override
        public float getOversample() {
            return MsdfFreeTypeGlyphProvider.this.oversample;
        }

        @Override
        public void upload(int x, int y) {
            FT_Face face = MsdfFreeTypeGlyphProvider.this.validateFontOpen();
            // Re-load with OUR unhinted flags: NativeImage.copyFromFont
            // re-loads with vanilla's hinted LCD flags, whose bitmap
            // dimensions differ, and would upload misaligned pixels.
            int errorCode = FreeType.FT_Load_Glyph(face, this.index, GLYPH_LOAD_FLAGS);
            if (errorCode != 0) {
                return;
            }
            FT_Bitmap bitmap = face.glyph().bitmap();
            if (bitmap.width() != this.width || bitmap.rows() != this.height) {
                return;
            }
            // FreeType rows may have a padded pitch; copy each row into a
            // LUMINANCE NativeImage via per-pixel writes (no bulk API for
            // luminance buffers) and upload it like vanilla does. The
            // signature is (mipmapLevel, x, y, xOffset, yOffset, width,
            // height, ..., closeOnUpload) — 1.21.1 puts x/y after the level,
            // so passing (x, y, 0, ...) would feed the glyph's x into
            // mipmapLevel and trigger GL_INVALID_VALUE.
            ByteBuffer src = bitmap.buffer(bitmap.pitch() * bitmap.rows());
            int pitch = bitmap.pitch();
            NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, this.width, this.height, false);
            for (int row = 0; row < this.height; row++) {
                for (int col = 0; col < this.width; col++) {
                    image.setPixelLuminance(col, row, src.get(row * pitch + col));
                }
            }
            image.upload(0, x, y, 0, 0, this.width, this.height, false, true);
        }

        @Override
        public boolean isColored() {
            return false;
        }

        @Override
        public float getBearingLeft() {
            return this.bearingX;
        }

        @Override
        public float getBearingTop() {
            return this.bearingY;
        }
    }

    private static class GlyphEntry {
        private final int index;
        private volatile @Nullable GlyphInfo glyph;

        private GlyphEntry(int index) {
            this.index = index;
        }
    }
}
