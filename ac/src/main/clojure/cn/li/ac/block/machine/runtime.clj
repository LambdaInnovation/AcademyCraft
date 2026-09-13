(ns cn.li.ac.block.machine.runtime
  "Unified server-side block machine runtime: schema lifecycle, state commit, tick wrapper, GUI open."
  (:require [cn.li.ac.gui.open :as gui-open]
            [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.util.log :as log])
  (:import [clojure.lang ILookup IPersistentMap MapEntry]
           [java.util HashMap Map$Entry]))

(def ^:const save-dirty-mask 0x01)
(def ^:const client-sync-mask 0x02)
(def ^:const block-state-mask 0x04)
(def ^:const wireless-topology-mask 0x08)
(def ^:private changed-mask 0x10)
(def ^:private absent-value (Object.))

(definterface IMachineState
  (^long stateFlags [])
  (^long stateVersion [])
  (^long advanceTick [])
  (previousValue [key notFound])
  (clearDirty [])
  (snapshot []))

(declare machine-snapshot)

(deftype PreviousMachineState [^IMachineState state]
  ILookup
  (valAt [_ key]
    (.previousValue state key nil))
  (valAt [_ key not-found]
    (.previousValue state key not-found)))

(deftype MachineState [^HashMap values
                       ^HashMap field-flags
                       ^HashMap previous
                       ^:unsynchronized-mutable ^long ticker
                       ^:unsynchronized-mutable ^long flags
                       ^:unsynchronized-mutable ^long version]
  IMachineState
  (stateFlags [_] flags)
  (stateVersion [_] version)
  (advanceTick [_]
    (set! ticker (unchecked-inc ticker))
    ticker)
  (previousValue [_ key not-found]
    (if (.containsKey previous key)
      (let [value (.get previous key)]
        (if (identical? value absent-value) not-found value))
      (if (.containsKey values key) (.get values key) not-found)))
  (clearDirty [_]
    (.clear previous)
    (set! flags 0)
    nil)
  (snapshot [this]
    (machine-snapshot this))

  ILookup
  (valAt [_ key]
    (.get values key))
  (valAt [_ key not-found]
    (if (.containsKey values key) (.get values key) not-found))

  IPersistentMap
  (containsKey [_ key]
    (.containsKey values key))
  (entryAt [this key]
    (when (.containsKey ^IPersistentMap this key)
      (MapEntry/create key (.valAt ^ILookup this key))))
  (assoc [this key value]
    (let [present? (.containsKey values key)
          old-value (when present? (.get values key))]
      (when (or (not present?) (not= old-value value))
        (let [mask-value (.get field-flags key)
              mask (long (if (nil? mask-value) changed-mask mask-value))]
          (when (and (not (zero? mask)) (not (.containsKey previous key)))
            (.put previous key (if present? old-value absent-value)))
          (.put values key value)
          (set! flags (bit-or flags mask))
          (set! version (unchecked-inc version)))))
    this)
  (assocEx [this key value]
    (when (.containsKey ^IPersistentMap this key)
      (throw (RuntimeException. (str "Key already present: " key))))
    (.assoc ^IPersistentMap this key value))
  (without [this key]
    (when (.containsKey values key)
      (let [old-value (.get values key)
            mask-value (.get field-flags key)
            mask (long (if (nil? mask-value) changed-mask mask-value))]
        (when (and (not (zero? mask)) (not (.containsKey previous key)))
          (.put previous key old-value))
        (.remove values key)
        (set! flags (bit-or flags mask))
        (set! version (unchecked-inc version))))
    this)

  clojure.lang.IPersistentCollection
  (count [_]
    (.size values))
  (cons [this value]
    (cond
      (instance? Map$Entry value)
      (.assoc ^IPersistentMap this (.getKey ^Map$Entry value) (.getValue ^Map$Entry value))

      (and (vector? value) (= 2 (count value)))
      (.assoc ^IPersistentMap this (nth value 0) (nth value 1))

      :else
      (do
        (doseq [entry value]
          (.cons ^clojure.lang.IPersistentCollection this entry))
        this)))
  (empty [_] {})
  (equiv [this other]
    (= (machine-snapshot this) other))

  clojure.lang.Seqable
  (seq [this]
    (seq (machine-snapshot this)))

  Object
  (toString [this]
    (str (machine-snapshot this))))

(defn machine-state?
  [value]
  (instance? MachineState value))

(defn machine-snapshot
  [^MachineState state]
  (let [^HashMap values (.-values state)
        iterator (.iterator (.entrySet values))]
    (loop [result (transient {})]
      (if (.hasNext iterator)
        (let [^Map$Entry entry (.next iterator)]
          (recur (assoc! result (.getKey entry) (.getValue entry))))
        (persistent! result)))))

