(ns cn.li.ac.ability.config
  "Ability system configuration descriptors and typed getters.

  AC owns the domain defaults and business-facing accessors. Platform projects
  only provide file storage (Forge TOML / Fabric JSON) through mcmod's config
  registry, matching the wireless configuration pattern.

  NO net.minecraft.* imports - this namespace is platform-neutral."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.config-common :as ability-config-common]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.util.log :as log]))

(def expected-level-count 5)

(defn max-level
  "Internal structural maximum ability level.

  This is intentionally not a player config descriptor: level arrays, skill
  levels, progression data, and UI all assume the same structural count."
  []
  expected-level-count)

(def default-values
  {:attack-player true
   :destroy-blocks true
   :worlds-whitelisted-destroying-blocks []

   ;; Defaults intentionally mirror the currently active MagMovement hardcoded
   ;; sets so extracting config does not silently change targeting behavior.
   :normal-metal-blocks
   ["minecraft:activator_rail"
    "minecraft:detector_rail"
    "minecraft:iron_bars"
    "minecraft:iron_block"
    "minecraft:piston"
    "minecraft:powered_rail"
    "minecraft:rail"
    "minecraft:sticky_piston"]
   :weak-metal-blocks
   ["minecraft:dispenser"
    "minecraft:hopper"
    "minecraft:iron_ore"]
   :metal-entities
   ["minecraft:chest_minecart"
    "minecraft:command_block_minecart"
    "minecraft:furnace_minecart"
    "minecraft:hopper_minecart"
    "minecraft:iron_golem"
    "minecraft:minecart"
    "minecraft:spawner_minecart"
    "minecraft:tnt_minecart"
    (modid/namespaced-path "entity_mag_hook")]

   :init-cp [1800.0 2800.0 4000.0 5800.0 8000.0]
   :add-cp [900.0 1000.0 1500.0 1700.0 12000.0]
   :init-overload [100.0 150.0 240.0 350.0 500.0]
   :add-overload [40.0 70.0 80.0 100.0 500.0]
   :cp-recover-cooldown 15
   :cp-recover-speed 1.0
   :overload-recover-cooldown 32
   :overload-recover-speed 1.0
   :maxcp-incr-rate 0.0025
   :maxo-incr-rate 0.0058
  :cp-recovery-rate-base 0.0003
  :cp-recovery-lerp-start 1.0
  :cp-recovery-lerp-end 2.0
  :overload-recovery-min-rate 0.002
  :overload-recovery-active-rate 0.007
  :overload-recovery-lerp-start 1.0
  :overload-recovery-lerp-end 0.5
  :overload-recovery-ratio-divisor 2.0
  :max-overload-growth-per-event 10.0
   :damage-scale 1.0
  :reflected-damage-multiplier 0.5
  :reflection-search-radius 10.0
   :runtime-cp-consume-per-tick 1.0
   :runtime-overload-per-tick 0.6
  :prog-incr-rate 1.0
  :level-threshold-skill-count-multiplier 1.333
  :level-threshold-all-mastered-discount 0.5
  :skill-learning-cost-base 3.0
  :skill-learning-cost-level-square-factor 0.5
  :level-up-stim-base 5})

(def descriptors
  [{:key :attack-player
    :path "general.attack-player"
    :section :general
    :type :boolean
    :default (:attack-player default-values)
    :comment "Whether ability skills may damage players."}
   {:key :destroy-blocks
    :path "general.destroy-blocks"
    :section :general
    :type :boolean
    :default (:destroy-blocks default-values)
    :comment "Whether ability skills may destroy blocks by default."}
   {:key :worlds-whitelisted-destroying-blocks
    :path "general.worlds-whitelisted-destroying-blocks"
    :section :general
    :type :string-list
    :default (:worlds-whitelisted-destroying-blocks default-values)
    :comment "Dimensions where skills may still destroy blocks when the global toggle is off (dimension names like minecraft:overworld, or numeric ids 0/-1/1)."}

   {:key :normal-metal-blocks
    :path "targeting.metal.normal-metal-blocks"
    :section :targeting.metal
    :type :string-list
    :default (:normal-metal-blocks default-values)
    :comment "Block IDs treated as normal metal targets by magnetic abilities."}
   {:key :weak-metal-blocks
    :path "targeting.metal.weak-metal-blocks"
    :section :targeting.metal
    :type :string-list
    :default (:weak-metal-blocks default-values)
    :comment "Block IDs treated as weak metal targets by magnetic abilities."}
   {:key :metal-entities
    :path "targeting.metal.metal-entities"
    :section :targeting.metal
    :type :string-list
    :default (:metal-entities default-values)
    :comment "Entity IDs treated as metal targets by magnetic abilities."}

   {:key :init-cp
    :path "resource.init-cp"
    :section :resource
    :type :double-list
    :min 0.0
    :default (:init-cp default-values)
    :comment "Initial max CP for ability levels 1-5."}
   {:key :add-cp
    :path "resource.add-cp"
    :section :resource
    :type :double-list
    :min 0.0
    :default (:add-cp default-values)
    :comment "Maximum CP growth ceiling for ability levels 1-5."}
   {:key :init-overload
    :path "resource.init-overload"
    :section :resource
    :type :double-list
    :min 0.0
    :default (:init-overload default-values)
    :comment "Initial max overload for ability levels 1-5."}
   {:key :add-overload
    :path "resource.add-overload"
    :section :resource
    :type :double-list
    :min 0.0
    :default (:add-overload default-values)
    :comment "Maximum overload growth ceiling for ability levels 1-5."}
   {:key :cp-recover-cooldown
    :path "resource.cp-recover-cooldown"
    :section :resource
    :type :int
    :min 0
    :default (:cp-recover-cooldown default-values)
    :comment "Ticks to wait after CP use before recovery starts."}
   {:key :cp-recover-speed
    :path "resource.cp-recover-speed"
    :section :resource
    :type :double
    :min 0.0
    :default (:cp-recover-speed default-values)
    :comment "CP recovery speed multiplier."}
   {:key :overload-recover-cooldown
    :path "resource.overload-recover-cooldown"
    :section :resource
    :type :int
    :min 0
    :default (:overload-recover-cooldown default-values)
    :comment "Ticks to wait after overload use before recovery starts."}
   {:key :overload-recover-speed
    :path "resource.overload-recover-speed"
    :section :resource
    :type :double
    :min 0.0
    :default (:overload-recover-speed default-values)
    :comment "Overload recovery speed multiplier."}
   {:key :maxcp-incr-rate
    :path "resource.maxcp-incr-rate"
    :section :resource
    :type :double
    :min 0.0
    :default (:maxcp-incr-rate default-values)
    :comment "Fraction of consumed CP added to max CP growth."}
   {:key :maxo-incr-rate
    :path "resource.maxo-incr-rate"
    :section :resource
    :type :double
    :min 0.0
    :default (:maxo-incr-rate default-values)
    :comment "Fraction of overload added to max overload growth."}
  {:key :cp-recovery-rate-base
   :path "resource.recovery.cp-rate-base"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:cp-recovery-rate-base default-values)
   :comment "Base CP recovery coefficient used by the per-tick recovery formula."}
  {:key :cp-recovery-lerp-start
   :path "resource.recovery.cp-lerp-start"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:cp-recovery-lerp-start default-values)
   :comment "CP recovery multiplier when current CP ratio is 0."}
  {:key :cp-recovery-lerp-end
   :path "resource.recovery.cp-lerp-end"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:cp-recovery-lerp-end default-values)
   :comment "CP recovery multiplier when current CP ratio is 1."}
  {:key :overload-recovery-min-rate
   :path "resource.recovery.overload-min-rate"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:overload-recovery-min-rate default-values)
   :comment "Minimum overload recovery coefficient multiplied by max overload."}
  {:key :overload-recovery-active-rate
   :path "resource.recovery.overload-active-rate"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:overload-recovery-active-rate default-values)
   :comment "Active overload recovery coefficient multiplied by max overload and lerp factor."}
  {:key :overload-recovery-lerp-start
   :path "resource.recovery.overload-lerp-start"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:overload-recovery-lerp-start default-values)
   :comment "Overload recovery lerp multiplier at low overload ratio."}
  {:key :overload-recovery-lerp-end
   :path "resource.recovery.overload-lerp-end"
   :section :resource.recovery
   :type :double
   :min 0.0
   :default (:overload-recovery-lerp-end default-values)
   :comment "Overload recovery lerp multiplier at high overload ratio."}
  {:key :overload-recovery-ratio-divisor
   :path "resource.recovery.overload-ratio-divisor"
   :section :resource.recovery
   :type :double
   :min 0.000001
   :default (:overload-recovery-ratio-divisor default-values)
   :comment "Divisor applied to current/max overload ratio in the recovery curve."}
  {:key :max-overload-growth-per-event
   :path "resource.growth.max-overload-growth-per-event"
   :section :resource.growth
   :type :double
   :min 0.0
   :default (:max-overload-growth-per-event default-values)
   :comment "Maximum max-overload growth gained from one overload growth update."}

   {:key :damage-scale
    :path "combat.damage-scale"
    :section :combat
    :type :double
    :min 0.0
    :default (:damage-scale default-values)
    :comment "Global ability damage multiplier."}
    {:key :reflected-damage-multiplier
     :path "combat.reflected-damage-multiplier"
     :section :combat
     :type :double
     :min 0.0
     :default (:reflected-damage-multiplier default-values)
     :comment "Multiplier applied each time reflected damage jumps to the next target."}
    {:key :reflection-search-radius
     :path "combat.reflection-search-radius"
     :section :combat
     :type :double
     :min 0.0
     :default (:reflection-search-radius default-values)
     :comment "Search radius in blocks for selecting the next reflected damage target."}

   {:key :runtime-cp-consume-per-tick
    :path "runtime.cp-consume-per-tick"
    :section :runtime
    :type :double
    :min 0.0
    :default (:runtime-cp-consume-per-tick default-values)
    :comment "Default CP cost for runtime-speed ticking skills."}
   {:key :runtime-overload-per-tick
    :path "runtime.overload-per-tick"
    :section :runtime
    :type :double
    :min 0.0
    :default (:runtime-overload-per-tick default-values)
    :comment "Default overload cost for runtime-speed ticking skills."}

   {:key :prog-incr-rate
    :path "progression.prog-incr-rate"
    :section :progression
    :type :double
    :min 0.0
    :default (:prog-incr-rate default-values)
    :comment "Global multiplier applied to ability experience and level progress."}
     {:key :level-threshold-skill-count-multiplier
    :path "progression.level-threshold.skill-count-multiplier"
    :section :progression.level-threshold
    :type :double
    :min 0.0
    :default (:level-threshold-skill-count-multiplier default-values)
    :comment "Multiplier applied to controllable skill count when computing level-up EXP threshold."}
     {:key :level-threshold-all-mastered-discount
    :path "progression.level-threshold.all-mastered-discount"
    :section :progression.level-threshold
    :type :double
    :min 0.0
    :default (:level-threshold-all-mastered-discount default-values)
    :comment "Multiplier applied to level-up threshold when all current skills are mastered."}
     {:key :skill-learning-cost-base
    :path "progression.learning.skill-cost-base"
    :section :progression.learning
    :type :double
    :min 0.0
    :default (:skill-learning-cost-base default-values)
    :comment "Base Developer stim cost for learning a skill."}
     {:key :skill-learning-cost-level-square-factor
    :path "progression.learning.skill-cost-level-square-factor"
    :section :progression.learning
    :type :double
    :min 0.0
    :default (:skill-learning-cost-level-square-factor default-values)
    :comment "Multiplier for level^2 in the skill learning stim cost formula."}
     {:key :level-up-stim-base
    :path "progression.learning.level-up-stim-base"
    :section :progression.learning
    :type :int
    :min 1
    :default (:level-up-stim-base default-values)
    :comment "Base Developer stim count multiplied by next level when leveling up."}])

