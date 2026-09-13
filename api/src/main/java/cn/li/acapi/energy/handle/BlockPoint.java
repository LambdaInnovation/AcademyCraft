package cn.li.acapi.energy.handle;

/**
 * Immutable block coordinate value object.
 */
public record BlockPoint(int x, int y, int z) {
    public static BlockPoint of(int x, int y, int z) {
        return new BlockPoint(x, y, z);
    }
}
