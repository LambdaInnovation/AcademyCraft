package cn.li.mcmod.math;

/**
 * Immutable 3-D double vector value type. Replaces {@code {:x :y :z}} maps on
 * hot render-loop paths — a map allocates 3 boxed Doubles plus a HAMT node
 * per operation; this is one object with three primitive fields.
 *
 * mcmod may not depend on net.minecraft.* / Loader APIs, so this is a plain
 * Java class rather than reusing Minecraft's Vec3.
 */
public final class V3 {
    public final double x, y, z;

    public V3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static V3 add(V3 a, V3 b) {
        return new V3(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static V3 sub(V3 a, V3 b) {
        return new V3(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static V3 scale(V3 a, double s) {
        return new V3(a.x * s, a.y * s, a.z * s);
    }

    public static V3 cross(V3 a, V3 b) {
        return new V3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x);
    }

    public static double dot(V3 a, V3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    public static double length(V3 a) {
        return Math.sqrt(dot(a, a));
    }

    /** Zero-length input returns (0, 1, 0), matching the prior map-based helper. */
    public static V3 normalize(V3 a) {
        double len = length(a);
        if (len < 1.0e-6) {
            return new V3(0.0, 1.0, 0.0);
        }
        return scale(a, 1.0 / len);
    }

    @Override
    public String toString() {
        return "V3[" + x + ", " + y + ", " + z + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof V3)) return false;
        V3 other = (V3) o;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        return result;
    }
}
