(ns cn.li.ac.content.ability.meltdowner.scatter-bomb
  "ScatterBomb skill - hold to accumulate balls, release for scatter shot.

  Pattern: :hold-channel
  Down cost: overload lerp(80, 60, exp)
  Tick cost: CP lerp(3, 6, exp) per tick — increases with exp in original
  Ball spawn: 1 ball every 10 ticks from tick 20 to 80 (max 7 balls)
  On release: each ball independently raycasts toward a randomly
  pitch/yaw-deviated destination point (or, above exp 0.5, some balls
  auto-aim at nearby living entities instead)
  Anti-AFK: at tick 200, apply flat 6 self-damage (generic, not magic)
  Overload floor: the actual post-consumption overload stat, enforced
  every tick during hold
  No cooldown at all — original has none; the only limiter is CP cost.
  Exp: +0.001 per ball fired

  No Minecraft imports."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
                        [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.delayed-projectiles :as delayed-projectiles]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-effects]
                        [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :scatter-bomb)
(def ^:private mdball-entity-id (modid/namespaced-path "entity_md_ball"))
(def ^:private scatter-bomb-skill-id :scatter-bomb)

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- current-hold-ticks
  [ctx-id]
  (long (or (get-in (ctx-skill/get-context ctx-id) [:skill-state :hold-ticks]) 0)))

(defn- set-skill-state!
  [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- enforce-overload-floor!
  [player-id floor-value]
  (skill-effects/enforce-overload-floor! player-id floor-value))

;; Matches vanilla Vec3d#rotatePitch(float)/rotateYaw(float) (radians),
;; applied sequentially (pitch first, then yaw on the pitched result) —
;; same order as original's look.rotatePitch(...).rotateYaw(...).
(defn- rotate-pitch
  [{:keys [x y z]} pitch-rad]
  (let [f (Math/cos (double pitch-rad)) f1 (Math/sin (double pitch-rad))]
    {:x x
     :y (+ (* (double y) f) (* (double z) f1))
     :z (- (* (double z) f) (* (double y) f1))}))

(defn- rotate-yaw
  [{:keys [x y z]} yaw-rad]
  (let [f (Math/cos (double yaw-rad)) f1 (Math/sin (double yaw-rad))]
    {:x (+ (* (double x) f) (* (double z) f1))
     :y y
     :z (- (* (double z) f) (* (double x) f1))}))

(defn- random-scatter-dest
  "Matches original's newDest: begin (a point RAY_RANGE along the exact look
  direction) plus a second RAY_RANGE-scaled vector along an independently
  pitch- and yaw-rotated look direction — a sum of two vectors, not a
  perturbed-then-normalized direction."
  [eye look-vec]
  (let [range      (double (cfg-double :projectile.scatter-range))
        angle      (double (cfg-double :projectile.scatter-angle-degrees))
        pitch-rad  (Math/toRadians (* (- (rand) 0.5) angle))
        yaw-rad    (Math/toRadians (* (- (rand) 0.5) angle))
        rotated    (-> look-vec (rotate-pitch pitch-rad) (rotate-yaw yaw-rad))
        begin      {:x (+ (double (:x eye)) (* range (double (:x look-vec))))
                    :y (+ (double (:y eye)) (* range (double (:y look-vec))))
                    :z (+ (double (:z eye)) (* range (double (:z look-vec))))}]
    {:x (+ (:x begin) (* range (double (:x rotated))))
     :y (+ (:y begin) (* range (double (:y rotated))))
     :z (+ (:z begin) (* range (double (:z rotated))))}))

(def ^:dynamic *scatter-dest-sampler*
  "Injectable sampler seam for deterministic tests."
  random-scatter-dest)

(defn- random-ball-offset
  "Approximate EntityMdBall's original player-relative spawn offset. The Y
  range matches the raised md-ball orbit (y-from 0.2, y-to 1.6) — the old
  -1.2..0.2 range put the release ray origins below the ground, hiding them
  in the terrain."
  [look-vec]
  (let [base-theta (Math/atan2 (double (or (:x look-vec) 0.0))
                                (double (or (:z look-vec) 1.0)))
        theta (+ base-theta (* (- (rand) 0.5) 0.9 Math/PI))
        radius (+ 0.8 (* (rand) 0.5))]
    {:x (* (Math/sin theta) radius)
     :y (+ 0.2 (* (rand) 1.4))
     :z (* (Math/cos theta) radius)}))

(def ^:dynamic *ball-offset-sampler*
  "Injectable EntityMdBall offset sampler for deterministic tests."
  random-ball-offset)

