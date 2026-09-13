package cn.li.acapi.energy.handle;

import java.util.Objects;

/**
 * Opaque reference to a wireless node connection model owned by runtime modules.
 */
public final class NodeConnectionRef {
    private final Object rawConnection;

    private NodeConnectionRef(Object rawConnection) {
        this.rawConnection = Objects.requireNonNull(rawConnection, "rawConnection");
    }

    public static NodeConnectionRef of(Object rawConnection) {
        return new NodeConnectionRef(rawConnection);
    }

    public Object rawConnection() {
        return rawConnection;
    }
}
