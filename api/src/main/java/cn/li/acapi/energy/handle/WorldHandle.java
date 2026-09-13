package cn.li.acapi.energy.handle;

import java.util.Objects;

/**
 * Opaque world handle for wireless queries.
 */
public final class WorldHandle {
    private final Object rawWorld;

    private WorldHandle(Object rawWorld) {
        this.rawWorld = Objects.requireNonNull(rawWorld, "rawWorld");
    }

    public static WorldHandle of(Object rawWorld) {
        return new WorldHandle(rawWorld);
    }

    public Object rawWorld() {
        return rawWorld;
    }
}
