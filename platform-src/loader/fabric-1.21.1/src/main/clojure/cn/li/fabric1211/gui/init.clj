(ns cn.li.fabric1211.gui.init
  "Fabric 1.21.1 GUI System Initialization"
  (:require [cn.li.mcbase.gui.init.orchestrator :as gui-orchestrator]
            [cn.li.mcbase.gui.init.checks :as init-checks]
            [cn.li.mcbase.runtime.spi.gui-registry :as registry-api]
            [cn.li.platform.target :as target]
            [cn.li.platform.neutral.gui-runtime :as gui]
            [cn.li.fabric1211.adapter.gui-registry :as registry-impl]
            [cn.li.fabric1211.gui.network.server :as network-server]
            [cn.li.mcmod.util.log :as log]))

(def ^:private platform-label "Fabric 1.21.1")

(defn- optional-init!
  [var-sym missing-message]
  (try
    (require (symbol (namespace var-sym)))
    (if-let [init! (when-let [v (find-var var-sym)]
                     (when (bound? v) @v))]
      (init!)
      (log/warn missing-message))
    (catch Exception _
      (log/warn missing-message))))

(def ^:private common-phase
  {:platform-label platform-label
   :phase-label "Common"
   :steps [{:run registry-impl/init!}]})

(def ^:private server-phase
  {:platform-label platform-label
   :phase-label "Server"
   :steps [{:run network-server/init-server!}]})

(def ^:private client-phase
  {:platform-label platform-label
   :phase-label "Client"
   :steps [{:run #(optional-init! 'cn.li.fabric1211.gui.screen-impl/init-client!
                                  "Fabric GUI screen impl not available on current side")}
           {:run #(optional-init! 'cn.li.fabric1211.gui.network.client/init-client!
                                  "Fabric GUI client network not available on current side")}]})

(defn init-common! []
  (gui-orchestrator/run-phase! common-phase))

(defn init-server! []
  (gui-orchestrator/run-phase! server-phase))

(defn init-client! []
  (gui-orchestrator/run-phase! client-phase))

(defn verify-initialization []
  (let [gui-checks (init-checks/build-gui-checks
                     (gui/get-all-gui-ids)
                     "gui-"
                     (fn [gui-id]
                       (some? (registry-impl/get-handler-type gui-id))))
        checks gui-checks]
    (gui-orchestrator/verify-checks! "Verifying Fabric GUI system initialization..." checks)))

(defn cleanup! []
  (log/debug "Cleaning up Fabric GUI system")
  (registry-api/invalidate-menu-registry! (target/current-target-key!))
  (log/debug "Fabric GUI system cleanup complete"))
