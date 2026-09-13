(ns cn.li.fabric262.gui.network.client
  "Fabric 26.2 typed-payload GUI/RPC client transport."
  (:require [cn.li.fabric262.gui.network.shared :as shared]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.network-payload :as runtime-payload]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.client-network :as net-client]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.platform.target :as target]
            [cn.li.mcbase.client.session :as mc-session]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.fabric262.network FabricPayloadBridge]
           [net.minecraft.client Minecraft]))

(defn- client-session-id []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [connection (try (.getConnection mc) (catch Throwable _ nil))]
      [:client-session (System/identityHashCode mc) (System/identityHashCode connection)])))

(defn- with-client-owner [payload f]
  (let [session-id (client-session-id)
        player-uuid (try (mc-session/local-player-uuid) (catch Throwable _ nil))]
    (runtime-hooks/with-client-ctx-fn
      {:session-id session-id
       :player-owner (cond-> {:client-session-id session-id}
                       player-uuid (assoc :player-uuid (str player-uuid)))}
      f)))

(defn send-to-server! [msg-id request-id payload]
  (FabricPayloadBridge/sendToServer (shared/encode-map-bytes
                                     (packet-base/request-map msg-id request-id payload))))

(defn- handle-response! [request-id payload]
  (with-client-owner payload
    #(packet-base/dispatch-client-response! runtime-hooks/player-state-owner request-id payload)))

(defn init-client! []
  (install/process-once! ::client-initialized
    #(do
       (net-client/register-request-transport!
         (target/current-target-key!)
         (fn [msg-id payload request-id] (send-to-server! msg-id request-id payload)))
       (FabricPayloadBridge/installClient (shared/configured-mod-id!)
         (fn [bytes client]
           (.execute ^Minecraft client
                     (fn []
                       (let [{:keys [request-id payload]} (packet-base/normalize-response
                                                           (shared/decode-map-bytes bytes))]
                         (handle-response! request-id payload)))))
         (fn [bytes client]
           (.execute ^Minecraft client
                     (fn [] (net-client/handle-push runtime-payload/runtime-sync-message-id
                                                     (shared/decode-runtime-bytes bytes))))))
       (log/debug "Fabric 26.2 typed GUI network client transport initialized")))
  nil)