(defn- field-runtime-mask
  [spec]
  (bit-or changed-mask
          (if (:persist? spec) save-dirty-mask 0)
          (if (:client-sync? spec) client-sync-mask 0)
          (if (:block-state spec) block-state-mask 0)
          (if (:wireless-topology? spec) wireless-topology-mask 0)))

(defn- schema-field-flags
  [schema]
  (reduce (fn [result spec]
            (assoc result (:key spec) (field-runtime-mask spec)))
          {}
          schema))

(defn- create-machine-state
  [state default-state]
  (let [initial (merge default-state state)
        values (HashMap. ^java.util.Map initial)
        ^java.util.Map field-mask-map (or (::field-flags (meta default-state)) {})
        flags (HashMap. field-mask-map)]
    (MachineState. values flags (HashMap.) 0 0 0)))

(defn ensure-machine-state
  [state default-state]
  (if (machine-state? state)
    state
    (create-machine-state (or state {}) default-state)))

(defn advance-tick!
  ^long [^IMachineState state]
  (.advanceTick state))

(defn schema-runtime
  "Build runtime bundle from a full or server schema vector."
  [schema & {:keys [server-only?]}]
  (let [server-schema (if server-only?
                        schema
                        (state-schema/filter-server-fields schema))
        blockstate-fields (filterv #(get % :block-state) server-schema)
        default-state (with-meta (state-schema/schema->default-state server-schema)
                        {::field-flags (schema-field-flags server-schema)})]
    {:schema schema
     :server-schema server-schema
     :default-state default-state
     :load-fn (state-schema/schema->load-fn server-schema)
     :save-fn (state-schema/schema->save-fn server-schema)
     :blockstate-fields blockstate-fields
     :block-state-properties (state-schema/extract-block-state-properties blockstate-fields)
     :blockstate-updater (when (seq blockstate-fields)
                           (state-schema/build-block-state-updater blockstate-fields))}))

(defn state-or-default
  [be default-state]
  (or (platform-be/get-custom-state be) default-state))

(defn- commit-machine-state!
  [be level pos ^MachineState state blockstate-updater mark-changed? sync-client? after-commit!]
  (let [flags (.stateFlags ^IMachineState state)
        changed? (not (zero? (bit-and flags changed-mask)))
        old-state (when (and changed? after-commit!) (PreviousMachineState. state))]
    (when changed?
      (when (and mark-changed? (not (zero? (bit-and flags save-dirty-mask))))
        (try (platform-be/set-changed! be) (catch Exception _ nil)))
      (when (and sync-client? (not (zero? (bit-and flags client-sync-mask))))
        (try (platform-be/sync-to-client! be) (catch Exception _ nil)))
      (when (and blockstate-updater level pos
                 (not (zero? (bit-and flags block-state-mask))))
        (blockstate-updater state level pos))
      (when after-commit!
        (after-commit! be level pos old-state state))
      (.clearDirty ^IMachineState state))
    flags))

(defn commit-state!
  "Single boundary for BE state writes, dirty flag, client sync, and optional blockstate projection.

  `:mark-changed?` (default true) controls setChanged (NBT save dirty).
  `:sync-client?` (default false) controls an explicit client block-update packet.
  Most machines don't need this: visuals go through BlockState properties or GUI
  DataSlots. Declare true only when a TESR reads BE custom-state directly."
  [be level pos old-state new-state & {:keys [blockstate-updater mark-changed? sync-client?]
                                       :or {mark-changed? true sync-client? false}}]
  (if (machine-state? new-state)
    (commit-machine-state! be level pos new-state blockstate-updater
                           (boolean mark-changed?) (boolean sync-client?) nil)
    (when (not= new-state old-state)
      (platform-be/set-custom-state! be new-state)
      (when mark-changed?
        (try (platform-be/set-changed! be) (catch Exception _ nil)))
      (when sync-client?
        (try (platform-be/sync-to-client! be) (catch Exception _ nil)))
      (when (and blockstate-updater level pos)
        (blockstate-updater new-state level pos)))))

(defn- resolve-tile-level-pos
  "Return [level pos] for a tile, or [nil nil] when unavailable."
  [tile]
  (when tile
    (let [level (try (platform-be/be-get-world-safe tile) (catch Exception _ nil))
          block-pos (when level (try (pos/block-pos tile) (catch Exception _ nil)))]
      [level block-pos])))

(defn- compute-dirty-flag
  [mark-changed? state0 state1]
  (if (fn? mark-changed?)
    (mark-changed? state0 state1)
    (boolean mark-changed?)))

(defn commit-transform!
  "Apply `transform` to current tile state and commit when changed.

  `transform` receives current state map and returns new state.
  Options match `commit-state!`; optional `:after-commit!` runs when state changes.

  NOTE: defaults for `:mark-changed?`/`:sync-client?` are resolved HERE (not left to
  `commit-state!`'s `:or`) — an omitted keyword here would otherwise pass an explicit
  `nil` through to `commit-state!`, which defeats its own `:or` default (a present key
  bound to nil is not the same as an absent key)."
  [tile default-state transform & {:keys [blockstate-updater mark-changed? sync-client? after-commit!]
                                   :or {mark-changed? true sync-client? false}}]
  (when tile
    (let [[level pos] (resolve-tile-level-pos tile)
          raw-state (state-or-default tile default-state)
          old (ensure-machine-state raw-state default-state)
          _ (when-not (identical? raw-state old)
              (platform-be/set-custom-state! tile old))
          new-state (transform old)
          state (ensure-machine-state new-state default-state)]
      (when-not (identical? state old)
        (platform-be/set-custom-state! tile state))
      (commit-machine-state! tile level pos state blockstate-updater
                             (boolean mark-changed?) (boolean sync-client?) after-commit!))))

(defn commit-from-tile!
  "Commit an explicit new state for a tile (old state read from tile).
  See `commit-transform!` for why defaults are resolved here."
  [tile default-state new-state & {:keys [blockstate-updater mark-changed? sync-client? after-commit!]
                                   :or {mark-changed? true sync-client? false}}]
  (when tile
    (let [[level pos] (resolve-tile-level-pos tile)
          raw-state (state-or-default tile default-state)
          old (ensure-machine-state raw-state default-state)
          state (ensure-machine-state new-state default-state)]
      (when-not (identical? raw-state state)
        (platform-be/set-custom-state! tile state))
      (commit-machine-state! tile level pos state blockstate-updater
                             (boolean mark-changed?) (boolean sync-client?) after-commit!))))

(defn server-side?
  [level]
  (and level (not (world/client-side? level))))

(defn make-tick-fn
  "Wrap a pure tick step with server guard and commit-state!.

  Options:
  - :tick-state (fn [state level pos block-state be] -> state')
  - :initial-state (fn [be level pos block-state] -> state) optional
  - :after-commit! (fn [be level pos old-state new-state] -> nil) optional
  - :mark-changed? may be true, false, or (fn [old-state new-state] -> bool).
  - :sync-client? may be true, false, or (fn [old-state new-state] -> bool); default false.
    Only declare true for machines whose TESR reads BE custom-state directly — most
    visuals go through BlockState properties or GUI DataSlots and need no BE packet."
  [{:keys [default-state initial-state tick-state blockstate-updater after-commit! mark-changed? sync-client?]
    :or {mark-changed? true sync-client? false}}]
  (fn [level pos block-state be]
    (when (server-side? level)
      (let [raw-state (if initial-state
                        (initial-state be level pos block-state)
                        (state-or-default be default-state))
            state0 (ensure-machine-state raw-state default-state)
            _ (when-not (identical? raw-state state0)
                (platform-be/set-custom-state! be state0))
            result (tick-state state0 level pos block-state be)
            state1 (ensure-machine-state result default-state)]
        (when-not (identical? state1 state0)
          (platform-be/set-custom-state! be state1))
        (commit-machine-state! be level pos state1 blockstate-updater
                               (if (fn? mark-changed?) true (boolean mark-changed?))
                               (if (fn? sync-client?) true (boolean sync-client?))
                               after-commit!)))))

(defn make-open-gui-handler-with-predicate
  "Build a block right-click handler that opens an AC GUI.

  `can-open?` receives (player world pos sneaking item-stack).
  Optional:
  - `:resolve-open-pos` (fn [player world pos] -> pos)
  - `:server-before-open!` (fn [player world open-pos] -> truthy)"
  [gui-type can-open? & {:keys [resolve-open-pos server-before-open!]}]
  (fn [player world pos block-id & {:keys [sneaking item-stack]}]
    (when (and player world pos (not sneaking) (can-open? player world pos sneaking item-stack))
      (try
        (let [open-pos (if resolve-open-pos (resolve-open-pos player world pos) pos)]
          (when open-pos
            (if (world/client-side? world)
              (gui-open/open-gui-by-type player gui-type world open-pos)
              (when (or (not server-before-open!) (server-before-open! player world open-pos))
                (gui-open/open-gui-by-type player gui-type world open-pos)))))
        (catch Exception e
          (log/stacktrace (str "Failed to open GUI " gui-type) e)
          nil)))))

(defn make-open-gui-handler
  "Build a block right-click handler that opens an AC GUI by business type keyword."
  [gui-type]
  (make-open-gui-handler-with-predicate gui-type (fn [_ _ _ _ _] true)))
