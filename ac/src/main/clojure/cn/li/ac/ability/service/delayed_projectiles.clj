(ns cn.li.ac.ability.service.delayed-projectiles
  "Server-side delayed projectile settlement for MdBall-based skills.

  Pending tasks live in a server-thread-confined deadline scheduler and are
  never serialized into player state."
  (:require [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util ArrayList HashMap]))

(def ^:private mdball-default-life-ticks 20)
(def ^:private mdball-settle-offset-ticks 5)
(def ^:private electron-bomb-ray-distance 15.0)

(definterface IScheduledProjectile
  (^long dueTick [])
  (^Object taskValue []))

(deftype ScheduledProjectile [^long due task]
  IScheduledProjectile
  (dueTick [_] due)
  (taskValue [_] task))

(definterface IPlayerProjectileSchedule
  (^java.util.ArrayList scheduledTasks [])
  (^longs currentTick []))

(deftype PlayerProjectileSchedule [^ArrayList tasks ^longs tick]
  IPlayerProjectileSchedule
  (scheduledTasks [_] tasks)
  (currentTick [_] tick))

(defonce ^:private ^HashMap schedules-by-player (HashMap.))

(defn- player-schedule
  ^PlayerProjectileSchedule [player-uuid create?]
  (or (.get schedules-by-player player-uuid)
      (when create?
        (let [schedule (PlayerProjectileSchedule. (ArrayList.) (long-array 1))]
          (.put schedules-by-player player-uuid schedule)
          schedule))))

(defn pending-tasks-snapshot
  [player-uuid]
  (if-let [^PlayerProjectileSchedule schedule (player-schedule player-uuid false)]
    (let [^ArrayList tasks (.scheduledTasks schedule)
          now (aget (.currentTick schedule) 0)]
      (mapv (fn [^ScheduledProjectile scheduled]
              (assoc (.taskValue scheduled)
                     :ticks-left (max 0 (- (.dueTick scheduled) now))))
            tasks))
    []))

(defn clear-player-tasks!
  [player-uuid]
  (.remove schedules-by-player player-uuid)
  nil)

(defn clear-all-tasks!
  []
  (.clear schedules-by-player)
  nil)

(defn schedule-task!
  [player-uuid delay-ticks task]
  (let [^PlayerProjectileSchedule schedule (player-schedule player-uuid true)
        now (aget (.currentTick schedule) 0)
        due (+ now (max 1 (long (or delay-ticks 1))))]
    (.add (.scheduledTasks schedule) (ScheduledProjectile. due task)))
  nil)

(defn reset-pending-tasks-for-test!
  ([]
   (reset-pending-tasks-for-test! {}))
  ([snapshot]
   (clear-all-tasks!)
   (doseq [[player-uuid tasks] snapshot
           task tasks]
     (doseq [t tasks]
       (schedule-task! player-uuid (:ticks-left t) (dissoc t :ticks-left))))
   nil))

(defn default-mdball-life-ticks
  []
  mdball-default-life-ticks)

(defn mdball-near-expire-delay
  ([]
   (mdball-near-expire-delay mdball-default-life-ticks mdball-settle-offset-ticks))
  ([life-ticks]
   (mdball-near-expire-delay life-ticks mdball-settle-offset-ticks))
  ([life-ticks offset-ticks]
   (max 1 (- (int (or life-ticks mdball-default-life-ticks))
             (int (or offset-ticks mdball-settle-offset-ticks))))))

(defn schedule-electron-bomb-beam!
  [{:keys [player-id] :as task}]
  (schedule-task! player-id (:delay-ticks task) (assoc task :kind :electron-bomb-beam)))

(defn schedule-scatter-bomb-beam!
  [{:keys [player-id] :as task}]
  (schedule-task! player-id (:delay-ticks task) (assoc task :kind :scatter-bomb-beam)))

(defn resolve-ball-position
  "Locate the spawned MdBall entity by uuid at settle time. The ball orbits
  the caster (body-level, radius 0.8-1.3), so a small AABB around the caster's
  eye covers it. Returns the ball's world position map, or nil when the ball
  is gone (world unloaded) — callers then fall back to the caster's eye.
  Shared by electron-bomb and scatter-bomb so release rays originate from
  the balls' actual orbit positions."
  [world-id player-id ball-uuid]
  (when (and ball-uuid (world-effects/available?))
    (let [eye (geom/eye-pos player-id)
          ex (double (:x eye)) ey (double (:y eye)) ez (double (:z eye))
          r 3.5]
      (some (fn [{:keys [uuid x y z]}]
              (when (= (str uuid) (str ball-uuid))
                {:x (double x) :y (double y) :z (double z)}))
            (world-effects/find-entities-in-aabb
              world-id (- ex r) (- ey r) (- ez r)
              (+ ex r) (+ ey r) (+ ez r))))))

