package cn.li.mcbase.clj;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

/**
 * Shared lightweight helpers for Java→Clojure interop in platform shells.
 */
public final class ClojureInterop {

    private ClojureInterop() {
    }

    public static void requireNamespace(String namespace) {
        // Clojure 1.12's plain `require` takes no lock (only the private
        // serialized-require/requiring-resolve path does). Forge/Fabric can
        // dispatch mod lifecycle events on parallel workers, so concurrent
        // requires of overlapping namespace graphs can observe a partially
        // initialized namespace. Lock on the same monitor serialized-require
        // uses so all Java entry points cooperate with it.
        synchronized (clojure.lang.RT.REQUIRE_LOCK) {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read(namespace));
        }
    }

    public static Object invoke(String namespace, String functionName, Object... args) {
        IFn fn = Clojure.var(namespace, functionName);
        return fn.applyTo(clojure.lang.RT.seq(args));
    }
}
