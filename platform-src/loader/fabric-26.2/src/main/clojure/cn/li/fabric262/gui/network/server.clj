(ns cn.li.fabric262.gui.network.server
  "Fabric 26.2 typed-payload GUI/RPC server transport."
  (:require [cn.li.fabric262.gui.network.shared :as shared]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.network-payload :as runtime-payload]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.network-runtime :as net-server]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.fabric262.network FabricPayloadBridge]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel ServerPlayer]))

(defn- send-map! [^ServerPlayer player channel payload]
  (FabricPayloadBridge/sendToClient player channel (shared/encode-map-bytes payload)))

(defn send-response-to-client! [^ServerPlayer player request-id payload]
  (send-map! player "s2c" (packet-base/response-map request-id payload)))

(defn send-push-to-client! [^ServerPlayer player msg-id payload]
  (if (= runtime-payload/runtime-sync-message-id msg-id)
    (FabricPayloadBridge/sendToClient player "runtime" (shared/encode-runtime-bytes payload))
    (send-map! player "s2c" (packet-base/push-map msg-id payload))))

(defn- handle-request! [^bytes bytes ^ServerPlayer player]
  (let [{:keys [msg-id request-id payload]} (packet-base/normalize-request
                                               (shared/decode-map-bytes bytes))]
    (runtime-hooks/with-client-ctx-fn
      {:player-owner {:server-session-id [:server (System/identityHashCode (.getServer ^ServerLevel (.level player)))]
                      :player-uuid (str (.getUUID player))}}
      #(net-server/handle-request
         (str msg-id) request-id payload player
         (fn [rid response]
           (send-response-to-client! player (int rid) (or response {})))))))

(defn init-server! []
  (install/process-once! ::server-initialized
    #(do
       (FabricPayloadBridge/installServer (shared/configured-mod-id!)
         (fn [bytes player]
            (let [^ServerPlayer player player
                  ^MinecraftServer server (.getServer ^ServerLevel (.level player))]
             (.execute server (fn [] (handle-request! bytes player))))))
       (log/debug "Fabric 26.2 typed GUI network server transport initialized")))
  nil)
