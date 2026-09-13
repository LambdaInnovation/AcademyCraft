(ns cn.li.mc262.runtime.world-effects-core
  "Shared Minecraft-side world effects helpers (no loader API imports).

  26.2 drifts handled here:
  - AbstractArrow / LargeFireball package moves
  - Registry.get → getValue; EntityType.create(Level, EntitySpawnReason)
  - Entity.saveWithoutId(CompoundTag) gone → WorldEntity LargeFireball accessors"
  (:require [cn.li.mcbase.runtime.adapter.world-effects :as world-adapter]
            [cn.li.mc262.runtime.registry :as registry])
  (:import [cn.li.mc262.entity ScriptedBlockBodyEntity ScriptedEffectEntity]
           [cn.li.mc262.runtime WorldEntity]
           [net.minecraft.core BlockPos]
           [net.minecraft.resources Identifier]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.sounds SoundSource SoundEvent]
           [net.minecraft.world.entity Entity EntitySpawnReason EntityType LivingEntity]
           [net.minecraft.world.entity.item ItemEntity]
           [net.minecraft.world.item Item ItemStack]
           [net.minecraft.world.entity.monster Monster]
           [net.minecraft.world.entity.projectile Projectile]
           [net.minecraft.world.entity.projectile.arrow AbstractArrow]
           [net.minecraft.world.entity.projectile.hurtingprojectile LargeFireball]
           [net.minecraft.world.level Level]
           [net.minecraft.world.phys AABB Vec3]))

(defn entity->map
  [^Entity entity resolve-entity-id-fn multipart-entity?-fn]
  (let [^Vec3 pos (.position entity)
        scripted? (instance? ScriptedEffectEntity entity)
        ^ScriptedEffectEntity scripted-entity (when scripted? entity)
        age-ticks (when scripted? (.getAgeTicks scripted-entity))
        motion-progress (when (and scripted? (.hasMotionProgress scripted-entity))
                          (.getMotionProgress scripted-entity))
        projectile? (instance? Projectile entity)
        owner (when projectile? (.getOwner ^Projectile entity))
        explosion-power (when (instance? LargeFireball entity)
                          (WorldEntity/getLargeFireballExplosionPower entity))]
    {:uuid (str (.getUUID entity))
     :x (.x pos)
     :y (.y pos)
     :z (.z pos)
     :width (double (.getBbWidth entity))
     :height (double (.getBbHeight entity))
     :eye-height (double (.getEyeHeight entity))
     :entity-id (when resolve-entity-id-fn (resolve-entity-id-fn entity))
     :type (str (.getDescriptionId (.getType entity)))
     :living? (instance? LivingEntity entity)
     :invulnerable-time (if (instance? LivingEntity entity)
                          (long (.-invulnerableTime ^LivingEntity entity))
                          0)
     :mob? (instance? Monster entity)
     :multipart? (boolean (and multipart-entity?-fn
                               (multipart-entity?-fn entity)))
     :item? (instance? ItemEntity entity)
     :projectile? projectile?
     :arrow? (instance? AbstractArrow entity)
     :vec-deviation-marked? (contains? (.entityTags entity) "ac_vm_deviated")
     :owner-uuid (when owner (str (.getUUID ^Entity owner)))
     :explosion-power explosion-power
     :age-ticks age-ticks
     :motion-progress motion-progress}))

(defn find-blocks-in-radius-in-level
  [^Level level x y z radius block-predicate block-id-fn]
  (let [r (int radius)
        deltas (range (- r) (inc r))]
    (persistent!
     (reduce
      (fn [results dx]
        (reduce
         (fn [results' dy]
           (reduce
            (fn [results'' dz]
              (let [bx (+ (int x) dx)
                    by (+ (int y) dy)
                    bz (+ (int z) dz)
                    dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))]
                (if (<= dist radius)
                  (let [pos (BlockPos. bx by bz)
                        block-state (.getBlockState level pos)
                        block (.getBlock block-state)
                        block-id (block-id-fn block block-state)]
                    (cond-> results''
                      (block-predicate block-id)
                      (conj! {:x bx :y by :z bz :block-id block-id})))
                  results'')))
            results'
            deltas))
         results
         deltas))
      (transient [])
      deltas))))

(def ^:private sound-source-map
  {:ambient SoundSource/AMBIENT
   :players SoundSource/PLAYERS
   :blocks SoundSource/BLOCKS
   :hostile SoundSource/HOSTILE
   :neutral SoundSource/NEUTRAL
   :music SoundSource/MUSIC
   :master SoundSource/MASTER
   :weather SoundSource/WEATHER
   :records SoundSource/RECORDS})

