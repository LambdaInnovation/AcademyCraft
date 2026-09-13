(ns cn.li.ac.content.ability.vecmanip.directed-shock
  "DirectedShock - melee punch with charge window.

  Pattern: :charge-window (valid window 6-50 ticks, abort at 200)
  Cost on perform: CP lerp(50,100), overload lerp(18,12) by exp
  Cooldown: lerp(60,20) ticks by exp (hit-only)
  Exp: +0.0035 on hit / +0.001 on miss"
  (:require [cn.li.ac.ability.util.scaling :as scaling]
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :directed-shock)

(defn- scaled-damage [player-id target-id raw-damage]
  (skill-effects/scale-damage
   (skill-registry/get-skill :directed-shock)
   (ability-event/fire-calc-event!
    ability-event/CALC-SKILL-ATTACK
    raw-damage
    {:player-id player-id
     :target-id target-id
     :skill-id :directed-shock})))

(defn- entity-trace
  [player-id]
  (when (raycast/available?)
    (raycast/raycast-from-player
                                 player-id
                                 (cfg-double :targeting.raycast-distance)
                                 true)))

(defn- terminate-with-end!
  [ctx-id performed?]
  (fx/send! ctx-id {:topic :directed-shock/fx-end :mode :end} nil {:performed? performed?})
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (ctx-skill/replace-skill-state! ctx-id (assoc (:skill-state ctx-data) :performed? performed?)))
  (ctx-skill/clear-skill-state! ctx-id)
  (ctx/terminate-context! ctx-id nil))

(defn- hit-impulse [caster-pos hit-pos]
  (-> (geom/v- hit-pos caster-pos)
      geom/vnorm
      (geom/v* (cfg-double :movement.hit-impulse))))

(defn- knockback-velocity
  "Reproduce the original DirectedShock knockback, including its motionZ =
  delta.y quirk."
  [player-id hit-pos]
  (let [player-head (geom/eye-pos player-id)
        target-head {:x (:x hit-pos)
                     :y (+ (:y hit-pos)
                           (double (or (:eye-height hit-pos)
                                       (cfg-double :targeting.eye-height))))
                     :z (:z hit-pos)}
        d0 (geom/vnorm (geom/v- player-head target-head))
        d1 (geom/vnorm {:x (:x d0)
                        :y (- (:y d0) (cfg-double :movement.knockback-y-adjust))
                        :z (:z d0)})
        scale (cfg-double :movement.knockback-scale)]
    {:x (* (:x d1) scale)
     :y (* (:y d1) scale)
     :z (* (:y d1) scale)}))

(defn- player-body-position
  [player-id eye-pos]
  (or (raycast/player-position player-id)
      {:x (:x eye-pos)
       :y (- (double (:y eye-pos)) (cfg-double :targeting.eye-height))
       :z (:z eye-pos)}))

(defn- directed-shock-down!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (ctx-skill/replace-skill-state! ctx-id
                         {:charge-ticks 0
                          :performed? false
                          :punched? false
                          :punch-ticks 0})
  (fx/send! ctx-id {:topic :directed-shock/fx-start :mode :start}))

(defn- directed-shock-tick!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (if (true? (get-in ctx-data [:skill-state :punched?]))
      (let [next-punch (inc (long (or (get-in ctx-data [:skill-state :punch-ticks]) 0)))]
        (ctx-skill/replace-skill-state! ctx-id (assoc (:skill-state ctx-data) :punch-ticks next-punch))
        (when (> next-punch (cfg-int :charge.punch-anim-ticks))
          (terminate-with-end! ctx-id true)))
      (let [next-charge (inc (long (or (get-in ctx-data [:skill-state :charge-ticks]) 0)))]
        (ctx-skill/replace-skill-state! ctx-id (assoc (:skill-state ctx-data) :charge-ticks next-charge))
        (when (>= next-charge (cfg-int :charge.max-tolerant-ticks))
          (terminate-with-end! ctx-id false))))))

