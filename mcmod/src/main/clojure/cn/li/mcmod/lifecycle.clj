(ns cn.li.mcmod.lifecycle
  "Content lifecycle coordination across platform adapters.

   All lifecycle state is stored in the single Framework atom
   under [:service :lifecycle]. Content modules register their
   init callbacks through these functions; platform adapters
   invoke the run-*! functions at the appropriate lifecycle phases.

   Replaced pattern:
     BEFORE: private delay singleton + ^:dynamic *lifecycle-runtime* fallback
     AFTER:  reads/writes (get-in @(fw/fw-atom) [:service :lifecycle])"
  (:require [cn.li.mcmod.framework :as fw]))

;; ============================================================================
;; Lifecycle state shape
;; ============================================================================

(defn- default-lifecycle-state
  "Return the initial lifecycle state map."
  []
  {:content-init-fns []
   :runtime-content-activation-fns []
   :world-tick-fns []
   :datagen-metadata-init-fns []
   :client-init-fns []
   :client-init-done? false
   :post-spi-client-init-fns []})

;; ============================================================================
;; State access helpers (internal)
;; ============================================================================

(defn- lifecycle-state
  "Read current lifecycle state from Framework.
   Returns nil during AOT compilation when holder is nil."
  []
  (when-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom [:service :lifecycle])))

(defn- update-lifecycle!
  "Atomically update lifecycle state within Framework.
   Initializes state if not yet present.
   No-op during AOT compilation when framework is nil."
  [f & args]
  (let [fw-atom (fw/fw-atom)]
    (when fw-atom
      (swap! fw-atom
           (fn [fw-state]
             (update-in fw-state [:service :lifecycle]
                        (fn [current]
                          (apply f (or current (default-lifecycle-state)) args))))))
  nil))

;; ============================================================================
;; Test support
;; ============================================================================

(defn reset-lifecycle-state-for-test!
  "Reset lifecycle state for tests."
  []
  (swap! (fw/fw-atom) assoc-in [:service :lifecycle] (default-lifecycle-state))
  nil)

;; ============================================================================
;; Content Init
;; ============================================================================

(defn register-content-init!
  "Register content init function (fn [] ...).
   Called by content modules declared in generated suite metadata.
   The function will be executed by platform adapters via run-content-init!."
  [init-fn]
  (update-lifecycle! update :content-init-fns conj init-fn)
  nil)

(defn run-content-init!
  "Run registered content init function, if present."
  []
  (doseq [f (:content-init-fns (lifecycle-state))]
    (f)))

;; ============================================================================
;; Runtime Content Activation
;; ============================================================================

(defn register-runtime-content-activation!
  "Register runtime content activation function (fn [] ...).
   Content modules should register their runtime-loader activation
   through this hook so platform adapters do not reference content
   namespaces directly."
  [activate-fn]
  (update-lifecycle! update :runtime-content-activation-fns conj activate-fn)
  nil)

(defn run-runtime-content-activation!
  "Run registered runtime content activation function, if present."
  []
  (doseq [f (:runtime-content-activation-fns (lifecycle-state))]
    (f)))

(defn register-world-tick! [tick-fn]
  (update-lifecycle! update :world-tick-fns conj tick-fn)
  nil)

(defn run-world-tick! [world]
  (doseq [f (:world-tick-fns (lifecycle-state))]
    (f world)))

;; ============================================================================
;; Datagen Metadata Init
;; ============================================================================

(defn register-datagen-metadata-init!
  "Register content-owned datagen metadata initialization (fn [] ...).
   Platform datagen entrypoints execute these hooks through the selected Minecraft component
   shared setup utilities instead of referencing concrete content namespaces."
  [init-fn]
  (update-lifecycle! update :datagen-metadata-init-fns conj init-fn)
  nil)

(defn run-datagen-metadata-init!
  "Run all registered datagen metadata initialization hooks."
  []
  (doseq [f (:datagen-metadata-init-fns (lifecycle-state))]
    (f)))

;; ============================================================================
;; Client Init
;; ============================================================================

(defn register-client-init!
  "Register client-side init function. Called by content modules.
   The function will be executed by platform adapters during client setup."
  [init-fn]
  (update-lifecycle! update :client-init-fns conj init-fn)
  nil)

(defn run-client-init!
  "Run all registered client init functions exactly once per session.

   Platform adapters legitimately invoke this from more than one call site
   because Forge/Fabric can dispatch RegisterRenderers before FMLClientSetup
   enqueued work (see RenderProfileBootstrap.runContentClientInitHooks and
   register-scripted-block-entity-renderers!). Client init hooks (UI widget
   factories, fonts, push handlers) are run-once installations, so a redundant
   invocation must be a no-op rather than re-register and crash. The done-flag
   is claimed atomically before running so concurrent dispatch on the render
   thread can never double-run the hooks."
  []
  (let [fw-atom (fw/fw-atom)
        first-run?
        (if fw-atom
          (let [[old _] (swap-vals! fw-atom
                                    (fn [fw-state]
                                      (update-in fw-state [:service :lifecycle]
                                                 (fn [current]
                                                   (assoc (or current (default-lifecycle-state))
                                                          :client-init-done? true)))))]
            (not (get-in old [:service :lifecycle :client-init-done?])))
          true)]
    (when first-run?
      (doseq [f (:client-init-fns (lifecycle-state))]
        (f)))))

;; ============================================================================
;; Post-SPI Client Init
;; ============================================================================

(defn register-post-spi-client-init!
  "Register a post-SPI client init function (fn [] ...).
   These callbacks run after platform SPI implementations are installed
   but before KeyMapping/input registration."
  [init-fn]
  (update-lifecycle! update :post-spi-client-init-fns conj init-fn)
  nil)

(defn run-post-spi-client-init!
  "Run all registered post-SPI client init functions."
  []
  (doseq [f (:post-spi-client-init-fns (lifecycle-state))]
    (f)))
