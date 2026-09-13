package cn.li.mc262.client.font.msdf;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Wraps a vanilla {@link GlyphProvider} so that when monospace mode is active,
 * glyph advances are fixed to {@code monospaceAdvance} via {@link MSDFAwareGlyph}.
 * The face/codePoint are also passed through so the wrapper can apply the
 * 1.20.1 vertical placement convention per glyph.
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
    public UnbakedGlyph getGlyph(final int codePoint) {
        final UnbakedGlyph original = delegate.getGlyph(codePoint);
        if (original == null) {
            return null;
        }
        return new MSDFAwareGlyph(original,
                MsdfFontManager.isMonospace(),
                MsdfFontManager.monospaceAdvance(),
                codePoint,
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
