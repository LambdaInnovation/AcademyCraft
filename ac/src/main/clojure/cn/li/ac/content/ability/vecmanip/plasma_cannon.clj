(ns cn.li.ac.content.ability.vecmanip.plasma-cannon
  "PlasmaCannon - charged plasma body and remote explosion."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.util.scaling :as scaling]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :plasma-cannon)
(def ^:private plasma-cannon-skill-id :plasma-cannon)

(defn- charge-time [exp]
  (cfg-lerp :charge.time exp))

(defn- cp-per-tick [exp]
  (cfg-lerp :cost.tick.cp exp))

(defn- overload-keep [exp]
  (cfg-lerp :cost.overload-keep exp))

(defn- damage-amount [exp]
  (cfg-lerp :combat.damage exp))

(defn- explosion-radius [exp]
  (cfg-lerp :combat.explosion-radius exp))

(defn- cooldown-ticks [exp]
  (int (cfg-lerp :cooldown.ticks exp)))

(defn get-skill-exp [player-id]
  (scaling/clamp-exp
   (skill-effects/skill-exp player-id plasma-cannon-skill-id)))

(defn- get-skill-exp-from [exp]
  (scaling/clamp-exp (double (or exp 0.0))))

(defn- get-player-position [player-id]
  (or (motion-effects/player-position player-id)
      (skill-effects/player-path
       player-id
       :position
       {:world-id "minecraft:overworld"
        :x 0.0
        :y 64.0
        :z 0.0})))

(defn- get-world-id [player-id]
  (or (skill-effects/player-path
       player-id [:position :world-id])
      "minecraft:overworld"))

(defn- add-exp! [player-id amount]
  (skill-effects/add-skill-exp!
   player-id :plasma-cannon amount))