(defn- auto-aim-targets
  "Living entities within targeting.auto-aim-radius of the player, excluding
  the player themselves. Matches original's WorldUtils.getEntities(player,5,
  EntitySelectors.exclude(player).and(_.isInstanceOf[EntityLiving]))."
  [world-id player-id eye]
  (if-not (world-effects/available?)
    []
    (->> (world-effects/find-entities-in-radius
           world-id (:x eye) (:y eye) (:z eye)
           (double (cfg-double :targeting.auto-aim-radius)))
         (remove #(= (str (:uuid %)) (str player-id)))
         (filter :living?)
         vec)))

;; ---------------------------------------------------------------------------
;; Actions
;; ---------------------------------------------------------------------------

(defn scatter-bomb-down!
  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when cost-ok?
    ;; Matches original's overloadKeep = ctx.cpData.getOverload: snapshot the
    ;; actual resulting overload stat post-consumption, not the raw cost
    ;; delta (and no arbitrary discount — original's floor is 100% of it).
    (let [floor (double (or (skill-effects/player-path player-id [:resource-data :cur-overload] 0.0) 0.0))]
    (ctx-skill/replace-skill-state! ctx-id
               {:balls        0
            :ball-offsets []
            :hold-ticks   0
            :overload-floor floor})
      ;; Original has no explicit sendTo* at all here — every ball is a real
      ;; server-spawned entity, vanilla-visible to everyone by default; this
      ;; port-added charge/release FX follows the same broadcast default.
      (fx/send-local-and-nearby! ctx-id {:topic :scatter-bomb/fx-start} nil {}))))

(declare settle-scatter-bomb!)

(defn scatter-bomb-tick!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage player-ref]
  (let [ctx-data (ctx-skill/get-context ctx-id)]
    (when ctx-data
      (let [ticks (inc (long (or (get-in ctx-data [:skill-state :hold-ticks]) 0)))
            _ (set-skill-state! ctx-id [:hold-ticks] ticks)
            balls (int (or (get-in ctx-data [:skill-state :balls]) 0))
            floor (double (or (get-in ctx-data [:skill-state :overload-floor]) 0.0))]
        ;; Enforce overload floor
        (enforce-overload-floor! player-id floor)
        ;; Anti-AFK self-damage at tick 200
        (when (= ticks (cfg-int :effect.anti-afk-tick))
          (when (entity-damage/available?)
            (entity-damage/apply-direct-damage!
              (geom/world-id-of player-id)
              player-id
              (cfg-double :effect.anti-afk-damage)
              :generic))
          (settle-scatter-bomb! ctx-id player-id exp)
          (ctx/terminate-context! ctx-id nil))
        ;; Spawn new ball every N ticks
        (when (and (<= ticks (cfg-int :projectile.max-hold-ticks))
                   (>= ticks (cfg-int :projectile.spawn-start-tick))
                   (< balls (cfg-int :projectile.max-balls))
                   (zero? (mod (- ticks (cfg-int :projectile.spawn-start-tick))
                               (cfg-int :projectile.spawn-interval-ticks))))
          (let [new-balls (inc balls)]
            (set-skill-state! ctx-id [:balls] new-balls)
            (let [look-vec (when (raycast/available?)
                             (raycast/player-look-vector player-id))
                  offsets (vec (or (get-in (ctx-skill/get-context ctx-id)
                                            [:skill-state :ball-offsets])
                                   []))]
              (set-skill-state! ctx-id [:ball-offsets]
                                (conj offsets (*ball-offset-sampler* look-vec))))
            ;; Tracked spawn keeps the ball uuid so settle-scatter-bomb! can
            ;; remove the balls (original ball.setDead() on release) — the
            ;; spec life alone (50) would let early balls die mid-hold.
            ;;
            ;; Upstream's balls are effectively immortal (life 2333333) and only
            ;; ever end at setDead. The context itself is bounded by the
            ;; anti-AFK settle, so that plus a margin is the equivalent here:
            ;; max-hold-ticks + 40 expired the first balls at tick 140, sixty
            ;; ticks before a held cast is forced to fire, and they were simply
            ;; missing from the volley.
            (when player-ref
              (when-let [ball-uuid (entity/player-spawn-tracked-entity-by-id!
                                     player-ref
                                     mdball-entity-id
                                     0.0
                                     (+ (cfg-int :effect.anti-afk-tick) 40))]
                (set-skill-state! ctx-id [:ball-uuids]
                                  (conj (vec (or (get-in (ctx-skill/get-context ctx-id)
                                                          [:skill-state :ball-uuids])
                                                 []))
                                        ball-uuid))))
            (let [eye (geom/eye-pos player-id)]
              (fx/send-local-and-nearby! ctx-id {:topic :scatter-bomb/fx-ball} nil
                        {:x (:x eye) :y (:y eye) :z (:z eye)
                         :count new-balls}))))))))

(defn- discard-balls!
  "Remove the spawned ball entities (original ball.setDead() on release)."
  [ctx-id player-id]
  (when (world-effects/available?)
    (let [world-id (geom/world-id-of player-id)
          uuids (vec (or (get-in (ctx-skill/get-context ctx-id) [:skill-state :ball-uuids]) []))]
      (doseq [uuid uuids]
        (world-effects/discard-entity-by-uuid! world-id uuid)))))

