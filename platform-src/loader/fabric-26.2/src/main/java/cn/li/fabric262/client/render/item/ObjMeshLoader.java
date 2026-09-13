package cn.li.fabric262.client.render.item;

/**
 * Compatibility marker for the removed pre-26.2 BakedQuad OBJ loader.
 * Custom item geometry must be ported to Fabric's ItemModel API before it can
 * be enabled on this target.
 */
@Deprecated
public final class ObjMeshLoader {
    private ObjMeshLoader() {
    }
}
