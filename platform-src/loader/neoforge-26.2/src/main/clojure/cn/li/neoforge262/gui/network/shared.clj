(ns cn.li.neoforge262.gui.network.shared
  "Shared NeoForge GUI network owner-binding, payload decode, and handler install."
  (:require [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.network-payload :as runtime-payload]
            [cn.li.mcbase.runtime.sync-codec :as sync-codec]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mc262.bridge McAccess] [cn.li.neoforge262.network ClojureNetwork]
           [net.minecraft.server.level ServerPlayer]
           [clojure.lang IFn]))

(def ^:private client-session-id-fn
  (atom (fn []
          (throw (ex-info "NeoForge GUI network client session id function is not installed"
                          {:namespace 'cn.li.neoforge262.gui.network.shared})))))

(def ^:private local-player-uuid-fn
  (atom (fn []
          (throw (ex-info "NeoForge GUI network local player UUID function is not installed"
                          {:namespace 'cn.li.neoforge262.gui.network.shared})))))

(def ^:private with-bound-client-owner-fn
  (atom (fn [_ _]
          (throw (ex-info "NeoForge GUI network client owner binding function is not installed"
                          {:namespace 'cn.li.neoforge262.gui.network.shared})))))

(def ^:private request-handler-ref (atom nil))
(def ^:private response-handler-ref (atom nil))

(defn install-client-owner-functions!
  [{:keys [client-session-id local-player-uuid with-bound-client-owner]}]
  (reset! client-session-id-fn client-session-id)
  (reset! local-player-uuid-fn local-player-uuid)
  (reset! with-bound-client-owner-fn with-bound-client-owner)
  nil)

(defn payload-player-uuid
  [payload]
  (some-> (or (:uuid payload)
              (:player-uuid payload)
              (get-in payload [:payload :uuid])
              (get-in payload [:payload :player-uuid]))
          str))

(defn with-client-response-owner
  [payload f]
  (let [session-id (@client-session-id-fn)
        ;; Response payloads rarely carry :player-uuid; fall back to the local
        ;; Minecraft player so require-client-owner validation passes during dispatch.
        player-uuid (or (payload-player-uuid payload)
                        (try (@local-player-uuid-fn) (catch Exception _ nil)))]
    (when-not session-id
      (throw (ex-info "Client GUI network response requires bound client session"
                      {:payload payload})))
    (@with-bound-client-owner-fn
     (cond-> {:logical-side :client :client-session-id session-id}
       player-uuid (assoc :player-uuid player-uuid))
     f)))

(defn server-player-owner
  [^ServerPlayer player]
  {:logical-side :server
   :server-session-id (when-let [server (McAccess/serverOf player)]
                        [:server (System/identityHashCode server)])
   :player-uuid (str (.getUUID player))})

(defn with-server-player-owner
  [^ServerPlayer player f]
  (runtime-hooks/with-client-ctx-fn {:player-owner (server-player-owner player)} f))

(defn decode-request-payload
  [payload-bytes]
  (packet-base/decode-payload-bytes
    payload-bytes
    #(log/stacktrace "Failed to deserialize Forge request payload:" %)))

(defn decode-response-payload
  [request-id response-bytes]
  (let [runtime-sync? (and (neg? (int request-id))
                           (sync-codec/runtime-sync-bytes? response-bytes))]
    (if runtime-sync?
      {:msg-id runtime-payload/runtime-sync-message-id
       :payload (sync-codec/decode-bytes response-bytes)}
      (packet-base/decode-payload-bytes
        response-bytes
        #(log/stacktrace "Failed to deserialize Forge response payload:" %)))))

(defn invoke-network-static
  [method-name & args]
  (case method-name
    "sendToServer"
    (let [[msg-id request-id payload] args]
      (ClojureNetwork/sendToServer ^String msg-id (int request-id) ^bytes payload))

    "sendToClient"
    (let [[player req-id response] args]
      (ClojureNetwork/sendToClient ^ServerPlayer player (int req-id) ^bytes response))

    "init"
    (let [[req-handler resp-handler] args]
      (ClojureNetwork/init ^IFn req-handler ^IFn resp-handler))

    (throw (IllegalArgumentException.
             (str "Unknown ClojureNetwork method: " method-name)))))

(defn set-request-handler!
  [handler]
  (reset! request-handler-ref handler)
  nil)

(defn set-response-handler!
  [handler]
  (reset! response-handler-ref handler)
  nil)

(defn try-install-handlers!
  "NeoForge payloads need both handlers in one ClojureNetwork/init call.
  Installs exactly once once both side handlers have been registered."
  []
  (when (and @request-handler-ref @response-handler-ref)
    (install/process-once! ::handlers-installed
      (fn []
        (invoke-network-static "init" @request-handler-ref @response-handler-ref)
        (log/debug "NeoForge 26.2 GUI network system initialized"))))
  nil)