(defn- update-skill-state-root! [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

(defn- maintain-overload! [player-id min-overload]
  (skill-effects/enforce-overload-floor!
   player-id min-overload))

(defn- apply-cooldown! [player-id exp]
  (skill-effects/set-main-cooldown!
   player-id :plasma-cannon (cooldown-ticks exp)))

(defn- try-move [charge-pos destination]
  (let [raw-delta
        {:x (- (double (:x destination))
               (double (:x charge-pos)))
         :y (- (double (:y destination))
               (double (:y charge-pos)))
         :z (- (double (:z destination))
               (double (:z charge-pos)))}
        length (geom/vlen raw-delta)]
    (if (< length 1.0)
      [charge-pos charge-pos]
      (let [step (geom/vnorm raw-delta)]
        [{:x (+ (double (:x charge-pos)) (:x step))
          :y (+ (double (:y charge-pos)) (:y step))
          :z (+ (double (:z charge-pos)) (:z step))}
         charge-pos]))))

(defn- path-hit? [world-id last-pos new-pos]
  (when (raycast/available?)
    (let [delta {:x (- (double (:x new-pos))
                       (double (:x last-pos)))
                 :y (- (double (:y new-pos))
                       (double (:y last-pos)))
                 :z (- (double (:z new-pos))
                       (double (:z last-pos)))}
          distance (geom/vlen delta)]
      (when (pos? distance)
        (let [dir (geom/vnorm delta)]
          (some?
           (raycast/raycast-combined-all
            world-id
            (double (:x last-pos))
            (double (:y last-pos))
            (double (:z last-pos))
            (:x dir)
            (:y dir)
            (:z dir)
            (+ distance
               (cfg-double
                :projectile.block-hit-extra-distance)))))))))

(defn- scaled-damage [player-id target-id raw-damage]
  (skill-effects/scale-damage
   (skill-registry/get-skill plasma-cannon-skill-id)
   (ability-event/fire-calc-event!
    ability-event/CALC-SKILL-ATTACK
    raw-damage
    {:player-id player-id
     :target-id target-id
     :skill-id plasma-cannon-skill-id})))

(defn- do-explode! [player-id world-id destination exp]
  (let [tx (double (:x destination))
        ty (double (:y destination))
        tz (double (:z destination))
        damage (damage-amount exp)
        radius (explosion-radius exp)]
    (when (world-effects/available?)
      (doseq [entity
              (let [r (cfg-double :combat.damage-radius)]
                ;; Upstream WorldUtils.getEntities builds the ±r box and then
                ;; filters it with EntitySelectors.within(x,y,z,r) — a SPHERE.
                ;; find-entities-in-radius only does the box, which reaches
                ;; r*sqrt(3) into the corners.
                (filter (fn [e]
                          (<= (geom/vdist e {:x tx :y ty :z tz}) r))
                        (world-effects/find-entities-in-radius world-id tx ty tz r)))
              :let [target-id (:uuid entity)]
              :when target-id]
        (when (entity-damage/available?)
          ;; Original resets hurtResistantTime to -1 after each ctx.attack.
          (entity-damage/apply-direct-damage!
           world-id
           target-id
           (scaled-damage player-id target-id damage)
           :skill
           {:attacker-uuid player-id
            :reset-invulnerable-time-after? true}))))
    (when (world-effects/available?)
      (world-effects/create-explosion!
       world-id
       tx
       ty
       tz
       radius
       false
       {:terrain? (block-manip/destroy-allowed?)
        :attacker-uuid player-id}))
    (log/info
     "PlasmaCannon: Exploded at"
     [tx ty tz]
     "radius:" (int radius)
     "damage:" (int damage))))

(defn- resolve-destination [player-id _world-id player-pos]
  (let [eye-x (double (:x player-pos))
        eye-y (+ (double (:y player-pos))
                 (cfg-double :targeting.eye-height))
        eye-z (double (:z player-pos))
        max-distance (cfg-double :targeting.raycast-distance)]
    (if (raycast/available?)
      (let [look (raycast/player-look-vector player-id)
            dx (double (or (:x look) 0.0))
            dy (double (or (:y look) 0.0))
            dz (double (or (:z look) 1.0))
            ;; s_perform: Raytrace.getLookingPos(player, 100,
            ;; EntitySelectors.living) — LIVING entities only, from the
            ;; player's actual eye position. The generic combined trace also
            ;; stops on any pickable entity (armour stands, boats, item
            ;; frames), which would drop the shot short of what was aimed at.
            hit (raycast/raycast-combined-from-player player-id max-distance true)]
        (if hit
          (let [entity-hit? (= :entity (:hit-type hit))
                hit-x (double (or (:hit-x hit) (:x hit) eye-x))
                hit-y (double (or (:hit-y hit) (:y hit) eye-y))
                hit-z (double (or (:hit-z hit) (:z hit) eye-z))]
            {:x hit-x
             :y (+ hit-y
                   (if entity-hit?
                     (* (double (or (:eye-height hit) 0.0))
                        0.6)
                     0.0))
             :z hit-z})
          ;; LambdaLib2's no-hit fallback starts at getPositionVector rather
          ;; than the eye position used for the trace.
          {:x (+ (double (:x player-pos))
                 (* dx max-distance))
           :y (+ (double (:y player-pos))
                 (* dy max-distance))
           :z (+ (double (:z player-pos))
                 (* dz max-distance))}))
      {:x eye-x :y eye-y :z eye-z})))

(defn- tornado-base-pos
  "Original Tornado's constructor: trace straight down from the charge
  position (blocks only, `EntitySelectors.nothing`) and seat the column on the
  first block hit; on a miss it sits at the end of the ray. The entity never
  moves afterwards, so this is resolved once, when the charge starts."
  [world-id charge-pos]
  (let [search (cfg-double :effect.tornado-ground-search-distance)
        x (double (:x charge-pos))
        y (double (:y charge-pos))
        z (double (:z charge-pos))
        hit (when (raycast/available?)
              (raycast/raycast-blocks world-id x y z 0.0 -1.0 0.0 search))]
    (if hit
      {:x (double (or (:hit-x hit) (:x hit) x))
       :y (double (or (:hit-y hit) (:y hit) (- y search)))
       :z (double (or (:hit-z hit) (:z hit) z))}
      {:x x :y (- y search) :z z})))

(defn- send-end-and-terminate! [ctx-id performed?]
  (fx/send-local-and-nearby!
   ctx-id
   {:topic :plasma-cannon/fx-end :mode :end}
   nil
   {:performed? (boolean performed?)})
  (ctx/terminate-context! ctx-id nil)
  nil)

(defn plasma-cannon-on-key-down
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (let [exp (get-skill-exp-from exp)
          charge-time (charge-time exp)
          position (get-player-position player-id)
          spawn-pos {:x (double (:x position))
                     :y (+ (double (:y position))
                           (cfg-double
                            :projectile.spawn-y-offset))
                     :z (double (:z position))}
          overload-cost (overload-keep exp)]
      ;; s_madeAlive ignores consume's return and stores actual overload.
      (skill-effects/perform-resource!
       player-id overload-cost 0.0 false)
      (let [actual-overload
            (double
             (skill-effects/player-path
              player-id
              [:resource-data :cur-overload]
              0.0))]
        (ctx-skill/replace-skill-state!
         ctx-id
         {:state :charging
          :charge-ticks 0
          :charge-time charge-time
          :overload-keep actual-overload
          :sync-ticks 0
          :flight-ticks 0
          :world-id (:world-id position)
          :charge-pos spawn-pos
          :destination nil})
        (maintain-overload! player-id actual-overload)
        (fx/send-local-and-nearby!
         ctx-id
         {:topic :plasma-cannon/fx-start :mode :start}
         nil
         {:charge-pos spawn-pos
          :tornado-base (tornado-base-pos (or (:world-id position)
                                              (get-world-id player-id))
                                          spawn-pos)
          :player-id player-id})
        (fx/send-local-and-nearby!
         ctx-id
         {:topic :plasma-cannon/fx-update :mode :update}
         nil
         {:state :charging
          :charge-pos spawn-pos
          :charge-ticks 0
          :fully-charged? false
          :player-id player-id})
        (log/debug
         "PlasmaCannon: Charge started, need"
         charge-time
         "ticks")))
    (catch Exception e
      (log/warn
       "PlasmaCannon key-down failed:"
       (ex-message e)))))

(defn- tick-charging! [ctx-id player-id exp skill-state]
  (let [charge-ticks (long (or (:charge-ticks skill-state) 0))
        charge-time (double
                     (or (:charge-time skill-state)
                         (charge-time exp)))
        next-ticks (inc charge-ticks)
        should-consume? (< next-ticks charge-time)
        result
        (when should-consume?
          (skill-effects/perform-resource!
           player-id
           0.0
           (cp-per-tick exp)
           false))]
    (if (and should-consume? (not (:success? result)))
      (do
        (send-end-and-terminate! ctx-id false)
        (log/debug
         "PlasmaCannon: Ran out of CP, aborting"))
      (do
        (update-skill-state-root!
         ctx-id
         #(assoc % :charge-ticks next-ticks))
        (fx/send-local-and-nearby!
         ctx-id
         {:topic :plasma-cannon/fx-update :mode :update}
         nil
         {:charge-ticks next-ticks
          ;; Charged cue is at chargeTime.toInt; readiness uses the float.
          :fully-charged? (= next-ticks (int charge-time))
          :release-ready? (>= next-ticks charge-time)
          :player-id player-id})))))

(defn- finish-flight!
  [ctx-id player-id world-id destination exp explosion-count]
  ;; Collision and terminal checks are independent upstream. If both are true,
  ;; explode() runs twice in the same tick.
  (dotimes [_ explosion-count]
    (do-explode! player-id world-id destination exp))
  (fx/send-local-and-nearby!
   ctx-id
   {:topic :plasma-cannon/fx-perform :mode :perform}
   nil
   {:pos destination
    :player-id player-id})
  (send-end-and-terminate! ctx-id true))

(defn- tick-flight! [ctx-id player-id exp skill-state]
  (let [charge-pos (:charge-pos skill-state)
        destination (:destination skill-state)
        flight-ticks (long (or (:flight-ticks skill-state) 0))
        sync-ticks (long (or (:sync-ticks skill-state) 0))
        world-id (or (:world-id skill-state)
                     (get-world-id player-id))
        next-flight (inc flight-ticks)]
    (when (and charge-pos destination)
      (let [[new-pos last-pos] (try-move charge-pos destination)
            distance-to-destination (geom/vdist new-pos destination)
            path-hit? (path-hit? world-id last-pos new-pos)
            terminal?
            (or (< distance-to-destination
                   (cfg-double :projectile.destination-epsilon))
                (>= next-flight
                    (cfg-int :projectile.max-flight-ticks)))
            explosion-count
            (+ (if path-hit? 1 0)
               (if terminal? 1 0))]
        (if (pos? explosion-count)
          (finish-flight!
           ctx-id
           player-id
           world-id
           destination
           exp
           explosion-count)
          (let [next-sync
                (if (zero? sync-ticks)
                  (cfg-int
                   :projectile.sync-interval-ticks)
                  (dec sync-ticks))]
            (update-skill-state-root!
             ctx-id
             #(assoc %
                     :charge-pos new-pos
                     :flight-ticks next-flight
                     :sync-ticks next-sync))
            (when (zero? sync-ticks)
              (fx/send-local-and-nearby!
               ctx-id
               {:topic :plasma-cannon/fx-update :mode :update}
               nil
               {:charge-pos new-pos
                :flight-ticks next-flight
                :player-id player-id}))))))))

(defn plasma-cannon-on-key-tick
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      (let [skill-state (:skill-state ctx-data)
            state (or (:state skill-state) :charging)
            exp (get-skill-exp-from exp)
            floor (double
                   (or (:overload-keep skill-state)
                       (overload-keep exp)))]
        (maintain-overload! player-id floor)
        (case state
          :charging
          (tick-charging!
           ctx-id player-id exp skill-state)

          :go
          (tick-flight!
           ctx-id player-id exp skill-state)

          (send-end-and-terminate! ctx-id false))))
    (catch Exception e
      (log/warn
       "PlasmaCannon key-tick failed:"
       (ex-message e)))))

