(ns cn.li.ac.wireless.data.node-conn
  "NodeConn model and device management.

  A node connection binds one wireless node to its generators/receivers.
  Integrity validation lives in `wireless.data.node-conn-validation`; per-tick
  energy transfer lives in `wireless.runtime.node-transfer`; NBT codecs live
  in `wireless.data.persistence`.

  Transient stale-device timestamps ({device-pos -> first-stale-game-time})
  are tracked here: never persisted, never mark the SavedData dirty."
  (:require [clojure.string :as str]
            [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.core.capability-resolver :as resolver]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.ac.wireless.domain.topology :as topology]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessNode]))

;; ============================================================================
;; NodeConn Record
;; ============================================================================

(defrecord NodeConn
  [world-data
   node
   state])

(defn- default-state
  []
  {:receivers []
   :generators []
   :disposed false})

(defn create-node-conn
  "Create a new node connection"
  [world-data node-vblock]
  (->NodeConn world-data node-vblock (default-state)))

;; ============================================================================
;; Accessors
;; ============================================================================

(defn- state-value [conn key] (get (:state conn) key))

(defn- update-state [conn f] (assoc conn :state (f (:state conn))))

(defn get-node
  "Get the node capability"
  [conn world]
  (resolver/resolve-node-cap world (:node conn)))

(defn get-receivers
  "`:receivers` is always a vector at every write site (default-state, conj-only
  mutation, NBT load) — no re-`vec` needed on this per-tick read path."
  [conn]
  (or (state-value conn :receivers) []))

(defn get-generators
  "`:generators` is always a vector — see `get-receivers`."
  [conn]
  (or (state-value conn :generators) []))

(defn is-disposed? [conn]
  (boolean (state-value conn :disposed)))

(defn set-disposed!
  [conn value]
  (entity-commit/commit-connection!
    (:world-data conn)
    (update-state conn #(assoc % :disposed (boolean value)))))

(defn get-load
  "Get total load (receivers + generators)"
  [conn]
  (+ (count (get-receivers conn))
     (count (get-generators conn))))

(defn get-capacity
  "Get node capacity"
  [conn world]
  (if-let [node (get-node conn world)]
    (.getCapacity ^IWirelessNode node)
    Integer/MAX_VALUE))

(declare remove-receiver! remove-generator!)

;; ============================================================================
;; Transient stale-device tracking — {device-pos -> first-stale-game-time}
;; ============================================================================

(defn stale-devices
  "Read this world's transient stale-device timestamp map."
  [world-data]
  (or (world-registry/transient-value world-data :stale-devices) {}))

(defn- assoc-if-absent [m k v]
  (let [m (or m {})]
    (if (contains? m k) m (assoc m k v))))

(defn- dissoc-when-present [m k]
  (if (and m (contains? m k)) (dissoc m k) m))

(defn mark-stale!
  "Record the first game time a device was seen stale (no-op when present)."
  [world-data device-pos game-time]
  (world-registry/update-transient-value!
    world-data :stale-devices assoc-if-absent device-pos game-time))

(defn clear-stale-entry!
  "Drop the transient stale timestamp for a device position."
  [world-data device-pos]
  (world-registry/update-transient-value!
    world-data :stale-devices dissoc-when-present device-pos))

;; ============================================================================
;; Range Checking
;; ============================================================================

(defn- check-range
  "Check if vblock is within node range"
  [conn vblock world]
  (if-let [node (get-node conn world)]
    (let [range (.getRange ^IWirelessNode node)
          dist-sq (vb/dist-sq vblock (:node conn))]
      (<= dist-sq (* range range)))
    false))

;; ============================================================================
;; Device Management (Unified)
;; ============================================================================

(defn- capacity-available? [conn world]
  (< (get-load conn) (get-capacity conn world)))

(defn- resolve-device-context
  [device-type]
  {:device-key (case device-type
                 :receiver :receivers
                 :generator :generators)
   :remove-fn (case device-type
                :receiver remove-receiver!
                :generator remove-generator!)
   :device-name (name device-type)})

(defn- remove-device-from-old-connection!
  [conn device-vb remove-fn]
  (when-let [old-conn (lookup/get-node-connection (:world-data conn) device-vb)]
    (remove-fn old-conn device-vb)))

(defn- attach-device-state
  [state conn* device-vb]
  (-> state
      (assoc-in [:connections (vb/pos-of (:node conn*))] conn*)
      (topology/link-connection-device conn* device-vb)))

(defn- attach-device!
  [conn device-vb device-key device-name]
  (let [world-data (:world-data conn)
        conn (entity-commit/resolve-connection world-data conn)
        ;; Clear :disposed flag — a previously empty connection may have been
        ;; disposed by validation, but adding a device revives it.
        conn* (update-state conn #(-> %
                                      (update device-key conj device-vb)
                                      (assoc :disposed false)))]
    (world-registry/update-state! world-data attach-device-state conn* device-vb)
    (log/info (format "Added %s %s to node %s"
                      device-name
                      (vb/vblock-to-string device-vb)
                      (vb/vblock-to-string (:node conn*))))
    true))

(defn- remove-device-state
  [state conn* device-vb]
  (-> state
      (assoc-in [:connections (vb/pos-of (:node conn*))] conn*)
      (topology/unlink-connection-device device-vb)))

(defn- remove-matching-device [state device-key device-vb]
  (update state device-key
          (fn [items]
            (filterv #(not (vb/vblock-equals? % device-vb)) items))))

(defn- remove-device!
  [conn device-vb device-key device-name]
  (let [world-data (:world-data conn)
        conn (entity-commit/resolve-connection world-data conn)
        devices (state-value conn device-key)
        removed? (boolean (some #(vb/vblock-equals? % device-vb) devices))]
    (when removed?
      (let [conn* (update-state conn #(remove-matching-device % device-key device-vb))]
        (world-registry/update-state! world-data remove-device-state conn* device-vb)
        (clear-stale-entry! world-data (vb/pos-of device-vb))
        (log/info (format "Removed %s %s from node %s"
                          device-name
                          (vb/vblock-to-string device-vb)
                          (vb/vblock-to-string (:node conn))))))
    removed?))

(defn- add-device!
  "Generic function to add a device (receiver or generator) to node connection
  Returns true if successful"
  [conn device-vb device-type world]
  (let [conn (entity-commit/resolve-connection (:world-data conn) conn)
        {:keys [device-key remove-fn device-name]} (resolve-device-context device-type)]
    (cond
      (not (capacity-available? conn world))
      (do
        (log/debug (format "%s add failed: node at capacity" (str/capitalize device-name)))
        false)

      (not (check-range conn device-vb world))
      (do
        (log/debug (format "%s add failed: out of range" (str/capitalize device-name)))
        false)

      :else
      (do
        (remove-device-from-old-connection! conn device-vb remove-fn)
        (attach-device! conn device-vb device-key device-name)))))

(defn add-receiver!
  "Add a receiver to this node connection
  Returns true if successful"
  [conn receiver-vb world]
  (add-device! conn receiver-vb :receiver world))

(defn remove-receiver!
  "Remove receiver from this node connection immediately."
  [conn receiver-vb]
  (remove-device! conn receiver-vb :receivers "receiver"))

(defn add-generator!
  "Add a generator to this node connection
  Returns true if successful"
  [conn generator-vb world]
  (add-device! conn generator-vb :generator world))

(defn remove-generator!
  "Remove generator from this node connection immediately."
  [conn generator-vb]
  (remove-device! conn generator-vb :generators "generator"))
