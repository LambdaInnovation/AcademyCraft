package cn.li.mcver;

import net.minecraft.resources.Identifier;

/**
 * Version seam for identifier construction.
 * 26.2 renamed {@code Identifier} to {@link Identifier}; the factory
 * methods ({@link Identifier#fromNamespaceAndPath}, {@link Identifier#parse})
 * kept the same names.
 */
public final class ResourceLocations {
    private ResourceLocations() {
    }

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier parse(String id) {
        return Identifier.parse(id);
    }

    /** Native id class ({@link Identifier}) for type checks. */
    public static Class<?> idClass() {
        return Identifier.class;
    }

    /**
     * PackOutput.PathProvider.json() takes the version's own resource-location
     * type, and on 26.2 it is overloaded, so Clojure cannot resolve the call
     * from a shared (mcbase) datagen shell where the id is just an Object.
     * Cast it here, where the concrete type is known.
     */
    public static java.nio.file.Path jsonPath(net.minecraft.data.PackOutput.PathProvider provider, Object id) {
        return provider.json((net.minecraft.resources.Identifier) id);
    }
}
