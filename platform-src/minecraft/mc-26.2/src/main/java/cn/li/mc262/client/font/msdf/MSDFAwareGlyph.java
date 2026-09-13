package cn.li.mc262.client.font.msdf;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

/**
 * Dual-mode glyph wrapper for 26.2's {@link UnbakedGlyph} model.
 * <ul>
 *   <li><b>Proportional</b> — passes through the original glyph unchanged.</li>
 *   <li><b>Monospace</b> — overrides {@link GlyphInfo#getAdvance()} to a fixed
 *       grid width via a wrapping info().</li>
 * </ul>
 *
 * <p>Vertical placement reproduces the 1.20.1 convention exactly (26.2's
 * GlyphBitmap.getTop() is vanilla's {@code 7.0F - bearingTop}):</p>
 * <pre>
 *   1.20.1 quad top = y + (pixelHeight - descenderPx - 3) + y0_stb
 *                     (3 = 1.20.1 BakedGlyph.render constant)
 *   26.2 quad top    = y + (7 - bitmap_top)                (7 = vanilla ascent)
 * </pre>
 * Because the glyph content sits {@code bearingTop - xheight} pixels below
 * the bitmap top, the two per-glyph terms cancel and the compensation is a
 * CONSTANT shift per font — no per-glyph term (that would re-introduce the
 * bitmap-top variation for t/r/h vs a/m/n):
 * {@code ascentPx - (7 + 3)}. The bake() wraps the Stitcher so the FreeType
 * glyph's bitmap can be shifted before it is stitched into the font texture.
 */
public final class MSDFAwareGlyph implements UnbakedGlyph {

    /** 26.2's GlyphBitmap.getTop() = ASCENT - bearingTop. */
    private static final float VANILLA_ASCENT = 7.0f;
    /** 1.20.1's BakedGlyph.render subtracted this from every glyph quad. */
    private static final float LEGACY_RENDER_SHIFT = 3.0f;

    private final UnbakedGlyph original;
    private final boolean monospace;
    private final float monospaceAdvance;
    private final GlyphInfo monospaceInfo;
    private final int codePoint;
    private final MsdfFontFace face;

    public MSDFAwareGlyph(final UnbakedGlyph original, final boolean monospace,
                          final float monospaceAdvance, final int codePoint,
                          final MsdfFontFace face) {
        this.original = original;
        this.monospace = monospace;
        this.monospaceAdvance = monospaceAdvance;
        this.codePoint = codePoint;
        this.face = face;
        final GlyphInfo base = original.info();
        this.monospaceInfo = new GlyphInfo() {
            @Override
            public float getAdvance() {
                return monospaceAdvance;
            }

            @Override
            public float getBoldOffset() {
                return base.getBoldOffset();
            }

            @Override
            public float getShadowOffset() {
                return base.getShadowOffset();
            }
        };
    }

    @Override
    public GlyphInfo info() {
        return monospace ? monospaceInfo : original.info();
    }

    @Override
    public BakedGlyph bake(final Stitcher stitcher) {
        return original.bake(new Stitcher() {
            @Override
            public BakedGlyph stitch(final GlyphInfo info, final GlyphBitmap glyphBitmap) {
                return stitcher.stitch(info,
                        new VerticallyShiftedGlyphBitmap(
                                glyphBitmap, codePoint, face));
            }

            @Override
            public BakedGlyph getMissing() {
                return stitcher.getMissing();
            }
        });
    }

    private static final class VerticallyShiftedGlyphBitmap implements GlyphBitmap {
        private final GlyphBitmap delegate;
        private final int codePoint;
        private final MsdfFontFace face;

        VerticallyShiftedGlyphBitmap(final GlyphBitmap delegate,
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
        public void upload(final int x, final int y, final GpuTexture texture) {
            delegate.upload(x, y, texture);
        }

        @Override
        public boolean isColored() {
            return delegate.isColored();
        }

        @Override
        public float getOversample() {
            return delegate.getOversample();
        }

        /**
         * Horizontal placement: the quad starts at the FreeType bitmap-left
         * (bbox left edge); the glyph content sits inside the bitmap after
         * its left bearing columns, so the rendered position equals the
         * glyph's content left edge.
         */
        @Override
        public float getLeft() {
            return delegate.getBearingLeft();
        }

        @Override
        public float getTop() {
            return delegate.getTop() + verticalShift();
        }

        @Override
        public float getBottom() {
            return delegate.getBottom() + verticalShift();
        }

        /**
         * Constant shift for every glyph. The rendered glyph top is
         *   top = (7 - bearingTop) + shift, and the visual glyph top (the
         * opaque content) sits below the bitmap top by
         * (bearingTop - xheight). Both bearingTop terms cancel:
         *   visual top = 7 + shift - xheight
            * So a CONSTANT shift aligns all x-height content on one line —
            * per-glyph y0/bearingTop terms would re-introduce the bitmap-top
            * variation (visible for t/r/h vs a/m/n) and must not be in the
            * formula. The FreeType provider already rasterizes at the matching
            * size, so rescaling this compensation would shift the whole face.
         */
        private float verticalShift() {
                return face.ascentPixels() - (VANILLA_ASCENT + LEGACY_RENDER_SHIFT);
        }
    }
}
