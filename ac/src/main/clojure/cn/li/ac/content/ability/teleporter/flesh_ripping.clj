(ns cn.li.ac.content.ability.teleporter.flesh-ripping

  "FleshRipping skill - grab and injure nearby enemies by partial teleportation.



  Pattern: :release-cast

  Range: lerp(6, 14, exp)

  Damage: lerp(5, 12, exp)

  Nausea chance: 5% on the caster for 100 ticks

  CP cost: lerp(130, 270, exp)

  Overload: lerp(60, 50, exp)

  Cooldown: lerp(90, 40, exp) ticks

  Exp: +0.005 per entity hit



  No Minecraft imports."

  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]

            [cn.li.ac.ability.fx :as fx]

            [cn.li.ac.ability.service.context-dispatcher :as ctx]

            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]

                        [cn.li.ac.ability.service.skill-effects :as skill-effects]

            [cn.li.ac.ability.effects.geom :as geom]

            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]

            [cn.li.ac.ability.effects.potion :as potion-effects]
            [cn.li.mcmod.platform.entity :as entity]

            [cn.li.mcmod.util.log :as log]))



;; ---------------------------------------------------------------------------

;; Actions

;; ---------------------------------------------------------------------------



(def-skill-config-ops :flesh-ripping)

(def ^:private flesh-ripping-skill-id :flesh-ripping)



(defn- build-trace

  [player-id exp]

  (let [range (cfg-lerp :targeting.range exp)
        hit (helper/raycast-entity player-id range)
        position (helper/player-position player-id)
        look (helper/player-look-vec player-id)]
    (when (and position look)
      (if hit
        {:world-id (or (:world-id position) (geom/world-id-of player-id))
         :hit? true
         :target-uuid (:entity-uuid hit)
         :target-x (double (or (:hit-x hit) (:entity-x hit)))
         :target-y (double (or (:hit-y hit) (:entity-y hit)))
         :target-z (double (or (:hit-z hit) (:entity-z hit)))
         :entity-x (double (:entity-x hit))
         :entity-y (double (:entity-y hit))
         :entity-z (double (:entity-z hit))
         :target-width (double (or (:width hit) 0.6))
         :target-height (double (or (:height hit) 1.8))}
        {:world-id (or (:world-id position) (geom/world-id-of player-id))
         :hit? false
         :target-uuid nil
         :target-x (+ (double (:x position))
                      (* (double (:x look)) range))
         :target-y (+ (double (:y position))
                      (* (double (:y look)) range))
         :target-z (+ (double (:z position))
                      (* (double (:z look)) range))}))))

(defn- trace-fx-payload

  "Shared fx payload for start/update/perform: the aim point plus the target's
  position/size (the client scales the marker by width*1.2 / height*1.2 like
  upstream l_updateEffect)."

  [trace]

  {:target-x (:target-x trace)

   :target-y (:target-y trace)

   :target-z (:target-z trace)

   :hit? (:hit? trace)

   :target-uuid (:target-uuid trace)

   :entity-x (:entity-x trace)

   :entity-y (:entity-y trace)

   :entity-z (:entity-z trace)

   :target-width (double (or (:target-width trace) 0.6))

   :target-height (double (or (:target-height trace) 1.8))})



