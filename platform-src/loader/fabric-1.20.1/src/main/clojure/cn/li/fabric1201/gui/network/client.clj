(ns cn.li.fabric1201.gui.network.client
  "Fabric 1.20.1 GUI/RPC client transport via
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking."
  (:require [cn.li.fabric1201.gui.network.shared :as shared]
            [cn.li.mcbase.gui.network.packet :as packet-base]
            [cn.li.mcbase.runtime.network-payload :as runtime-payload]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.client-network :as net-client]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcbase.client.session :as mc-session])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.network FriendlyByteBuf]
           [net.fabricmc.fabric.api.client.networking.v1 ClientPlayNetworking ClientPlayNetworking$PlayChannelHandler]
           [net.fabricmc.fabric.api.networking.v1 PacketSender]))

(defn- client-session-id
  []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [connection (try (.getConnection mc) (catch Throwable _ nil))]
      [:client-session
       (System/identityHashCode mc)
       (System/identityHashCode connection)])))

(defn- payload-player-uuid
  [payload]
  (some-> (or (:uuid payload)
              (:player-uuid payload)
              (get-in payload [:payload :uuid])
              (get-in payload [:payload :player-uuid]))
          str))

(defn- with-client-response-owner
  [payload f]
  (let [session-id (client-session-id)
        player-uuid (or (payload-player-uuid payload)
                        (try (mc-session/local-player-uuid) (catch Exception _ nil)))]
    (runtime-hooks/with-client-ctx-fn
      {:session-id session-id
       :player-owner (cond-> {:client-session-id session-id}
                       player-uuid (assoc :player-uuid player-uuid))}
      f)))

(defn send-to-server!
  [msg-id request-id payload]
  (ClientPlayNetworking/send shared/c2s-channel (shared/make-buf (packet-base/request-map msg-id request-id payload))))

(defn- handle-client-response!
  [request-id payload]
  (with-client-response-owner payload
    (fn []
      (packet-base/dispatch-client-response!
        runtime-hooks/player-state-owner
        request-id
        payload))))

(defn- on-client-play-receive
  [^Minecraft client _handler buf _sender]
  (let [{:keys [request-id payload]} (packet-base/normalize-response (shared/read-buf-map buf))]
    (.execute client
              (reify Runnable
                (run [_]
                  (handle-client-response! request-id payload)))))
  nil)

(defn- on-runtime-sync-receive
  [^Minecraft client _handler buf _sender]
  (let [payload (shared/read-runtime-sync-payload buf)]
    (.execute client
              (reify Runnable
                (run [_]
                  (net-client/handle-push runtime-payload/runtime-sync-message-id payload)))))
  nil)

(defn init-client!
  "Process-scoped guard: ClientPlayNetworking/registerGlobalReceiver throws
   if the channel already has a registered receiver, so this must not redo
   on Framework reinjection."
  []
  (install/process-once! ::client-initialized
    (fn []
      (shared/install-channels!)
      (net-client/register-request-transport!
        (target/current-target-key!)
        (fn [msg-id payload request-id]
          (send-to-server! msg-id request-id payload)))
      (let [receiver (reify ClientPlayNetworking$PlayChannelHandler
                       (receive [_ client _handler buf _sender]
                         (on-client-play-receive client _handler buf _sender)))
            runtime-receiver (reify ClientPlayNetworking$PlayChannelHandler
                               (receive [_ client _handler buf _sender]
                                 (on-runtime-sync-receive client _handler buf _sender)))]
        (ClientPlayNetworking/registerGlobalReceiver shared/s2c-channel receiver)
        (ClientPlayNetworking/registerGlobalReceiver shared/runtime-sync-s2c-channel runtime-receiver)
        (log/debug "Fabric GUI network client transport initialized"))))
  nil)
