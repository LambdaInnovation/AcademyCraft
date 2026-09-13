(ns cn.li.ac.content.ability.vecmanip.storm-wing
  "StormWing - persistent charged flight matching AcademyCraft."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :storm-wing)
(def ^:private storm-wing-skill-id :storm-wing)
(def ^:private move-down-channel :storm-wing/move-down)
(def ^:private move-tick-channel :storm-wing/move-tick)
(def ^:private move-up-channel :storm-wing/move-up)

(defn- get-player-pos [player-id]
  (when (motion-effects/teleportation-available?)
    (motion-effects/player-position player-id)))

(defn- apply-cooldown! [player-id exp]
  ;; Scala's .toInt truncates rather than rounds.
  (skill-effects/set-main-cooldown!
   player-id
   :storm-wing
   (int (cfg-lerp :cooldown.ticks exp))))

(defn- add-exp! [player-id]
  (skill-effects/add-skill-exp!
   player-id
   storm-wing-skill-id
   (cfg-double :progression.exp-tick)))

(defn- update-skill-state-root! [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

(defn- ranged-random [lower upper]
  (+ (double lower) (* (rand) (- (double upper) (double lower)))))

(defn- break-soft-blocks! [player-id world-id px py pz]
  (when (block-manip/available?)
    (let [tries (cfg-int :breaking.soft-block-tries)
          radius (double (cfg-int :breaking.soft-block-search-radius))
          max-hardness (cfg-double :breaking.soft-hardness-max)]
      (dotimes [_ tries]
        ;; Original applies .toInt after adding a continuous ranged random
        ;; offset to the player's coordinate.
        (let [bx (int (+ px (ranged-random (- radius) radius)))
              by (int (+ py (ranged-random (- radius) radius)))
              bz (int (+ pz (ranged-random (- radius) radius)))
              block-id (block-manip/get-block world-id bx by bz)
              hardness (block-manip/get-block-hardness world-id bx by bz)]
          (when (and block-id
                     (skill-effects/skill-destroy-allowed? storm-wing-skill-id)
                     (number? hardness)
                     (<= 0.0 (double hardness) max-hardness))
            (block-manip/break-block!
             player-id world-id bx by bz false)))))))

(defn- mastery-speed []
  (let [[lower upper]
        (skill-config/tunable-double-list
         storm-wing-skill-id
         :combat.mastery-knockback-speed)]
    (ranged-random lower upper)))

(defn- knockback-nearby-entities! [player-id world-id px py pz]
  (when (and (world-effects/available?)
             (motion-effects/entity-motion-available?))
    (doseq [entity
            (world-effects/find-entities-in-radius
             world-id
             (double px)
             (double py)
             (double pz)
             (cfg-double :combat.mastery-knockback-radius))
            :let [entity-id (:uuid entity)]
            :when (and entity-id (not= entity-id player-id))]
      (let [dx (- (double (or (:x entity) 0.0)) (double px))
            dy (- (+ (double (or (:y entity) 0.0))
                     (double (or (:eye-height entity)
                                 (:height entity)
                                 0.0)))
                  (double py))
            dz (- (double (or (:z entity) 0.0)) (double pz))
            length (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))]
        (when (pos? length)
          (let [speed (mastery-speed)]
            (motion-effects/set-entity-velocity!
             world-id
             entity-id
             (* (/ dx length) speed)
             (* (/ dy length) speed)
             (* (/ dz length) speed))))))))

(defn- accel-toward [current target]
  (let [delta (- (double target) (double current))
        limit (cfg-double :movement.acceleration)]
    (+ (double current)
       (* (min (Math/abs delta) limit)
          (Math/signum delta)))))

(defn validate-move-direction
  "Validate and normalize a world-space movement direction."
  [payload]
  (cond
    (nil? payload) nil
    (not (map? payload))
    (do
      (log/warn "StormWing: move-dir payload is not a map:" (type payload))
      nil)

    :else
    (let [x (double (or (:x payload) 0.0))
          y (double (or (:y payload) 0.0))
          z (double (or (:z payload) 0.0))
          length (Math/sqrt (+ (* x x) (* y y) (* z z)))]
      (cond
        (Double/isNaN length)
        (do (log/warn "StormWing: move-dir has NaN") nil)

        (Double/isInfinite length)
        (do (log/warn "StormWing: move-dir has Infinity") nil)

        (> length 1.0e-6)
        {:x (/ x length) :y (/ y length) :z (/ z length)}

        :else nil))))

(defn- movement-key-direction [player-id movement-key]
  (when (and (raycast/available?) movement-key)
    (when-let [look (raycast/player-look-vector player-id)]
      (let [forward (validate-move-direction look)
            fx (double (or (:x forward) 0.0))
            fy (double (or (:y forward) 0.0))
            fz (double (or (:z forward) 0.0))]
        (case movement-key
          :forward forward
          :back {:x (- fx) :y (- fy) :z (- fz)}
          :left (validate-move-direction {:x fz :y 0.0 :z (- fx)})
          :right (validate-move-direction {:x (- fz) :y 0.0 :z fx})
          nil)))))

(defn- register-movement-listeners! [ctx-id]
  (ctx/ctx-on!
   ctx-id
   move-down-channel
   (fn [{:keys [key]}]
     (when-let [ctx-data (ctx-skill/get-context ctx-id)]
       (when (= :flying (get-in ctx-data [:skill-state :phase]))
         (let [player-id (:player-uuid ctx-data)]
           (update-skill-state-root!
            ctx-id
            #(assoc %
                    :move-key key
                    :move-dir (movement-key-direction player-id key))))))))
  (ctx/ctx-on!
   ctx-id
   move-tick-channel
   (fn [{:keys [key]}]
     (when-let [ctx-data (ctx-skill/get-context ctx-id)]
       (when (and (= :flying (get-in ctx-data [:skill-state :phase]))
                  (= key (get-in ctx-data [:skill-state :move-key])))
         (let [player-id (:player-uuid ctx-data)]
           (update-skill-state-root!
            ctx-id
            #(assoc % :move-dir
                    (movement-key-direction player-id key))))))))
  (ctx/ctx-on!
   ctx-id
   move-up-channel
   (fn [{:keys [key]}]
     (when-let [ctx-data (ctx-skill/get-context ctx-id)]
       (when (= key (get-in ctx-data [:skill-state :move-key]))
         (update-skill-state-root!
          ctx-id
          #(dissoc % :move-key :move-dir))))))
  nil)

(defn- near-ground? [world-id px py pz]
  (boolean
   (when (raycast/available?)
     (raycast/raycast-blocks
      world-id
      px
      (+ py (cfg-double :targeting.near-ground-eye-height))
      pz
      0.0
      -1.0
      0.0
      (cfg-double :targeting.near-ground-distance)))))

(defn- current-player-velocity [player-id skill-state]
  (or (when (motion-effects/player-motion-available?)
        (motion-effects/player-velocity player-id))
      {:x (double (or (:vx skill-state) 0.0))
       :y (double (or (:vy skill-state) 0.0))
       :z (double (or (:vz skill-state) 0.0))}))

(defn- flight-speed [exp]
  (let [[low-speed high-speed]
        (skill-config/tunable-double-list
         storm-wing-skill-id
         :movement.speed-multipliers)]
    (* (if (< exp (cfg-double :movement.speed-exp-threshold))
         low-speed
         high-speed)
       (cfg-lerp :movement.speed-scale exp))))

(defn- apply-flight-motion! [ctx-id player-id exp phase skill-state pos]
  (when (motion-effects/player-motion-available?)
    (let [world-id (:world-id pos)
          px (double (:x pos))
          py (double (:y pos))
          pz (double (:z pos))
          current (current-player-velocity player-id skill-state)
          dir (when (= phase :flying) (:move-dir skill-state))
          next-velocity
          (if dir
            (let [speed (flight-speed exp)]
              (motion-effects/dismount-riding! player-id)
              {:x (accel-toward (:x current) (* (:x dir) speed))
               :y (accel-toward (:y current) (* (:y dir) speed))
               :z (accel-toward (:z current) (* (:z dir) speed))})
            ;; Hover changes only Y; horizontal velocity is untouched.
            {:x (double (:x current))
             :y (if (near-ground? world-id px py pz)
                  (cfg-double :movement.hover-near-ground-velocity)
                  (+ (double (:y current))
                     (cfg-double :movement.hover-air-velocity)))
             :z (double (:z current))})]
      (motion-effects/set-player-velocity!
       player-id
       (:x next-velocity)
       (:y next-velocity)
       (:z next-velocity))
      (motion-effects/reset-fall-damage! player-id)
      (update-skill-state-root!
       ctx-id
       #(assoc %
               :vx (:x next-velocity)
               :vy (:y next-velocity)
               :vz (:z next-velocity))))))

(defn- restore-flight-permission! [player-id skill-state]
  (when (contains? skill-state :previous-can-fly?)
    (motion-effects/set-player-can-fly!
     player-id
     (:previous-can-fly? skill-state)))
  nil)

(defn- finish! [ctx-id player-id exp terminate? reason]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (restore-flight-permission! player-id (:skill-state ctx-data)))
  (apply-cooldown! player-id exp)
  (fx/send-local-and-nearby!
   ctx-id {:topic :storm-wing/fx-end :mode :end} nil nil)
  (ctx-skill/clear-skill-state! ctx-id)
  (when terminate?
    ;; Notify the client so its mirror context is cleaned up (and the
    ;; movement-key hints stop showing) — plain terminate-context! with nil
    ;; leaves the client-side context registered forever.
    (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))
  (log/debug "StormWing: Terminated" reason)
  nil)

(defn- active-storm-wing-ctx-id
  "First storm-wing context of `player-id` in :charging/:flying, optionally
  excluding `exclude-ctx-id`."
  ([player-id]
   (active-storm-wing-ctx-id player-id nil))
  ([player-id exclude-ctx-id]
   (->> (ctx/get-all-contexts)
        (filter (fn [[ctx-id ctx-data]]
                  (and (not= ctx-id exclude-ctx-id)
                       (= (:player-uuid ctx-data) player-id)
                       (contains? #{:charging :flying}
                                  (get-in ctx-data [:skill-state :phase])))))
        first
        first)))

(defn storm-wing-on-key-down
  "Press-to-toggle like the original StormWing onKeyDown: the client's slot
  ctx-id is cleared at key-up, so the second press arrives on a NEW context
  - deactivate the still-active context of the previous press (and this
  one); otherwise activate."
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (let [ctx-data (ctx-skill/get-context ctx-id)
          phase (get-in ctx-data [:skill-state :phase])
          exp (double (or exp 0.0))]
      (if (or (#{:charging :flying} phase)
              (active-storm-wing-ctx-id player-id ctx-id))
        (do
          (finish! ctx-id player-id exp true :manual)
          (when-let [active-ctx-id (active-storm-wing-ctx-id player-id ctx-id)]
            (finish! active-ctx-id player-id exp true :manual)))
        (let [charge-time (cfg-lerp :charge.time exp)
              previous-can-fly?
              (motion-effects/player-can-fly? player-id)]
          (ctx-skill/replace-skill-state!
           ctx-id
           {:phase :charging
            :charge-ticks 0
            :charge-time charge-time
            :previous-can-fly? previous-can-fly?
            :vx 0.0
            :vy 0.0
            :vz 0.0})
          (motion-effects/set-player-can-fly! player-id true)
          (register-movement-listeners! ctx-id)
          (fx/send-local-and-nearby!
           ctx-id
           {:topic :storm-wing/fx-start :mode :start}
           nil
           {:charge-ticks (long charge-time)}))))
    (catch Exception e
      (log/stacktrace "StormWing key-down failed" e)
      (ctx-skill/clear-skill-state! ctx-id))))

(defn- transition-to-flying! [ctx-id player-id exp pos]
  (update-skill-state-root!
   ctx-id
   #(assoc % :phase :flying :charge-ticks 0))
  (when (== exp 1.0)
    (knockback-nearby-entities!
     player-id
     (:world-id pos)
     (:x pos)
     (:y pos)
     (:z pos)))
  nil)

(defn storm-wing-on-key-tick
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      (let [skill-state (:skill-state ctx-data)
            phase (:phase skill-state)
            exp (double (or exp 0.0))]
        (when (#{:charging :flying} phase)
          (if-let [pos (get-player-pos player-id)]
            (do
              ;; Server s_tick performs this during both charge and flight.
              (when (< exp (cfg-double :breaking.low-exp-threshold))
                (break-soft-blocks!
                 player-id
                 (:world-id pos)
                 (:x pos)
                 (:y pos)
                 (:z pos)))
              ;; Client l_tick already hovers during the charge phase.
              (apply-flight-motion!
               ctx-id player-id exp phase skill-state pos)
              (if (= phase :charging)
                (let [charge-ticks (inc (long (or (:charge-ticks skill-state) 0)))
                      charge-time (double (:charge-time skill-state))]
                  (update-skill-state-root!
                   ctx-id
                   #(assoc % :charge-ticks charge-ticks))
                  (fx/send-local-and-nearby!
                   ctx-id
                   {:topic :storm-wing/fx-update :mode :update}
                   nil
                   {:phase :charging
                    :charge-ticks charge-ticks
                    :charge-ratio
                    (min 1.0
                         (/ (double charge-ticks)
                            (max 1.0 charge-time)))})
                  ;; Original condition is stateTick > chargeTime.
                  (when (> charge-ticks charge-time)
                    (transition-to-flying!
                     ctx-id player-id exp pos)))
                (do
                  ;; doConsume adds experience before attempting payment.
                  (add-exp! player-id)
                  (let [result
                        (skill-effects/perform-resource!
                         player-id
                         (cfg-lerp :cost.tick.overload exp)
                         (cfg-lerp :cost.tick.cp exp)
                         false)]
                    (fx/send-local-and-nearby!
                     ctx-id
                     {:topic :storm-wing/fx-update :mode :update}
                     nil
                     {:phase :flying
                      :charge-ticks 0
                      :charge-ratio 1.0})
                    (when-not (:success? result)
                      (finish!
                       ctx-id player-id exp true :insufficient-resource))))))
            (finish! ctx-id player-id exp true :position-unavailable)))))
    (catch Exception e
      (log/debug "StormWing key-tick failed:" e)
      (finish!
       ctx-id
       player-id
       (double (or exp 0.0))
       true
       :exception))))

(defn storm-wing-on-key-up
  [_ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  nil)

(defn storm-wing-on-key-abort
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (finish! ctx-id player-id (double (or exp 0.0)) false :abort)
    (catch Exception e
      (log/stacktrace "StormWing key-abort failed" e)
      (ctx-skill/clear-skill-state! ctx-id))))

(defskill storm-wing
  :id :storm-wing
  :category-id :vecmanip
  :name-key "ability.skill.vecmanip.storm_wing"
  :description-key "ability.skill.vecmanip.storm_wing.desc"
  :icon "textures/abilities/vecmanip/skills/storm_wing.png"
  :ui-position [130 20]
  :ctrl-id :storm-wing
  :cp-consume-speed 0.0
  :overload-consume-speed 0.0
  :cooldown-ticks 0
  :pattern :release-cast
  :cooldown {:mode :manual}
  :input-policy {:terminate-on-key-up? false
                 :keep-active-on-key-up? true}
  :actions {:down! storm-wing-on-key-down
            :tick! storm-wing-on-key-tick
            :up! storm-wing-on-key-up
            :abort! storm-wing-on-key-abort}
  :prerequisites [{:skill-id :vec-accel :min-exp 0.0}])
