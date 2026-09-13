(ns cn.li.ac.content.ability.electromaster.arc-gen
  "Arc Gen skill - instant electric arc attack with raycast targeting.

  Pattern: :instant (single key press)
  Cost: CP lerp(30, 70, exp), overload lerp(18, 11, exp)
  Cooldown: int(lerp(15, 5, post-cast-exp)) ticks
  Damage: lerp(5, 9, exp), then AbilityContext-compatible attack scaling
  Range: lerp(6, 15, exp) blocks
  Ignite probability: lerp(0, 0.6, exp)
  Fishing probability: 0.1 if exp > 0.5, else 0
  Creeper charging probability: 0.3

  Exp gain:
  - Hit entity: 0.0048 + 0.0024 * exp
  - Hit block or miss: 0.0018 + 0.0009 * exp

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.server.platform-bridge :as server-bridge]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :arc-gen)
(def ^:private arc-gen-skill-id :arc-gen)
(def ^:private fish-item-id "minecraft:cooked_cod")

;; ---------------------------------------------------------------------------
;; World interactions
;; ---------------------------------------------------------------------------

(defn- try-ignite-block!
  "Attempt to ignite the air block immediately above [x y z]."
  [world-id x y z probability]
  (when (and (block-manip/available?)
             (< (rand) probability))
    (let [current-block (block-manip/get-block world-id x (inc y) z)]
      (when (or (nil? current-block)
                (= current-block "minecraft:air"))
        (block-manip/set-block!
          world-id x (inc y) z
          "minecraft:fire")))))

(defn- try-fishing!
  "Spawn cooked cod at the precise water hit vector, matching EntityItem."
  [world-id x y z probability player]
  (when (< (rand) probability)
    (when-let [fish-stack (pitem/stack-by-id fish-item-id 1)]
      (if (server-bridge/server-bridge-available?)
        (server-bridge/spawn-item-stack-at!
          player world-id x y z fish-stack)
        ;; The bridge is installed in-game; retain this safe fallback for
        ;; isolated runtimes and tests.
        (entity/player-give-item-stack! player fish-stack))
      (log/debug "Arc Gen: fishing reward spawned at" x y z))))

(defn- try-charge-creeper!
  "Match EMDamageHelper.attack's post-attack 30% creeper power roll."
  [world-id target-uuid]
  (when (and target-uuid
             (motion/entity-motion-available?)
             (entity/entity-type-id-fn-available?)
             (= "minecraft:creeper"
                (entity/get-type-id world-id target-uuid))
             (< (rand) (cfg-double :effect.creeper-charge-chance)))
    (motion/power-creeper! world-id target-uuid)))

;; ---------------------------------------------------------------------------
;; Targeting and damage
;; ---------------------------------------------------------------------------

(defn- hit-distance
  [hit]
  (if (number? (:distance hit))
    (double (:distance hit))
    Double/POSITIVE_INFINITY))

(defn- nearest-hit
  "Match LambdaLib2 Raytrace.traceLiving with ArcGen's block filter:
  collidable entities (excluding the caster) win equal-distance ties, while
  blocks are limited to collision-bearing blocks and water."
  [player-id world-id eye look range]
  (let [block-hit (raycast/raycast-collidable-blocks-or-water
                    world-id
                    (:x eye) (:y eye) (:z eye)
                    (:x look) (:y look) (:z look)
                    range)
        entity-hit (raycast/raycast-from-player
                     player-id range false)]
    (cond
      (and entity-hit
           (<= (hit-distance entity-hit)
               (hit-distance block-hit)))
      (assoc entity-hit :hit-type :entity)

      block-hit
      (assoc block-hit :hit-type :block)

      :else nil)))

(defn- scaled-target-damage
  "Apply CalcEvent.SkillAttack, global damage scale, and skill damage scale."
  [player-id target-uuid raw-damage]
  (skill-effects/scale-damage
    (skill-registry/get-skill arc-gen-skill-id)
    (ability-event/fire-calc-event!
      ability-event/CALC-SKILL-ATTACK
      raw-damage
      {:player-id player-id
       :target-id target-uuid
       :skill-id arc-gen-skill-id})))

(defn- attack-target!
  [player-id world-id target-uuid raw-damage]
  (when (and target-uuid
             (entity-damage/available?))
    (let [damage (scaled-target-damage
                   player-id target-uuid raw-damage)]
      (when (pos? damage)
        (entity-damage/apply-direct-damage!
          world-id
          target-uuid
          damage
          :skill
          {:attacker-uuid player-id
           :skill-id arc-gen-skill-id}))))
  nil)

(defn- cooldown-ticks
  [exp]
  ;; Java's (int) lerpf truncates toward zero.
  (int (skill-config/lerp-double
         arc-gen-skill-id
         :cooldown.ticks
         (double (or exp 0.0)))))

