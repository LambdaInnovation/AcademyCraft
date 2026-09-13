package cn.li.fabric262.client.render.item;

/**
 * Compatibility marker retained for source consumers of the pre-26.2 OBJ
 * bridge. Fabric 26.2 uses the ItemModel pipeline, so no BakedModel wrapper
 * is created on this target.
 */
@Deprecated
public final class ObjBakedModel {
    private ObjBakedModel() {
    }
}
