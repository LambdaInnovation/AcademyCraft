(ns cn.li.ac.content.ability.electromaster.mag-movement
  "MagMovement skill - magnetic acceleration toward metal blocks/entities.

  Mechanics:
  - Raycast to metal blocks/entities
  - Accelerate player toward target with smooth interpolation
  - Energy and progression values are read from ability skill config
  - Accept both normal and weak metal blocks, matching the original's effective check
  - Visual: Arc with wiggle animation
  - Audio: Looping ambient sound
  - Resets fall damage on completion
  - Grants experience based on distance traveled"
  (:require [clojure.string :as str]
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-op]
            [cn.li.ac.ability.effects.state :as state-op]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :mag-movement)
(def ^:private mag-movement-skill-id :mag-movement)
(def ^:private default-eye-height 1.62)

(defn- normalize-id
  [raw-id]
  (let [id (some-> raw-id str str/lower-case)]
    (cond
      (nil? id) nil
      (str/includes? id ":") id
      (re-matches #"(?:block|item|entity)\.[^.]+\..+" id)
      (let [[_ namespace path]
            (re-matches #"(?:block|item|entity)\.([^.]+)\.(.+)" id)]
        (str namespace ":" path))
      :else id)))

(defn- is-metal-block? [block-id]
  (let [id      (normalize-id block-id)
        normal? (ability-config/is-normal-metal-block? id)
        weak?   (ability-config/is-weak-metal-block? id)]
    ;; Source parity: the original's exp < 0.6 branch calls isMetalBlock,
    ;; which already includes weak metal, so the threshold has no effect.
    (boolean (and id (or normal? weak?)))))

(defn- is-metal-entity? [entity-type]
  (ability-config/is-metal-entity? (normalize-id entity-type)))

(defn- player-pos [player-id]
  (geom/body-pos player-id))

(defn- set-skill-state!
  [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- try-adjust [from to]
  (let [d (- (double to) (double from))
        accel (cfg-double :movement.acceleration)]
    (if (< (Math/abs d) accel)
      (double to)
      (if (pos? d)
        (+ (double from) accel)
        (- (double from) accel)))))

(defn- update-entity-target
  [{:keys [target-world-id target-entity-uuid] :as skill-state}]
  (when (and target-world-id target-entity-uuid)
    ;; EntityTarget in the original holds the entity reference directly, so it
    ;; keeps following the target regardless of how far it moves in one tick.
    (when-let [position (motion-effects/entity-position target-world-id
                                                        target-entity-uuid)]
      (when (not= false (:alive? position))
        (assoc skill-state
               :target-x (double (:x position))
               :target-y (+ (double (:y position))
                             (double (or (:eye-height position)
                                         default-eye-height)))
               :target-z (double (:z position)))))))

(defn- hit-distance
  [hit]
  (if (number? (:distance hit))
    (double (:distance hit))
    Double/POSITIVE_INFINITY))

(defn- nearest-target-hit
  "Match LambdaLib2 Raytrace.traceLiving: trace all collidable entities, not
  only LivingEntity, exclude the caster, and let an entity win equal-distance
  ties."
  [player-id world-id eye look]
  (let [range      (cfg-double :targeting.range)
        block-hit  (raycast/raycast-blocks
                     world-id
                     (:x eye) (:y eye) (:z eye)
                     (double (:x look))
                     (double (:y look))
                     (double (:z look))
                     range)
        entity-hit (raycast/raycast-from-player player-id range false)]
    (cond
      (and entity-hit
           (<= (hit-distance entity-hit) (hit-distance block-hit)))
      (assoc entity-hit :hit-type :entity)

      block-hit
      (assoc block-hit :hit-type :block)

      :else nil)))

(defn- resolve-target [player-id]
  (when-let [look (when (raycast/available?)
                     (raycast/player-look-vector player-id))]
    (let [eye      (geom/eye-pos player-id)
          world-id (geom/world-id-of player-id)
          hit      (nearest-target-hit player-id world-id eye look)]
      (case (:hit-type hit)
        :block
        (let [block-id (normalize-id (:block-id hit))]
          (when (is-metal-block? block-id)
            {:target-kind     :block
             :target-world-id world-id
             :target-x        (double (or (:hit-x hit) (:x hit)))
             :target-y        (double (or (:hit-y hit) (:y hit)))
             :target-z        (double (or (:hit-z hit) (:z hit)))
             :target-block-id block-id}))
        :entity
        (let [entity-uuid (or (:entity-id hit) (:uuid hit))
              live-entity (when entity-uuid
                            (motion-effects/entity-position world-id entity-uuid))
              entity-type (normalize-id (or (:type live-entity) (:type hit)))
              position    (or live-entity hit)]
          (when (and entity-uuid
                     (not= false (:alive? position))
                     (is-metal-entity? entity-type))
            {:target-kind        :entity
             :target-world-id    world-id
             :target-entity-uuid entity-uuid
             :target-entity-type entity-type
             :target-x           (double (:x position))
             :target-y           (+ (double (:y position))
                                     (double (or (:eye-height position)
                                                 default-eye-height)))
             :target-z           (double (:z position))}))
        nil))))

;; ---------------------------------------------------------------------------
;; Cost fn (tick CP is conditional on having a target)
;; ---------------------------------------------------------------------------

(defn- player-creative?
  [player-id]
  (try
    (boolean
      (when-let [fw-atom (fw/fw-atom)]
        (when (platform/get-adapter fw-atom :runtime-interop)
          (when-let [player (platform/call-adapter
                             fw-atom
                             :runtime-interop
                             :get-player-entity
                             player-id)]
            (entity/player-creative? player)))))
    (catch Exception _
      false)))

(defn- tick-cp-cost [player-id _skill-id exp]
  (if (player-creative? player-id)
    0.0
    ;; No ctx-id in the cost arity, so this has to scan — but only over alive
    ;; SERVER contexts. The client twin registered under the same ctx-id in
    ;; single player carries no projected :skill-state, so matching it made
    ;; live-target? always false and the whole skill free of CP.
    (if-let [ctx-data (ctx-skill/active-server-skill-context
                       player-id mag-movement-skill-id)]
      (let [skill-state (:skill-state ctx-data)
            live-target? (and (:has-target skill-state)
                              (or (not= :entity (:target-kind skill-state))
                                  (some? (update-entity-target skill-state))))]
        (if live-target?
          (cfg-lerp :cost.tick.cp (double (or exp 0.0)))
          0.0))
      0.0)))

(defn- down-overload-cost [player-id _skill-id exp]
  (if (player-creative? player-id)
    0.0
    (cfg-lerp :cost.down.overload (double (or exp 0.0)))))

(defn- current-overload
  [player-id fallback]
  (double
    (or (get-in (skill-effects/get-player-state player-id)
                [:resource-data :cur-overload])
        fallback
        0.0)))

;; ---------------------------------------------------------------------------
;; Actions
;; ---------------------------------------------------------------------------

(defn- finalize-and-terminate!
  [ctx-id player-id & {:keys [grant-exp?] :or {grant-exp? true}}]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx-data)
          finalized? (boolean (get-in ctx-data [:skill-state :finalized?]))
          should-finalize? (and skill-state (not finalized?))]
      (when should-finalize?
        (set-skill-state! ctx-id [:finalized?] true)
        (when (and grant-exp?
                   (number? (:start-x skill-state))
                   (number? (:start-y skill-state))
                   (number? (:start-z skill-state)))
          (let [{:keys [x y z]} (player-pos player-id)
                traveled (geom/vdist {:x x :y y :z z}
                                    {:x (double (:start-x skill-state))
                                     :y (double (:start-y skill-state))
                                     :z (double (:start-z skill-state))})]
            (skill-effects/add-skill-exp! player-id mag-movement-skill-id
                                          (max (cfg-double :progression.exp-min)
                                               (* (cfg-double :progression.exp-distance-scale) traveled)))))
        (motion-op/execute-reset-fall-damage! {:player-id player-id} nil)
        ;; Original's MSG_TERMINATED is broadcast to owner + nearby (they all
        ;; got MSG_EFFECT_START earlier via sendToClient), so everyone who saw
        ;; the arc appear must also see it torn down.
        (fx/send-local-and-nearby! ctx-id {:topic :mag-movement/fx-end :mode :end} nil nil)
        (ctx-skill/clear-skill-state! ctx-id)
        (ctx/terminate-context! ctx-id nil)))))

