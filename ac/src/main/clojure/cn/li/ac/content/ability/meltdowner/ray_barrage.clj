(ns cn.li.ac.content.ability.meltdowner.ray-barrage
  "RayBarrage skill - dual branch behavior.

  Branch A (plain): precise single-target hitscan raytrace, matching
  original's Raytrace.getLookingPos — not an AOE beam.
  Branch B (scattered): when the first thing in the crosshair is an
  in-flight, not-yet-hit silbarn, force-detonate it and hit EVERY entity
  within a cone centered on the player's CURRENT aim (55 degrees yaw span,
  110 degrees pitch span — matches original's asymmetric range/2 vs range
  reuse of a single constant), independent of the silbarn's own position.

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [clojure.string :as str]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(def-skill-config-ops :ray-barrage)
(def ^:private ray-barrage-skill-id :ray-barrage)

(defn reset-ray-barrage-state-for-test!
  []
  nil)

(defn- silbarn-type?
  [entity-type]
  (let [s (some-> entity-type str str/lower-case)]
    (boolean (and s (str/includes? s "silbarn")))))

(defn- normalize-look-dir
  [look-vec]
  (let [dx (double (or (:dx look-vec) (:x look-vec) 0.0))
        dy (double (or (:dy look-vec) (:y look-vec) 0.0))
        dz (double (or (:dz look-vec) (:z look-vec) 1.0))
        len (max 1.0e-6 (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz))))]
    {:dx (/ dx len) :dy (/ dy len) :dz (/ dz len)}))

;; Player look inversion uses the corrected pitch denominator; target
;; dz*dz+dz*dz typo in original's pitch horizontal-distance calc — the
;; correct term is dx*dx+dz*dz). Applying the same formula to a look
;; DIRECTION vector (instead of a relative position delta) recovers the
;; player's own yaw/pitch, since Minecraft derives the look vector from
;; yaw/pitch via the inverse of this exact relationship.
(defn- yaw-degrees
  [dx dz]
  (- (Math/toDegrees (Math/atan2 (double dx) (double dz)))))

(defn- pitch-degrees
  [dx dy dz]
  (- (Math/toDegrees (Math/atan2 (double dy)
                                  (Math/sqrt (+ (* (double dx) (double dx))
                                                (* (double dz) (double dz))))))))

(defn- target-pitch-degrees
  "Preserve RayBarrage's original dz*dz+dz*dz denominator typo."
  [dy dz]
  (- (Math/toDegrees
       (Math/atan2 (double dy)
                   (Math/sqrt (+ (* (double dz) (double dz))
                                 (* (double dz) (double dz))))))))

(defn- rotate-yaw-raw
  "Minecraft 1.12 Vec3d.rotateYaw semantics. Original passes degree-valued
  yaw directly to this radians API; keep that behavior for its candidate AABB."
  [{:keys [dx dy dz]} angle]
  (let [c (Math/cos (double angle))
        s (Math/sin (double angle))]
    {:dx (+ (* dx c) (* dz s))
     :dy dy
     :dz (- (* dz c) (* dx s))}))

(defn- rotate-pitch-raw
  [{:keys [dx dy dz]} angle]
  (let [c (Math/cos (double angle))
        s (Math/sin (double angle))]
    {:dx dx
     :dy (+ (* dy c) (* dz s))
     :dz (- (* dz c) (* dy s))}))

(defn- scatter-aabb
  [body look-dir min-yaw max-yaw min-pitch max-pitch range]
  (let [endpoint (fn [yaw pitch]
                   (let [{:keys [dx dy dz]} (-> look-dir
                                                (rotate-yaw-raw yaw)
                                                (rotate-pitch-raw pitch))]
                     {:x (+ (:x body) (* range dx))
                      :y (+ (:y body) (* range dy))
                      :z (+ (:z body) (* range dz))}))
        points [body
                (endpoint min-yaw min-pitch)
                (endpoint min-yaw max-pitch)
                (endpoint max-yaw max-pitch)
                (endpoint max-yaw min-pitch)]]
    {:min-x (apply min (map :x points))
     :min-y (apply min (map :y points))
     :min-z (apply min (map :z points))
     :max-x (apply max (map :x points))
     :max-y (apply max (map :y points))
     :max-z (apply max (map :z points))}))

(defn- normalize-angle
  [a]
  (let [m (mod (double a) 360.0)]
    (if (> m 180.0) (- m 360.0) m)))

(defn- yaw-in-range?
  "Wraparound-safe yaw-in-[min,max] check, matching original's
  MathUtils.angleYawinRange."
  [min-yaw max-yaw yaw]
  (let [span (- (double max-yaw) (double min-yaw))]
    (if (>= span 360.0)
      true
      (<= 0.0 (normalize-angle (- (double yaw) (double min-yaw))) span))))

