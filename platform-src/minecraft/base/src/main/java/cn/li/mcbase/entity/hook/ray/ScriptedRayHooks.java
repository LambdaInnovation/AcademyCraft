package cn.li.mcbase.entity.hook.ray;

import cn.li.mcbase.entity.hook.AbstractHookRegistry;

public final class ScriptedRayHooks {
    private static final Class<?> REGISTRY_CLASS = ScriptedRayHooks.class;
    private static final Class<? extends ScriptedRayHook> HOOK_INTERFACE = ScriptedRayHook.class;

    public static void register(String hookId, ScriptedRayHook hook) {
        AbstractHookRegistry.register(REGISTRY_CLASS, hookId, hook);
    }

    private static final ScriptedRayHook NOOP = new ScriptedRayHook() {
    };

    public static ScriptedRayHook resolve(String hookId) {
        ScriptedRayHook hook = AbstractHookRegistry.resolve(REGISTRY_CLASS, hookId);
        return hook != null ? hook : NOOP;
    }

    public static boolean registerByClassName(String hookId, String className) {
        return AbstractHookRegistry.registerByClassName(REGISTRY_CLASS, HOOK_INTERFACE, hookId, className);
    }
}
