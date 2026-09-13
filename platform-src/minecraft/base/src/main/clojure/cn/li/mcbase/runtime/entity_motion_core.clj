(ns cn.li.mcbase.runtime.entity-motion-core
  "Shared Minecraft-side entity motion helpers (no loader API imports)."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcbase.entity IScriptedBlockBodyEntity]
           [cn.li.mcver WorldOps]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.entity Entity]
           [net.minecraft.world.phys Vec3]))

(defn set-velocity-for-entity!
  [^Entity entity x y z]
  (when entity
    (.setDeltaMovement entity (double x) (double y) (double z))
    (set! (.-hurtMarked entity) true)
    true))

(defn add-velocity-for-entity!
  [^Entity entity x y z]
  (when entity
    (let [^Vec3 current (.getDeltaMovement entity)]
      (.setDeltaMovement entity
                         (+ (.x current) (double x))
                         (+ (.y current) (double y))
                         (+ (.z current) (double z)))
      (set! (.-hurtMarked entity) true)
      true)))

(defn set-position-for-entity!
  [^Entity entity x y z]
  (when entity
    (.setPos entity (double x) (double y) (double z))
    (set! (.-hurtMarked entity) true)
    true))

(defn set-block-id-for-entity!
  [^Entity entity block-id]
  (when (instance? IScriptedBlockBodyEntity entity)
    (.setSyncedBlockId ^IScriptedBlockBodyEntity entity (str block-id))
    true))

(defn set-place-when-collide-for-entity!
  [^Entity entity place-when-collide?]
  (when (instance? IScriptedBlockBodyEntity entity)
    (.setPlaceWhenCollide ^IScriptedBlockBodyEntity entity (boolean place-when-collide?))
    true))

(defn discard-entity!
  [^Entity entity]
  (when entity
    (.discard entity)
    true))

(defn set-projectile-damage-for-entity!
  [^Entity entity damage]
  (WorldOps/setArrowBaseDamage entity (double damage)))

(defn add-tag-for-entity!
  [^Entity entity tag]
  (when entity
    (.addTag entity (str tag))))

(defn get-velocity-for-entity
  [^Entity entity]
  (when entity
    (let [^Vec3 vel (.getDeltaMovement entity)]
      {:x (.x vel)
       :y (.y vel)
       :z (.z vel)})))

(defn get-position-for-entity
  [^Entity entity]
  (when entity
    {:x (.getX entity)
     :y (.getY entity)
     :z (.getZ entity)
     :eye-height (.getEyeHeight entity)
     :type (.. entity getType getDescriptionId)
     :alive? (not (.isRemoved entity))}))

(defn power-creeper-for-entity!
  [^ServerLevel level ^Entity entity]
  (when (and level entity)
    (boolean (WorldOps/tryPowerCreeper level entity))))

(defn resolve-entity
  [^MinecraftServer server world-id entity-uuid]
  (try
    (when server
      (some-> (query-core/resolve-level server world-id)
              (query-core/get-entity-by-uuid entity-uuid)))
    (catch Exception e
      (log/warn "Failed to resolve entity:" world-id entity-uuid (ex-message e))
      nil)))

(defn resolve-level-and-entity
  "Returns [level entity], either of which may be nil."
  [^MinecraftServer server world-id entity-uuid]
  (try
    (when server
      (let [^ServerLevel level (query-core/resolve-level server world-id)]
        [level (some-> level (query-core/get-entity-by-uuid entity-uuid))]))
    (catch Exception e
      (log/warn "Failed to resolve level/entity:" world-id entity-uuid (ex-message e))
      [nil nil])))

(defn create-entity-motion
  "Create an IEntityMotion adapter using a platform-provided server supplier."
  [get-server]
  {:set-velocity! (fn [world-id entity-uuid x y z]
                    (boolean (set-velocity-for-entity! (resolve-entity (get-server) world-id entity-uuid) x y z)))
   :add-velocity! (fn [world-id entity-uuid x y z]
                    (boolean (add-velocity-for-entity! (resolve-entity (get-server) world-id entity-uuid) x y z)))
   :set-position! (fn [world-id entity-uuid x y z]
                    (boolean (set-position-for-entity! (resolve-entity (get-server) world-id entity-uuid) x y z)))
   :set-block-id! (fn [world-id entity-uuid block-id]
                    (boolean (set-block-id-for-entity! (resolve-entity (get-server) world-id entity-uuid) block-id)))
   :set-place-when-collide! (fn [world-id entity-uuid place-when-collide?]
                              (boolean
                                (set-place-when-collide-for-entity!
                                  (resolve-entity (get-server) world-id entity-uuid)
                                  place-when-collide?)))
   :discard-entity! (fn [world-id entity-uuid]
                      (boolean (discard-entity! (resolve-entity (get-server) world-id entity-uuid))))
   :set-projectile-damage! (fn [world-id entity-uuid damage]
                             (boolean
                              (set-projectile-damage-for-entity!
                               (resolve-entity (get-server) world-id entity-uuid)
                               damage)))
   :add-tag! (fn [world-id entity-uuid tag]
               (boolean
                (add-tag-for-entity!
                 (resolve-entity (get-server) world-id entity-uuid)
                 tag)))
   :get-velocity (fn [world-id entity-uuid]
                   (get-velocity-for-entity (resolve-entity (get-server) world-id entity-uuid)))
   :get-position (fn [world-id entity-uuid]
                   (get-position-for-entity (resolve-entity (get-server) world-id entity-uuid)))
   :power-creeper! (fn [world-id entity-uuid]
                     (let [[level entity] (resolve-level-and-entity (get-server) world-id entity-uuid)]
                       (boolean (power-creeper-for-entity! level entity))))})