(defn- raycast-front-hit
  [world-id eye look-dir]
  (when (raycast/available?)
    (raycast/raycast-combined
                              world-id
                              (double (:x eye)) (double (:y eye)) (double (:z eye))
                              (double (:dx look-dir)) (double (:dy look-dir)) (double (:dz look-dir))
                              (double (cfg-double :targeting.range)))))

(defn- front-hit-end
  "The preray's aim point. Original getLookingPos returns the trace's
  hitVec — the point where the ray intersects the target's bounding box —
  NOT the entity's center position: using the center would leave a visible
  gap between the ray tip and the silbarn from a side view."
  [eye look-dir front-hit]
  (if (and (map? front-hit)
           (some? (or (:hit-x front-hit) (:x front-hit)))
           (some? (or (:hit-y front-hit) (:y front-hit)))
           (some? (or (:hit-z front-hit) (:z front-hit))))
    {:x (double (or (:hit-x front-hit) (:x front-hit)))
     :y (double (or (:hit-y front-hit) (:y front-hit)))
     :z (double (or (:hit-z front-hit) (:z front-hit)))}
    (let [dist (double (cfg-double :targeting.range))]
      {:x (+ (double (:x eye)) (* dist (double (:dx look-dir))))
       :y (+ (double (:y eye)) (* dist (double (:dy look-dir))))
       :z (+ (double (:z eye)) (* dist (double (:dz look-dir))))})))

(defn- send-preray-fx!
  [ctx-id eye target-end hit?]
  (fx/send-local-and-nearby! ctx-id {:topic :ray-barrage/fx-preray} nil
                               {:start {:x (:x eye) :y (:y eye) :z (:z eye)}
                                :end {:x (:x target-end) :y (:y target-end) :z (:z target-end)}
                                :hit? (boolean hit?)}))

