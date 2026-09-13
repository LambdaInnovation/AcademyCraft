(ns cn.li.mcbase.client.effects.particle
  "CLIENT-ONLY shared particle effect bridge."
  (:require [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.util.log :as log]
            [clojure.string :as str])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.client.multiplayer ClientLevel]
           [net.minecraft.core BlockPos]
           [net.minecraft.core.particles BlockParticleOption ParticleTypes ParticleType]
           [cn.li.mcver RegistryValues ResourceLocations]
           [net.minecraft.world.level.block.state BlockState]))

(defn- ->resource-location
  [particle-type]
  (cond
    (keyword? particle-type)
    (if-let [ns (namespace particle-type)]
      (ResourceLocations/of ns (name particle-type))
      (ResourceLocations/of "minecraft" (name particle-type)))

    (string? particle-type)
    (if (str/includes? particle-type ":")
      (ResourceLocations/parse ^String particle-type)
      (ResourceLocations/of "minecraft" ^String particle-type))

    :else nil))

(defn- resolve-particle
  [^ClientLevel level particle-cmd]
  (let [{:keys [particle-type block-id x y z]} particle-cmd]
    (if (= :block-crack particle-type)
      (let [^BlockState state (or (when block-id
                                     (some-> (RegistryValues/getBlock (ResourceLocations/parse ^String block-id))
                                             (.defaultBlockState)))
                                   (.getBlockState level (BlockPos. (int (Math/floor (double x)))
                                                                    (int (Math/floor (double y)))
                                                                    (int (Math/floor (double z))))))]
        (if (.isAir state)
          ParticleTypes/SMOKE
          (BlockParticleOption. ParticleTypes/BLOCK state)))
      (let [ptype ^ParticleType (some-> particle-type ->resource-location (RegistryValues/getParticleType))]
        (or ptype ParticleTypes/SMOKE)))))

(defn spawn-particle-effect!
  [particle-cmd]
  (try
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [^ClientLevel level (.level mc)]
        (let [{:keys [x y z count speed offset-x offset-y offset-z]} particle-cmd
              particle (resolve-particle level particle-cmd)]
          (.addParticle level particle
                        x y z
                        (* offset-x speed)
                        (* offset-y speed)
                        (* offset-z speed))
          (dotimes [_ (dec count)]
            (let [dx (- (rand offset-x) (/ offset-x 2))
                  dy (- (rand offset-y) (/ offset-y 2))
                  dz (- (rand offset-z) (/ offset-z 2))]
              (.addParticle level particle
                            (+ x dx) (+ y dy) (+ z dz)
                            (* dx speed)
                            (* dy speed)
                            (* dz speed)))))))
    (catch Exception e
      (log/stacktrace "Error spawning particle effect" e))))

(defn tick-particles!
  []
  (try
    (when-let [owner (client-session/current-local-player-owner)]
      (doseq [particle-cmd (power-runtime/client-poll-particle-effects owner)]
        (spawn-particle-effect! particle-cmd)))
    (catch Exception e
      (log/debug "Error in particle tick:" (ex-message e)))))

(defn init!
  []
  (log/debug "Shared particle effect bridge initialized"))
