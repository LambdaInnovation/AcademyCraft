(ns cn.li.ac.content.ability.teleporter.tp-skill-helper
  "Shared utility functions for Teleporter category skills.

  Centralizes: raycast helpers, entity damage wrappers, passive crit pipeline,
  and common balance formulas.

  No Minecraft imports."
  (:require [cn.li.ac.content.ability.teleporter.passive-hooks :as passive-hooks]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.util.log :as log]))

(declare player-look-vec player-position)

(defn crit-applied?
  "Return true when the original Teleporter crit roll succeeded.

  AcademyCraft fires the crit event and passive progression before attempting
  the final damage application, so armor/PvP/invulnerability rejection does
  not suppress the crit feedback."
  [damage-result]
  (boolean (:critical? damage-result)))

;; ---------------------------------------------------------------------------
;; Teleport helpers
;; ---------------------------------------------------------------------------

(defn teleport-to!
  "Teleport player-id to (x y z) in world-id.
  Returns true if successful."
  [player-id world-id x y z]
  (when (motion-effects/teleportation-available?)
    (let [result (motion-effects/teleport-player! player-id world-id
                                                 (double x) (double y) (double z))]
      (when result
        (motion-effects/reset-fall-damage! player-id))
      result)))

(defn reset-fall-damage!
  "Reset player's fall damage state. Returns true on success."
  [player-id]
  (when (motion-effects/teleportation-available?)
    (motion-effects/reset-fall-damage! player-id)))

(defn raycast-combined
  "Raycast from world position and direction, returning first hit map or nil."
  [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
  (when (raycast/available?)
    (raycast/raycast-combined
                              world-id
                              (double start-x)
                              (double start-y)
                              (double start-z)
                              (double dir-x)
                              (double dir-y)
                              (double dir-z)
                              (double max-distance))))

(defn raycast-blocks
  "Raycast blocks from world position and direction, returning block hit or nil."
  [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
  (when (raycast/available?)
    (raycast/raycast-blocks
                            world-id
                            (double start-x)
                            (double start-y)
                            (double start-z)
                            (double dir-x)
                            (double dir-y)
                            (double dir-z)
                            (double max-distance))))

;; ---------------------------------------------------------------------------
;; Entity raycast helper
;; ---------------------------------------------------------------------------

(defn raycast-entity
  "Cast ray from player, returning first living non-player entity UUID, or nil."
  [player-id max-dist]
  (when (raycast/available?)
    (let [position (raycast/player-position player-id)
          look (raycast/player-look-vector player-id)]
      (when (and position look)
        (when-let [result (raycast/raycast-combined-from-player
                            player-id
                            (double max-dist)
                            true)]
          (let [entity-uuid (or (:entity-uuid result)
                                (:entity-id result)
                                (:uuid result))]
            (when (and (= :entity (:hit-type result))
                       entity-uuid
                       (not= (str entity-uuid) (str player-id)))
              (assoc result
                     :hit-entity true
                     :entity-uuid (str entity-uuid)
                     :entity-x (double (or (:entity-x result) (:x result) 0.0))
                     :entity-y (double (or (:entity-y result) (:y result) 0.0))
                     :entity-z (double (or (:entity-z result) (:z result) 0.0))))))))))

;; ---------------------------------------------------------------------------
;; Damage helper
;; ---------------------------------------------------------------------------

(defn- scaled-skill-damage
  [attacker-id skill-id entity-uuid raw-damage]
  (if-let [spec (and skill-id (skill-registry/get-skill skill-id))]
    (skill-effects/scale-damage
      spec
      (ability-event/fire-calc-event!
        ability-event/CALC-SKILL-ATTACK
        (double raw-damage)
        {:player-id attacker-id
         :target-id entity-uuid
         :skill-id skill-id}))
    (double raw-damage)))

(defn- deal-teleporter-damage!
  [attacker-id skill-id world-id entity-uuid damage ignore-armor?]
  (let [crit-result (passive-hooks/calc-teleporter-crit attacker-id (double damage))]
    ;; Original TPSkillHelper applies all crit feedback before ctx.attack.
    (when (:critical? crit-result)
      (passive-hooks/apply-crit-side-effects!
        attacker-id
        (:crit-level crit-result)
        (:events crit-result)
        crit-result))
    (let [final-damage (scaled-skill-damage attacker-id
                                            skill-id
                                            entity-uuid
                                            (:damage-after crit-result))
          applied? (when (entity-damage/available?)
                     (entity-damage/apply-direct-damage!
                       world-id
                       entity-uuid
                       final-damage
                       (if ignore-armor? :magic :skill)
                       {:attacker-uuid attacker-id
                        :skill-id skill-id}))]
      (assoc crit-result
             :final-damage final-damage
             :applied? (boolean applied?)))))

(defn deal-magic-damage!
  "Apply magic (armor-bypassing) damage to entity uuid in world.
  3-arity is plain damage; attacker arities apply the Teleporter crit pipeline,
  CalcEvent, and global/per-skill damage scaling."
  ([world-id entity-uuid damage]
   (when (entity-damage/available?)
     (entity-damage/apply-direct-damage!
       world-id entity-uuid (double damage) :magic)))
  ([attacker-id world-id entity-uuid damage]
   (deal-teleporter-damage! attacker-id nil world-id entity-uuid damage true))
  ([attacker-id skill-id world-id entity-uuid damage]
   (deal-teleporter-damage! attacker-id skill-id world-id entity-uuid damage true)))

(defn deal-skill-damage!
  "Apply armor-respecting, attacker-attributed Teleporter skill damage."
  [attacker-id skill-id world-id entity-uuid damage]
  (deal-teleporter-damage! attacker-id skill-id world-id entity-uuid damage false))

;; ---------------------------------------------------------------------------
;; Look direction helper
;; ---------------------------------------------------------------------------

(defn player-look-vec [player-id]
  (when (raycast/available?)
    (raycast/player-look-vector player-id)))

(defn player-position [player-id]
  (when (motion-effects/teleportation-available?)
    (motion-effects/player-position player-id)))