(defn- looking-pos
  "Raytrace.getLookingPos(player, dist), which is what ElectronBomb's getDest
  is. Three things the port had wrong by projecting a flat 15 blocks from the
  eye: the trace STOPS at whatever it hits, an entity hit is raised by
  0.6 * its eye height, and the nothing-hit fallback starts at the player's
  FEET rather than their eye."
  [world-id player-id dir dist]
  (let [eye (geom/eye-pos player-id)
        hit (raycast/raycast-combined-excluding
              world-id
              (double (:x eye)) (double (:y eye)) (double (:z eye))
              (double (:x dir)) (double (:y dir)) (double (:z dir))
              (double dist)
              (str player-id))]
    (if (:hit-type hit)
      (let [hit-y (double (:hit-y hit))
            ;; getLookingPos raises the dest by 0.6 * eye height, but a LOW hit
            ;; (aiming at the enemy's feet) then overshoots ABOVE the box: the
            ;; ball->dest ray skims over the target and hits nothing. Clamp the
            ;; raise into the entity's box — at most 90% of its height — so the
            ;; ray always crosses it (upstream's uncapped raise is geometrically
            ;; flaky the same way; players expect a hit on a visible beam hit).
            raise-y (+ hit-y
                      (if (= :entity (:hit-type hit))
                        (* 0.6 (double (or (:eye-height hit) 0.0)))
                        0.0))
            box-top (+ (double (or (:y hit) hit-y))
                       (* 0.9 (double (or (:height hit) 0.0))))
            dest-y (max hit-y (min raise-y box-top))]
        {:x (double (:hit-x hit))
         :y dest-y
         :z (double (:hit-z hit))})
      (geom/v+ (geom/body-pos player-id) (geom/v* dir dist)))))

(defn- run-electron-bomb-beam!
  "Matches original's callback: getDest(player)/eye are re-evaluated fresh at
  settle time (not the values captured when the skill was cast), so the
  player can turn or move during the delay and still redirect the shot. The
  ray originates from the ball's current orbit position (original: ray from
  ball.pos to getDest(player)); the destination stays the player's look point."
  [{:keys [player-id ctx-id damage ball-uuid]}]
  (try
    (when (raycast/available?)
      (let [world-id (geom/world-id-of player-id)
            eye (geom/eye-pos player-id)
            look-vec (raycast/player-look-vector player-id)]
        (when look-vec
          (when-let [origin (resolve-ball-position world-id player-id ball-uuid)]
            (let [dir (geom/vnorm {:x (double (or (:x look-vec) 0.0))
                                 :y (double (or (:y look-vec) 0.0))
                                 :z (double (or (:z look-vec) 0.0))})
                dest (looking-pos world-id player-id dir electron-bomb-ray-distance)
                shot-dir (geom/vnorm (geom/v- dest origin))
                ;; Extend 0.5 past the dest so the ray definitely crosses the
                ;; target's box when the (clamped) dest lands on its surface;
                ;; the entity is always hit before anything beyond it.
                shot-dist (+ (geom/vdist origin dest) 0.5)
                ;; Raytrace.perform(world, ballEyes, dest,
                ;; exclude(player).and(not MdBall)) traces BLOCKS as well, so a
                ;; wall between the ball and the aim point stops the shot.
                ;; Tracing entities alone let it hit clean through terrain, and
                ;; without the exclusion it could also pick the caster off the
                ;; ball's own orbit. (Effect entities are not pickable, which
                ;; covers the "not MdBall" half.)
                hit (raycast/raycast-combined-excluding
                                              world-id
                                              (double (:x origin))
                                              (double (:y origin))
                                              (double (:z origin))
                                              (double (:x shot-dir))
                                              (double (:y shot-dir))
                                              (double (:z shot-dir))
                                              shot-dist
                                              (str player-id))
                end-pos dest
                target-uuid (when (= :entity (:hit-type hit)) (:uuid hit))
                damage-amt (double (or damage 0.0))
                payload {:mode :perform
                         :start origin
                         :end end-pos
                         :hit-distance electron-bomb-ray-distance
                         :performed? true
                         :target-uuid target-uuid}]
            (when (and target-uuid (entity-damage/available?))
              (entity-damage/apply-direct-damage!
                world-id
                target-uuid
                damage-amt
                :magic)
              (md-damage/mark-target! player-id target-uuid
                                      {:ctx-id ctx-id
                                       :target-pos {:x (:x hit)
                                                    :y (:y hit)
                                                    :z (:z hit)}}))
            ;; Original broadcasts the small ray even when it hits no entity.
            (ctx-mgr/push-channel-to-player! player-id ctx-id :electron-bomb/fx-beam payload)
            (ctx-mgr/push-channel-to-nearby-players! player-id ctx-id :electron-bomb/fx-beam payload))))))
    (catch Exception e
      (log/warn "Delayed ElectronBomb settle failed:" (ex-message e)))))

