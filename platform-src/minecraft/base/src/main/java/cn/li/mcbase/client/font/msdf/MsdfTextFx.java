package cn.li.mcbase.client.font.msdf;

/**
 * Per-flush MSDF text style and effect uniforms (read on render thread).
 */
public final class MsdfTextFx {

  public static final float DEFAULT_THICKNESS = 0.0f;
  public static final float BOLD_THICKNESS = 0.05f;

  private static volatile float thicknessOffset = DEFAULT_THICKNESS;
  private static volatile float outlineR = 0.0f;
  private static volatile float outlineG = 0.0f;
  private static volatile float outlineB = 0.0f;
  private static volatile float outlineA = 1.0f;
  private static volatile float outlineWidth = 0.0f;
  private static volatile float glowR = 1.0f;
  private static volatile float glowG = 0.8f;
  private static volatile float glowB = 0.0f;
  private static volatile float glowA = 0.5f;
  private static volatile float glowRadius = 0.0f;
  private static volatile float shadowOffsetX = 0.0f;
  private static volatile float shadowOffsetY = 0.0f;

  private MsdfTextFx() {
  }

  public static void resetForDraw(final boolean bold) {
    thicknessOffset = bold ? BOLD_THICKNESS : DEFAULT_THICKNESS;
    outlineWidth = 0.0f;
    glowRadius = 0.0f;
    shadowOffsetX = 0.0f;
    shadowOffsetY = 0.0f;
  }

  public static void setThicknessOffset(final float v) {
    thicknessOffset = v;
  }

  public static float getThicknessOffset() {
    return thicknessOffset;
  }

  public static void setOutline(final float r, final float g, final float b, final float a, final float width) {
    outlineR = r;
    outlineG = g;
    outlineB = b;
    outlineA = a;
    outlineWidth = width;
  }

  public static void setGlow(final float r, final float g, final float b, final float a, final float radius) {
    glowR = r;
    glowG = g;
    glowB = b;
    glowA = a;
    glowRadius = radius;
  }

  public static void setGlowRadius(final float radius) {
    glowRadius = radius;
  }

  public static void setShadowOffset(final float x, final float y) {
    shadowOffsetX = x;
    shadowOffsetY = y;
  }

  public static float getOutlineR() { return outlineR; }
  public static float getOutlineG() { return outlineG; }
  public static float getOutlineB() { return outlineB; }
  public static float getOutlineA() { return outlineA; }
  public static float getOutlineWidth() { return outlineWidth; }
  public static float getGlowR() { return glowR; }
  public static float getGlowG() { return glowG; }
  public static float getGlowB() { return glowB; }
  public static float getGlowA() { return glowA; }
  public static float getGlowRadius() { return glowRadius; }
  public static float getShadowOffsetX() { return shadowOffsetX; }
  public static float getShadowOffsetY() { return shadowOffsetY; }

  /** Max normalized field offset for conservative atlas bake padding. */
  public static float maxBakeFieldOffset() {
    return BOLD_THICKNESS + 0.15f + 0.15f + 0.05f;
  }

  /** Max normalized field offset used for atlas padding (thickness + outline + glow). */
  public static float maxFieldOffset() {
    return Math.max(thicknessOffset, outlineWidth + glowRadius + 0.05f);
  }
}
