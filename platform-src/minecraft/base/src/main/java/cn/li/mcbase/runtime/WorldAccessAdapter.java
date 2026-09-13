package cn.li.mcbase.runtime;

/**
 * Loader-agnostic adapter seam for resolving world/level references.
 */
public abstract class WorldAccessAdapter {
    public abstract Object getLevel(Object server, String worldId);
}
