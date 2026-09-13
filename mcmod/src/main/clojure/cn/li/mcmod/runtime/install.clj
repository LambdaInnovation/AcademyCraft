(ns cn.li.mcmod.runtime.install
  "Unified exactly-once / SPI injection primitives.

   Replaces all other init-guard shapes (defonce-guard atoms, ^:dynamic
   boolean guards + Object locks, top-level let-closure atoms) project-wide.

   Two exactly-once entry points:
   - framework-once! — flag lives inside the Framework atom. A fresh
     Framework injection (with-fresh-framework in tests, or a real
     integrated-server world reload) resets the flag automatically, so
     the guarded action re-runs. Use for anything whose effect is scoped
     to Framework registries/services (content registration, SPI wiring
     that reads/writes the Framework atom).
   - process-once! — flag lives in a single process-wide atom (the one
     new top-level atom this namespace introduces; whitelisted). Use only
     for genuine JVM-level one-shot side effects that must NOT redo on
     Framework reinjection: event-bus listener registration, native/GLFW
     hookup, defmethod dispatch-table loading.

   install-root! replaces the '^:dynamic + Object lock + alter-var-root'
   three-piece SPI-holder pattern with a plain root-bound var."
  (:require [cn.li.mcmod.aot :as aot]
            [cn.li.mcmod.framework :as fw]))

;; ============================================================================
;; framework-once! — Framework-lifecycle-scoped exactly-once
;; ============================================================================

(def ^:private flags-path [:service :install :flags])

(defn framework-once!
  "Run f exactly once per Framework lifetime, keyed by install-key.

   - AOT compile phase: no-op, returns nil (framework is nil during AOT).
   - Runtime, framework not yet injected: fail-fast ex-info (surfaces a
     lifecycle-ordering bug rather than silently no-op-ing).
   - f throwing rolls the flag back, so a retry can re-attempt.

   Returns true if this call executed f, false if already claimed."
  [install-key f]
  (when-not (aot/compiling?)
    (let [fw-atom (or (fw/fw-atom)
                       (throw (ex-info "framework-once! called before framework injection"
                                       {:install-key install-key})))
          key-path (conj flags-path install-key)
          [old _] (swap-vals! fw-atom assoc-in key-path true)]
      (if (get-in old key-path)
        false
        (try
          (f)
          true
          (catch Throwable t
            (swap! fw-atom update-in flags-path dissoc install-key)
            (throw t)))))))

(defn framework-once-done?
  "Query whether a framework-once! install-key has already been claimed
   in the current Framework lifetime. Returns false if framework isn't
   injected (e.g. AOT phase)."
  [install-key]
  (boolean (when-let [fw-atom (fw/fw-atom)]
             (get-in @fw-atom (conj flags-path install-key)))))

(defn reset-framework-once-flag-for-test!
  "Test-only: clear a single framework-once! flag so its action can rerun,
   without resetting the whole Framework (with-fresh-framework already does
   that; use this when a test needs finer-grained control over one flag)."
  [install-key]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in flags-path dissoc install-key))
  nil)

;; ============================================================================
;; process-once! — JVM-process-scoped exactly-once
;; ============================================================================

(def ^:private process-flags
  ;; The single project-wide process-level guard registry. Whitelisted as
  ;; the sole new top-level atom this refactor introduces. Only for
  ;; JVM-level one-shot side effects (event-bus listeners, native hookup,
  ;; defmethod dispatch-table require) that must not redo on Framework
  ;; reinjection.
  (atom #{}))

(defn process-once!
  "Run f exactly once per JVM process, keyed by install-key.
   AOT compile phase: no-op. Returns true if this call executed f."
  [install-key f]
  (when-not (aot/compiling?)
    (let [[old _] (swap-vals! process-flags conj install-key)]
      (when-not (contains? old install-key)
        (try
          (f)
          true
          (catch Throwable t
            (swap! process-flags disj install-key)
            (throw t)))))))

(defn reset-process-flag-for-test!
  "Test-only: clear a single process-once! flag so its action can rerun."
  [install-key]
  (swap! process-flags disj install-key)
  nil)

;; ============================================================================
;; install-root! — SPI injection without ^:dynamic
;; ============================================================================

(defn install-root!
  "Install impl as the root binding of target-var.

   Replaces the '^:dynamic + Object lock + alter-var-root' SPI-holder
   pattern: callers write once at init/install time (single writer),
   readers deref the var directly (plain root lookup, no thread-binding
   frame chase).

   target-var must be a Var (pass with #'), e.g.:
     (install-root! #'server-context-impl impl)"
  [target-var impl]
  (alter-var-root target-var (constantly impl))
  nil)
