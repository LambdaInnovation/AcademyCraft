(ns cn.li.ac.wireless.data.node-conn-validation
  "Integrity validation for node connections (mirrors network-validation).

  A device is stale when its chunk is loaded but its capability no longer
  resolves; it is removed after being stale for
  `:stale-device-cooldown-ticks` of game time (tracked via the transient
  timestamps in `wireless.data.node-conn`)."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.mcmod.util.log :as log]))

(defn- validate-devices!
  [conn world devices resolve-fn remove-fn kind
   {:keys [game-time cfg cap-cache]}]
  (let [world-data (:world-data conn)
        stale-map (node-conn/stale-devices world-data)
        cooldown (long (get cfg :stale-device-cooldown-ticks))]
    (doseq [dev-vb devices]
      (let [dev-pos (vb/pos-of dev-vb)]
        (if (and (vb/is-chunk-loaded? dev-vb world)
                 (nil? (resolver/resolve-cap-cached cap-cache resolve-fn world dev-vb)))
          (if-let [since (get stale-map dev-pos)]
            (when (>= (- (long game-time) (long since)) cooldown)
              (log/warn "[wireless] Validate: removing stale" kind "after" cooldown
                        "ticks — capability missing:" (vb/vblock-to-string dev-vb))
              (remove-fn conn dev-vb))
            (node-conn/mark-stale! world-data dev-pos game-time))
          (when (contains? stale-map dev-pos)
            (node-conn/clear-stale-entry! world-data dev-pos)))))))

(defn validate!
  "Validate connection integrity. Dispose empty or orphaned connections.
  Returns true if valid, false if disposed."
  [conn world ctx]
  (let [world-data (:world-data conn)
        conn (entity-commit/resolve-connection world-data conn)
        node-vb (:node conn)]
    (validate-devices! conn world (node-conn/get-receivers conn)
                       resolver/resolve-receiver-cap node-conn/remove-receiver!
                       "receiver" ctx)
    (validate-devices! conn world (node-conn/get-generators conn)
                       resolver/resolve-generator-cap node-conn/remove-generator!
                       "generator" ctx)
    ;; Whole-connection dispose: node gone or everything empty.
    (let [conn (entity-commit/resolve-connection world-data conn)]
      (if (and (not (node-conn/is-disposed? conn))
               (vb/is-chunk-loaded? node-vb world))
        (let [node-exists? (some? (resolver/resolve-cap-cached
                                    (get ctx :cap-cache)
                                    resolver/resolve-node-cap world node-vb))
              empty-conn? (and (empty? (node-conn/get-generators conn))
                               (empty? (node-conn/get-receivers conn)))]
          (if (or (not node-exists?) empty-conn?)
            (do (when empty-conn?
                  (log/debug "[wireless] Validate: disposing empty connection for node"
                            (vb/vblock-to-string node-vb)))
                (node-conn/set-disposed! conn true)
                false)
            true))
        (not (node-conn/is-disposed? conn))))))
