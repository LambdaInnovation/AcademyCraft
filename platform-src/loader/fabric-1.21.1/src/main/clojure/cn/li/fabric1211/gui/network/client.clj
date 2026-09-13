(ns cn.li.fabric1211.gui.network.client
  "Fabric 1.21.1 typed-payload GUI/RPC client transport."
  (:require [cn.li.fabric1211.gui.network.shared :as shared]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.network-payload :as runtime-payload]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.client-network :as net-client]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcbase.client.session :as mc-session])
  (:import [cn.li.fabric1211.network FabricPayloadBridge]
           [net.minecraft.client Minecraft]))

(defn- client-session-id []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [connection (try (.getConnection mc) (catch Throwable _ nil))]
      [:client-session (System/identityHashCode mc) (System/identityHashCode connection)])))

(defn- payload-player-uuid [payload]
  (some-> (or (:uuid payload) (:player-uuid payload)
              (get-in payload [:payload :uuid])
              (get-in payload [:payload :player-uuid])) str))

(defn- with-client-owner [payload f]
  (let [session-id (client-session-id)
        uuid (or (payload-player-uuid payload)
                 (try (mc-session/local-player-uuid) (catch Exception _ nil)))]
    (runtime-hooks/with-client-ctx-fn
      {:session-id session-id
       :player-owner (cond-> {:client-session-id session-id}
                       uuid (assoc :player-uuid uuid))}
      f)))

(defn send-to-server! [msg-id request-id payload]
  (FabricPayloadBridge/sendToServer "c2s"
                                    (shared/encode-map-bytes
                                      (packet-base/request-map msg-id request-id payload))))

(defn- handle-response! [^bytes bytes]
  (let [{:keys [request-id payload]} (packet-base/normalize-response
                                       (shared/decode-map-bytes bytes))]
    (with-client-owner payload
      #(packet-base/dispatch-client-response!
         runtime-hooks/player-state-owner request-id payload))))

(defn- handle-runtime-sync! [^bytes bytes]
  (net-client/handle-push runtime-payload/runtime-sync-message-id
                          (shared/decode-runtime-bytes bytes)))

(defn init-client! []
  (install/process-once! ::client-initialized
    #(do
       (net-client/register-request-transport!
         (target/current-target-key!)
         (fn [msg-id payload request-id]
           (send-to-server! msg-id request-id payload)))
       (FabricPayloadBridge/installClient (shared/configured-mod-id!)
         (fn [bytes ^Minecraft client]
           (.execute client (fn [] (handle-response! bytes))))
         (fn [bytes ^Minecraft client]
           (.execute client (fn [] (handle-runtime-sync! bytes)))))
       (log/debug "Fabric 1.21.1 typed GUI network client transport initialized")))
  nil)
