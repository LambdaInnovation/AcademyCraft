(ns cn.li.ac.ability.effects.motion
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]))

(defn player-motion-available?
  []
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/get-adapter fw-atom :player-motion))))

(defn set-player-velocity!
  [player-id x y z]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :set-velocity!
                            player-id x y z))))

(defn add-player-velocity!
  [player-id x y z]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :add-velocity!
                            player-id x y z))))

(defn player-velocity
  [player-id]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :player-motion :get-velocity player-id)))

(defn set-player-on-ground!
  [player-id on-ground?]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :set-on-ground!
                            player-id on-ground?))))

(defn player-on-ground?
  [player-id]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :is-on-ground? player-id))))

(defn dismount-riding!
  [player-id]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :dismount-riding! player-id))))

(defn player-can-fly?
  [player-id]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :can-fly? player-id))))

(defn set-player-can-fly!
  [player-id can-fly?]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :player-motion :set-can-fly!
                            player-id (boolean can-fly?)))))

(defn entity-motion-available?
  []
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/get-adapter fw-atom :entity-motion))))

(defn set-entity-velocity!
  [world-id entity-uuid x y z]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :set-velocity!
                            world-id entity-uuid x y z))))

(defn add-entity-velocity!
  [world-id entity-uuid x y z]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :add-velocity!
                            world-id entity-uuid x y z))))

(defn set-entity-position!
  [world-id entity-uuid x y z]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :set-position!
                            world-id entity-uuid x y z))))

(defn set-block-body-block-id!
  [world-id entity-uuid block-id]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :set-block-id!
                            world-id entity-uuid block-id))))

(defn set-block-body-place-when-collide!
  [world-id entity-uuid place-when-collide?]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :set-place-when-collide!
                            world-id entity-uuid
                            (boolean place-when-collide?)))))

(defn discard-entity!
  [world-id entity-uuid]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :discard-entity!
                            world-id entity-uuid))))

(defn set-projectile-damage!
  [world-id entity-uuid damage]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :set-projectile-damage!
                            world-id entity-uuid (double damage)))))

(defn add-entity-tag!
  [world-id entity-uuid tag]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :add-tag!
                            world-id entity-uuid (str tag)))))

(defn entity-velocity
  [world-id entity-uuid]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :entity-motion :get-velocity
                           world-id entity-uuid)))

(defn entity-position
  [world-id entity-uuid]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :entity-motion :get-position
                           world-id entity-uuid)))

(defn power-creeper!
  "True if entity-uuid resolved to a creeper and it was flipped to powered
  (charged) — matches original EMDamageHelper.attack's creeper side effect."
  [world-id entity-uuid]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :entity-motion :power-creeper!
                            world-id entity-uuid))))

(defn teleportation-available?
  []
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/get-adapter fw-atom :teleportation))))

(defn teleport-player!
  [player-uuid world-id x y z]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :teleportation :teleport-player!
                            player-uuid world-id x y z))))

(defn teleport-with-entities!
  [player-uuid world-id x y z radius]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :teleportation :teleport-with-entities!
                            player-uuid world-id x y z radius))))

(defn reset-fall-damage!
  [player-uuid]
  (boolean
   (when-let [fw-atom (fw/fw-atom)]
     (platform/call-adapter fw-atom :teleportation :reset-fall-damage!
                            player-uuid))))

(defn player-position
  [player-uuid]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :teleportation :get-player-position player-uuid)))

(defn player-dimension
  [player-uuid]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :teleportation :get-player-dimension player-uuid)))

(defn execute-set-player-velocity!
  [evt {:keys [x y z]}]
  (when (player-motion-available?)
    (set-player-velocity!
                                 (:player-id evt)
                                 (double (or x 0.0))
                                 (double (or y 0.0))
                                 (double (or z 0.0))))
  evt)

(defn execute-add-entity-velocity!
  [evt {:keys [target x y z]}]
  (when (entity-motion-available?)
    (let [uuid (or (when (map? target) (:uuid target))
                   (get evt target)
                   target)]
      (when (string? uuid)
        (add-entity-velocity!
                                     (:world-id evt)
                                     uuid
                                     (double (or x 0.0))
                                     (double (or y 0.0))
                                     (double (or z 0.0))))))
  evt)

(defn execute-reset-fall-damage!
  [evt _params]
  (when (teleportation-available?)
    (reset-fall-damage! (:player-id evt)))
  evt)