(defn- raw-value
  [k]
  (get (config-common/ability-config) k (get default-values k)))

(defn- non-negative-double
  [k]
  (let [default (double (get default-values k))
        ^double value (ability-config-common/finite-double (raw-value k) default)]
    (if (neg? value) default value)))

(defn- positive-double
  [k]
  (let [default (double (get default-values k))
        ^double value (ability-config-common/finite-double (raw-value k) default)]
    (if (pos? value) value default)))

(defn- non-negative-int
  [k]
  (let [value (double (non-negative-double k))]
    (max 0 (int (Math/round value)))))

(defn- positive-int
  [k]
  (let [value (double (positive-double k))]
    (max 1 (int (Math/round value)))))

(defn- boolean-value
  [k]
  (ability-config-common/boolean-value (raw-value k) (get default-values k)))

(defn- string-list
  [k]
  (let [v (raw-value k)
        fallback (vec (get default-values k))]
    (if (ability-config-common/list-like? v)
      (->> v
           (keep #(let [s (some-> % str str/trim)]
                    (when-not (str/blank? s) s)))
           vec)
      fallback)))

(defn normalized-level-list
  "Return a validated level-indexed numeric list for levels 1-5. Invalid
  length or invalid entries fall back to the descriptor default."
  [k]
  (let [fallback (vec (get default-values k))
        raw (raw-value k)]
    (if (and (ability-config-common/list-like? raw)
             (= expected-level-count (count raw)))
      (mapv (fn [value default]
          (let [d (ability-config-common/finite-double value default)]
                (if (neg? d) (double default) d)))
            raw
            fallback)
      fallback)))

