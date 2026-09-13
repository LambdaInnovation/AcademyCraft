package cn.li.mcver;

import net.minecraft.resources.ResourceLocation;

/**
 * Version seam for ResourceLocation construction.
 * Contract matches the versioned factory methods exposed by the mapped API.
 */
public final class ResourceLocations {
    private ResourceLocations() {
    }

    public static ResourceLocation of(String namespace, String path) {
        ResourceLocation location = ResourceLocation.tryBuild(namespace, path);
        if (location == null) {
            throw new IllegalArgumentException("Invalid resource location: " + namespace + ":" + path);
        }
        return location;
    }

    public static ResourceLocation parse(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            throw new IllegalArgumentException("Invalid resource location: " + id);
        }
        return location;
    }
    /** Native id class ({@link ResourceLocation}) for type checks. */
    public static Class<?> idClass() {
        return ResourceLocation.class;
    }

    /**
     * PackOutput.PathProvider.json() takes the version's own resource-location
     * type, and on 26.2 it is overloaded, so Clojure cannot resolve the call
     * from a shared (mcbase) datagen shell where the id is just an Object.
     * Cast it here, where the concrete type is known.
     */
    public static java.nio.file.Path jsonPath(net.minecraft.data.PackOutput.PathProvider provider, Object id) {
        return provider.json((net.minecraft.resources.ResourceLocation) id);
    }
}
