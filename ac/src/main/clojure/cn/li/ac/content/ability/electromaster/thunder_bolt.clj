(ns cn.li.ac.content.ability.electromaster.thunder-bolt
  "ThunderBolt skill - instant targeted lightning strike with AOE damage.

  Pattern: :instant
  Cost: CP lerp(280,420), overload lerp(50,27) by exp
  Cooldown: lerp(120,50) ticks by exp
  Exp: +0.005 effective / +0.003 ineffective"
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.util.attack :as attack]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.effects.potion :as potion-effects]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]))

(def-skill-config-ops :thunder-bolt)
(def ^:private thunder-bolt-skill-id :thunder-bolt)

(defn- down-cp-cost
  [_player-id _skill-id exp]
  ;; Original casts lerp(280, 420, exp) to Int before consumption.
  (int (cfg-lerp :cost.down.cp (double (or exp 0.0)))))

(defn- down-overload-cost
  [_player-id _skill-id exp]
  (cfg-lerp :cost.down.overload (double (or exp 0.0))))

(defn- cooldown-ticks
  [exp]
  ;; Scala Float#toInt truncates toward zero; do not round to nearest.
  (int (skill-config/lerp-double thunder-bolt-skill-id
                                  :cooldown.ticks
                                  (double (or exp 0.0)))))

(defn- try-charge-creeper!
  "Matches original EMDamageHelper.attack: a flat chance to power a creeper
  selected for this strike (direct or AOE). The original only consumes the
  random roll for creepers and performs this after AbilityContext.attack."
  [world-id target-uuid]
  (when (and target-uuid
             (motion/entity-motion-available?)
             (entity/entity-type-id-fn-available?)
             (= "minecraft:creeper" (entity/get-type-id world-id target-uuid))
             (< (rand) (cfg-double :effect.creeper-charge-chance)))
    (motion/power-creeper! world-id target-uuid)))

(defn- try-apply-slowness!
  [target-uuid exp duration-ticks]
  (let [exp-threshold (cfg-double :effect.slowness-exp-threshold)
        chance (double (skill-config/probability thunder-bolt-skill-id :effect.slowness-chance))]
    (when (and (potion-effects/available?)
               target-uuid
               (> exp exp-threshold)
               (< (rand) chance))
      (potion-effects/apply-effect!
        target-uuid
        :slowness
        duration-ticks
        (cfg-int :effect.slowness-amplifier))
      true)))

(defn- scaled-target-damage
  "Match AbilityContext.attack: target-specific SkillAttack calculation first,
  followed by the global and per-skill damage multipliers."
  [player-id target-uuid raw-damage]
  (skill-effects/scale-damage
    (skill-registry/get-skill thunder-bolt-skill-id)
    (ability-event/fire-calc-event!
      ability-event/CALC-SKILL-ATTACK
      raw-damage
      {:player-id player-id
       :target-id target-uuid
       :skill-id thunder-bolt-skill-id})))

(defn- attack-target!
  [player-id world-id target-uuid raw-damage]
  (when (and target-uuid (entity-damage/available?))
    (let [damage (scaled-target-damage player-id target-uuid raw-damage)]
      (when (pos? damage)
        (entity-damage/apply-direct-damage!
          world-id
          target-uuid
          damage
          :skill
          {:attacker-uuid player-id
           :skill-id thunder-bolt-skill-id}))))
  nil)