(defn plasma-cannon-on-key-up
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      (let [skill-state (:skill-state ctx-data)
            state (or (:state skill-state) :charging)
            charge-ticks
            (long (or (:charge-ticks skill-state) 0))
            charge-time
            (double (or (:charge-time skill-state) 60.0))
            exp (get-skill-exp-from exp)]
        (cond
          (= state :go)
          nil

          (< charge-ticks charge-time)
          (do
            (send-end-and-terminate! ctx-id false)
            (log/debug
             "PlasmaCannon: Released before fully charged, aborting"))

          :else
          (let [position (get-player-position player-id)
                world-id (or (:world-id position)
                             (get-world-id player-id))
                destination
                (resolve-destination
                 player-id world-id position)]
            ;; s_perform adds exp and cooldown when the shot is fired.
            (add-exp!
             player-id
             (cfg-double :progression.exp-use))
            (apply-cooldown! player-id exp)
            (update-skill-state-root!
             ctx-id
             #(assoc %
                     :state :go
                     :destination destination
                     :flight-ticks 0
                     :sync-ticks 0
                     :world-id world-id))
            (fx/send-local-and-nearby!
             ctx-id
             {:topic :plasma-cannon/fx-update :mode :update}
             nil
             {:state :go
              :charge-pos (:charge-pos skill-state)
              :destination destination
              :player-id player-id})
            (log/info
             "PlasmaCannon: Fired - destination"
             [(int (:x destination))
              (int (:y destination))
              (int (:z destination))])))))
    (catch Exception e
      (log/warn
       "PlasmaCannon key-up failed:"
       (ex-message e)))))

