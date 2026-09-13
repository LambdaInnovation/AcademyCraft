(ns cn.li.mcbase.runtime.adapter.entity-damage
  "Shared IEntityDamage adapter factory.

  Platform namespaces provide only server lookup and platform-specific entity
  query callbacks; this namespace owns the damage-flow orchestration."
  (:require [cn.li.mcbase.runtime.entity-damage-core :as core]
            [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcbase.runtime.multipart-entity :as multipart]
            [cn.li.mcmod.platform.entity-damage :as damage-effects]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcbase.runtime DamageSourceAccess]
           [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.damagesource DamageSource]
           [net.minecraft.world.entity Entity LivingEntity]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.phys AABB]))

(defn- resolve-level* [server resolve-level-fn world-id]
  (when server
    (resolve-level-fn server world-id)))

(defn- make-aabb [x y z radius]
  (AABB. (- x radius) (- y radius) (- z radius)
         (+ x radius) (+ y radius) (+ z radius)))

(defn- apply-vanilla-hurt!
  [^Entity entity dmg-source damage]
  (.hurt entity dmg-source (float damage)))

(defn- pvp-blocked?
  "True when `entity` is a player and the content-registered PvP gate
   (mcmod.platform.entity-damage/install-pvp-gate!) currently disallows it --
   matches upstream AbilityContext.dealDamage's
   (canAttackPlayer() || !(target instanceof EntityPlayer)) check."
  [entity]
  (and (instance? Player entity) (not (damage-effects/pvp-allowed?))))

(defn create-entity-damage
  "Return a function map implementing the entity-damage contract.

   Keys: :apply-direct-damage! :apply-aoe-damage! :apply-reflection-damage!"
  [server-fn {:keys [resolve-level-fn get-entity-by-uuid-fn get-living-entities-in-aabb-fn living-entity?-fn apply-hurt-fn]
              :or {resolve-level-fn query-core/resolve-level
                   get-entity-by-uuid-fn query-core/get-entity-by-uuid
                   living-entity?-fn (fn [entity] (instance? LivingEntity entity))
                   apply-hurt-fn apply-vanilla-hurt!}}]
  (let [get-entity-by-uuid (or get-entity-by-uuid-fn query-core/get-entity-by-uuid)
        get-living-entities-in-aabb (or get-living-entities-in-aabb-fn (fn [_level _aabb] []))
        apply-hurt! (or apply-hurt-fn apply-vanilla-hurt!)
        living? (or living-entity?-fn (fn [entity] (instance? LivingEntity entity)))
        resolve-level (fn [world-id]
                        (when-let [^MinecraftServer server (server-fn)]
                          (resolve-level* server resolve-level-fn world-id)))]
    {:direct-source-entity-id
     (fn [damage-source]
       (when (instance? DamageSource damage-source)
         (when-let [^Entity direct-source (.getDirectEntity ^DamageSource damage-source)]
           (str (.getUUID direct-source)))))

     :reflection-target-entity-id
     (fn [damage-source]
       (when (instance? DamageSource damage-source)
         (let [^DamageSource source damage-source
               causing-root (some-> (.getEntity source)
                                    (multipart/combat-root))
               direct-root (some-> (.getDirectEntity source)
                                   (multipart/combat-root))
               ^Entity target (cond
                                (and causing-root (living? causing-root)) causing-root
                                (and direct-root (living? direct-root)) direct-root
                                :else nil)]
           (when target
             (str (.getUUID target))))))

     :vec-reflection-damage-source?
     (fn [damage-source]
       (and (instance? DamageSource damage-source)
            (DamageSourceAccess/isVecReflection ^DamageSource damage-source)))

     :apply-direct-damage!
     (fn apply-direct-damage-impl
       ([world-id entity-uuid damage source-type]
        (apply-direct-damage-impl world-id entity-uuid damage source-type nil))
       ([world-id entity-uuid damage source-type opts]
        (try
          (if-let [^ServerLevel level (resolve-level world-id)]
            (if-let [entity (get-entity-by-uuid level entity-uuid)]
              (if-not (pvp-blocked? entity)
                (let [attacker (when-let [attacker-uuid (:attacker-uuid opts)]
                                 (get-entity-by-uuid level attacker-uuid))
                      dmg-source (core/resolve-damage-source level source-type attacker)]
                  (when (and (:reset-invulnerable-time? opts)
                             (instance? LivingEntity entity))
                    (set! (.-invulnerableTime ^LivingEntity entity) (int 0)))
                  (apply-hurt! entity dmg-source (float damage))
                  (when (and (:reset-invulnerable-time-after? opts)
                             (instance? LivingEntity entity))
                    (set! (.-invulnerableTime ^LivingEntity entity) (int -1)))
                  true)
                false)
              false)
            false)
          (catch Exception e
            (log/warn "Failed to apply direct damage:" (ex-message e))
            false))))

     :apply-aoe-damage!
     (fn [world-id x y z radius damage source-type falloff?]
       (try
         (if-let [^ServerLevel level (resolve-level world-id)]
           (let [origin-pos {:x x :y y :z z}
                 aabb (make-aabb x y z radius)
                 entities (or (get-living-entities-in-aabb level aabb) [])
                 dmg-source (core/resolve-damage-source level source-type)]
             (core/apply-aoe-damage-flow!
               entities origin-pos radius damage falloff?
               (fn [^LivingEntity entity actual-damage]
                 (when-not (pvp-blocked? entity)
                   (apply-hurt! entity dmg-source (float actual-damage))))))
           [])
         (catch Exception e
           (log/warn "Failed to apply AOE damage:" (ex-message e))
           [])))

     :apply-reflection-damage!
     (fn [world-id entity-uuid damage source-type reflection-count max-reflections]
       (try
         (if-let [^ServerLevel level (resolve-level world-id)]
           (if-let [entity (get-entity-by-uuid level entity-uuid)]
             (if (living? entity)
               (let [^LivingEntity living entity
                     dmg-source (core/resolve-damage-source level source-type)
                     search-radius (core/reflection-search-radius)]
                 (core/apply-reflection-damage-flow!
                   living damage reflection-count max-reflections search-radius
                   (fn [^LivingEntity current-entity radius]
                     (let [current-pos (core/entity-pos-map current-entity)
                           aabb (make-aabb (:x current-pos) (:y current-pos) (:z current-pos) radius)
                           candidates (mapv core/candidate-map (or (get-living-entities-in-aabb level aabb) []))
                           target-uuid (core/select-reflection-target-uuid
                                         (str (.getUUID current-entity))
                                         current-pos candidates radius)]
                       (when target-uuid
                         (get-entity-by-uuid level target-uuid))))
                   (fn [^LivingEntity target damage-value]
                     (when-not (pvp-blocked? target)
                       (apply-hurt! target dmg-source (float damage-value))))))
               [])
             [])
           [])
         (catch Exception e
           (log/warn "Failed to apply reflection damage:" (ex-message e))
           [])))}))

(defn install-entity-damage!
  [entity-damage label]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter! fw-atom :entity-damage entity-damage))
  nil)