(defn- living-victims
  "Original AOE selector is EntitySelectors.living(), not every entity in the
  platform AABB query."
  [victims]
  (filterv #(true? (:living? %)) victims))

(defn thunder-bolt-perform!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [exp* (double (or exp 0.0))
        range (cfg-double :targeting.range)
        direct-damage (cfg-lerp :combat.direct-damage exp*)
        aoe-radius (cfg-double :combat.aoe-radius)
        aoe-damage (cfg-lerp :combat.aoe-damage exp*)
        cooldown (cooldown-ticks exp*)
        {:keys [world-id eye look hit-kind target-uuid impact]}
        (attack/resolve-attack-data player-id range)
        excluded (cond-> #{player-id}
                   target-uuid (conj target-uuid))
        ;; Raytrace.traceLiving returns a MISS hit at the full-range endpoint,
        ;; and the original still performs its AOE query around that point.
        victims (living-victims
                  (attack/aoe-victims world-id impact aoe-radius excluded))
        aoe-points (mapv (fn [{:keys [x y z eye-height]}]
                           {:x (double x)
                            :y (+ (double y) (double (or eye-height 0.0)))
                            :z (double z)})
                         victims)
        ;; c_spawnEffect never calls setFromTo on the main arcs: it spawns
        ;; three EntityArcs at the caster's eye along their look direction with
        ;; `mainArc.length = RANGE`, so they always run the full 20 blocks and
        ;; pass through whatever was hit. AttackData.point (the trace result)
        ;; is used only as the AOE arcs' origin. Ending the main arcs at the
        ;; impact point instead made a close-range hit draw a short stub.
        main-end (attack/fallback-end-point eye look range)
        effective? (boolean (or target-uuid (seq victims)))]
    (when target-uuid
      (attack-target! player-id world-id target-uuid direct-damage)
      (try-charge-creeper! world-id target-uuid)
      (try-apply-slowness! target-uuid exp*
                           (cfg-int :effect.slowness-duration-ticks)))
    (doseq [{victim-uuid :uuid} victims]
      (attack-target! player-id world-id victim-uuid aoe-damage)
      (try-charge-creeper! world-id victim-uuid)
      ;; Preserve the shipped original's observable quirk: every AOE victim
      ;; rerolls slowness on the direct target for 20 ticks. If the initial
      ;; 40-tick roll succeeded, vanilla potion merging keeps the longer one.
      (when target-uuid
        (try-apply-slowness!
          target-uuid
          exp*
          (cfg-int :effect.slowness-aoe-retry-duration-ticks))))
    ;; Original's s_perform sendToClient(MSG_PERFORM, ad) reaches owner +
    ;; nearby unconditionally (no isLocal gate in c_spawnEffect) — the strong
    ;; arcs, AOE arcs, and sound must render for bystanders too.
    (fx/send-local-and-nearby! ctx-id {:topic :thunder-bolt/fx-perform} nil {:start eye
                              ;; Three main arcs always retain RANGE length;
                              ;; the AOE arcs originate at AttackData.point.
                              :end main-end
                              :aoe-origin impact
                              :aoe-points aoe-points
                              :source-player-id player-id
                              :world-id world-id
                              :hit-kind hit-kind
                              :performed? true})
    (skill-effects/add-skill-exp! player-id
                                  thunder-bolt-skill-id
                                  (if effective?
                                    (cfg-double :progression.exp-effective)
                                    (cfg-double :progression.exp-ineffective)))
    (skill-effects/set-main-cooldown! player-id :thunder-bolt cooldown)
    nil))

(defskill thunder-bolt
  :id          :thunder-bolt
  :category-id :electromaster
  :name-key    "ability.skill.electromaster.thunder_bolt"
  :description-key "ability.skill.electromaster.thunder_bolt.desc"
  :icon        "textures/abilities/electromaster/skills/thunder_bolt.png"
  :ui-position [86 67]
  :ctrl-id     :thunder-bolt
  :pattern     :instant
  :cooldown    {:mode :manual}
  :cost        {:down {:cp       down-cp-cost
                       :overload down-overload-cost}}
  :cooldown-ticks (fn [{:keys [exp]}]
                    (cooldown-ticks exp))
  :actions     {:perform! thunder-bolt-perform!}
  :prerequisites [{:skill-id :arc-gen         :min-exp 0.0}
                  {:skill-id :current-charging :min-exp 0.7}])