(defn- on-down!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [state-pos (player-pos player-id)]
    (if-let [{:keys [target-x target-y target-z] :as target-state}
             (resolve-target player-id)]
      (let [velocity-now (when (motion-effects/player-motion-available?)
                           (motion-effects/player-velocity player-id))]
        (ctx-skill/replace-skill-state! ctx-id
          (merge target-state
                  {:has-target true
                   :finalized? false
                   :movement-ticks 0
                   :overload-floor (current-overload
                                     player-id
                                     (cfg-lerp :cost.down.overload
                                               (double (or exp 0.0))))
                   :start-x (double (:x state-pos))
                   :start-y (double (:y state-pos))
                  :start-z (double (:z state-pos))
                   :motion-x (double (or (:x velocity-now) 0.0))
                   :motion-y (double (or (:y velocity-now) 0.0))
                   :motion-z (double (or (:z velocity-now) 0.0))}))
        ;; Original's s_onEffectStart/s_onEffectUpdate both use sendToClient
        ;; (owner + nearby) — the arc + loop sound are ordinary world FX, not
        ;; gated to isLocal like MineDetect's overlay.
        ;; :source-player-id is what anchors the looping sound to the caster on
        ;; every recipient's client (original FollowEntitySound(player, SOUND));
        ;; :caster-pos (the eye) is what the client anchors the arc to —
        ;; upstream setFromTo(player.posX, player.posY + getHeightFix, ...)
        ;; uses the CASTER's own position, not each viewer's hand.
        (fx/send-local-and-nearby! ctx-id {:topic :mag-movement/fx-start :mode :start} nil
                  {:source-player-id player-id
                   :caster-pos (geom/eye-pos player-id)})
        (fx/send-local-and-nearby! ctx-id {:topic :mag-movement/fx-update :mode :update} nil
                  {:source-player-id player-id
                   :caster-pos (geom/eye-pos player-id)
                   :target {:x (double target-x)
                            :y (double target-y)
                            :z (double target-z)}})
        (log/debug "MagMovement started" (:target-kind target-state)))
      (do
        (ctx-skill/replace-skill-state!
          ctx-id
          {:has-target false
           :finalized? false
           :start-x (double (:x state-pos))
           :start-y (double (:y state-pos))
           :start-z (double (:z state-pos))})
        (log/debug "MagMovement: no valid magnetic target")
        ;; MovementContext.s_onEnd always calls getExpIncr, so even an invalid
        ;; target grants the original minimum 0.005 experience.
        (finalize-and-terminate! ctx-id player-id {:grant-exp? true})))))

