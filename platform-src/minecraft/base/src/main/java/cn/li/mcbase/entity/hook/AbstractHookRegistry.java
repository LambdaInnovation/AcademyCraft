package cn.li.mcbase.entity.hook;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic hook registry for entity-specific hooks.
 * Provides common registration, resolution, and reflection-based loading patterns.
 *
 * Subclasses must implement:
 * - {@link #getHookInterface()} to provide the hook interface class for type checking
 * - {@link #createNoop()} to provide a noop implementation when no hook is registered
 *
 * @param <T> The hook interface type (e.g., ClientEntityHook<ScriptedEffectEntity>)
 */
public final class AbstractHookRegistry {
    private static final Map<Class<?>, Map<String, ?>> REGISTRIES = new ConcurrentHashMap<>();
    private AbstractHookRegistry() {
    }
    /**
     * Register a hook implementation by hook ID.
     * Null checks on parameters ensure safe no-op behavior.
     */
    public static <T> void register(Class<?> registryClass, String hookId, T hook) {
        if (hookId == null || hookId.isEmpty() || hook == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, T> hooks = (Map<String, T>) (Object) getRegistryMap(registryClass);
        hooks.put(hookId, hook);
    }
    /**
     * Resolve a hook by hook ID.
     * Returns the registered hook or null if not found.
     */
    public static <T> T resolve(Class<?> registryClass, String hookId) {
        if (hookId == null || hookId.isEmpty()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, T> hooks = (Map<String, T>) (Object) getRegistryMap(registryClass);
        return hooks.get(hookId);
    }
    /**
     * Register a hook by loading its class via reflection.
     * Performs type checking to ensure the class implements the hook interface.
     * Returns true if successfully registered, false otherwise.
     */
    public static <T> boolean registerByClassName(Class<?> registryClass, Class<? extends T> hookInterface, String hookId, String className) {
        if (hookId == null || hookId.isEmpty() || className == null || className.isEmpty()) {
            return false;
        }
        try {
            Class<?> rawClass = Class.forName(className);
            if (!hookInterface.isAssignableFrom(rawClass)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Class<? extends T> hookClass = (Class<? extends T>) rawClass;
            T hook = hookClass.getDeclaredConstructor().newInstance();
            register(registryClass, hookId, hook);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
    /**
     * Get the registry map for this class instance.
     * Each subclass gets its own registry to avoid cross-contamination.
     */
    private static Map<String, ?> getRegistryMap(Class<?> registryClass) {
        return REGISTRIES.computeIfAbsent(registryClass, clz -> new ConcurrentHashMap<>());
    }
}