(defn- flesh-ripping-ctx-trace

  "The stored trace of the live flesh-ripping context, if any."

  [player-id]

  (when-let [ctx-id (some->> (ctx/active-contexts player-id)

                             (filter #(= flesh-ripping-skill-id (:skill-id %)))

                             first

                             :id)]

    (get-in (ctx-skill/get-context ctx-id) [:skill-state :trace])))



(defn flesh-ripping-cost-up-cp

  "Up-stage CP cost. Upstream s_end charges only when the release has an
  entity target (a miss sends MSG_ABORT and consumes nothing) — return 0 for
  a miss so the framework's apply-cost! deducts nothing."

  [player-id _skill-id exp]

  (if-let [trace (flesh-ripping-ctx-trace player-id)]

    (if (and (:hit? trace) (:target-uuid trace))

      (cfg-lerp :cost.up.cp exp)

      0.0)

    (cfg-lerp :cost.up.cp exp)))



(defn flesh-ripping-cost-up-overload

  [player-id _skill-id exp]

  (if-let [trace (flesh-ripping-ctx-trace player-id)]

    (if (and (:hit? trace) (:target-uuid trace))

      (cfg-lerp :cost.up.overload exp)

      0.0)

    (cfg-lerp :cost.up.overload exp)))



(defn flesh-ripping-down!

  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  (ctx-skill/replace-skill-state! ctx-id {:hold-ticks 0

                                 :trace nil})

  ;; Upstream l_startEffect spawns the marker on MSG_MADEALIVE; send the first
  ;; aim so the marker is visible the moment the key goes down.
  (when-let [trace (build-trace player-id (skill-exp player-id))]

    (ctx-skill/replace-skill-state! ctx-id {:hold-ticks 0 :trace trace})

    (fx/send! ctx-id {:topic :flesh-ripping/fx-start :mode :start} nil

              (trace-fx-payload trace))))



(defn flesh-ripping-tick!

  [ctx-id player-id _skill-id _exp _cost-ok? hold-ticks _cost-stage player-ref]

  (let [exp (skill-exp player-id)
        cp-cost (cfg-lerp :cost.up.cp exp)
        affordable? (or (and player-ref
                             (boolean (entity/player-creative? player-ref)))
                        (>= (skill-effects/current-cp player-id) cp-cost))
        trace (build-trace player-id exp)]

    (when-not affordable?
      (ctx/terminate-context! ctx-id nil))

    (ctx-skill/replace-skill-state! ctx-id {:hold-ticks (long hold-ticks)

                     :trace trace})

    (when trace

      (fx/send! ctx-id {:topic :flesh-ripping/fx-update :mode :update} nil

                (trace-fx-payload trace)))))



(defn flesh-ripping-up!

  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]

  (try

    (let [exp (skill-exp player-id)

          damage (cfg-lerp :combat.damage exp)

          trace (or (get-in (ctx-skill/get-context ctx-id) [:skill-state :trace])

                    (build-trace player-id exp))]

      ;; Upstream s_end sendToClient(MSG_EFFECT_END, target) is unconditional —
      ;; a miss still ends the effect (the client kills the marker; only the
      ;; blood splashes and sound are target-gated in c_endEffect).
      (fx/send-local-and-nearby! ctx-id {:topic :flesh-ripping/fx-perform :mode :perform} nil

                (trace-fx-payload trace))

      (when (and cost-ok? (:hit? trace) (:target-uuid trace))

        (let [world-id (:world-id trace)

              e-uuid (:target-uuid trace)

              damage-result (helper/deal-magic-damage!
                              player-id
                              flesh-ripping-skill-id
                              world-id
                              e-uuid
                              damage)]

          (when (helper/crit-applied? damage-result)

            (fx/send! ctx-id {:topic :teleporter/fx-crit-hit} nil

                      {:x (:target-x trace)

                       :y (:target-y trace)

                       :z (:target-z trace)

                       :crit-level (:crit-level damage-result)

                       :crit-rate (:crit-rate damage-result)

                       :message-key (:message-key damage-result)

                       :message-args (:message-args damage-result)

                       :target-uuid e-uuid

                       :target-width (:target-width trace)

                       :target-height (:target-height trace)

                       :skill-id flesh-ripping-skill-id}))

          (when (and (< (rand) (cfg-probability :effect.nausea-chance))

                     (potion-effects/available?))

            (potion-effects/apply-effect!

              player-id :nausea

              (cfg-int :effect.nausea-duration-ticks)

              (cfg-int :effect.nausea-amplifier)))

          (skill-effects/add-skill-exp! player-id flesh-ripping-skill-id

                                        (cfg-double :progression.exp-hit))

          (let [cd (cfg-lerp-int :cooldown.ticks exp)]

            (skill-effects/set-main-cooldown! player-id flesh-ripping-skill-id cd)))))

    (catch Exception e

      (log/warn "FleshRipping up! failed:" (ex-message e)))))



(defn flesh-ripping-abort!

  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  (ctx-skill/clear-skill-state! ctx-id)

  ;; Upstream l_onKeyAbort sends MSG_EFFECT_END(empty) -> marker dies.
  (fx/send! ctx-id {:topic :flesh-ripping/fx-end :mode :end} nil))



;; ---------------------------------------------------------------------------

;; Skill registration

;; ---------------------------------------------------------------------------



(declare flesh-ripping-skill)



(defskill flesh-ripping-skill

  :id             :flesh-ripping

  :category-id    :teleporter

  :name-key       "ability.skill.teleporter.flesh_ripping"

  :description-key "ability.skill.teleporter.flesh_ripping.desc"

  :icon           "textures/abilities/teleporter/skills/flesh_ripping.png"

  :ui-position    [130 12]



  :ctrl-id        :flesh-ripping

  :cp-consume-speed 0.0

  :overload-consume-speed 0.0

  :pattern        :release-cast

  :cost           {:up {:cp       flesh-ripping-cost-up-cp

                      :overload flesh-ripping-cost-up-overload

                      :creative? (fn [_player-id _skill-id _exp player-ref]
                                   (boolean
                                     (and player-ref
                                          (entity/player-creative? player-ref))))}}

  :cooldown       {:mode :manual}

  :actions        {:down!  flesh-ripping-down!

                   :tick!  flesh-ripping-tick!

                   :up!    flesh-ripping-up!

                   :abort! flesh-ripping-abort!}

  :fx             {:start {:topic :flesh-ripping/fx-start :payload (fn [_] {})}

                   :update {:topic :flesh-ripping/fx-update :payload (fn [_] {})}

                   :end   {:topic :flesh-ripping/fx-end   :payload (fn [_] {})}}

  :prerequisites  [{:skill-id :mark-teleport :min-exp 0.5}

                   {:skill-id :penetrate-teleport :min-exp 0.5}])



