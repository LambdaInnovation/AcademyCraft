(ns cn.li.mcbase.runtime.entity-damage-core
  "Loader-agnostic helpers for entity damage runtime flows.

  Keeps generic shaping/calculation logic in shared Minecraft glue while
  platform layers own world/entity lookup and concrete damage application calls."
  (:require [cn.li.platform.neutral.hooks :as power-runtime])
  (:import [cn.li.mcbase.runtime DamageSourceAccess]
           [net.minecraft.world.entity LivingEntity]
           [net.minecraft.world.level Level]
           [net.minecraft.world.phys Vec3]))

(defn resolve-damage-source
  "Resolve level.damageSources().<kind>() via shared Java accessor."
  ([^Level level source-type]
   (when level
     (DamageSourceAccess/resolveKeyword level source-type)))
  ([^Level level source-type attacker]
   (when level
     (DamageSourceAccess/resolveKeyword level source-type attacker))))

(defn entity-pos-map
  [^LivingEntity entity]
  (let [^Vec3 pos (.position entity)]
    {:x (.x pos)
     :y (.y pos)
     :z (.z pos)}))

(defn candidate-map
  [^LivingEntity entity]
  (let [pos (entity-pos-map entity)]
    {:entity-uuid (str (.getUUID entity))
     :x (:x pos)
     :y (:y pos)
     :z (:z pos)}))

(defn compute-aoe-damage
  [origin-pos target-pos radius damage falloff?]
  (power-runtime/compute-aoe-damage origin-pos target-pos radius damage falloff?))

(defn- aoe-hit-step
  "Single AOE hit step: compute damage and apply if > 0.  All context passed
  explicitly so this fn stays statically compilable."
  [origin-pos radius damage falloff? apply-damage! hit-uuids ^LivingEntity entity]
  (let [target-pos (entity-pos-map entity)
        actual-damage (compute-aoe-damage origin-pos target-pos radius damage falloff?)]
    (if (> actual-damage 0.0)
      (do
        (apply-damage! entity actual-damage)
        (conj hit-uuids (str (.getUUID entity))))
      hit-uuids)))

(defn apply-aoe-damage-flow!
  "Apply AOE damage over an entity sequence and return hit entity UUID strings.

  `apply-damage!` receives `[entity actual-damage]` and should perform concrete
  platform-side hurt calls."
  [entities origin-pos radius damage falloff? apply-damage!]
  (reduce (partial aoe-hit-step origin-pos radius damage falloff? apply-damage!)
          []
          entities))

(defn select-reflection-target-uuid
  [current-entity-uuid current-pos candidates max-radius]
  (power-runtime/select-reflection-target
    current-entity-uuid
    current-pos
    candidates
    max-radius))

(defn compute-reflected-damage
  [current-damage]
  (power-runtime/compute-reflected-damage current-damage))

(defn reflection-search-radius
  []
  (double (power-runtime/get-reflection-search-radius)))

(defn apply-reflection-damage-flow!
  "Apply reflection chain damage and return hit entity UUID strings.

  `find-next-entity` receives `[current-entity search-radius]` and returns the
  next LivingEntity in chain or nil.
  `apply-damage!` receives `[entity damage]` and performs platform hurt call."
  [^LivingEntity start-entity
   initial-damage
   reflection-count
   max-reflections
   search-radius
   find-next-entity
   apply-damage!]
  (let [hit-entities (transient [(str (.getUUID start-entity))])]
    (apply-damage! start-entity initial-damage)
    (loop [^LivingEntity current-entity start-entity
           current-damage initial-damage
           reflection-num reflection-count]
      (when (< reflection-num max-reflections)
        (when-let [^LivingEntity next-entity (find-next-entity current-entity search-radius)]
          (let [reflected-damage (compute-reflected-damage current-damage)]
            (apply-damage! next-entity reflected-damage)
            (conj! hit-entities (str (.getUUID next-entity)))
            (recur next-entity reflected-damage (inc reflection-num))))))
    (persistent! hit-entities)))
