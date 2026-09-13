package cn.li.mcbase.runtime;

/**
 * Loader-agnostic adapter seam for entity/player resolution.
 */
public abstract class EntityLookupAdapter {
    public abstract Object getEntityByUuid(Object level, String entityUuid);

    public abstract Object getPlayerByUuid(Object server, String playerUuid);
}
