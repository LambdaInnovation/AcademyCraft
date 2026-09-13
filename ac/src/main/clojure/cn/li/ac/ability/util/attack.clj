(ns cn.li.ac.ability.util.attack
  "Shared raycast + impact-point + AOE helpers for point-target attack skills
  (instant/charge-window skills that raycast from the caster's eye and hit a
  block or entity). Depends on platform raycast/world-effects/entity-damage
  bridges, so kept separate from the pure-geometry util/targeting.clj.

  Extracted from thunder-bolt/thunder-clap, which had byte-for-byte identical
  copies of hit-kind/block-impact-point/entity-impact-point."
  (:require [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]))

(defn hit-kind
  "Classify a raycast hit map as :entity, :block, or :miss."
  [hit]
  (let [kind (:hit-type hit)]
    (cond
      (= kind :entity) :entity
      (= kind :block) :block
      :else :miss)))

(defn block-impact-point
  [hit]
  {:x (double (or (:hit-x hit) (:x hit) 0.0))
   :y (double (or (:hit-y hit) (:y hit) 0.0))
   :z (double (or (:hit-z hit) (:z hit) 0.0))})

(defn entity-impact-point
  "The point where the trace intersects the target's bounding box (upstream
  AttackData.point = result.hitVec). Using the entity CENTER (or center +
  eye-height) leaves a visible gap between the ray/arc tip and the target
  from a side view, and shifts the AOE arc origin off the hit spot."
  [hit]
  {:x (double (or (:hit-x hit) (:x hit) 0.0))
   :y (double (or (:hit-y hit) (:y hit) 0.0))
   :z (double (or (:hit-z hit) (:z hit) 0.0))})

(defn fallback-end-point
  "eye + look*range, defaulting look to +Z when the player has no look vector
  available (matches thunder-bolt's hardcoded {eye.x eye.y (+ eye.z range)})."
  [eye look range]
  (if look
    (geom/v+ eye (geom/v* look range))
    {:x (:x eye) :y (:y eye) :z (+ (:z eye) range)}))

(defn resolve-attack-data
  "Raycast from player-id's eye out to range and classify the hit.

  Returns {:world-id :eye :look :hit-kind :target-uuid :impact}. :target-uuid
  is only set for :entity hits; :impact falls back to fallback-end-point on
  a miss."
  [player-id range]
  (let [world-id (geom/world-id-of player-id)
        eye (geom/eye-pos player-id)
        look (when (raycast/available?)
               (raycast/player-look-vector player-id))
        hit (when (and (raycast/available?) look)
              (raycast/raycast-combined
                world-id
                (:x eye) (:y eye) (:z eye)
                (double (or (:x look) 0.0))
                (double (or (:y look) 0.0))
                (double (or (:z look) 1.0))
                (double range)))
        kind (hit-kind hit)
        target-uuid (when (= kind :entity)
                      (or (:uuid hit) (:entity-uuid hit) (:entity-id hit)))
        impact (case kind
                 :entity (entity-impact-point hit)
                 :block (block-impact-point hit)
                 (fallback-end-point eye look range))]
    {:world-id world-id
     :eye eye
     :look look
     :hit-kind kind
     :target-uuid target-uuid
     :impact impact}))

(defn damage-entity!
  "Apply direct damage of damage-type to target-uuid. Returns true if applied."
  [world-id target-uuid damage damage-type]
  (when (and (entity-damage/available?) target-uuid (> (double damage) 0.0))
    (entity-damage/apply-direct-damage!
      world-id target-uuid (double damage) damage-type)
    true))

(defn- entity-in-excluded-set?
  [excluded entity]
  (contains? excluded (:uuid entity)))

(defn- within-radius?
  [center radius {:keys [x y z]}]
  (let [dx (- (double (or x 0.0)) (double (:x center)))
        dy (- (double (or y 0.0)) (double (:y center)))
        dz (- (double (or z 0.0)) (double (:z center)))
        radius* (double radius)]
    (<= (+ (* dx dx) (* dy dy) (* dz dz))
        (* radius* radius*))))

(defn aoe-victims
  "Entities within the spherical radius of center, excluding uuids in excluded.

  The platform query is AABB-backed, so retain the original AcademyCraft
  EntitySelectors.within distance check here."
  [world-id center radius excluded]
  (if-not (world-effects/available?)
    []
    (->> (world-effects/find-entities-in-radius
           world-id
           (double (:x center))
           (double (:y center))
           (double (:z center))
           (double radius))
         (remove (partial entity-in-excluded-set? excluded))
         (filter (partial within-radius? center radius))
         vec)))

(defn apply-flat-aoe-damage!
  "Flat (no distance falloff) AOE damage to all victims. Returns hit count.

  Optional `on-hit!` is called with each victim's uuid after a successful
  hit — for per-victim side effects (e.g. ThunderBolt's creeper-charging)
  that should only fire for entities the skill actually damaged."
  ([world-id victims amount damage-type]
   (apply-flat-aoe-damage! world-id victims amount damage-type nil))
  ([world-id victims amount damage-type on-hit!]
   (reduce (fn [hit-count {:keys [uuid]}]
             (if (damage-entity! world-id uuid (double amount) damage-type)
               (do (when on-hit! (on-hit! uuid))
                   (inc hit-count))
               hit-count))
           0
           victims)))
