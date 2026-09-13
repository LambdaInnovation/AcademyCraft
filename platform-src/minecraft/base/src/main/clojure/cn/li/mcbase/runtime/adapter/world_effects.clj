(ns cn.li.mcbase.runtime.adapter.world-effects
  "Shared IWorldEffects adapter factory.

  Platform namespaces provide the server lookup plus platform-specific
  entity/lightning/explosion callbacks; this namespace owns the shared
  world-query orchestration and protocol-var installation."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcbase.runtime.multipart-entity :as multipart]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server MinecraftServer]
           [net.minecraft.server.level ServerLevel]
           [net.minecraft.world.entity Entity]
           [net.minecraft.world.level.block Block]))

(defonce ^:private world-core-atom (atom nil))

(defn install-world-core!
  [m]
  (reset! world-core-atom m)
  m)

(defn- core []
  (let [m @world-core-atom]
    (when (nil? m)
      (throw (IllegalStateException. "world-effects-core not installed")))
    m))

(defn- resolve-level [server resolve-level-fn world-id]
  (when server
    (resolve-level-fn server world-id)))

(defn create-world-effects
  [server-fn {:keys [resolve-level-fn spawn-lightning-fn create-explosion-fn spawn-projectile-fn get-entities-in-aabb-fn resolve-entity-id-fn block-id-fn get-entity-by-uuid-fn]
              :or {resolve-level-fn query-core/resolve-level-strict
                   get-entity-by-uuid-fn query-core/get-entity-by-uuid
                   resolve-entity-id-fn (fn [^Entity entity] (str (.getDescriptionId (.getType entity))))
                   block-id-fn (fn [^Block block _block-state] (str (.getDescriptionId block)))}}]
  (let [spawn-lightning! (or spawn-lightning-fn (fn [_level _x _y _z _visual-only?] false))
        create-explosion! (or create-explosion-fn
                              (fn [_level _source _x _y _z _radius _fire? _terrain?]
                                false))
        spawn-projectile! (or spawn-projectile-fn
                              (fn [level projectile-spec]
                                ((:spawn-projectile-in-level! (core))
                                  level projectile-spec resolve-entity-id-fn get-entity-by-uuid-fn)))
        get-entities-in-aabb (or get-entities-in-aabb-fn (fn [_level _aabb] []))
        multipart-entity? multipart/multipart?]
    {:spawn-lightning! (fn spawn-lightning-adapter!
                         ([world-id x y z] (spawn-lightning-adapter! world-id x y z false))
                         ([world-id x y z visual-only?]
                          (try
                            (when-let [^MinecraftServer server (server-fn)]
                              (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                (spawn-lightning! level x y z (boolean visual-only?))))
                            (catch Exception e
                              (log/warn "Failed to spawn lightning:" (ex-message e))
                              false))))
     :create-explosion! (fn create-explosion-adapter!
                          ([world-id x y z radius fire?]
                           (create-explosion-adapter!
                            world-id x y z radius fire?
                            {:terrain? (boolean fire?)}))
                          ([world-id x y z radius fire? opts]
                           (try
                             (when-let [^MinecraftServer server (server-fn)]
                               (when-let [^ServerLevel level
                                          (resolve-level server resolve-level-fn world-id)]
                                 (let [source
                                       (when-let [attacker-uuid
                                                  (:attacker-uuid opts)]
                                         (get-entity-by-uuid-fn
                                          level attacker-uuid))]
                                   (boolean
                                    (create-explosion!
                                     level source x y z radius
                                     (boolean fire?)
                                     (boolean (:terrain? opts)))))))
                             (catch Exception e
                               (log/warn "Failed to create explosion:" (ex-message e))
                               false))))
     :spawn-projectile! (fn [world-id projectile-spec]
                          (try
                            (when-let [^MinecraftServer server (server-fn)]
                              (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                (spawn-projectile! level projectile-spec)))
                            (catch Exception e
                              (log/warn "Failed to spawn projectile:" (ex-message e))
                              {:success? false})))
     :find-entities-in-radius (fn [world-id x y z radius]
                                (try
                                  (when-let [^MinecraftServer server (server-fn)]
                                    (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                      ((:entities-in-radius (core))
                                        level
                                        x y z radius
                                        get-entities-in-aabb
                                        resolve-entity-id-fn
                                        multipart-entity?)))
                                  (catch Exception e
                                    (log/warn "Failed to find entities:" (ex-message e))
                                    [])))
     :find-entities-in-aabb (fn [world-id min-x min-y min-z max-x max-y max-z]
                              (try
                                (when-let [^MinecraftServer server (server-fn)]
                                  (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                    ((:entities-in-aabb (core))
                                      level
                                      min-x min-y min-z max-x max-y max-z
                                      get-entities-in-aabb
                                      resolve-entity-id-fn
                                      multipart-entity?)))
                                (catch Exception e
                                  (log/warn "Failed to find entities in AABB:" (ex-message e))
                                  [])))
     :find-blocks-in-radius (fn [world-id x y z radius block-predicate]
                              (try
                                (when-let [^MinecraftServer server (server-fn)]
                                  (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                    ((:find-blocks-in-radius-in-level (core))
                                      level x y z radius block-predicate block-id-fn)))
                                (catch Exception e
                                  (log/warn "Failed to find blocks:" (ex-message e))
                                  [])))
     :play-sound! (fn [world-id x y z sound-id source volume pitch]
                    (try
                      (when-let [^MinecraftServer server (server-fn)]
                        (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                          (boolean ((:play-sound-in-level! (core)) level x y z sound-id source volume pitch))))
                      (catch Exception e
                        (log/warn "Failed to play world sound:" (ex-message e))
                        false)))
     :trigger-behavior-hit! (fn [world-id entity-uuid]
                             (try
                               (when-let [^MinecraftServer server (server-fn)]
                                 (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                   (boolean ((:trigger-behavior-hit-in-level! (core)) level entity-uuid get-entity-by-uuid-fn))))
                               (catch Exception e
                                 (log/warn "Failed to trigger behavior hit:" (ex-message e))
                                 false)))
     :discard-entity-by-uuid! (fn [world-id entity-uuid]
                                (try
                                  (when-let [^MinecraftServer server (server-fn)]
                                    (when-let [^ServerLevel level (resolve-level server resolve-level-fn world-id)]
                                      (when-let [^Entity entity (get-entity-by-uuid-fn level (str entity-uuid))]
                                        (.discard entity)
                                        true)))
                                  (catch Exception e
                                    (log/warn "Failed to discard entity:" (ex-message e))
                                    false)))}))  ;; close fn, map, let, defn

(defn install-world-effects!
  [world-effects label]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter! fw-atom :world-effects world-effects))
  nil)