(defn level-value
  "Read a 1-based ability level value from a normalized level list."
  [values level]
  (let [idx (if (number? level) (dec (int level)) -1)]
    (if (<= 0 idx (dec expected-level-count))
      (double (nth values idx))
      0.0)))

(defn init-config!
  "Ensure ability defaults are present in the shared config registry."
  []
  (config-reg/register-config-descriptors! config-common/ability-domain descriptors)
  (config-reg/ensure-default-values! config-common/ability-domain default-values)
  (log/debug "Initialized ability config descriptors" {:domain config-common/ability-domain})
  nil)

(defn attack-player-enabled? []
  (boolean-value :attack-player))

(defn destroy-blocks-enabled? []
  (boolean-value :destroy-blocks))

(defn whitelisted-destroying-worlds
  "Dimensions exempt from the global destroy toggle (upstream
   worldsWhitelistedDestroyingBlocks). Entries may be dimension resource paths
   (minecraft:overworld) or numeric ids (0/-1/1)."
  []
  (string-list :worlds-whitelisted-destroying-blocks))

(defn get-normal-metal-blocks []
  (string-list :normal-metal-blocks))

(defn get-weak-metal-blocks []
  (string-list :weak-metal-blocks))

(defn get-metal-entities []
  (string-list :metal-entities))

