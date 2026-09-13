package cn.li.mc1201.client.font.msdf;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * STB TrueType font face loaded from a system font file (no AWT).
 * Exposes a vanilla {@link TrueTypeGlyphProvider} for {@link net.minecraft.client.gui.font.FontSet}.
 */
public final class MsdfFontFace implements AutoCloseable {

    private final TrueTypeGlyphProvider glyphProvider;
    private final STBTTFontinfo fontInfo;
    private final float scale;

    public MsdfFontFace(final Path fontPath, final float pixelHeight) throws IOException {
        final byte[] raw = Files.readAllBytes(fontPath);
        final ByteBuffer fontData = MemoryUtil.memAlloc(raw.length);
        fontData.put(raw);
        fontData.flip();

        final STBTTFontinfo info = STBTTFontinfo.create();
        final int fontOffset = resolveFontOffset(fontData);
        if (!STBTruetype.stbtt_InitFont(info, fontData, fontOffset)) {
            MemoryUtil.memFree(fontData);
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
        this.glyphProvider = new TrueTypeGlyphProvider(fontData, info, pixelHeight, 1.0f, 0.0f, 0.0f, "");
    }

    private final int ascent;

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
        glyphProvider.close();
    }

    /**
     * Pick the best TTC sub-font offset. Each probe uses a fresh {@link STBTTFontinfo};
     * reusing one struct across {@code stbtt_InitFont} calls crashes native STB.
     */
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
