package cn.li.mcbase.client.font.msdf;

/**
 * Per-glyph MSDF style flags encoded in the low 3 bits of the vertex color blue channel.
 * Enables mixed bold/outline/glow within a single draw batch (see msdf_text.fsh).
 */
public final class MsdfGlyphFlags {

    public static final int FLAG_BOLD = 0x01;
    public static final int FLAG_OUTLINE = 0x02;
    public static final int FLAG_GLOW = 0x04;
    public static final int FLAG_MASK = 0x07;

    private MsdfGlyphFlags() {
    }

    public static int encodeRgb(final int rgb, final int flags) {
        if ((flags & FLAG_MASK) == 0) {
            return rgb & 0xFFFFFF;
        }
        return (rgb & 0xFFFFF8) | (flags & FLAG_MASK);
    }

    public static int encodeRgb(final int rgb, final boolean bold, final boolean outline, final boolean glow) {
        int flags = 0;
        if (bold) {
            flags |= FLAG_BOLD;
        }
        if (outline) {
            flags |= FLAG_OUTLINE;
        }
        if (glow) {
            flags |= FLAG_GLOW;
        }
        return encodeRgb(rgb, flags);
    }
}
