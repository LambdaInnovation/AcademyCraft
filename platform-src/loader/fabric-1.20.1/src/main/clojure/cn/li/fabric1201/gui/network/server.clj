(ns cn.li.fabric1201.gui.network.server
  "Fabric 1.20.1 GUI/RPC server transport."
  (:require [cn.li.fabric1201.gui.network.shared :as shared]
            [cn.li.fabricbase.owner :as fabric-owner]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.network-payload :as runtime-payload]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.network-runtime :as net-server]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.fabric.api.networking.v1 ServerPlayNetworking ServerPlayNetworking$PlayChannelHandler]
           [net.minecraft.network FriendlyByteBuf]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerPlayer]))

(defn send-response-to-client!
  [^ServerPlayer player request-id payload]
  (ServerPlayNetworking/send player shared/s2c-channel (shared/make-buf (packet-base/response-map request-id payload))))

(defn send-push-to-client!
  [^ServerPlayer player msg-id payload]
  (if (= runtime-payload/runtime-sync-message-id msg-id)
    (ServerPlayNetworking/send player shared/runtime-sync-s2c-channel
                               (shared/make-runtime-sync-buf payload))
    (ServerPlayNetworking/send player shared/s2c-channel
                               (shared/make-buf (packet-base/push-map msg-id payload)))))

(defn- handle-server-request!
  [^ServerPlayer player msg-id request-id payload]
  (runtime-hooks/with-client-ctx-fn {:player-owner (fabric-owner/server-owner player)}
    (fn []
      (net-server/handle-request
        (str msg-id)
        request-id
        payload
        player
        (fn [rid response]
          (send-response-to-client!
            player
            (int rid)
            (or response {})))))))

(defn- on-server-play-receive
  [^MinecraftServer server player _handler buf _sender]
  (let [{:keys [msg-id request-id payload]} (packet-base/normalize-request (shared/read-buf-map buf))]
    (.execute server
              (reify Runnable
                (run [_]
                  (handle-server-request! player msg-id request-id payload)))))
  nil)

(defn init-server!
  "Process-scoped guard: ServerPlayNetworking/registerGlobalReceiver throws
   if the channel already has a registered receiver, so this must not redo
   on Framework reinjection."
  []
  (install/process-once! ::server-initialized
    (fn []
      (shared/install-channels!)
      (let [receiver (reify ServerPlayNetworking$PlayChannelHandler
                       (receive [_ server player _handler buf _sender]
                         (on-server-play-receive server player _handler buf _sender)))]
        (ServerPlayNetworking/registerGlobalReceiver shared/c2s-channel receiver)
        (log/debug "Fabric GUI network server transport initialized"))))
  nil)
