(ns cn.li.ac.wireless.data.persistence
  "Canonical NBT persistence for the current wireless runtime state.

  Schema v2: networks/connections serialized as flat lists; the position-keyed
  maps and lookup tables are rebuilt on load. Older schemas are not migrated —
  loading one logs a warning and starts from a fresh state."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.vblock-codec :as vblock-codec]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.util.log :as log]))

(def schema-version 2)

(defn- required-vblock
  "Decode a nested vblock compound, throwing when it is missing or invalid so
  the corrupt entry is skipped instead of materializing at [0 0 0]."
  [compound key default-type default-ignore-chunk]
  (let [tag (sd/get-structured compound key)
        vblock (when tag
                 (vb/from-foundation
                   (vblock-codec/vblock-from-nbt tag default-type default-ignore-chunk)))]
    (when-not (and vblock (vb/validate-vblock vblock))
      (throw (ex-info (str "wireless NBT entry missing/invalid '" key "'") {:key key})))
    vblock))

;; ============================================================================
;; Networks
;; ============================================================================

(defn network-to-nbt
  [network]
  (let [compound (sd/create-structured)]
    (sd/set-entry! compound "matrix" (vblock-codec/vblock-to-nbt (:matrix network)))
    (sd/set-string! compound "ssid" (network-state/get-ssid network))
    (sd/set-string! compound "password" (network-state/get-password network))
    (sd/set-entry! compound "nodes" (vblock-codec/vblocks-to-nbt-list (network-state/get-nodes network)))
    (sd/set-double! compound "buffer" (double (network-state/get-buffer network)))
    (sd/set-boolean! compound "disposed" (network-state/is-disposed? network))
    compound))

(defn network-from-nbt
  "Deserialize a network. Pure — no world-state commits; world-data-from-nbt
  queues the result via world-registry/enqueue-rebuild! for a budgeted
  rebuild-network-indexes! commit across subsequent ticks."
  [world-data compound]
  (-> (network-state/create-wireless-net
        world-data
        (required-vblock compound "matrix" :matrix true)
        (sd/get-string compound "ssid")
        (sd/get-string compound "password"))
      (network-state/set-state-value
        :nodes
        (vec (vblock-codec/nbt-list->vblocks
               (sd/get-list compound "nodes")
               :node
               false
               vb/from-foundation)))
      (network-state/set-state-value :buffer (sd/get-double compound "buffer"))
      (network-state/set-state-value :disposed (sd/get-boolean compound "disposed"))))

;; ============================================================================
;; Connections
;; ============================================================================

(defn connection-to-nbt
  "Serialize a node connection. Devices whose chunk is loaded but whose
  capability is gone are dropped; unverifiable ones are kept to avoid data loss."
  [conn world]
  (let [compound (sd/create-structured)
        receivers-list (sd/create-list)
        generators-list (sd/create-list)]
    (sd/set-entry! compound "node" (vblock-codec/vblock-to-nbt (:node conn)))
    (doseq [receiver-vb (node-conn/get-receivers conn)]
      (if (and (vb/is-chunk-loaded? receiver-vb world)
               (nil? (resolver/resolve-receiver-cap world receiver-vb)))
        (log/warn "[wireless] Save: skipping receiver, chunk loaded but capability missing:"
                  (vb/vblock-to-string receiver-vb))
        (sd/append! receivers-list (vblock-codec/vblock-to-nbt receiver-vb))))
    (doseq [generator-vb (node-conn/get-generators conn)]
      (if (and (vb/is-chunk-loaded? generator-vb world)
               (nil? (resolver/resolve-generator-cap world generator-vb)))
        (log/warn "[wireless] Save: skipping generator, chunk loaded but capability missing:"
                  (vb/vblock-to-string generator-vb))
        (sd/append! generators-list (vblock-codec/vblock-to-nbt generator-vb))))
    (sd/set-entry! compound "receivers" receivers-list)
    (sd/set-entry! compound "generators" generators-list)
    (sd/set-boolean! compound "disposed" (node-conn/is-disposed? conn))
    compound))

(defn- device-vblocks
  [compound key]
  (vec (vblock-codec/nbt-list->vblocks
         (sd/get-list compound key)
         :node
         false
         vb/from-foundation)))

(defn connection-from-nbt
  "Deserialize a node connection. Pure — no world-state commits."
  [world-data compound]
  (assoc (node-conn/create-node-conn
           world-data
           (required-vblock compound "node" :node-conn true))
         :state {:receivers (device-vblocks compound "receivers")
                 :generators (device-vblocks compound "generators")
                 :disposed (boolean (sd/get-boolean compound "disposed"))}))

;; ============================================================================
;; World state
;; ============================================================================

(defn world-data-to-nbt
  [world-data world]
  (let [compound (sd/create-structured)
        networks-list (sd/create-list)
        connections-list (sd/create-list)]
    (sd/set-int! compound "schemaVersion" schema-version)
    (doseq [network (vals (world-registry/networks world-data))]
      (when-not (network-state/is-disposed? network)
        (sd/append! networks-list (network-to-nbt network))))
    (doseq [conn (vals (world-registry/connections world-data))]
      (when-not (node-conn/is-disposed? conn)
        (sd/append! connections-list (connection-to-nbt conn world))))
    (sd/set-entry! compound "networks" networks-list)
    (sd/set-entry! compound "connections" connections-list)
    compound))

(defn world-data-from-nbt
  [world compound]
  (let [world-data (world-registry/create-world-data world)
        found-version (sd/get-int compound "schemaVersion")]
    (if (not= found-version schema-version)
      (do
        (log/warn "[wireless] Unsupported save schema version" found-version
                  "(expected" schema-version ") — starting with fresh wireless state")
        world-data)
      (let [networks-list (sd/get-list compound "networks")
            connections-list (sd/get-list compound "connections")
            networks-size (if networks-list (sd/list-size networks-list) 0)
            connections-size (if connections-list (sd/list-size connections-list) 0)]
        (doseq [index (range networks-size)]
          (when-let [network-compound (sd/list-structured networks-list index)]
            (try
              (world-registry/enqueue-rebuild!
                world-data world-registry/network-rebuild-queue-key
                (network-from-nbt world-data network-compound))
              (catch Exception e
                (log/warn "Skipping invalid wireless network NBT entry" index ":" (ex-message e))))))
        (doseq [index (range connections-size)]
          (when-let [connection-compound (sd/list-structured connections-list index)]
            (try
              (world-registry/enqueue-rebuild!
                world-data world-registry/connection-rebuild-queue-key
                (connection-from-nbt world-data connection-compound))
              (catch Exception e
                (log/warn "Skipping invalid wireless connection NBT entry" index ":" (ex-message e))))))
        world-data))))