(defn plasma-cannon-on-key-abort
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (fx/send-local-and-nearby!
     ctx-id
     {:topic :plasma-cannon/fx-end :mode :end}
     nil
     {:performed? false})
    (ctx-skill/clear-skill-state! ctx-id)
    (log/debug "PlasmaCannon: Aborted")
    (catch Exception e
      (log/warn
       "PlasmaCannon key-abort failed:"
       (ex-message e)))))

(defskill plasma-cannon
  :id :plasma-cannon
  :category-id :vecmanip
  :name-key "ability.skill.vecmanip.plasma_cannon"
  :description-key "ability.skill.vecmanip.plasma_cannon.desc"
  :icon "textures/abilities/vecmanip/skills/plasma_cannon.png"
  :ui-position [175 14]
  :ctrl-id :plasma-cannon
  :cp-consume-speed 0.0
  :overload-consume-speed 0.0
  :cooldown-ticks (fn [_player-id _skill-id exp]
                    (int
                     (cfg-lerp
                      :cooldown.ticks
                      (double (or exp 0.0)))))
  :pattern :charge-window
  :cooldown {:mode :manual}
  :input-policy {:terminate-on-key-up? false
                 :keep-active-on-key-up? true}
  :actions {:down! plasma-cannon-on-key-down
            :tick! plasma-cannon-on-key-tick
            :up! plasma-cannon-on-key-up
            :abort! plasma-cannon-on-key-abort}
  :prerequisites [{:skill-id :storm-wing :min-exp 0.0}])
