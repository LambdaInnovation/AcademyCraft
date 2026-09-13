package cn.li.mc1211.client.font.msdf;

import cn.li.mcver.ResourceLocations;
import cn.li.mcmod.ModId;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Shadow font: isolated FontSet + Font for mod CGUI text.
 * Supports monospace/proportional dual mode via {@link MSDFAwareGlyph}.
 */
public final class MsdfFontManager {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final ResourceLocation SHADOW_FONT_ID =
            ResourceLocations.of(ModId.ID, "msdf_shadow");

    public static final float DESIGN_PIXEL_HEIGHT = 32.0f;
    public static final float CGUI_BASE_HEIGHT = 32.0f;

    private static Font shadowFont;
    private static FontSet shadowFontSet;
    private static MsdfFontFace face;
    private static volatile boolean bakeProbed;

    private static volatile boolean monospace;
    private static volatile float monospaceAdvance;

    private MsdfFontManager() {}

    public static boolean init(final Path fontPath) {
        synchronized (MsdfFontManager.class) {
            if (shadowFont != null) return true;
            try {
                if (!RenderSystem.isOnRenderThread()) {
                    // FontSet.reload creates GL textures (GlyphStitcher), which
                    // must happen on the render thread; clientTick retries here.
                    LOGGER.debug("MSDF init deferred: not on render thread");
                    return false;
                }
                final Minecraft mc = Minecraft.getInstance();
                if (mc == null) {
                    LOGGER.debug("MSDF init deferred: Minecraft not ready");
                    return false;
                }

                face = new MsdfFontFace(fontPath, DESIGN_PIXEL_HEIGHT);
                final GlyphProvider vanilla = face.glyphProvider();
                final GlyphProvider provider =
                        new MonospaceAwareGlyphProvider(vanilla, face);

                final FontSet fontSet =
                        new FontSet(mc.getTextureManager(), SHADOW_FONT_ID);
                fontSet.reload(
                        List.of(new GlyphProvider.Conditional(
                                provider, FontOption.Filter.ALWAYS_PASS)),
                        Set.of());
                shadowFontSet = fontSet;
                shadowFont = new Font(rl -> shadowFontSet, false);

                // Pre-warm glyph cache
                if (RenderSystem.isOnRenderThread()) {
                    fontSet.getGlyph('A');
                    fontSet.getGlyph(0x4E2D);
                }

                LOGGER.info(
                        "MSDF shadow font loaded from {} (A={}, U+4E2D={})",
                        fontPath,
                        face.hasGlyph('A'),
                        face.hasGlyph(0x4E2D));
                return true;
            } catch (Exception e) {
                LOGGER.error("MSDF font init failed for {}", fontPath, e);
                shutdown();
                return false;
            }
        }
    }

    public static void shutdown() {
        if (face != null) { face.close(); face = null; }
        shadowFont = null;
        shadowFontSet = null;
        bakeProbed = false;
    }

    public static void setMonospace(final boolean v) { monospace = v; }
    public static boolean isMonospace() { return monospace; }

    /** Fixed advance for monospace mode, derived from '0' glyph width. */
    public static float monospaceAdvance() { return monospaceAdvance; }

    public static boolean isAvailable() { return hasFontFace(); }
    public static boolean hasFontFace() { return shadowFont != null && face != null; }
    public static boolean hasGlyph(final int codePoint) {
        return face != null && face.hasGlyph(codePoint);
    }
    public static Font shadowFont() { return shadowFont; }
    public static float cguiBaseHeight() { return CGUI_BASE_HEIGHT; }

    public static void clientTick() {
        if (!bakeProbed && shadowFont != null) {
            bakeProbed = true;
            try {
                monospaceAdvance = shadowFont.width(Component.literal("0"));
                final int wi = shadowFont.width(Component.literal("i"));
                final int wW = shadowFont.width(Component.literal("W"));
                final int wCjk = shadowFont.width(Component.literal("中"));
                LOGGER.info("MSDF probe: i={} W={} U+4E2D={} monoAdv={}",
                        wi, wW, wCjk, monospaceAdvance);
            } catch (Exception e) {
                LOGGER.warn("MSDF probe failed", e);
            }
        }
    }
}
