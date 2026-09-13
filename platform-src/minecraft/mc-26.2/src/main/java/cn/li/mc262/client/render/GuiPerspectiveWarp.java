package cn.li.mc262.client.render;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/**
 * A frame-scoped projective warp for extracted GUI elements.
 *
 * <p>26.2 hands every GUI element a {@link Matrix3x2fc} pose. That can express
 * translation, rotation, scale and shear, but not the projective term that makes
 * a rotated plane's far edge shorter than its near edge. AcademyCraft's terminal
 * is drawn through a real {@code gluPerspective} camera whose far edge is ~20%
 * shorter than its near edge, so an affine pose renders it as a parallelogram
 * instead of a trapezoid — the panel is placed and tilted correctly but reads as
 * flat.</p>
 *
 * <p>The terminal is a plane and the camera is a pinhole, so the whole chain
 * collapses exactly into a 3x3 homography. Drawing helpers ask {@link #active()}
 * and, when a warp is installed, project their own geometry through it instead of
 * handing the pose to vanilla. {@code null} means no warp, so every other UI keeps
 * the untouched affine path.</p>
 *
 * <p>Client render thread only. The matrix is row-major:
 * {@code [Xw Yw w] = H * [x y 1]}, screen = {@code (Xw/w, Yw/w)}.</p>
 */
public final class GuiPerspectiveWarp {

    /** Below this the point is at or behind the eye and cannot be projected. */
    private static final float MIN_W = 1.0e-4F;

    private static float[] active;

    private GuiPerspectiveWarp() {}

    /** Install the warp for the current frame. Pass a row-major 3x3. */
    public static void set(float[] rowMajor3x3) {
        active = rowMajor3x3 != null && rowMajor3x3.length == 9 ? rowMajor3x3 : null;
    }

    public static void clear() {
        active = null;
    }

    /** The active warp, or {@code null} when GUI drawing is plain affine. */
    public static float[] active() {
        return active;
    }

    /**
     * Fold an affine GUI pose into the warp so callers can keep working in their
     * own local coordinates. The pose runs first, then the projection.
     */
    public static float[] compose(float[] h, Matrix3x2fc pose) {
        // pose as a 3x3: columns are (m00,m01), (m10,m11), (m20,m21).
        float a00 = pose == null ? 1.0F : pose.m00();
        float a01 = pose == null ? 0.0F : pose.m10();
        float a02 = pose == null ? 0.0F : pose.m20();
        float a10 = pose == null ? 0.0F : pose.m01();
        float a11 = pose == null ? 1.0F : pose.m11();
        float a12 = pose == null ? 0.0F : pose.m21();
        // The reactive tape draws at absolute coordinates under an identity
        // pose, so this is the overwhelmingly common case and it runs once per
        // element and once per glyph. Aliasing the caller's matrix is safe:
        // a warp is published as a fresh array each frame and never mutated in
        // place, and render states are consumed within that frame.
        if (a00 == 1.0F && a01 == 0.0F && a02 == 0.0F
                && a10 == 0.0F && a11 == 1.0F && a12 == 0.0F) {
            return h;
        }
        float[] out = new float[9];
        for (int row = 0; row < 3; row++) {
            float r0 = h[row * 3];
            float r1 = h[row * 3 + 1];
            float r2 = h[row * 3 + 2];
            out[row * 3] = r0 * a00 + r1 * a10;
            out[row * 3 + 1] = r0 * a01 + r1 * a11;
            out[row * 3 + 2] = r0 * a02 + r1 * a12 + r2;
        }
        return out;
    }

    /**
     * Project a point through the warp. Returns false when the point is at or
     * behind the eye, which the caller should treat as "draw nothing".
     */
    public static boolean project(float[] h, float x, float y, Vector2f out) {
        float w = h[6] * x + h[7] * y + h[8];
        if (!(w > MIN_W)) {
            return false;
        }
        out.set((h[0] * x + h[1] * y + h[2]) / w, (h[3] * x + h[4] * y + h[5]) / w);
        return true;
    }

    /**
     * The active warp, folded with {@code pose} and linearised at local
     * {@code (x, y)} — ready to replace {@code pose} outright.
     *
     * <p>For content vanilla will only draw through an affine pose: text, items,
     * picture-in-picture. Those are small next to the warped surface, so a
     * tangent plane taken at the element's own anchor is visually exact, while
     * one affine shared by the whole surface is not.</p>
     *
     * @return the local affine, or {@code null} when no warp is installed or the
     *         anchor cannot be projected
     */
    public static Matrix3x2f localAnchor(Matrix3x2fc pose, float x, float y) {
        float[] warp = active;
        return warp == null ? null : localAffine(compose(warp, pose), x, y);
    }

    /**
     * {@link #localAnchor} writing into a caller-owned matrix.
     *
     * <p>Text anchors once per glyph, so the allocation-free form is the one the
     * font path uses.</p>
     *
     * @return false when no warp is installed or the anchor cannot be projected,
     *         leaving {@code dest} untouched
     */
    public static boolean localAnchorInto(Matrix3x2fc pose, float x, float y, Matrix3x2f dest) {
        float[] warp = active;
        if (warp == null) {
            return false;
        }
        float[] h = compose(warp, pose);
        float w = h[6] * x + h[7] * y + h[8];
        if (!(w > MIN_W)) {
            return false;
        }
        float xw = h[0] * x + h[1] * y + h[2];
        float yw = h[3] * x + h[4] * y + h[5];
        float invW = 1.0F / w;
        float invW2 = invW * invW;
        dest.m00 = (h[0] * w - xw * h[6]) * invW2;
        dest.m01 = (h[3] * w - yw * h[6]) * invW2;
        dest.m10 = (h[1] * w - xw * h[7]) * invW2;
        dest.m11 = (h[4] * w - yw * h[7]) * invW2;
        dest.m20 = xw * invW;
        dest.m21 = yw * invW;
        return true;
    }

    /**
     * The given warp linearised at {@code (x, y)}: an affine matrix that lands
     * the origin exactly on the projected point and matches the projection's
     * first derivatives there.
     *
     * @return the local affine, or {@code null} if the anchor cannot be projected
     */
    public static Matrix3x2f localAffine(float[] h, float x, float y) {
        float w = h[6] * x + h[7] * y + h[8];
        if (!(w > MIN_W)) {
            return null;
        }
        float xw = h[0] * x + h[1] * y + h[2];
        float yw = h[3] * x + h[4] * y + h[5];
        float invW = 1.0F / w;
        float px = xw * invW;
        float py = yw * invW;
        float invW2 = invW * invW;
        // d(Xw/w)/dx etc. by the quotient rule.
        float dxdx = (h[0] * w - xw * h[6]) * invW2;
        float dxdy = (h[1] * w - xw * h[7]) * invW2;
        float dydx = (h[3] * w - yw * h[6]) * invW2;
        float dydy = (h[4] * w - yw * h[7]) * invW2;
        // Matrix3x2f is column-major: (m00,m01) (m10,m11) (m20,m21).
        return new Matrix3x2f(dxdx, dydx, dxdy, dydy, px, py);
    }
}
