(ns cn.li.mcmod.framework.service
  "Facade API for runtime dynamic services (:service/* sub-namespace).

   Unlike :registry/* which is frozen after init, :service/* may be read
   and written during gameplay. All write operations go through these guard
   functions to ensure consistent access patterns.

   For high-frequency shared mutable state (multiplayer effect counters, global
   buff counters), prefer java.util.concurrent.ConcurrentHashMap over Clojure
   atom swap! to avoid CAS spin-lock contention."
  )

(defn get-service
  "Read a service value from the framework.

   Pure deref — safe for concurrent reads.

   Args:
     fw — framework atom instance
     k  — service key, e.g. :lifecycle, :runtime-service"
  [fw k]
  (get @fw k))

(defn update-service!
  "Atomically update a service value.

   Uses swap! under the hood — safe for low-frequency writes.
   For high-frequency writes (multiplayer per-tick updates), use per-player
   atoms or ConcurrentHashMap to avoid CAS contention.

   Args:
     fw   — framework atom instance
     k    — service key
     f    — update function (receives current service value, returns new value)
     args — additional args passed to f after the current value"
  [fw k f & args]
  (apply swap! fw update k f args)
  nil)
