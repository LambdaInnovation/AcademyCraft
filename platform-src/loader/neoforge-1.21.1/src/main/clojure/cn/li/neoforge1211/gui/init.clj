(ns cn.li.neoforge1211.gui.init
  "NeoForge 1.21.1 GUI System Initialization"
  (:require [cn.li.mcbase.gui.init.orchestrator :as gui-orchestrator]
            [cn.li.mcbase.gui.init.checks :as init-checks]
            [cn.li.platform.neutral.gui-runtime :as gui]
            [cn.li.mcmod.util.log :as log]
            [cn.li.neoforge1211.gui.network.server :as network-server]
            [cn.li.neoforge1211.gui.network.client :as network-client]
            [cn.li.neoforge1211.adapter.gui-registry :as gui-registry]))

(def ^:private platform-label "NeoForge 1.21.1")

(defn- optional-init!
  [init-fn missing-message]
  (if init-fn
    (init-fn)
    (log/warn missing-message)))

(def ^:private common-phase
  {:platform-label platform-label
   :phase-label "Common"
   ;; NeoForge payload handlers install together in one ClojureNetwork/init.
   ;; Both sides register during common setup (same timing as the former
   ;; monolithic gui.network/init!); shared/try-install-handlers! fires once
   ;; both handlers are present.
   :steps [{:run #(optional-init! network-server/init-server!
                                  "NeoForge GUI network server init fn not available")}
           {:run #(optional-init! network-client/init-client!
                                  "NeoForge GUI network client init fn not available")}]})

(def ^:private client-phase
  {:platform-label platform-label
   :phase-label "Client"
   :steps []})

(def ^:private server-phase
  {:platform-label platform-label
   :phase-label "Server"
   :steps []})

(defn init-common!
  "Initialize common GUI system (server + client).
  MenuType registration is handled earlier via DeferredRegister during Forge bootstrap;
  only non-registry setup belongs here."
  []
  (gui-orchestrator/run-phase! common-phase))

;; ============================================================================
;; Client-Only Initialization
;; ============================================================================

(defn init-client!
  "Initialize client-side GUI system
  
  Should be called during FMLClientSetupEvent"
  []
  (gui-orchestrator/run-phase! client-phase))

;; ============================================================================
;; Server-Only Initialization
;; ============================================================================

(defn init-server!
  "Initialize server-side GUI system
  
  Should be called during FMLDedicatedServerSetupEvent"
  []
  (gui-orchestrator/run-phase! server-phase))

;; ============================================================================
;; Verification
;; ============================================================================

(defn verify-initialization
  "Verify that GUI system is properly initialized
  
  Platform-agnostic design: Dynamically verifies all GUI IDs from metadata.
  
  Returns: boolean (true if all checks pass)"
  []
  (let [checks (init-checks/build-gui-checks
                 (gui/get-all-gui-ids)
                 "gui-"
                 (fn [gui-id]
                   (let [menu-type (gui-registry/get-menu-type gui-id)]
                     (some? menu-type))))]
    (gui-orchestrator/verify-checks! "Verifying GUI system initialization..." checks)))

;; ============================================================================
;; Error Recovery
;; ============================================================================

(defn safe-init-common!
  "Initialize common GUI system with error handling"
  []
  (gui-orchestrator/safe-run-phase! common-phase))

(defn safe-init-client!
  "Initialize client GUI system with error handling"
  []
  (gui-orchestrator/safe-run-phase! client-phase))

(defn safe-init-server!
  "Initialize server GUI system with error handling"
  []
  (gui-orchestrator/safe-run-phase! server-phase))
