(ns cn.li.forge1201.init
  "Forge 1.20.1 initialization and version-specific implementations"
  (:require [cn.li.mcbase.bootstrap.init-common :as init-common]
            [cn.li.platform.target :as target]
            [cn.li.platform.bootstrap :as platform-bootstrap]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.forge1201.capability.fluid-handler :as fluid-handler]
            [cn.li.forge1201.integration.imc-dispatch :as imc-dispatch]
            [cn.li.forge1201.achievement.bridge :as achievement-bridge])
  (:import [cn.li.mc1201.trigger ModTriggers]))

(defn set-version!
  "Set the forge version for multimethod dispatch"
  []
  (init-common/set-platform-version! (target/current-target-key!)))

(defn- assert-platform-ready!
  []
  (init-common/assert-platform-ready! (target/current-target-key!)))

(defn init-from-java
  "Called from Java @Mod constructor - sets up version dispatch"
  []
  (init-common/init-from-java!
    (target/current-target-key!)
    (fn []
      ;; Bind platform-neutral event bridge to Forge IMC dispatcher.
      (when-let [fw-atom (fw/fw-atom)]
        (platform/install-adapter! fw-atom :event-bus {:fire! imc-dispatch/dispatch-event!}))
      ;; Register custom advancement triggers.
      (ModTriggers/init)
      ;; Register Forge-specific capabilities (must precede content init so that
      ;; handler factories are available when register-tile-capability! is called).
      (fluid-handler/register!)
      ;; Ensure discovered content init providers are registered through the generic content SPI.
      (let [target-model (target/current-target!)]
        (platform-bootstrap/initialize-common-content! target-model))
      (achievement-bridge/init!))))
