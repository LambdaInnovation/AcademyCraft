package cn.li.mc262.client.font.msdf;

import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FreeType font face loaded from a system font file (no AWT).
 *
 * <p>The glyph provider is 26.2's FreeType-backed {@link TrueTypeGlyphProvider}
 * (the same class vanilla uses for {@code "type": "ttf"} font definitions);
 * STB is kept only for the MSDF monospace metrics helpers.</p>
 */
public final class MsdfFontFace implements AutoCloseable {

    private final GlyphProvider glyphProvider;
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer fontData;
    private final float scale;
    private final float stbEquivalentScale;
    private final int ascent;

    public MsdfFontFace(final Path fontPath, final float pixelHeight) throws IOException {
        final byte[] raw = Files.readAllBytes(fontPath);
        final ByteBuffer data = MemoryUtil.memAlloc(raw.length);
        data.put(raw);
        data.flip();
        this.fontData = data;

        final STBTTFontinfo info = STBTTFontinfo.create();
        final int fontOffset = resolveFontOffset(data);
        if (!STBTruetype.stbtt_InitFont(info, data, fontOffset)) {
            MemoryUtil.memFree(data);
            throw new IOException("stbtt_InitFont failed for " + fontPath + " at offset " + fontOffset);
        }

        this.fontInfo = info;
        this.scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, pixelHeight);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer ascentBuf = stack.mallocInt(1);
            final IntBuffer descentBuf = stack.mallocInt(1);
            final IntBuffer lineGapBuf = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentBuf, descentBuf, lineGapBuf);
            this.ascent = ascentBuf.get(0);
        }
        // FreeType-backed provider, constructed exactly like vanilla
        // TrueTypeGlyphProviderDefinition.load (size, oversample, shift, skip),
        // but with hinting disabled so bitmap tops match the unhinted STB
        // reference 1.20.1 used (see MsdfFreeTypeGlyphProvider).
        final PointerBuffer facePtr = PointerBuffer.allocateDirect(1);
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            FreeTypeUtil.assertError(
                    FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), data, 0L, facePtr),
                    "Failed to create MSDF font face");
        }
        final FT_Face face = FT_Face.create(facePtr.get(0));
        FreeTypeUtil.assertError(
                FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE),
                "Find unicode charmap");
        // 1.20.1 rasterized via stbtt_ScaleForPixelHeight: scale =
        // height / (hhea.ascender - hhea.descender), so a full-em glyph renders
        // at height*em/(asc-desc) pixels — for CJK fonts (em < asc-desc) well
        // below the nominal height. FreeType's FT_Set_Pixel_Sizes sets the em
        // box exactly, so convert the requested height to the STB-equivalent
        // pixel size to keep glyph geometry identical to 1.20.1.
        // Read the hhea values via STB: FT_Face.ascender/descender follow the
        // OS/2 win metrics on some CJK faces (e.g. msyh.ttc), which diverge
        // from the hhea table STB's ScaleForPixelHeight uses.
        final int hheaAsc;
        final int hheaDesc;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer ascBuf = stack.mallocInt(1);
            final IntBuffer descBuf = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascBuf, descBuf, null);
            hheaAsc = ascBuf.get(0);
            hheaDesc = descBuf.get(0);
        }
        final float vMetric = (float) (hheaAsc - hheaDesc);
        final float stbEquivalentSize = vMetric > 0.0f
                ? pixelHeight * face.units_per_EM() / vMetric
                : pixelHeight;
        // STB metrics for the vertical placement conversion must be measured
        // at the same size as the FreeType provider (stbEquivalentSize), not
        // at the nominal 32px: stbGlyphTop/descenderPixels are added to the
        // FreeType bearingTop in MSDFAwareGlyph's shift, and mixing the two
        // scales makes every glyph land at a slightly different height.
        this.stbEquivalentScale =
                STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, stbEquivalentSize);
        this.glyphProvider =
                new MsdfFreeTypeGlyphProvider(data, face, stbEquivalentSize, 1.0f, 0.0f, 0.0f, "");
    }

    public STBTTFontinfo fontInfo() {
        return fontInfo;
    }

    public int findGlyphIndex(final int codePoint) {
        return STBTruetype.stbtt_FindGlyphIndex(fontInfo, codePoint);
    }

    public float getAdvance(final int glyphIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer advance = stack.mallocInt(1);
            final IntBuffer lsb = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advance, lsb);
            return advance.get(0) * scale;
        }
    }

    public float getLeftSideBearing(final int glyphIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer advance = stack.mallocInt(1);
            final IntBuffer lsb = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advance, lsb);
            return lsb.get(0) * scale;
        }
    }

    public int[] getBitmapBox(final int glyphIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer x0 = stack.mallocInt(1);
            final IntBuffer y0 = stack.mallocInt(1);
            final IntBuffer x1 = stack.mallocInt(1);
            final IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel(
                    fontInfo, glyphIndex, scale, scale, 0.0f, 0.0f, x0, y0, x1, y1);
            return new int[] { x0.get(0), y0.get(0), x1.get(0), y1.get(0) };
        }
    }

    public float ascentPixels() {
        return ascent * scale;
    }

    /**
     * Baseline-to-em-bottom distance in pixels at the nominal (32px) scale —
     * the value 1.20.1's quad math used. MSDFAwareGlyph rescales it with
     * {@link #stbEquivalentFactor()} before combining with the FreeType
     * bearingTop, keeping every term of the 1.20.1 formula in its own scale.
     */
    public float descenderPixels() {
        final int hheaAsc;
        final int hheaDesc;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer ascBuf = stack.mallocInt(1);
            final IntBuffer descBuf = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascBuf, descBuf, null);
            hheaAsc = ascBuf.get(0);
            hheaDesc = descBuf.get(0);
        }
        final float vMetric = (float) (hheaAsc - hheaDesc);
        return vMetric > 0.0f ? -hheaDesc * scale : 0.0f;
    }

    /**
     * STB bitmap-top y-coordinate relative to the baseline for a code point,
     * at the nominal (32px) scale — exactly the signed value 1.20.1's
     * provider used to position glyphs (see MSDFAwareGlyph).
     */
    public float stbGlyphTop(final int codePoint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer x0 = stack.mallocInt(1);
            final IntBuffer y0 = stack.mallocInt(1);
            final IntBuffer x1 = stack.mallocInt(1);
            final IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointBitmapBox(
                    fontInfo, codePoint, scale, scale, x0, y0, x1, y1);
            return y0.get(0);
        }
    }

    /**
     * Ratio of the FreeType provider's raster size to the nominal 32px
     * (stbEquivalentSize / pixelHeight). 1.20.1's vertical placement formula
     * is defined at 32px; the FreeType bearingTop the shift is combined with
     * lives at the provider size, so the 32px terms are rescaled by this
     * factor before summing.
     */
    public float stbEquivalentFactor() {
        return stbEquivalentScale / scale;
    }

    /**
     * STB bitmap-left x-coordinate relative to the baseline for a code point,
     * at the nominal (32px) scale — the grid-aligned value 1.20.1's provider
     * used (negative when the glyph sits left of the baseline). Added to the
     * FreeType bearingLeft in MSDFAwareGlyph so the horizontal placement uses
     * the same reference grid as the vertical one.
     */
    public float stbGlyphLeft(final int codePoint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer x0 = stack.mallocInt(1);
            final IntBuffer y0 = stack.mallocInt(1);
            final IntBuffer x1 = stack.mallocInt(1);
            final IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointBitmapBox(
                    fontInfo, codePoint, scale, scale, x0, y0, x1, y1);
            return x0.get(0);
        }
    }

    public GlyphProvider glyphProvider() {
        return glyphProvider;
    }

    public float scale() {
        return scale;
    }

    public boolean hasGlyph(final int codePoint) {
        return STBTruetype.stbtt_FindGlyphIndex(fontInfo, codePoint) != 0;
    }

    @Override
    public void close() {
        // The glyph provider owns the FT_Face and the font memory buffer.
        if (glyphProvider != null) {
            glyphProvider.close();
        }
    }

    private static int resolveFontOffset(final ByteBuffer fontData) {
        fontData.rewind();
        if (fontData.remaining() < 12) {
            return 0;
        }
        final byte b0 = fontData.get(0);
        final byte b1 = fontData.get(1);
        final byte b2 = fontData.get(2);
        final byte b3 = fontData.get(3);
        fontData.rewind();
        if (b0 != 't' || b1 != 't' || b2 != 'c' || b3 != 'f') {
            return 0;
        }

        final int fontCount = STBTruetype.stbtt_GetNumberOfFonts(fontData);
        if (fontCount <= 0) {
            return 0;
        }

        final ByteBuffer header = fontData.duplicate().order(ByteOrder.BIG_ENDIAN);
        int bestOffset = header.getInt(12);
        int bestScore = -1;
        final int[] probes = new int[] { 'A', 'a', '0', ' ', '\n', 0x4E2D, 0x7535, 0x91CF };
        final int limit = Math.min(fontCount, 32);

        for (int i = 0; i < limit; i++) {
            final int offset = header.getInt(12 + i * 4);
            if (offset <= 0 || offset >= fontData.capacity()) {
                continue;
            }
            final STBTTFontinfo probe = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(probe, fontData, offset)) {
                continue;
            }
            int score = 0;
            for (final int cp : probes) {
                if (STBTruetype.stbtt_FindGlyphIndex(probe, cp) != 0) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestOffset = offset;
            }
        }
        fontData.rewind();
        return bestOffset;
    }
}
