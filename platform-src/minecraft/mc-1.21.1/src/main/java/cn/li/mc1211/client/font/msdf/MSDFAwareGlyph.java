package cn.li.mc1211.client.font.msdf;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

import java.util.function.Function;

/**
 * Dual-mode glyph wrapper.
 * <ul>
 *   <li><b>Proportional</b> — passes through the original {@link GlyphInfo} unchanged.</li>
 *   <li><b>Monospace</b> — overrides {@link #getAdvance()} to a fixed grid width.
 *       Glyph rendering is not shifted (centering would require a Mixin on
 *       {@code BakedGlyph.render} because its fields are private).</li>
 * </ul>
 *
 * <p>Vertical placement reproduces the 1.20.1 convention exactly:</p>
 * <pre>
 *   1.20.1 quad top = y + bearingY - 3
 *                     (bearingY = ascentPx - inkTop; 3 = render constant)
 *   1.21.1 quad top = y + (7 - bitmap_top)   (7 = 1.21.1 ascent; no -3)
 * </pre>
 * With the unhinted provider the two per-glyph terms (bearingY, bitmap_top)
 * cancel, so the compensation is the CONSTANT
 * {@code ascentPx - (7 + 3) = ascentPx - 10} (see verticalShift()).
 */
public class MSDFAwareGlyph implements GlyphInfo {

    /** 1.21.1's SheetGlyphInfo.getTop() = ASCENT - bearingTop. */
    private static final float VANILLA_ASCENT = 7.0f;
    /** 1.20.1's BakedGlyph.render subtracted this from every glyph quad. */
    private static final float LEGACY_RENDER_SHIFT = 3.0f;

    private final GlyphInfo original;
    private final boolean monospace;
    private final float monospaceAdvance;
    private final MsdfFontFace face;

    public MSDFAwareGlyph(final GlyphInfo original, final boolean monospace,
                          final float monospaceAdvance, final MsdfFontFace face) {
        this.original = original;
        this.monospace = monospace;
        this.monospaceAdvance = monospaceAdvance;
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
                new VerticallyShiftedSheetGlyph(sgi, face)));
    }

    private static final class VerticallyShiftedSheetGlyph implements SheetGlyphInfo {
        private final SheetGlyphInfo delegate;
        private final MsdfFontFace face;

        VerticallyShiftedSheetGlyph(final SheetGlyphInfo delegate,
                                    final MsdfFontFace face) {
            this.delegate = delegate;
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
        public float getTop() {
            return delegate.getTop() + verticalShift();
        }

        @Override
        public float getBottom() {
            return delegate.getBottom() + verticalShift();
        }

        // Horizontal placement deliberately uses the rasterizer's native
        // metrics (no per-glyph adjustment): the provider rasterizes
        // unhinted, whose bitmap_left equals the 1.20.1 STB reference grid
        // exactly, so the quad left edge lands on the glyph's content left.
        // SheetGlyphInfo's bearing defaults are 0.0F/7.0F, so they MUST be
        // forwarded to the delegate — vanilla's default getLeft() derives
        // from getBearingLeft() (26.2's GlyphBitmap makes them abstract and
        // forces this; 1.21.1's defaults would silently zero the quad).
        @Override
        public float getBearingLeft() {
            return delegate.getBearingLeft();
        }

        @Override
        public float getBearingTop() {
            return delegate.getBearingTop();
        }

        /**
         * Constant shift for every glyph, derived from matching 1.20.1
         * exactly (the reference for vertical placement):
         * <pre>
         *   1.20.1 quad top = y + bearingY - 3   (render subtracts 3)
         *   1.21.1 quad top = y + 7 - bearingTop + shift   (render does not)
         * </pre>
         * 1.20.1's STB provider measures bearingY downward from the em top
         * (bearingY = ascentPx - inkTop; diagnostic logs: e = 11.65 =
         * 25.65 - 14, R = 6.65 = 25.65 - 19). The provider rasterizes
         * unhinted, whose bitmap_top equals the STB ink top (same scale —
         * the diagnostic logs confirm the metrics match per glyph), so the
         * two per-glyph terms cancel and the compensation is the CONSTANT
         * {@code ascentPx - (7 + 3) = ascentPx - 10} — exact for every
         * glyph. (26.2's shift formula is calibrated for its own GUI's y
         * semantics and must not be copied here.)
         */
        private float verticalShift() {
            return face.ascentPixels() - (VANILLA_ASCENT + LEGACY_RENDER_SHIFT);
        }
    }
}
