package cn.li.mcbase.entity.hook.effect;

import cn.li.mcbase.entity.hook.AbstractHookRegistry;

public final class ScriptedEffectHooks {
    private static final Class<?> REGISTRY_CLASS = ScriptedEffectHooks.class;
    private static final Class<? extends ScriptedEffectHook> HOOK_INTERFACE = ScriptedEffectHook.class;

    public static void register(String hookId, ScriptedEffectHook hook) {
        AbstractHookRegistry.register(REGISTRY_CLASS, hookId, hook);
    }

    private static final ScriptedEffectHook NOOP = new ScriptedEffectHook() {
    };

    public static ScriptedEffectHook resolve(String hookId) {
        ScriptedEffectHook hook = AbstractHookRegistry.resolve(REGISTRY_CLASS, hookId);
        return hook != null ? hook : NOOP;
    }

    public static boolean registerByClassName(String hookId, String className) {
        return AbstractHookRegistry.registerByClassName(REGISTRY_CLASS, HOOK_INTERFACE, hookId, className);
    }
}
