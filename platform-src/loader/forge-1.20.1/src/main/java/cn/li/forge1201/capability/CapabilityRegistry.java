package cn.li.forge1201.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified named capability registry.
 *
 * <p>All topology capabilities are keyed by stable string ids. Known capabilities
 * should be registered during platform init; unknown keys are lazily backed by one
 * anonymous token capability and then cached.</p>
 */
public final class CapabilityRegistry {

    private static final Map<String, Capability<?>> KEY_TO_CAP = new ConcurrentHashMap<>();
    private static final Map<Capability<?>, String> CAP_TO_KEY = new IdentityHashMap<>();

    private CapabilityRegistry() {
    }

    private static Capability<Object> createAnonymousCapability() {
        return CapabilityManager.get(new CapabilityToken<>() {});
    }

    public static synchronized <T> void register(String key, Capability<T> cap) {
        KEY_TO_CAP.put(key, cap);
        CAP_TO_KEY.put(cap, key);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> Capability<T> getOrCreate(String key) {
        Capability<?> existing = KEY_TO_CAP.get(key);
        if (existing != null) {
            return (Capability<T>) existing;
        }
        Capability<T> cap = (Capability<T>) createAnonymousCapability();
        register(key, cap);
        return cap;
    }

    public static synchronized String getKey(Capability<?> cap) {
        return CAP_TO_KEY.get(cap);
    }

    @SuppressWarnings("unchecked")
    public static <T> Capability<T> get(String key) {
        return (Capability<T>) KEY_TO_CAP.get(key);
    }
}
