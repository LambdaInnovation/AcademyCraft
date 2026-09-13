(ns cn.li.mcmod.client.content-actions
  "Client content action hooks via Framework function map.

   Content actions stored at [:platform :content-actions].
   Client tick hooks stored at [:service :client-tick-hooks]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn install-client-content-actions!
  [actions _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :content-actions] actions)) nil)

(defn content-actions-available?
  []
  (boolean (get-in @(fw/fw-atom) [:platform :content-actions])))

;; ============================================================================
;; Client tick hooks
;; ============================================================================

(defn register-client-tick-hook!
  "Register a zero-arg fn to run on every client tick."
  [f]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in [:service :client-tick-hooks] (fn [v] (conj (or v []) f)))))

(defn- log-tick-hook-error!
  "Log a tick-hook failure at most once per unique (hook, error-signature).

   A hook that throws on every client tick would otherwise spam a full,
   Forge-decorated (`re:classloading` …) stack trace ~20×/second — and that
   repeated logging (exception fill-in-stack-trace + per-frame transform
   decoration + I/O), not the hook body, is the real per-tick cost. We log
   the first occurrence of each distinct error in full so the underlying bug
   stays diagnosable, then suppress identical repeats. A changed error
   signature logs again."
  [fw-atom f ^Throwable e]
  (let [sig  [(class e) (ex-message e)]
        path [:service :client-tick-hook-errors f]]
    (when (not= sig (get-in @fw-atom path))
      (swap! fw-atom assoc-in path sig)
      (log/stacktrace "Client tick hook failed (identical repeats suppressed)" e))))

(defn run-client-tick-hooks!
  "Run all registered client tick hooks. Called by platform client tick handler."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (doseq [f (get-in @fw-atom [:service :client-tick-hooks] [])]
      (try (f) (catch Throwable e (log-tick-hook-error! fw-atom f e))))))

(defn reset-client-content-actions-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in [:platform :content-actions] nil)
    (swap! fw-atom assoc-in [:service :client-tick-hooks] [])
    (swap! fw-atom assoc-in [:service :client-tick-hook-errors] nil))
  nil)