(defn- list-predicate
  [values-fn]
  (fn [id]
    (contains? (set (map str (values-fn))) (str id))))

(defn is-normal-metal-block? [block-id]
  ((list-predicate get-normal-metal-blocks) block-id))

(defn is-weak-metal-block? [block-id]
  ((list-predicate get-weak-metal-blocks) block-id))

(defn is-metal-block? [block-id]
  (or (is-normal-metal-block? block-id)
      (is-weak-metal-block? block-id)))

(defn is-metal-entity? [entity-id]
  ((list-predicate get-metal-entities) entity-id))

(defn get-init-cp [level]
  (level-value (normalized-level-list :init-cp) level))

(defn get-add-cp [level]
  (level-value (normalized-level-list :add-cp) level))

(defn get-init-overload [level]
  (level-value (normalized-level-list :init-overload) level))

(defn get-add-overload [level]
  (level-value (normalized-level-list :add-overload) level))

(defn add-cp-ceiling [level]
  (get-add-cp level))

(defn add-overload-ceiling [level]
  (get-add-overload level))

;; Upstream CPData keeps the level grant and the earned growth apart:
;; recalcMaxValue sets maxCP = getInitCP(level), and getMaxCP() returns that
;; plus addMaxCP, which starts at 0 and accumulates through skill use up to
;; add-cp-ceiling. These functions are the level grant only — the growth
;; allowance belongs to :add-max-cp / :add-max-overload, and adding it here
;; too would hand out at level 1 a maximum the player is supposed to earn.

(defn base-max-cp-for-level
  "Max CP granted by `level` alone, before any growth from skill use."
  [level]
  (when-not (and (>= level 1) (<= level (max-level)))
    (throw (IllegalArgumentException. "base-max-cp-for-level: level must be 1 to max-level")))
  (get-init-cp level))

(defn base-max-overload-for-level
  "Max overload granted by `level` alone, before any growth from skill use."
  [level]
  (when-not (and (>= level 1) (<= level (max-level)))
    (throw (IllegalArgumentException. "base-max-overload-for-level: level must be 1 to max-level")))
  (get-init-overload level))

(defn cp-recover-cooldown []
  (non-negative-int :cp-recover-cooldown))

(defn cp-recover-speed []
  (positive-double :cp-recover-speed))

(defn overload-recover-cooldown []
  (non-negative-int :overload-recover-cooldown))

(defn overload-recover-speed []
  (positive-double :overload-recover-speed))

(defn maxcp-incr-rate []
  (non-negative-double :maxcp-incr-rate))

