package cn.li.mcbase.runtime;

/**
 * Loader-agnostic adapter seam for resolving current server instance.
 */
public abstract class ServerAccessAdapter {
    public abstract Object getCurrentServer();
}
