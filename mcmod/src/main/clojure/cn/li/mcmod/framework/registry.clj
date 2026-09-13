(ns cn.li.mcmod.framework.registry
  "Facade API for static content registries (:registry/* sub-namespace).

   All registry write operations go through these guard functions.
   Direct swap!/reset! on the framework atom is forbidden.

   After content init completes, freeze-all! locks every registry domain.
   Frozen registries are lock-free HAMT structures — safe for concurrent
   reads from any thread with zero synchronization overhead."
  )

(def ^:private frozen-path
  "Freeze flags live outside the domain maps themselves, keyed by domain.
   Keeping them out of [:registry domain id] space means a domain's spec
   map is pure content data — vals/get-spec never see a stray flag entry."
  [:registry :_frozen])

(defn register!
  "Register a content spec into a registry domain.

   Rejects writes if the domain is frozen (content init already completed).
   Returns nil (mutation is purely a side effect on the framework atom).

   Args:
     fw   — framework atom instance
     domain — registry domain keyword, e.g. :blocks, :items, :particles
     id   — unique identifier within the domain (keyword or string)
     spec — the content spec map to store"
  [fw domain id spec]
  (when (get-in @fw (conj frozen-path domain))
    (throw (ex-info (str "Registry domain :" (name domain) " is frozen — content init already completed")
                    {:domain domain :id id})))
  (swap! fw assoc-in [:registry domain id] spec)
  nil)

(defn get-spec
  "Read a content spec from a registry domain.

   Pure HAMT lookup — safe for concurrent reads from any thread.
   Returns nil if the domain or id does not exist.

   Args:
     fw     — framework atom instance
     domain — registry domain keyword
     id     — spec identifier"
  [fw domain id]
  (get-in @fw [:registry domain id]))

(defn list-specs
  "List all specs in a registry domain.

   Returns a seq of spec values (not key-value pairs).
   Returns empty seq for unknown domains."
  [fw domain]
  (vals (get-in @fw [:registry domain])))

(defn frozen?
  "Check if a registry domain is frozen."
  [fw domain]
  (boolean (get-in @fw (conj frozen-path domain))))

(defn freeze!
  "Lock a single registry domain for writes.

   After freezing, register! will throw on any write attempt.
   Reads remain lock-free and thread-safe."
  [fw domain]
  (swap! fw assoc-in (conj frozen-path domain) true)
  nil)

(defn reset-domain-for-test!
  "Test-only: clear every spec in a registry domain (and its frozen flag)
   without touching sibling domains. Bypasses the frozen-write guard
   deliberately — tests need to rebuild a domain regardless of freeze state."
  [fw domain]
  (swap! fw (fn [state]
              (-> state
                  (assoc-in [:registry domain] {})
                  (update-in frozen-path dissoc domain))))
  nil)

(defn freeze-all!
  "Lock ALL registry domains for writes.

   Called once after content init completes (run-content-init! hook).
   After this point, every get-spec call is a lock-free HAMT pointer chase
   with zero CAS overhead and zero contention."
  [fw]
  (swap! fw (fn [state]
              (assoc-in state frozen-path
                        (zipmap (remove #{:_frozen} (keys (:registry state)))
                                (repeat true)))))
  nil)
