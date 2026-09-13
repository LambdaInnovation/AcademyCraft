(ns cn.li.mcbase.runtime.potion-effects-core
  "Loader-agnostic potion/mob-effect helpers via cn.li.mcver.Effects."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.platform.registry.metadata :as registry-metadata]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.core Holder]
           [cn.li.mcver Effects ResourceLocations]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.world.entity LivingEntity]))

(def ^:private vanilla-effect-keys
  {:speed               "minecraft:speed"
   :slowness            "minecraft:slowness"
   :haste               "minecraft:haste"
   :mining-fatigue      "minecraft:mining_fatigue"
   :strength            "minecraft:strength"
   :jump-boost          "minecraft:jump_boost"
   :nausea              "minecraft:nausea"
   :regeneration        "minecraft:regeneration"
   :resistance          "minecraft:resistance"
   :fire-resistance     "minecraft:fire_resistance"
   :water-breathing     "minecraft:water_breathing"
   :invisibility        "minecraft:invisibility"
   :blindness           "minecraft:blindness"
   :night-vision        "minecraft:night_vision"
   :hunger              "minecraft:hunger"
   :weakness            "minecraft:weakness"
   :poison              "minecraft:poison"
   :wither              "minecraft:wither"
   :health-boost        "minecraft:health_boost"
   :absorption          "minecraft:absorption"
   :saturation          "minecraft:saturation"
   :glowing             "minecraft:glowing"
   :levitation          "minecraft:levitation"
   :luck                "minecraft:luck"
   :unluck              "minecraft:unluck"
   :slow-falling        "minecraft:slow_falling"
   :conduit-power       "minecraft:conduit_power"
   :dolphins-grace      "minecraft:dolphins_grace"
   :bad-omen            "minecraft:bad_omen"
   :hero-of-the-village "minecraft:hero_of_the_village"
   :darkness            "minecraft:darkness"})

(defn- get-mob-effect
  "Return a Holder<MobEffect> for effect-type, or nil."
  ^Holder [effect-type]
  (let [vanilla-key (get vanilla-effect-keys effect-type)]
    (if vanilla-key
      (Effects/holderOf (ResourceLocations/parse vanilla-key))
      (let [effect-id (name effect-type)
            known-custom? (some #(= effect-id (str %)) (registry-metadata/get-all-effect-ids))]
        (if-not known-custom?
          (do (log/warn "Unknown potion effect type:" effect-type) nil)
          (let [registry-name (registry-metadata/get-effect-registry-name effect-id)
                rl (ResourceLocations/of modid/mod-id registry-name)
                effect (Effects/holderOf rl)]
            (when-not effect
              (log/warn "Custom effect not found in MOB_EFFECT registry:" rl))
            effect))))))

(defn server-levels
  "Seam over MinecraftServer.getAllLevels so resolve-living-target is testable
  without a live server."
  [^MinecraftServer server]
  (when server (.getAllLevels server)))

(defn resolve-living-target
  "Resolve a UUID to any living entity on the server, not just a player.

  These helpers only ever looked players up, so every skill that applies an
  effect to a MOB silently did nothing — thunder bolt's slowness never landed,
  taking both the slow and the potion tint vanilla renders on an affected mob
  with it. Effects/addEffect has always accepted a LivingEntity."
  ^LivingEntity [^MinecraftServer server target-uuid]
  (or (query-core/get-player-by-uuid server target-uuid)
      (some (fn [level]
              (let [entity (query-core/get-entity-by-uuid level target-uuid)]
                (when (instance? LivingEntity entity) entity)))
            (server-levels server))))

(defn apply-potion-effect!
  [^MinecraftServer server player-uuid effect-type duration amplifier]
  (try
    (when-let [^LivingEntity target (resolve-living-target server player-uuid)]
      (when-let [^Holder mob-effect (get-mob-effect effect-type)]
        (Effects/addEffect target mob-effect (int duration) (int amplifier))
        true))
    (catch Exception e
      (log/warn "Failed to apply potion effect:" (ex-message e))
      false)))

(defn remove-potion-effect!
  [^MinecraftServer server player-uuid effect-type]
  (try
    (when-let [^LivingEntity target (resolve-living-target server player-uuid)]
      (when-let [^Holder mob-effect (get-mob-effect effect-type)]
        (Effects/removeEffect target mob-effect)
        true))
    (catch Exception e
      (log/warn "Failed to remove potion effect:" (ex-message e))
      false)))

(defn has-potion-effect?
  [^MinecraftServer server player-uuid effect-type]
  (try
    (boolean
      (when-let [^LivingEntity target (resolve-living-target server player-uuid)]
        (when-let [^Holder mob-effect (get-mob-effect effect-type)]
          (Effects/hasEffect target mob-effect))))
    (catch Exception e
      (log/warn "Failed to check potion effect:" (ex-message e))
      false)))

(defn clear-all-effects!
  [^MinecraftServer server player-uuid]
  (try
    (when-let [^LivingEntity target (resolve-living-target server player-uuid)]
      (.removeAllEffects target)
      true)
    (catch Exception e
      (log/warn "Failed to clear all effects:" (ex-message e))
      false)))

(defn create-potion-effects
  "Create an IPotionEffects adapter using a platform-provided server supplier."
  [get-server]
  {:apply-potion-effect! (fn [player-uuid effect-type duration amplifier]
                           (apply-potion-effect! (get-server) player-uuid effect-type duration amplifier))
   :remove-potion-effect! (fn [player-uuid effect-type]
                            (remove-potion-effect! (get-server) player-uuid effect-type))
   :has-potion-effect? (fn [player-uuid effect-type]
                         (has-potion-effect? (get-server) player-uuid effect-type))
   :clear-all-effects! (fn [player-uuid]
                         (clear-all-effects! (get-server) player-uuid))})