(defn- directed-shock-up!
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [charge-ticks (long (or (get-in ctx-data [:skill-state :charge-ticks]) 0))
          exp* (scaling/clamp-exp (double (or exp 0.0)))]
      (if (and (> charge-ticks (cfg-int :charge.min-ticks))
               (< charge-ticks (cfg-int :charge.max-accepted-ticks)))
        (if-not cost-ok?
          (terminate-with-end! ctx-id false)
          (let [world-id (geom/world-id-of player-id)
                eye (geom/eye-pos player-id)
                player-pos (player-body-position player-id eye)
                trace (entity-trace player-id)]
            (if-let [target-id (:entity-id trace)]
              (let [hit-pos {:x (double (or (:x trace) 0.0))
                             :y (double (or (:y trace) 0.0))
                             :z (double (or (:z trace) 0.0))
                             :eye-height (double (or (:eye-height trace)
                                                     (cfg-double :targeting.eye-height)))}
                    damage (cfg-lerp :combat.damage exp*)
                    knockback (when (>= exp* (cfg-double :movement.knockback-exp-threshold))
                                (knockback-velocity player-id hit-pos))
                    moved-hit-pos (if knockback
                                    (update hit-pos :y + 0.1)
                                    hit-pos)
                    impulse (hit-impulse player-pos moved-hit-pos)]
                (when (entity-damage/available?)
                  (entity-damage/apply-direct-damage!
                   world-id
                   target-id
                   (scaled-damage player-id target-id damage)
                   :skill
                   {:attacker-uuid player-id}))
                (when (motion-effects/entity-motion-available?)
                  (when knockback
                    (motion-effects/set-entity-position!
                     world-id target-id
                     (:x moved-hit-pos) (:y moved-hit-pos) (:z moved-hit-pos))
                    (motion-effects/set-entity-velocity!
                     world-id target-id
                     (:x knockback) (:y knockback) (:z knockback)))
                  (motion-effects/add-entity-velocity! world-id target-id
                                                (:x impulse) (:y impulse) (:z impulse)))
                ;; Original's second MSG_GENERATE_EFFECT client listener plays
                ;; the punch sound unconditionally (outside the isLocal guard
                ;; that gates only the hand-punch animation) — a positional
                ;; sound at the caster, audible to owner + nearby. The impact
                ;; hand animation itself stays local (upstream l_effect is
                ;; isLocal): the owner-only fx-punch below drives the hand
                ;; punch while the fan-out here only carries the sound.
                (fx/send-local-and-nearby! ctx-id {:topic :directed-shock/fx-perform :mode :perform} nil {:target-id target-id
                                                                                          :world-id world-id
                                                                                          :impulse impulse
                                                                                          :knockback knockback
                                                                                          :x (:x player-pos)
                                                                                          :y (:y player-pos)
                                                                                          :z (:z player-pos)})
                (fx/send! ctx-id {:topic :directed-shock/fx-punch :mode :punch})
                (ctx-skill/replace-skill-state! ctx-id
                                                (merge (:skill-state ctx-data)
                                                       {:performed? true
                                                        :punched? true
                                                        :punch-ticks 0}))
                (skill-effects/add-skill-exp! player-id :directed-shock (cfg-double :progression.exp-hit))
                (skill-effects/set-main-cooldown!
                 player-id :directed-shock
                 (int (cfg-lerp :cooldown.ticks exp*)))
                (log/debug "DirectedShock hit" target-id "dmg" (int damage)))
              (do
                (skill-effects/add-skill-exp! player-id :directed-shock (cfg-double :progression.exp-miss))
                (terminate-with-end! ctx-id false)))))
        (terminate-with-end! ctx-id false)))))

(defn- directed-shock-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (terminate-with-end! ctx-id false))

(defskill directed-shock
  :id :directed-shock
  :category-id :vecmanip
  :name-key "ability.skill.vecmanip.directed_shock"
  :description-key "ability.skill.vecmanip.directed_shock.desc"
  :icon "textures/abilities/vecmanip/skills/dir_shock.png"
  :ui-position [16 45]
  :ctrl-id :directed-shock
  :pattern :charge-window
  :cooldown {:mode :manual}
  :cost {:up {:cp (fn [_player-id _skill-id exp]
                   (cfg-lerp :cost.up.cp (double (or exp 0.0))))
              :overload (fn [_player-id _skill-id exp]
                          (cfg-lerp :cost.up.overload (double (or exp 0.0))))}}
  :actions
  {:down! directed-shock-down!
   :tick! directed-shock-tick!
   :up! directed-shock-up!
   :abort! directed-shock-abort!})

