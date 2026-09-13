package cn.li.mc1211.client.font.msdf;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import it.unimi.dsi.fastutil.ints.IntSet;

import javax.annotation.Nullable;

/**
 * Wraps a vanilla {@link GlyphProvider} so that when monospace mode is active,
 * glyph advances are fixed to {@code monospaceAdvance} and glyphs are centred
 * inside their grid cell via {@link MSDFAwareGlyph}.
 */
public final class MonospaceAwareGlyphProvider implements GlyphProvider {

    private final GlyphProvider delegate;
    private final MsdfFontFace face;

    public MonospaceAwareGlyphProvider(final GlyphProvider delegate,
                                       final MsdfFontFace face) {
        this.delegate = delegate;
        this.face = face;
    }

    @Override
    @Nullable
    public GlyphInfo getGlyph(final int codePoint) {
        final GlyphInfo original = delegate.getGlyph(codePoint);
        if (original == null) return null;
        return new MSDFAwareGlyph(original,
                MsdfFontManager.isMonospace(),
                MsdfFontManager.monospaceAdvance(),
                face);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return delegate.getSupportedGlyphs();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
