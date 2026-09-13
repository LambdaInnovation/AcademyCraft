(ns cn.li.neoforgebase.setup.common
  "NeoForge common-setup wiring shared by 1.21.1 and 26.2.

  Version loaders install step fns via install-common-setup-steps! and call
  shared-event-install before exposing run-common-setup!."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [cn.li.neoforgebase.bootstrap ForgeBootstrapGuard]))

(defonce ^:private steps-atom (atom nil))

(defn install-common-setup-steps!
  "Install ordered common-setup step fns (no-arg)."
  [m]
  (reset! steps-atom m)
  m)

(defn- steps []
  (let [m @steps-atom]
    (when (nil? m)
      (throw (IllegalStateException. "common-setup steps not installed")))
    m))

(defn run-common-setup!
  []
  (if-not (ForgeBootstrapGuard/markCommonSetupCompleteIfAbsent)
    (log/info "Forge common setup wiring already complete; skipping duplicate invocation")
    (let [s (steps)]
      ((:assert-scripted-blocks-bundled! s))
      ((:init-common-gui! s))
      ((:init-common-lifecycle! s))
      ((:init-forge-energy! s))
      ((:init-ic2-energy! s))
      ((:init-item-handler! s))
      ((:init-tutorial-events! s))
      ((:init-imc! s))
      ((:register-world-state-changed! s))
      ((:register-common-event-listeners! s))
      (log/info "Forge common setup wiring complete"))))
