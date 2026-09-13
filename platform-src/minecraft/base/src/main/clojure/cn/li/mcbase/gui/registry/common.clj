(ns cn.li.mcbase.gui.registry.common
  "Shared helpers for platform GUI registry implementations."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.network FriendlyByteBuf]
           [net.minecraft.world.entity.player Inventory Player]))


(defn create-wrapped-container
  [create-container-fn wrap-container-fn resolve-handler-type-fn gui-id sync-or-window-id error-prefix]
  (let [clj-container (create-container-fn)]
    (if clj-container
      (wrap-container-fn sync-or-window-id (resolve-handler-type-fn gui-id) clj-container)
      (do
        (log/error error-prefix gui-id)
        nil))))

(defn read-block-pos
  "Read a required BlockPos from a platform buffer."
  [^FriendlyByteBuf buf]
  (.readBlockPos buf))

(defn write-block-pos!
  "Write a required BlockPos to a platform buffer."
  [^FriendlyByteBuf buf pos]
  (.writeBlockPos buf pos))

(defn read-extended-open-payload
  "Read the shared extended GUI open payload used by Fabric-style screen handlers."
  [^FriendlyByteBuf buf]
  (let [gui-id (.readInt buf)
        has-tile? (.readBoolean buf)]
    {:gui-id gui-id
     :pos (when has-tile?
            (.readBlockPos buf))}))

(defn write-extended-open-payload!
  "Write the shared extended GUI open payload used by Fabric-style screen handlers."
  [^FriendlyByteBuf buf gui-id pos]
  (.writeInt buf (int gui-id))
  (if pos
    (do
      (.writeBoolean buf true)
      (.writeBlockPos buf pos))
    (.writeBoolean buf false)))

(defn create-client-menu!
  "Create a Minecraft menu from a client-side menu factory callback.

  Platform adapters provide only handler lookup, MenuType resolution, bridge opts,
  and the platform-specific clj-container creator. This keeps the common
  player/world/pos/container/menu wrapping flow in shared Minecraft glue."
  [{:keys [gui-id window-id player-inventory pos handler create-container-fn
           create-menu-proxy-fn resolve-menu-type-fn bridge-opts error-prefix
           with-owner!]}]
    (let [^Inventory player-inventory player-inventory
      ^Player player (.player player-inventory)
        world (.level player)]
    (create-wrapped-container
      (fn []
        (create-container-fn handler gui-id player world pos))
      (fn [wid menu-type clj-container]
        (with-owner!
          #(create-menu-proxy-fn wid
                                 menu-type
                                 clj-container
                                 (assoc bridge-opts :player player))))
      resolve-menu-type-fn
      gui-id
      window-id
      error-prefix)))
