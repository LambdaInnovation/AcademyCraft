(ns cn.li.mcmod.block.state-schema
  "Generic BlockEntity state schema framework.

  A state schema is a vector of FieldSpec maps. Each FieldSpec describes one
  logical field of a block entity's customState map and declares how it should
  be serialised to/from NBT, included in GUI sync payloads, and reflected in
  BlockState properties.

  This namespace is platform-neutral and must not import `net.minecraft.*`
  types at the top-level. Use platform adapters for any platform-specific
  behaviour."
  (:require [cn.li.mcmod.platform.world :as platform-world]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.platform.be :as pbe]
            [cn.li.mcmod.nbt.dsl :as nbt-dsl]
            [cn.li.mcmod.block.inventory-helpers :as inv-helpers]
            [cn.li.mcmod.schema.core :as schema]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn- ^:private valid-network-editable-field?
  "Named predicate for Malli schema — avoids anon-fn AOT symbol leak."
  [field]
  (or (not (:network-editable? field))
      (:network-msg field)))

(def ^:private field-spec-schema
  [:and
   [:map
    [:key keyword?]
    [:default {:optional true} any?]
    [:persist? {:optional true} boolean?]
    [:gui-sync? {:optional true} boolean?]
    [:gui-only? {:optional true} boolean?]
    [:gui-init {:optional true} fn?]
    [:gui-coerce {:optional true} fn?]
    [:gui-container-key {:optional true} keyword?]
    [:gui-payload-key {:optional true} keyword?]
    [:gui-close-reset {:optional true} any?]
    [:nbt-key {:optional true} string?]
    [:type {:optional true} keyword?]
    [:load-fn {:optional true} fn?]
    [:save-fn {:optional true} fn?]
    [:block-state {:optional true}
     [:map
      [:prop string?]
      [:type keyword?]
      [:min {:optional true} any?]
      [:max {:optional true} any?]
      [:default {:optional true} any?]
      [:xf {:optional true} any?]]]
    [:network-editable? {:optional true} boolean?]
    [:network-msg {:optional true} keyword?]
    [:doc {:optional true} string?]]
   [:fn valid-network-editable-field?]])

(def ^:private field-schema
  [:vector field-spec-schema])

(def ^:private field-schema-validator (schema/lazy-validator field-schema))

(defn validate-field-schema!
  "Validate a block state FieldSpec vector at namespace load/compile time."
  [schema*]
  (schema/require-valid field-schema (field-schema-validator) :block-state-field-schema schema*))

(def ^:private network-get-world-fn
  (fn [_] nil))

(def ^:private network-get-tile-at-fn
  (fn [_ _] nil))