(defn- send-barrage-fx!
  [ctx-id silbarn-hit look-dir]
  ;; The caster's aim travels in the payload so EVERY viewer's client can
  ;; scatter the sub rays around the right direction (original's
  ;; c_spawnBarrage reads player.rotationYaw/Pitch on the caster's client).
  ;; The origin is the trace's HIT POINT — the same point the preray ray
  ;; terminates on — so the burst starts exactly where the main ray lands,
  ;; not at the silbarn's center (a ~0.2 block gap from the side view).
  (fx/send-local-and-nearby! ctx-id {:topic :ray-barrage/fx-barrage} nil
                               {:silbarn {:x (double (or (:hit-x silbarn-hit) (:x silbarn-hit) 0.0))
                                          :y (double (or (:hit-y silbarn-hit) (:y silbarn-hit) 0.0))
                                          :z (double (or (:hit-z silbarn-hit) (:z silbarn-hit) 0.0))}
                                :yaw (yaw-degrees (double (:dx look-dir)) (double (:dz look-dir)))
                                :pitch (pitch-degrees (double (:dx look-dir))
                                                      (double (:dy look-dir))
                                                      (double (:dz look-dir)))}))

(defn- attack!
  "Matches original's MDDamageHelper.attack: apply damage, then let the
  attacker's RadIntensify (if learned) mark the target."
  [player-id ctx-id world-id target-id damage]
  (when (and target-id (entity-damage/available?))
    (entity-damage/apply-direct-damage! world-id target-id (double damage) :magic)
    (md-damage/mark-target! player-id target-id {:ctx-id ctx-id})
    true))

(defn- cone-scatter-targets
  "All entities (excluding player + silbarn) within original's cone: yaw
  spans cone-angle-degrees total, pitch spans cone-angle-degrees on EACH
  side — both centered on the player's CURRENT aim, not the silbarn."
  [world-id player-id silbarn-uuid body eye look-dir]
  (if-not (world-effects/available?)
    []
    (let [range        (double (cfg-double :targeting.range))
          cone-angle    (double (cfg-double :scatter.cone-angle-degrees))
          player-yaw   (yaw-degrees (:dx look-dir) (:dz look-dir))
          player-pitch (pitch-degrees (:dx look-dir) (:dy look-dir) (:dz look-dir))
          half-yaw     (/ cone-angle 2.0)
          min-yaw      (- player-yaw half-yaw)
          max-yaw      (+ player-yaw half-yaw)
          min-pitch    (- player-pitch cone-angle)
          max-pitch    (+ player-pitch cone-angle)
          {:keys [min-x min-y min-z max-x max-y max-z]}
          (scatter-aabb body look-dir min-yaw max-yaw min-pitch max-pitch range)]
      (->> (world-effects/find-entities-in-aabb
             world-id min-x min-y min-z max-x max-y max-z)
           (remove (fn [{:keys [uuid]}]
                     (or (= (str uuid) (str player-id))
                         (= (str uuid) silbarn-uuid))))
           (filter (fn [{:keys [x y z eye-height]}]
                     (let [dx (- (double x) (double (:x body)))
                           dy (- (+ (double y) (double (or eye-height 0.0))) (double (:y eye)))
                           dz (- (double z) (double (:z body)))
                           target-yaw (yaw-degrees dx dz)
                           target-pitch (target-pitch-degrees dy dz)]
                       (and (yaw-in-range? min-yaw max-yaw target-yaw)
                            (<= min-pitch target-pitch max-pitch)))))))))

;; ---------------------------------------------------------------------------
;; Action
;; ---------------------------------------------------------------------------

(defn ray-barrage-perform!
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when cost-ok?
      (let [plain-damage   (cfg-lerp :combat.damage.plain exp)
            scatter-damage (cfg-lerp :combat.damage.scattered exp)
            world-id       (geom/world-id-of player-id)
            body           (geom/body-pos player-id)
            eye            (geom/eye-pos player-id)
            look-vec       (when (raycast/available?)
                             (raycast/player-look-vector player-id))
            look-dir       (when look-vec (normalize-look-dir look-vec))]
        (when look-dir
        (let [front-hit      (raycast-front-hit world-id eye look-dir)
              silbarn-hit?   (and (= :entity (:hit-type front-hit))
                                  (silbarn-type? (:type front-hit)))
              silbarn-ready? (and silbarn-hit?
                                  (not (true? (:is-hit front-hit)))
                                  (some? (:uuid front-hit)))]
          (cond
            silbarn-ready?
            (do
              (world-effects/trigger-behavior-hit! world-id (str (:uuid front-hit)))
              ;; Original c_spawnPreRay: setFromTo(player pos + 1.6 eye,
              ;; target hit point) — the ray must issue from the EYE (not
              ;; the feet, or it spawns underground) and terminate on the
              ;; trace's hit point (not the entity center, or a side view
              ;; shows a gap between ray tip and silbarn).
              (send-preray-fx! ctx-id eye (front-hit-end eye look-dir front-hit) true)
              (send-barrage-fx! ctx-id front-hit look-dir)
              (let [targets (cone-scatter-targets world-id player-id (str (:uuid front-hit))
                                                  body eye look-dir)]
                (doseq [{:keys [uuid]} targets]
                  (attack! player-id ctx-id world-id uuid scatter-damage)))
              (skill-effects/set-main-cooldown! player-id ray-barrage-skill-id
                                                (cfg-lerp-int :cooldown.ticks exp))
              (skill-effects/add-skill-exp! player-id ray-barrage-skill-id
                                            (cfg-double :progression.exp-hit)))

            ;; Not a ready silbarn (either not a silbarn at all, or an
            ;; already-hit one) — original's `hit` flag stays false in both
            ;; cases, falling through to the plain raytrace branch, which
            ;; may re-hit the same (now inert) silbarn as an ordinary target.
            :else
            (let [hit-end (front-hit-end eye look-dir front-hit)
                  hit-uuid (when (= :entity (:hit-type front-hit)) (:uuid front-hit))]
              (when hit-uuid
                (attack! player-id ctx-id world-id hit-uuid plain-damage))
              (send-preray-fx! ctx-id eye hit-end false)
              (skill-effects/set-main-cooldown! player-id ray-barrage-skill-id
                                                (cfg-lerp-int :cooldown.ticks exp))
              (skill-effects/add-skill-exp! player-id ray-barrage-skill-id
                                            (cfg-double :progression.exp-hit))))))))
    (catch Exception e
      (log/warn "RayBarrage perform! failed:" (ex-message e)))))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill ray-barrage
  :id             :ray-barrage
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.ray_barrage"
  :description-key "ability.skill.meltdowner.ray_barrage.desc"
  :icon           "textures/abilities/meltdowner/skills/ray_barrage.png"
  :ui-position    [140 10]
  :ctrl-id        :ray-barrage
  :pattern        :instant
  :cooldown       {:mode :manual}
  :cost           {:down {:cp       (fn [{:keys [player-id]}]
                                      (cfg-lerp :cost.down.cp (skill-exp player-id)))
                          :overload (fn [{:keys [player-id]}]
                                      (cfg-lerp :cost.down.overload (skill-exp player-id)))} }
  :cooldown-ticks (fn [{:keys [exp]}]
                    (cfg-lerp-int :cooldown.ticks (double (or exp 0.0))))
  :actions        {:perform! ray-barrage-perform!}
  :prerequisites  [{:skill-id :meltdowner :min-exp 0.5}])
