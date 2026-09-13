(ns cn.li.mcbase.runtime.raycast-core
  "Loader-agnostic IRaycast adapter built on shared Raycast helpers."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcbase.runtime.raycast-normalize :as rn]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server MinecraftServer]))

(defonce ^:private raycast-ops-atom (atom nil))

(defn install-raycast-ops!
  "Install map of Raycast static method wrappers."
  [m]
  (reset! raycast-ops-atom m)
  m)

(defn- raycast-ops []
  (let [m @raycast-ops-atom]
    (when (nil? m)
      (throw (IllegalStateException. "Raycast ops not installed")))
    m))

(defn- resolve-level [^MinecraftServer server world-id]
  (query-core/resolve-level server world-id))

(defn create-raycast
  "Create an IRaycast adapter using a platform-provided server supplier."
  [get-server]
  {:raycast-blocks (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
                     (try
                       (rn/normalize-bridge-map
                         ((:raycastBlocks (raycast-ops))
                           (resolve-level (get-server) world-id)
                           start-x start-y start-z dir-x dir-y dir-z max-distance))
                       (catch Exception e
                         (log/warn "Failed to raycast blocks:" (ex-message e))
                         nil)))
   :raycast-blocks-matching (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance accepted-block-ids]
                              (try
                                (rn/normalize-bridge-map
                                  ((:raycastBlocksMatching (raycast-ops))
                                    (resolve-level (get-server) world-id)
                                    start-x start-y start-z dir-x dir-y dir-z max-distance
                                    accepted-block-ids))
                                (catch Exception e
                                  (log/warn "Failed to raycast matching blocks:" (ex-message e))
                                  nil)))
   :raycast-collidable-blocks-or-water
   (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
     (try
       (rn/normalize-bridge-map
         ((:raycastCollidableBlocksOrWater (raycast-ops))
           (resolve-level (get-server) world-id)
           start-x start-y start-z dir-x dir-y dir-z max-distance))
       (catch Exception e
         (log/warn "Failed to raycast collidable blocks or water:" (ex-message e))
         nil)))
   :raycast-entities (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
                       (try
                         (rn/normalize-bridge-map
                           ((:raycastEntities (raycast-ops))
                             (resolve-level (get-server) world-id)
                             start-x start-y start-z dir-x dir-y dir-z max-distance))
                         (catch Exception e
                           (log/warn "Failed to raycast entities:" (ex-message e))
                           nil)))
   :raycast-combined (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
                       (let [level (resolve-level (get-server) world-id)]
                         (try
                           (when level
                             (rn/normalize-bridge-map
                               ((:raycastCombined (raycast-ops))
                                 level start-x start-y start-z dir-x dir-y dir-z max-distance)))
                           (catch Exception e
                             (log/warn "Failed to raycast combined:" (ex-message e))
                             nil))))
   :raycast-combined-excluding
   (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance excluded-entity-uuid]
     (let [level (resolve-level (get-server) world-id)]
       (try
         (when level
           (rn/normalize-bridge-map
             ((:raycastCombinedExcluding (raycast-ops))
               level
               start-x start-y start-z
               dir-x dir-y dir-z
               max-distance
               (some-> excluded-entity-uuid str))))
         (catch Exception e
           (log/warn "Failed to raycast combined excluding entity:" (ex-message e))
           nil))))
   :raycast-combined-all (fn [world-id start-x start-y start-z dir-x dir-y dir-z max-distance]
                           (let [level (resolve-level (get-server) world-id)]
                             (try
                               (when level
                                 (rn/normalize-bridge-map
                                  ((:raycastCombinedAll (raycast-ops))
                                   level start-x start-y start-z
                                   dir-x dir-y dir-z max-distance)))
                               (catch Exception e
                                 (log/warn "Failed to raycast combined entities:" (ex-message e))
                                 nil))))
   :raycast-combined-from-player
   (fn [player-uuid max-distance living-only?]
     (try
       (rn/normalize-bridge-map
         ((:raycastCombinedFromPlayer (raycast-ops))
           (query-core/get-player-by-uuid (get-server) player-uuid)
           max-distance
           (boolean living-only?)))
       (catch Exception e
         (log/warn "Failed to raycast combined from player:" (ex-message e))
         nil)))
   :get-player-look-vector (fn [player-uuid]
                             (try
                               (rn/normalize-bridge-map
                                 ((:getPlayerLookVector (raycast-ops)) (query-core/get-player-by-uuid (get-server) player-uuid)))
                               (catch Exception e
                                 (log/debug "Failed to get player look vector:" (ex-message e))
                                 nil)))
   :get-player-position (fn [player-uuid]
                          (try
                            (rn/normalize-bridge-map
                              ((:getPlayerPosition (raycast-ops)) (query-core/get-player-by-uuid (get-server) player-uuid)))
                            (catch Exception e
                              (log/debug "Failed to get player position:" (ex-message e))
                              nil)))
   :raycast-from-player (fn [player-uuid max-distance living-only?]
                          (try
                            (rn/normalize-bridge-map
                              ((:raycastFromPlayer (raycast-ops)) (query-core/get-player-by-uuid (get-server) player-uuid) max-distance living-only?))
                            (catch Exception e
                              (log/warn "Failed to raycast from player:" (ex-message e))
                              nil)))})
