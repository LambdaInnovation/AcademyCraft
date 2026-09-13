(ns cn.li.ac.content.ability.meltdowner.electron-missile
  "ElectronMissile skill - hold to accumulate and fire electron balls.

  Pattern: :hold-channel
  Spawn interval: 10 ticks
  Fire interval: 8 ticks
  Max stored balls: 5
  Damage per ball: lerp(10, 18, exp)
  Seek range: lerp(5, 13, exp) blocks
  Tick CP cost: lerp(12, 5, exp)
  Down overload floor: 200
  Per-attack extra: CP lerp(60, 25, exp) + overload lerp(9, 4, exp)
  Cooldown: 700 ticks (original's reversed clamp always returns 700)
  Max hold: lerp(80, 200, exp) ticks
  Exp: +0.001 per entity hit

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
                        [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
                        [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :electron-missile)
(def ^:private mdball-entity-id (modid/namespaced-path "entity_md_ball"))

;; Original: new EntityMdBall(player) -> life 2333333 ticks, i.e. the balls
;; never expire by themselves — they die when fired or when the channel ends,
;; and their renderer therefore never plays the burst-and-shrink death curve.
;; Left on the entity spec's 50-tick default they popped after 2.5s and left
;; dead ids behind in :ball-ids. Any life past the longest possible channel
;; (max-hold is at most 200 ticks) is equivalent; a finite one still keeps a
;; ball from outliving a context that died without settling.
(def ^:private mdball-life-ticks 400)
(def ^:private electron-missile-skill-id :electron-missile)

(defn- missile-filter-self
  "Remove the shooter from candidate list."
  [player-id e]
  (not= (str (:uuid e)) (str player-id)))

(defn- missile-dist-sq-from-player
  "Squared entity-position distance, matching Entity#getDistanceSq(player)."
  [player-pos e]
  (let [dx (- (double (:x e)) (double (:x player-pos)))
        dy (- (double (:y e)) (double (:y player-pos)))
        dz (- (double (:z e)) (double (:z player-pos)))]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn- find-nearest-entity [player-id world-id exp]
  (when (world-effects/available?)
    (let [seek-range (cfg-lerp :targeting.seek-range exp)
          player-pos (geom/body-pos player-id)
          candidates (world-effects/find-entities-in-radius
                       world-id
                       (double (:x player-pos))
                       (double (:y player-pos))
                       (double (:z player-pos))
                       (double seek-range))]
      (->> candidates
           (filter (partial missile-filter-self player-id))
           (filter (fn [e] (:living? e false)))
           (sort-by (partial missile-dist-sq-from-player player-pos))
           first))))

(defn- current-overload [player-id]
  (double (or (get-in (skill-effects/get-player-state player-id)
                      [:resource-data :cur-overload])
              0.0)))

(defn- try-pay-attack-cost! [player-id exp]
  (let [{:keys [success?]} (skill-effects/perform-resource!
                             player-id
                             (cfg-lerp :cost.attack.overload exp)
                             (cfg-lerp :cost.attack.cp exp))]
    (boolean success?)))

(defn- send-start-fx! [ctx-id]
  (fx/send-local-and-nearby! ctx-id {:topic :electron-missile/fx-start} nil {}))

(defn- send-update-fx! [ctx-id player-id ticks active-balls]
  ;; The caster's feet ride along: EMContextC puffs its orbit particles around
  ;; the player, and the client only ever sees world-space particle commands.
  (let [pos (geom/body-pos player-id)]
    (fx/send-local-and-nearby! ctx-id {:topic :electron-missile/fx-update} nil
                                 {:ticks ticks
                                  :balls active-balls
                                  :x (:x pos)
                                  :y (:y pos)
                                  :z (:z pos)})))

(defn- send-fire-fx! [ctx-id start-pos target]
  ;; MSG_EFFECT_SPAWN(ball.pos, target.pos + eyeHeight).
  (fx/send-local-and-nearby! ctx-id {:topic :electron-missile/fx-fire} nil
                               {:start start-pos
                                :end {:x (:x target)
                                      :y (+ (double (:y target)) (double (or (:eye-height target) 0.0)))
                                      :z (:z target)}}))

(defn- send-end-fx! [ctx-id]
  (fx/send-local-and-nearby! ctx-id {:topic :electron-missile/fx-end} nil {}))

(defn- cooldown-ticks []
  (cfg-int :cooldown.ticks))

(defn- discard-balls!
  [world-id ball-ids]
  (when (motion-effects/entity-motion-available?)
    (doseq [ball-id ball-ids]
      (motion-effects/discard-entity! world-id ball-id))))

(defn- settle!
  [ctx-id player-id]
  (let [state (get-in (ctx-skill/get-context ctx-id) [:skill-state])
        world-id (geom/world-id-of player-id)]
    (discard-balls! world-id (or (:ball-ids state) []))
    (skill-effects/set-main-cooldown! player-id electron-missile-skill-id
                                      (cooldown-ticks))
    (send-end-fx! ctx-id)
    (ctx-skill/replace-skill-state! ctx-id
                           {:ticks 0 :active-balls 0 :ball-ids [] :active? false})))

(defn electron-missile-down!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  ;; Original ignores the return value of the activation consume call and
  ;; always initializes the context, snapshotting the resulting overload.
  (let [overload-floor (current-overload player-id)]
    (ctx-skill/replace-skill-state! ctx-id
                           {:ticks 0
                            :active-balls 0
                            :ball-ids []
                            :active? true
                            :overload-floor overload-floor})
    (send-start-fx! ctx-id)))

(defn- remove-vector-index
  [v idx]
  (into (subvec v 0 idx) (subvec v (inc idx))))

(defn electron-missile-tick!
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage player-ref]
  (when cost-ok?
    (try
      (let [ctx-data (ctx-skill/get-context ctx-id)
            state (get ctx-data :skill-state {})
            ticks (long (or (:ticks state) 0))
            ball-ids (vec (or (:ball-ids state) []))
            overload-floor (double (or (:overload-floor state) (current-overload player-id)))
            max-hold (cfg-lerp-int :charge.max-hold-ticks exp)
            max-balls (cfg-int :projectile.max-hold-balls)
            spawn-interval (cfg-int :timing.spawn-interval-ticks)
            fire-interval (cfg-int :timing.fire-interval-ticks)
            world-id (geom/world-id-of player-id)]
        (skill-effects/enforce-overload-floor! player-id overload-floor)
        (if (> ticks max-hold)
          (do
            ;; Original still sends its update message on the timeout tick.
            (send-update-fx! ctx-id player-id ticks (count ball-ids))
            (settle! ctx-id player-id)
            (ctx/terminate-context! ctx-id nil))
          (let [spawned-id (when (and player-ref
                                      (zero? (mod ticks spawn-interval))
                                      (< (count ball-ids) max-balls))
                             (entity/player-spawn-tracked-entity-by-id!
                               player-ref mdball-entity-id 0.0 mdball-life-ticks))
                ball-ids-after-spawn (cond-> ball-ids
                                       spawned-id (conj (str spawned-id)))
                should-fire? (and (pos? ticks)
                                  (seq ball-ids-after-spawn)
                                  (zero? (mod ticks fire-interval)))
                ball-ids-after-fire
                (if-not should-fire?
                  ball-ids-after-spawn
                  (let [target (find-nearest-entity player-id world-id exp)]
                    (if (and target (try-pay-attack-cost! player-id exp))
                      (let [ball-index (rand-int (count ball-ids-after-spawn))
                            ball-id (nth ball-ids-after-spawn ball-index)
                            ;; No fallback to the eye: the ray must originate
                            ;; from the ACTUAL ball or not fire at all — a
                            ;; missing ball is a bug to surface, not something
                            ;; to silently redirect through the caster.
                            ball-pos (when (motion-effects/entity-motion-available?)
                                       (motion-effects/entity-position world-id ball-id))]
                        (if-not ball-pos
                          (do
                            (log/warn "ElectronMissile: ball entity missing, skipping shot" ball-id)
                            ;; Drop the dead id rather than keeping it: left in
                            ;; place it is picked again on every later fire tick
                            ;; and still counts against max-hold-balls, so five
                            ;; of them silently end the channel's ball supply.
                            (remove-vector-index ball-ids-after-spawn ball-index))
                          (do
                            (when (and (entity-damage/available?) (:uuid target))
                              (entity-damage/apply-direct-damage!
                                world-id
                                (:uuid target)
                                (cfg-lerp :combat.damage exp)
                                :magic
                                {:reset-invulnerable-time? true})
                              (md-damage/mark-target! player-id (:uuid target)
                                {:ctx-id ctx-id
                                 :target-pos {:x (:x target)
                                              :y (:y target)
                                              :z (:z target)}})
                              (skill-effects/add-skill-exp! player-id
                                                           electron-missile-skill-id
                                                           (cfg-double :progression.exp-hit)))
                            (send-fire-fx! ctx-id ball-pos target)
                            (when (motion-effects/entity-motion-available?)
                              (motion-effects/discard-entity! world-id ball-id))
                            (remove-vector-index ball-ids-after-spawn ball-index))))
                      ball-ids-after-spawn)))]
            (send-update-fx! ctx-id player-id ticks (count ball-ids-after-fire))
            (ctx-skill/replace-skill-state! ctx-id
                                   {:ticks (inc ticks)
                                    :active-balls (count ball-ids-after-fire)
                                    :ball-ids ball-ids-after-fire
                                    :active? true
                                    :overload-floor overload-floor}))))
      (catch Exception e
        (log/debug "ElectronMissile tick! failed:" (ex-message e))))))

(defn electron-missile-up!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (settle! ctx-id player-id))

(defn electron-missile-abort!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  ;; Original applies cooldown on abort too (same MSG_TERMINATED handler as
  ;; a normal release) — abort must not be a free zero-cooldown re-cast.
  (settle! ctx-id player-id))

(defn electron-missile-cost-fail!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks cost-stage _player-ref]
  (when (= cost-stage :tick)
    (settle! ctx-id player-id)
    (ctx/terminate-context! ctx-id nil)))

(defskill electron-missile
  :id             :electron-missile
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.electron_missile"
  :description-key "ability.skill.meltdowner.electron_missile.desc"
  :icon           "textures/abilities/meltdowner/skills/electron_missile.png"
  :ui-position    [210 35]
  :ctrl-id        :electron-missile
  :cp-consume-speed 1.0
  :overload-consume-speed 1.0
  :pattern        :hold-channel
  :cost           {:down {:overload (fn [_] (cfg-double :cost.down.overload))}
                   :tick {:cp (fn [{:keys [player-id]}]
                                (cfg-lerp :cost.tick.cp (skill-exp player-id)))}}
  :cooldown       {:mode :manual}
  :cooldown-ticks (fn [_] (cooldown-ticks))
  ;; matching original: clampi(700, 400, exp) �?cooldown �?[400, 700] ticks
  :actions        {:down!  electron-missile-down!
                   :tick!  electron-missile-tick!
                   :up!    electron-missile-up!
                   :abort! electron-missile-abort!
                   :cost-fail! electron-missile-cost-fail!}
  :fx             {:start  {:topic :electron-missile/fx-start :payload (fn [_] {})}
                   :update {:topic :electron-missile/fx-update :payload (fn [_] {})}
                   :end    {:topic :electron-missile/fx-end :payload (fn [_] {})}}
  :prerequisites  [{:skill-id :jet-engine :min-exp 0.3}])

