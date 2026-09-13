(ns cn.li.mcmod.runtime.require-lock
  "Serialized namespace loading for concurrent bootstrap paths.

  Clojure 1.12's plain `require` takes no lock — only the private
  `serialized-require` (used by `requiring-resolve`) locks on
  `clojure.lang.RT/REQUIRE_LOCK`. Forge/Fabric dispatch mod lifecycle events on
  parallel workers, so unlocked concurrent `require` calls into an overlapping
  namespace graph can observe a partially initialized namespace (a var defined
  late in a file may not be interned yet), or even race a failed load's
  `remove-ns` cleanup against another thread still using that namespace.

  `safe-require` locks on the same monitor `serialized-require` uses so every
  call site cooperates with it and with `requiring-resolve`.")

(defn safe-require
  "Like `require`, but serialized against every other caller of
  `safe-require`/`requiring-resolve` via `clojure.lang.RT/REQUIRE_LOCK`."
  [ns-sym]
  (locking clojure.lang.RT/REQUIRE_LOCK
    (require ns-sym)))