(defn- run-scatter-bomb-beam!
  "Matches original's per-ball resolution: a precise point-to-point raycast
  from the ball's (approximated) origin to its independently-randomized
  destination, single-entity hit, with hurtResistantTime=-1 (reset-
  invulnerable-time?) so multiple balls can all connect on the same target
  within one release — not a radius/falloff beam."
  [{:keys [player-id ctx-id world-id origin dest damage]}]
  (try
    (when (raycast/available?)
      (let [ox (double (:x origin)) oy (double (:y origin)) oz (double (:z origin))
            dx (- (double (:x dest)) ox)
            dy (- (double (:y dest)) oy)
            dz (- (double (:z dest)) oz)
            dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
            dir  (if (pos? dist)
                   {:x (/ dx dist) :y (/ dy dist) :z (/ dz dist)}
                   {:x 0.0 :y 0.0 :z 1.0})
            ;; Raytrace.perform(world, ballEyes, dest, everything.and(
            ;; exclude(player))) traces BLOCKS as well as entities, so a wall
            ;; between the ball and its scatter point stops the shot. Tracing
            ;; entities alone let every ball hit through terrain.
            hit  (raycast/raycast-combined-all
                   world-id ox oy oz
                   (:x dir) (:y dir) (:z dir)
                   (max 0.1 dist))
            ;; normalize-bridge-map keywords the Java bridge's "entity"/"block"
            ;; strings — a string comparison here silently never matched and
            ;; the ray never dealt damage.
            target-uuid (when (= :entity (:hit-type hit))
                          (or (:uuid hit) (:entity-id hit)))]
        (when (and target-uuid (entity-damage/available?))
          (entity-damage/apply-direct-damage!
            world-id target-uuid (double (or damage 0.0)) :magic
            {:reset-invulnerable-time? true})
          (md-damage/mark-target! player-id target-uuid {:ctx-id ctx-id}))
        (ctx-mgr/push-channel-to-player! player-id ctx-id :scatter-bomb/fx-beam
          {:start origin :end dest})
        (ctx-mgr/push-channel-to-nearby-players! player-id ctx-id :scatter-bomb/fx-beam
          {:start origin :end dest})))
    (catch Exception e
      (log/warn "Delayed ScatterBomb settle failed:" (ex-message e)))))

(defn- run-task!
  [{:keys [kind] :as task}]
  (case kind
    :electron-bomb-beam (run-electron-bomb-beam! task)
    :scatter-bomb-beam (run-scatter-bomb-beam! task)
    nil))

(defn tick-player!
  "Advance one player's deadline queue. Idle players do one HashMap lookup;
  active queues mutate in place and allocate no due/remaining collections."
  [player-uuid]
  (when-let [^PlayerProjectileSchedule schedule (player-schedule player-uuid false)]
    (let [^longs tick-cell (.currentTick schedule)
          now (unchecked-inc (aget tick-cell 0))
          ^ArrayList tasks (.scheduledTasks schedule)]
      (aset-long tick-cell 0 now)
      (loop [i 0]
        (when (< i (.size tasks))
          (let [^ScheduledProjectile scheduled (.get tasks i)]
            (if (<= (.dueTick scheduled) now)
              (do
                (.remove tasks (int i))
                (run-task! (.taskValue scheduled))
                (recur i))
              (recur (unchecked-inc-int i))))))
      (when (.isEmpty tasks)
        (.remove schedules-by-player player-uuid))))
  nil)
