package cn.li.mc1201.client.font.msdf;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

import java.util.function.Function;

/**
 * Dual-mode glyph wrapper.
 * <ul>
 *   <li><b>Proportional</b> — passes through the original {@link GlyphInfo} unchanged.</li>
 *   <li><b>Monospace</b> — overrides {@link #getAdvance()} to a fixed grid width.</li>
 * </ul>
 *
 * <p>Horizontal placement: the baked quad left edge comes from
 * {@link SheetGlyphInfo#getLeft()}, which vanilla derives from the STB
 * grid-aligned bitmap box x0. For negative-bearing glyphs (e.g. msyh's
 * {@code j}) the integer x0 rounds a fraction of a pixel further left than
 * the true left side bearing, so the glyph visibly shifts. The wrapper
 * overrides {@code getLeft()} with the STB float lsb — same rasterizer
 * metric as the bitmap, so the content left edge lands exactly at
 * {@code pen + lsb} (mirrors the 26.2 fix where the unhinted FreeType
 * bitmap_left matches the STB reference grid).</p>
 */
public class MSDFAwareGlyph implements GlyphInfo {

    private final GlyphInfo original;
    private final boolean monospace;
    private final float monospaceAdvance;
    private final int codePoint;
    private final MsdfFontFace face;

    public MSDFAwareGlyph(final GlyphInfo original, final boolean monospace,
                          final float monospaceAdvance, final int codePoint,
                          final MsdfFontFace face) {
        this.original = original;
        this.monospace = monospace;
        this.monospaceAdvance = monospaceAdvance;
        this.codePoint = codePoint;
        this.face = face;
    }

    @Override
    public float getAdvance() {
        return monospace ? monospaceAdvance : original.getAdvance();
    }

    @Override
    public float getBoldOffset() {
        return original.getBoldOffset();
    }

    @Override
    public BakedGlyph bake(final Function<SheetGlyphInfo, BakedGlyph> baker) {
        return original.bake(sgi -> baker.apply(
                new BearingCorrectedSheetGlyph(sgi, codePoint, face)));
    }

    private static final class BearingCorrectedSheetGlyph implements SheetGlyphInfo {
        private final SheetGlyphInfo delegate;
        private final int codePoint;
        private final MsdfFontFace face;

        BearingCorrectedSheetGlyph(final SheetGlyphInfo delegate,
                                   final int codePoint,
                                   final MsdfFontFace face) {
            this.delegate = delegate;
            this.codePoint = codePoint;
            this.face = face;
        }

        @Override
        public int getPixelWidth() {
            return delegate.getPixelWidth();
        }

        @Override
        public int getPixelHeight() {
            return delegate.getPixelHeight();
        }

        @Override
        public void upload(final int x, final int y) {
            delegate.upload(x, y);
        }

        @Override
        public boolean isColored() {
            return delegate.isColored();
        }

        @Override
        public float getOversample() {
            return delegate.getOversample();
        }

        @Override
        public float getBearingX() {
            return delegate.getBearingX();
        }

        @Override
        public float getBearingY() {
            return delegate.getBearingY();
        }

        /**
         * STB float left side bearing instead of the grid-aligned bitmap x0:
         * the bitmap is rasterized by STB at the same scale as this metric,
         * so the content left edge renders exactly at {@code pen + lsb}.
         * Vanilla's {@code getBearingX() + width} keeps the rasterized
         * extent, whose right edge stays within the grid cell.
         */
        @Override
        public float getLeft() {
            int glyphIndex = face.findGlyphIndex(codePoint);
            if (glyphIndex == 0) {
                return delegate.getLeft();
            }
            return face.getLeftSideBearing(glyphIndex);
        }

        @Override
        public float getRight() {
            return this.getLeft() + this.delegate.getPixelWidth() / this.delegate.getOversample();
        }

        // Vertical placement (getUp/getDown) passes through the vanilla STB
        // native metrics — 1.20.1 is the reference baseline the 26.2/1.21.1
        // conversion formulas target, so it needs no vertical shift.
    }
}