(defn- settle-scatter-bomb!
  [ctx-id player-id exp]
  (let [ctx-data (ctx-skill/get-context ctx-id)
        balls (int (or (get-in ctx-data [:skill-state :balls]) 0))
        world-id (geom/world-id-of player-id)
        ball-uuids (vec (or (get-in ctx-data [:skill-state :ball-uuids]) []))
        ;; Resolve each ball's ACTUAL orbit position BEFORE discarding them —
        ;; the rays must originate from the visible balls (original
        ;; ball.getPositionEyes), not the stored offset approximation.
        ball-positions (mapv (fn [uuid]
                               (delayed-projectiles/resolve-ball-position
                                 world-id player-id uuid))
                             ball-uuids)]
    (discard-balls! ctx-id player-id)
    (when (pos? balls)
      (let [eye        (geom/eye-pos player-id)
            player-pos (or (when (motion-effects/teleportation-available?)
                             (motion-effects/player-position player-id))
                           eye)
            look-vec   (when (raycast/available?)
                         (raycast/player-look-vector player-id))
            damage     (cfg-lerp :combat.damage exp)
            auto-aim?  (> (double exp) (double (cfg-double :targeting.auto-aim-exp-threshold)))
            targets    (if auto-aim? (auto-aim-targets world-id player-id player-pos) [])
            auto-count (if auto-aim? (long (* balls (double exp))) 0)
            auto-count* (long-array 1 auto-count)]
        (when look-vec
          (dotimes [i balls]
            (let [auto-target (when (and (pos? (aget auto-count* 0)) (seq targets))
                                (aset-long auto-count* 0 (dec (aget auto-count* 0)))
                                (rand-nth targets))
                  dest (if auto-target
                         {:x (double (:x auto-target))
                          :y (+ (double (:y auto-target)) (double (or (:eye-height auto-target) 0.0)))
                          :z (double (:z auto-target))}
                         (*scatter-dest-sampler* eye look-vec))
                  actual-pos (nth ball-positions i nil)]
              (if actual-pos
                (delayed-projectiles/schedule-scatter-bomb-beam!
                  {:player-id   player-id
                   :ctx-id      ctx-id
                   :world-id    world-id
                   :origin      actual-pos
                   :dest        dest
                   :damage      damage
                   :delay-ticks 1})
                ;; No silent fallback: a missing ball is a bug and must surface.
                (log/warn "ScatterBomb: ball entity not found for ray origin"
                          {:index i :ball-uuid (nth ball-uuids i nil)}))))))
        (skill-effects/add-skill-exp! player-id scatter-bomb-skill-id
                                      (* (cfg-double :progression.exp-per-ball) balls))
        (log/debug "ScatterBomb: fired" balls "balls"))
    (fx/send-local-and-nearby! ctx-id {:topic :scatter-bomb/fx-end} nil {:balls balls})))

(defn scatter-bomb-up!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (settle-scatter-bomb! ctx-id player-id exp))

(defn scatter-bomb-cost-fail!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks cost-stage _player-ref]
  (when (= cost-stage :tick)
    (settle-scatter-bomb! ctx-id player-id exp)
    (ctx/terminate-context! ctx-id nil)))

(defn scatter-bomb-abort!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (settle-scatter-bomb! ctx-id player-id exp))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill scatter-bomb
  :id             :scatter-bomb
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.scatter_bomb"
  :description-key "ability.skill.meltdowner.scatter_bomb.desc"
  :icon           "textures/abilities/meltdowner/skills/scatter_bomb.png"
  :ui-position    [70 50]
  :ctrl-id        :scatter-bomb
  :pattern        :hold-channel
  ;; No cooldown at all — matches original, which never calls
  ;; ctx.setCooldown anywhere in ScatterBomb; the only limiter is CP cost.
  :cooldown       {:mode :manual}
  :cost           {:down {:overload (fn [{:keys [player-id]}]
                (cfg-lerp :cost.down.overload (skill-exp player-id)))}
                   :tick {:cp (fn [{:keys [player-id ctx-id]}]
                                (let [ticks (current-hold-ticks ctx-id)]
                                  (if (<= ticks (cfg-int :projectile.max-hold-ticks))
                                    (cfg-lerp :cost.tick.cp (skill-exp player-id))
                                    0.0)))} }
  :actions        {:down!  scatter-bomb-down!
                   :tick!  scatter-bomb-tick!
                   :up!    scatter-bomb-up!
                   :abort! scatter-bomb-abort!
                   :cost-fail! scatter-bomb-cost-fail!}
  :prerequisites  [{:skill-id :electron-bomb :min-exp 0.8}])