(defn maxo-incr-rate []
  (non-negative-double :maxo-incr-rate))

(defn cp-recovery-rate-base []
  (non-negative-double :cp-recovery-rate-base))

(defn cp-recovery-lerp-start []
  (non-negative-double :cp-recovery-lerp-start))

(defn cp-recovery-lerp-end []
  (non-negative-double :cp-recovery-lerp-end))

(defn overload-recovery-min-rate []
  (non-negative-double :overload-recovery-min-rate))

(defn overload-recovery-active-rate []
  (non-negative-double :overload-recovery-active-rate))

(defn overload-recovery-lerp-start []
  (non-negative-double :overload-recovery-lerp-start))

(defn overload-recovery-lerp-end []
  (non-negative-double :overload-recovery-lerp-end))

(defn overload-recovery-ratio-divisor []
  (positive-double :overload-recovery-ratio-divisor))

(defn max-overload-growth-per-event []
  (non-negative-double :max-overload-growth-per-event))

(defn damage-scale []
  (positive-double :damage-scale))

(defn reflected-damage-multiplier []
  (non-negative-double :reflected-damage-multiplier))

(defn reflection-search-radius []
  (non-negative-double :reflection-search-radius))

(defn runtime-cp-consume-per-tick []
  (non-negative-double :runtime-cp-consume-per-tick))

(defn runtime-overload-per-tick []
  (non-negative-double :runtime-overload-per-tick))

(defn prog-incr-rate []
  (positive-double :prog-incr-rate))

(defn level-threshold-skill-count-multiplier []
  (positive-double :level-threshold-skill-count-multiplier))

(defn level-threshold-all-mastered-discount []
  (non-negative-double :level-threshold-all-mastered-discount))

(defn skill-learning-cost-base []
  (non-negative-double :skill-learning-cost-base))

(defn skill-learning-cost-level-square-factor []
  (non-negative-double :skill-learning-cost-level-square-factor))

(defn level-up-stim-base []
  (positive-int :level-up-stim-base))

(defn- collect-config-errors
  []
  (let [level-list-errors
        (for [k [:init-cp :add-cp :init-overload :add-overload]
              :let [values (raw-value k)]
              :when (not (and (ability-config-common/list-like? values)
                              (= expected-level-count (count values))
                              (every? #(and (number? %) (not (neg? (double %)))) values)))]
          (str (name k) " must be a non-negative numeric list with 5 elements"))
        string-list-errors
        (for [k [:normal-metal-blocks :weak-metal-blocks :metal-entities]
              :let [values (raw-value k)]
              :when (not (and (ability-config-common/list-like? values) (every? string? values)))]
          (str (name k) " must be a string list"))
        positive-errors
        (for [k [:cp-recover-speed :overload-recover-speed :damage-scale :prog-incr-rate
                 :overload-recovery-ratio-divisor :level-threshold-skill-count-multiplier
                 :level-up-stim-base]
              :let [value (ability-config-common/finite-double (raw-value k) 0.0)]
              :when (not (pos? value))]
          (str (name k) " must be positive"))
        non-negative-errors
        (for [k [:cp-recover-cooldown :overload-recover-cooldown
                 :maxcp-incr-rate :maxo-incr-rate
                 :runtime-cp-consume-per-tick :runtime-overload-per-tick
                 :cp-recovery-rate-base :cp-recovery-lerp-start :cp-recovery-lerp-end
                 :overload-recovery-min-rate :overload-recovery-active-rate
                 :overload-recovery-lerp-start :overload-recovery-lerp-end
                 :max-overload-growth-per-event :reflected-damage-multiplier
                 :reflection-search-radius :level-threshold-all-mastered-discount
                 :skill-learning-cost-base :skill-learning-cost-level-square-factor]
              :let [value (ability-config-common/finite-double (raw-value k) -1.0)]
              :when (neg? value)]
          (str (name k) " must be non-negative"))]
    (vec (concat level-list-errors
                 string-list-errors
                 positive-errors
                 non-negative-errors))))

(defn validate-config!
  "Validate currently effective ability configuration values. Getters remain
  defensive at runtime; this function is for diagnostics/tests."
  []
  (let [errors (collect-config-errors)]
    (if (empty? errors)
      (do
        (log/debug "Ability configuration validation passed")
        nil)
      (do
        (log/error "Ability configuration validation failed:" errors)
        (throw (ex-info "Invalid ability configuration" {:errors errors}))))))
