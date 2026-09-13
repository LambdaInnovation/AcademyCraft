package cn.li.acapi.energy.handle;

import java.util.Objects;

/**
 * Opaque item handle used by the public energy API.
 *
 * <p>The wrapped runtime object is intentionally hidden behind a named type so
 * callers do not pass raw Object values through energy API signatures.
 */
public final class EnergyItemHandle {
    private final Object rawStack;

    private EnergyItemHandle(Object rawStack) {
        this.rawStack = Objects.requireNonNull(rawStack, "rawStack");
    }

    public static EnergyItemHandle of(Object rawStack) {
        return new EnergyItemHandle(rawStack);
    }

    public Object rawStack() {
        return rawStack;
    }
}