(defn- on-tick!
  [ctx-id player-id skill-id exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx)
          has-target  (:has-target skill-state)]
      (when has-target
        (let [updated-state (if (= :entity (:target-kind skill-state))
                              (update-entity-target skill-state)
                              skill-state)]
          (if-not updated-state
            (finalize-and-terminate! ctx-id player-id {:grant-exp? true})
            (let [movement-ticks (inc (int (:movement-ticks updated-state)))]
              (ctx-skill/replace-skill-state! ctx-id
                                     (assoc updated-state :movement-ticks movement-ticks))
              (state-op/execute-overload-floor! ctx-id player-id skill-id exp
                                                {:floor (:overload-floor updated-state)})
              (if-not cost-ok?
                (finalize-and-terminate! ctx-id player-id {:grant-exp? true})
                (let [p         (player-pos player-id)
                      tx        (double (:target-x updated-state))
                      ty        (double (:target-y updated-state))
                      tz        (double (:target-z updated-state))
                      dx        (- tx (double (:x p)))
                      dy        (- ty (double (:y p)))
                      dz        (- tz (double (:z p)))
                      dist      (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
                      scale     (if (> dist 1.0e-6) dist 1.0)
                      desired-x (/ dx scale)
                      desired-y (/ dy scale)
                      desired-z (/ dz scale)
                      player-vel (when (motion-effects/player-motion-available?)
                                   (motion-effects/player-velocity player-id))
                      cur-vx    (double (or (:x player-vel) 0.0))
                      cur-vy    (double (or (:y player-vel) 0.0))
                      cur-vz    (double (or (:z player-vel) 0.0))
                      mx        (double (:motion-x updated-state))
                      my        (double (:motion-y updated-state))
                      mz        (double (:motion-z updated-state))
                      speed-sq-last (+ (* cur-vx cur-vx) (* cur-vy cur-vy) (* cur-vz cur-vz))
                      speed-sq-m    (+ (* mx mx) (* my my) (* mz mz))
                      [base-x base-y base-z] (if (> (Math/abs (- speed-sq-m speed-sq-last)) 0.5)
                                               [cur-vx cur-vy cur-vz]
                                               [mx my mz])
                      next-x    (try-adjust base-x desired-x)
                      next-y    (try-adjust base-y desired-y)
                      next-z    (try-adjust base-z desired-z)]
                  (when (motion-effects/player-motion-available?)
                    (motion-effects/set-player-velocity!
                                                 player-id next-x next-y next-z))
                  (ctx-skill/replace-skill-state! ctx-id
                                         (assoc updated-state
                                                :movement-ticks movement-ticks
                                                :motion-x next-x
                                                :motion-y next-y
                                                :motion-z next-z))
                  ;; Carry the caster on every update: the client's :update
                  ;; branch merges base-meta over the stored state, so omitting
                  ;; it here nulled :source-player-id and every viewer then drew
                  ;; the arc from their OWN hand instead of only the caster's.
                  ;; :caster-pos rides along for the same reason — the arc is
                  ;; anchored to the caster's eye (upstream setFromTo), not the
                  ;; viewer's hand.
                  (fx/send-local-and-nearby! ctx-id {:topic :mag-movement/fx-update :mode :update} nil
                            {:source-player-id player-id
                             :caster-pos (geom/eye-pos player-id)
                             :target {:x tx :y ty :z tz}})
                  (when (zero? (mod movement-ticks 10))
                    (log/debug "MagMovement: moving for" (/ movement-ticks 20.0) "seconds")))))))))))