(defn resolve-sound-source
  ^SoundSource
  [source]
  (or (get sound-source-map source)
      SoundSource/AMBIENT))

(defn play-sound-in-level!
  [^Level level x y z sound-id source volume pitch]
  (let [^SoundEvent sound-event (.getValue (registry/builtin "SOUND_EVENT")
                                           (ResourceLocations/parse ^String sound-id))]
    (when sound-event
      (.playSound level
                  nil
                  (double x)
                  (double y)
                  (double z)
                  ^SoundEvent sound-event
                  ^SoundSource (resolve-sound-source source)
                  (float volume)
                  (float pitch))
      true)))

(defn entities-in-radius
  [^Level level x y z radius get-entities-fn resolve-entity-id-fn multipart-entity?-fn]
  (let [aabb (AABB. (- x radius) (- y radius) (- z radius)
                    (+ x radius) (+ y radius) (+ z radius))
        entities (get-entities-fn level aabb)]
    (mapv #(entity->map % resolve-entity-id-fn multipart-entity?-fn) entities)))

(defn entities-in-aabb
  [^Level level min-x min-y min-z max-x max-y max-z get-entities-fn resolve-entity-id-fn multipart-entity?-fn]
  (let [aabb (AABB. min-x min-y min-z max-x max-y max-z)
        entities (get-entities-fn level aabb)]
    (mapv #(entity->map % resolve-entity-id-fn multipart-entity?-fn) entities)))

(defn spawn-projectile-in-level!
  [^Level level projectile-spec resolve-entity-id-fn get-entity-by-uuid-fn]
  (let [{:keys [entity-id x y z vx vy vz owner-uuid explosion-power]} projectile-spec]
    (try
      (let [^Identifier type-key (ResourceLocations/parse (str entity-id))
            ^EntityType entity-type (if (.containsKey (registry/builtin "ENTITY_TYPE") type-key)
                                      (.getValue (registry/builtin "ENTITY_TYPE") type-key)
                                      (throw (ex-info (str "Unknown entity type id '" entity-id "'")
                                                      {:entity-id entity-id})))]
        (if-not entity-type
          {:success? false}
          (if-let [^Entity entity (.create entity-type level EntitySpawnReason/TRIGGERED)]
            (do
              (when (and (instance? LargeFireball entity)
                         (number? explosion-power))
                (WorldEntity/setLargeFireballExplosionPower entity (int explosion-power)))
              (.snapTo entity
                       (double (or x 0.0))
                       (double (or y 0.0))
                       (double (or z 0.0))
                       (.getYRot entity)
                       (.getXRot entity))
              (when (instance? Projectile entity)
                (when-let [owner (when (and owner-uuid get-entity-by-uuid-fn)
                                   (get-entity-by-uuid-fn level owner-uuid))]
                  (.setOwner ^Projectile entity owner)))
              (.setDeltaMovement entity
                                 (double (or vx 0.0))
                                 (double (or vy 0.0))
                                 (double (or vz 0.0)))
              (if (.addFreshEntity level entity)
                {:success? true
                 :uuid (str (.getUUID entity))
                 :entity-id (or (when resolve-entity-id-fn (resolve-entity-id-fn entity))
                                (str entity-id))}
                {:success? false}))
            {:success? false})))
      (catch Exception _
        {:success? false}))))

(defn trigger-behavior-hit-in-level!
  [^Level level entity-uuid get-entity-by-uuid-fn]
  (when-let [entity (get-entity-by-uuid-fn level entity-uuid)]
    (when (instance? ScriptedBlockBodyEntity entity)
      (.forceBehaviorHit ^ScriptedBlockBodyEntity entity)
      true)))

(defn spawn-item-stack-at!
  [^Entity player world-id x y z item-stack]
  (try
    (when-let [^Level level (some-> player .level)]
      (let [item-id (:id item-stack)
            count (int (max 1 (or (:count item-stack) 1)))
            ^Item item (.getValue (registry/builtin "ITEM") (ResourceLocations/parse (str item-id)))
            ^ItemStack mc-stack (ItemStack. item count)]
        (let [^ItemEntity entity (ItemEntity. level (double x) (double y) (double z) mc-stack)]
          (.setPickUpDelay entity 10)
          (.addFreshEntity level entity)
          true)))
    (catch Exception _
      false)))

(world-adapter/install-world-core!
  {:spawn-projectile-in-level! spawn-projectile-in-level!
   :entities-in-radius entities-in-radius
   :entities-in-aabb entities-in-aabb
   :find-blocks-in-radius-in-level find-blocks-in-radius-in-level
   :play-sound-in-level! play-sound-in-level!
   :trigger-behavior-hit-in-level! trigger-behavior-hit-in-level!})