;; ---------------------------------------------------------------------------
;; Action
;; ---------------------------------------------------------------------------

(defn- perform-arc-gen!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage player]
  (try
    (let [exp*        (double (or exp 0.0))
          damage      (cfg-lerp :combat.damage exp*)
          range       (cfg-lerp :targeting.range exp*)
          ignite-prob (cfg-lerp :effect.ignite-probability exp*)
          fish-prob   (if (> exp*
                             (cfg-double :effect.fishing-exp-threshold))
                        (skill-config/probability
                          arc-gen-skill-id
                          :effect.fishing-probability)
                        0.0)
          world-id    (geom/world-id-of player-id)
          eye         (geom/eye-pos player-id)
          sound-pos   (geom/body-pos player-id)
          look        (when (raycast/available?)
                        (raycast/player-look-vector player-id))]
      (when look
        (let [miss-end (geom/v+ eye (geom/v* look range))
              hit      (nearest-hit
                         player-id world-id eye look range)
              hit-type (or (:hit-type hit) :miss)
              ;; Retain the port's enhanced precise impact endpoint instead
              ;; of reverting the arc to the original's unconditional range.
              hit-pos  (case hit-type
                         :entity
                         {:x (double (or (:x hit) 0.0))
                          :y (+ (double (or (:y hit) 0.0))
                                (double (or (:eye-height hit) 0.0)))
                          :z (double (or (:z hit) 0.0))}

                         :block
                         {:x (double (or (:hit-x hit) (:x hit) 0.0))
                          :y (double (or (:hit-y hit) (:y hit) 0.0))
                          :z (double (or (:hit-z hit) (:z hit) 0.0))}

                         nil)]
          ;; Context.sendToClient reaches the owner and linked nearby clients.
          (fx/send-local-and-nearby!
            ctx-id
            {:topic :arc-gen/fx-perform :mode :perform}
            nil
            {:start eye
             :end (or hit-pos miss-end)
             :hit-type hit-type
             :sound-pos sound-pos
             :source-player-id player-id})

          (if (= hit-type :entity)
            (let [entity-uuid (or (:entity-id hit) (:uuid hit))]
              (attack-target!
                player-id world-id entity-uuid damage)
              (try-charge-creeper!
                world-id entity-uuid)
              (skill-effects/add-skill-exp!
                player-id
                arc-gen-skill-id
                (cfg-progression
                  :progression.exp-entity exp*)))
            ;; Raytrace.perform returns a non-nil MISS result at full range.
            ;; ArcGen routes every non-entity result through its block branch,
            ;; so misses also gain block exp and perform the ignite roll.
            (let [impact  (or hit-pos miss-end)
                  block-x (int (or (:x hit) (:x miss-end)))
                  block-y (int (or (:y hit) (:y miss-end)))
                  block-z (int (or (:z hit) (:z miss-end)))]
              (if (and (block-manip/available?)
                       (= "minecraft:water"
                          (block-manip/get-block
                            world-id block-x block-y block-z)))
                (try-fishing!
                  world-id
                  (:x impact) (:y impact) (:z impact)
                  fish-prob
                  player)
                (try-ignite-block!
                  world-id
                  block-x block-y block-z
                  ignite-prob))
              (skill-effects/add-skill-exp!
                player-id
                arc-gen-skill-id
                (cfg-progression
                  :progression.exp-block exp*))))

          ;; The original awards exp first, then reads the updated exp for the
          ;; cooldown lerp and truncates the result.
          (skill-effects/set-main-cooldown!
            player-id
            arc-gen-skill-id
            (cooldown-ticks
              (skill-effects/skill-exp
                player-id arc-gen-skill-id))))))
    (catch Exception e
      (log/warn "Arc Gen perform! failed:" (ex-message e)))))

(defn arc-gen-perform!
  [& args]
  (apply perform-arc-gen! args))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill arc-gen
  :id              :arc-gen
  :category-id     :electromaster
  :name-key        "ability.skill.electromaster.arc_gen"
  :description-key "ability.skill.electromaster.arc_gen.desc"
  :icon            "textures/abilities/electromaster/skills/arc_gen.png"
  :ui-position     [24 46]
  :ctrl-id         :arc-gen
  :pattern         :instant
  :cooldown        {:mode :manual}
  :cost            {:down
                    {:cp
                     (fn [_player-id _skill-id exp]
                       (cfg-lerp :cost.down.cp exp))
                     :overload
                     (fn [_player-id _skill-id exp]
                       (cfg-lerp :cost.down.overload exp))}}
  :cooldown-ticks  (fn [_player-id _skill-id exp]
                     (cooldown-ticks exp))
  :actions         {:perform! arc-gen-perform!}
  :prerequisites   [])