(defn register-network-helper-fns!
  "Register helper fns used by generated network handlers."
  [{:keys [get-world get-tile-at]}]
  (when get-world
    (install/install-root! #'network-get-world-fn get-world))
  (when get-tile-at
    (install/install-root! #'network-get-tile-at-fn get-tile-at))
  nil)

(defn get-network-world
  [player]
  (network-get-world-fn player))

(defn get-network-tile-at
  [world payload]
  (network-get-tile-at-fn world payload))

(def ^:private nbt-writers
  "NBT writers extracted from nbt.dsl/type-converters"
  (into {} (map (fn [[k v]] [k (:write v)]) nbt-dsl/type-converters)))

(def ^:private nbt-readers
  "NBT readers extracted from nbt.dsl/type-converters"
  (into {} (map (fn [[k v]] [k (:read v)]) nbt-dsl/type-converters)))

;; ============================================================================
;; Core schema utilities
;; ============================================================================

(defn schema->default-state
  "Build the default state map from explicitly declared FieldSpec defaults.

  A missing `:default` is different from `:default nil`; do not synthesize nil
  defaults because doing so turns ordinary `(get state k fallback)` GUI init
  code into `(get {:k nil} k fallback)`, bypassing the fallback."
  [schema]
  (validate-field-schema! schema)
  (into {}
        (keep (fn [spec]
                (when (contains? spec :default)
                  [(:key spec) (:default spec)]))
              schema)))

(def ^:private field-index-cache
  "Schema vector -> {field-key spec} index; avoids capturing filter in get-field hot path."
  ^java.util.concurrent.ConcurrentHashMap (java.util.concurrent.ConcurrentHashMap.))

(defn schema->field-index
  "Build O(1) key->FieldSpec index for a validated schema vector."
  [schema]
  (validate-field-schema! schema)
  (let [^java.util.concurrent.ConcurrentHashMap cache field-index-cache]
    (or (.get cache schema)
        (let [index (into {} (map (fn [spec] [(:key spec) spec]) schema))]
          (.putIfAbsent cache schema index)
          (or (.get cache schema) index)))))

(defn get-field
  "Return the value of key from state, falling back to the schema default.
  Returns nil when the key is absent from both state and schema."
  [schema state key]
  (validate-field-schema! schema)
  (if-let [spec (get (schema->field-index schema) key)]
    (get state key (:default spec))
    (get state key)))

;; ============================================================================
;; NBT load / save
;; ============================================================================

(defn schema->load-fn
  "Return (fn [tag] -> state-map).

  Deserialises a CompoundTag into a fresh state map:
  - Fields with :persist? false receive their :default.
  - Fields with :load-fn delegate to (load-fn tag nbt-key default).
  - Other persisted fields use the internal 1.20.1 NBT readers."
  [schema]
  (validate-field-schema! schema)
  (let [defaults (schema->default-state schema)]
    (fn [tag]
      (reduce
        (fn [state spec]
          (let [k    (:key spec)
                dflt (:default spec)]
            (if-not (:persist? spec)
              (assoc state k dflt)
              (let [nk (:nbt-key spec)]
                (if-let [load-fn (:load-fn spec)]
                  (assoc state k (load-fn tag nk dflt))
                  (if (sd/has-key-safe? tag nk)
                    (if-let [reader (get nbt-readers (:type spec))]
                      (assoc state k (reader tag nk))
                      (assoc state k dflt))
                    (assoc state k dflt)))))))
        defaults
        schema))))

(defn schema->save-fn
  "Return (fn [be tag] -> nil).

  Serialises the BE's customState to a CompoundTag:
  - Only fields with :persist? true are written.
  - Fields with :save-fn delegate to (save-fn state tag nbt-key).
  - Other fields use the internal 1.20.1 NBT writers."
  [schema]
  (validate-field-schema! schema)
  (let [defaults (schema->default-state schema)]
    (fn [be tag]
      (let [state (or (pbe/get-custom-state be) defaults)]
        (doseq [spec schema]
          (when (:persist? spec)
            (let [k  (:key spec)
                  nk (:nbt-key spec)
                  v  (get state k (:default spec))]
              (if-let [save-fn (:save-fn spec)]
                (save-fn state tag nk)
                (when-let [writer (get nbt-writers (:type spec))]
                  (writer tag nk v))))))))))

;; ============================================================================
;; GUI sync payload
;; ============================================================================

(defn schema->sync-payload
  "Build a GUI sync payload map.

  Includes all fields where :gui-sync? is true, plus :pos-x/:pos-y/:pos-z
  derived from pos (a BlockPos-like object with .getX/.getY/.getZ)."
  [schema state pos]
  (validate-field-schema! schema)
  (-> (reduce
        (fn [m spec]
          (if (:gui-sync? spec)
            (assoc m (:key spec) (get state (:key spec) (:default spec)))
            m))
        {}
        schema)
            (assoc :pos-x (pos/pos-x pos)
              :pos-y (pos/pos-y pos)
              :pos-z (pos/pos-z pos))))

;; ============================================================================
;; Schema Organization Utilities
;; ============================================================================

(defn merge-field-definitions
  "Merge field definitions by :key, combining metadata from all groups.

  Enables organizing schema into conceptual layers (NBT, BlockState, GUI, etc.)
  where the same field can appear in multiple groups with different metadata.

  Example:
    (merge-field-definitions
      [[{:key :energy :persist? true :default 0.0}]
       [{:key :energy :block-state {:prop \"energy\" :type :integer}}]])
    => [{:key :energy :persist? true :default 0.0
         :block-state {:prop \"energy\" :type :integer}}]"
  [field-groups]
  (let [all-fields (apply concat field-groups)
        by-key (group-by #(get % :key) all-fields)]
    (validate-field-schema!
     (vec
      (for [[_k fields] by-key]
        (apply merge fields))))))

;; ============================================================================
;; BlockState Utilities (Nested Format)
;; ============================================================================

(defn extract-block-state-properties
  "Extract BlockState properties from nested :block-state format.

  Input: vector of field definitions with :block-state {:prop ... :type ...}
  Output: map of {property-keyword {:name :type :min :max :default}}

  Used for defblock :block-state-properties registration."
  [blockstate-fields]
  (into {}
    (for [field blockstate-fields
          :when (:block-state field)
          :let [bs (:block-state field)
                prop-name (:prop bs)]]
      [(keyword prop-name)
       {:name prop-name
        :type (:type bs)
        :min (:min bs)
        :max (:max bs)
        :default (:default bs)}])))

;; ── BlockState reduce step (top-level to prevent runtime class-gen explosion) ──
;; Extracted from build-block-state-updater so AOT compiles it to a single static
;; class.  Using partial at the call site passes the captured context explicitly,
;; avoiding the inner fn capturing outer closure vars which would force runtime JIT
;; class generation on every tick.

(defn- step-block-state-property
  "Apply a single BlockState property update.  All context is passed explicitly
  so this fn stays statically compilable (zero runtime class generation)."
  [state bs-state state-def pos bs spec]
  (let [prop-name (:prop (:block-state spec))
        raw-val   (get state (:key spec) (:default spec))
        xf-fn     (:xf (:block-state spec))
        val       (if xf-fn
                    (xf-fn raw-val state)
                    raw-val)
        prop      (when state-def
                    (platform-world/block-state-get-property bs-state state-def prop-name))]
    (when-not prop
      (log/warn "[blockstate-updater] property not found:" prop-name "for block at" pos))
    (if prop
      (platform-world/block-state-set-property bs prop val)
      bs)))

(defn build-block-state-updater
  "Generate BlockState updater function from nested :block-state format.

  Returns: (fn [state level pos] -> updates BlockState in world)

  Handles :xf transform function (can be symbol or fn).
  Replaces old schema->block-state-updater (flat format)."
  [blockstate-fields]
  (let [;; Pre-resolve symbol xf-fns at closure-creation time.
        bs-specs (mapv (fn [spec]
                         (if-let [xf (:xf (:block-state spec))]
                           (if (symbol? xf)
                             (let [ns-sym (symbol (namespace xf))]
                               (require ns-sym)
                               (assoc-in spec [:block-state :xf]
                                         (ns-resolve ns-sym (symbol (name xf)))))
                             spec)
                           spec))
                       (filterv #(get % :block-state) blockstate-fields))]
    (fn [state level pos]
      (try
        (if-let [blk-state (platform-world/get-block-state level pos)]
          (let [state-def (platform-world/block-state-get-state-definition blk-state)
                _ (when-not state-def
                    (log/warn "[blockstate-updater] state-def is nil for block at" pos))
                new-bs (reduce
                        (partial step-block-state-property state blk-state state-def pos)
                        blk-state
                        bs-specs)]
            (when (not= new-bs blk-state)
              (platform-world/set-block! level pos new-bs 3)))
          (log/warn "[blockstate-updater] world-get-block-state* returned nil for" pos))
        (catch Exception e
          (log/stacktrace (str "Failed to update block state for " pos) e))))))

;; ============================================================================
;; Network Handler Generation
;; ============================================================================

(defn build-network-handlers
  "Generate network message handlers from :network-editable? fields.

  Returns: map of {msg-keyword handler-fn}

  Each handler receives [payload player] and updates the BE field.
  Requires net-helpers namespace for get-world and get-tile-at."
  [network-editable-fields]
  (into {}
    (for [field network-editable-fields
          :when (:network-editable? field)
          :let [msg-key (:network-msg field)
                field-key (:key field)
                payload-key (:gui-payload-key field field-key)]]
      [msg-key
       (fn [payload player]
         (let [world (get-network-world player)
               tile (get-network-tile-at world payload)
               new-value (get payload payload-key)]
           (if (and tile new-value)
             (do
               (inv-helpers/update-be-field! tile field-key new-value)
               {:success true})
             {:success false})))])))


;; ============================================================================
;; Schema filtering utilities
;; ============================================================================

(defn filter-server-fields
  "Filter schema to only server-side fields (persist? true or no gui-only? flag).

  Used to extract fields that should be handled by server-side code (NBT persistence,
  BlockState updates, etc.). Excludes client-only GUI fields."
  [schema]
  (filterv #(and (not (:gui-only? %))
                 (or (:persist? %) (:block-state %)))
           schema))

(defn filter-gui-fields
  "Filter schema to only GUI-relevant fields (gui-sync? true or gui-only? true).

  Used to extract fields that should be present in GUI containers (client-side atoms)."
  [schema]
  (filterv #(or (:gui-sync? %) (:gui-only? %))
           schema))

(defn filter-by-tag
  "Filter schema to fields that have a specific tag key set to a truthy value.

  Example: (filter-by-tag schema :network-editable?) returns only network-editable fields."
  [schema tag-key]
  (filterv #(get % tag-key) schema))

