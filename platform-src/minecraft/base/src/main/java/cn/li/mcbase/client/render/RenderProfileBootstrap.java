package cn.li.mcbase.client.render;

import cn.li.mcbase.clj.ClojureInterop;

/**
 * Shared client render bootstrap for content-provided render profile hooks.
 */
public final class RenderProfileBootstrap {
    private static final String LIFECYCLE_NS = "cn.li.mcmod.lifecycle";

    private RenderProfileBootstrap() {
    }

    public static void runContentClientInitHooks() {
        try {
            ClojureInterop.requireNamespace(LIFECYCLE_NS);
            ClojureInterop.invoke(LIFECYCLE_NS, "run-client-init!");
        } catch (Throwable ignored) {
            // Keep native/default renderer paths operational even if content hooks fail.
        }
    }
}