(defn- on-up!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx)
          has-target  (:has-target skill-state)]
      (when has-target
        (finalize-and-terminate! ctx-id player-id {:grant-exp? true})
        (log/debug "MagMovement completed: ticks" (:movement-ticks skill-state))))))

(defn- on-abort!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (finalize-and-terminate! ctx-id player-id {:grant-exp? true})
  (log/debug "MagMovement aborted"))

(defn- on-cost-fail!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks cost-stage _player-ref]
  (finalize-and-terminate! ctx-id player-id {:grant-exp? (not= cost-stage :down)}))

(declare mag-movement-skill)

(defskill mag-movement-skill
  :id              :mag-movement
  :category-id     :electromaster
  :name-key        "ability.skill.electromaster.mag_movement"
  :description-key "ability.skill.electromaster.mag_movement.desc"
  :icon            "textures/abilities/electromaster/skills/mag_movement.png"
  :ui-position     [137 35]
  :ctrl-id         :mag-movement
  :pattern         :hold-channel
  :cooldown        {:mode :manual}
  :cost {:down {:overload down-overload-cost}
         :tick {:cp tick-cp-cost}}
  :actions {:down!  on-down!
            :tick!  on-tick!
            :up!    on-up!
            :abort! on-abort!
            :cost-fail! on-cost-fail!}
  :prerequisites [{:skill-id :arc-gen         :min-exp 0.0}
                  {:skill-id :current-charging :min-exp 0.7}])
