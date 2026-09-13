(ns cn.li.mcmod.nbt.dsl
  "NBT DSL - Declarative NBT serialization using Clojure macros
  
  Provides a clean, declarative way to define NBT read/write operations
  for tile entities and other game objects."
  (:require [clojure.string :as str]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.schema.core :as schema-core]
            [cn.li.mcmod.platform.structured-data :as sd]))

(declare validate-nbt-field-spec validate-world-list-spec)

;; ============================================================================
;; Type Converters
;; ============================================================================

(def type-converters
  "Map of field types to their read/write converters"
  {:double
   {:write (fn [nbt key value]
             (sd/set-double! nbt key value))
    :read (fn [nbt key]
            (sd/get-double nbt key))
    :has-key? (fn [nbt key]
                (sd/has-key-safe? nbt key))}

   :string
   {:write (fn [nbt key value]
             (sd/set-string! nbt key value))
    :read (fn [nbt key]
            (sd/get-string nbt key))
    :has-key? (fn [nbt key]
                (sd/has-key-safe? nbt key))}

   :int
   {:write (fn [nbt key value]
             (sd/set-int! nbt key value))
    :read (fn [nbt key]
            (sd/get-int nbt key))
    :has-key? (fn [nbt key]
                (sd/has-key-safe? nbt key))}

   :boolean
   {:write (fn [nbt key value]
             (sd/set-boolean! nbt key value))
    :read (fn [nbt key]
            (sd/get-boolean nbt key))
    :has-key? (fn [nbt key]
                (sd/has-key-safe? nbt key))}

   :float
   {:write (fn [nbt key value]
             (sd/set-float! nbt key value))
    :read (fn [nbt key]
            (sd/get-float nbt key))
    :has-key? (fn [nbt key]
                (sd/has-key-safe? nbt key))}

   :long
   {:write (fn [nbt key value]
             (sd/set-long! nbt key value))
    :read (fn [nbt key]
            (sd/get-long nbt key))
    :has-key? (fn [nbt key]
                (sd/has-key-safe? nbt key))}

     :keyword
     {:write (fn [nbt key value]
         (sd/set-string! nbt key (name value)))
      :read (fn [nbt key]
        (keyword (sd/get-string nbt key)))
      :has-key? (fn [nbt key]
      (sd/has-key-safe? nbt key))}

     :atom
     {:write (fn [_nbt key _value]
         ;; Atoms need to be dereferenced before writing
         ;; The actual type is handled by a nested converter
         (throw (ex-info "Atom type needs explicit sub-type" {:key key})))
      :read (fn [_nbt key]
        (throw (ex-info "Atom type needs explicit sub-type" {:key key})))}

     :string-list
     {:write (fn [nbt key value]
         (sd/set-string-list! nbt key (vec (or value []))))
      :read (fn [nbt key]
        (sd/get-string-list nbt key))
      :has-key? (fn [nbt key]
        (sd/has-key-safe? nbt key))}})

;; ============================================================================
;; Field Accessor Helpers
;; ============================================================================

(defn get-field-value
  "Get value from tile entity field, handling atoms and direct values"
  [tile field-path atom?]
  (let [value (get-in tile field-path)]
    (if atom?
      @value
      value)))

(defn set-field-value!
  "Set value in tile entity field, handling atoms and direct values"
  [tile field-path value atom?]
  (if atom?
    (reset! (get-in tile field-path) value)
    (assoc-in tile field-path value)))

(defn rebuild-item-lookups!
  "Rebuild lookup atom entries for a loaded item using a config map.

  Config keys:
  - :lookup-atom (keyword on world-data pointing to an atom map)
  - :direct-keys (vector of item keys mapped directly)
  - :collection-keys (vector of item keys whose deref'ed collections are mapped)"
  [world-data item config]
  (let [lookup-atom (get world-data (:lookup-atom config))]
    (when lookup-atom
      (doseq [k (:direct-keys config)]
        (when-let [v (get item k)]
          (swap! lookup-atom assoc v item)))
      (doseq [k (:collection-keys config)]
        (when-let [coll (get item k)]
          (doseq [val @coll]
            (swap! lookup-atom assoc val item)))))))

;; ============================================================================
;; NBT Field Specification
;; ============================================================================

(defrecord NbtFieldSpec
  [field-key       ; Keyword - Clojure field name in record (e.g., :energy)
   nbt-key         ; String - NBT key name (e.g., "energy")
   type            ; Keyword - field type (:double, :string, :int, etc.)
   atom?           ; Boolean - is field an atom?
   default         ; Any - default value if not present in NBT
   getter          ; Function - getter function (tile) -> value (optional)
   setter          ; Function - setter function (tile value) -> tile (optional)
   custom-write    ; Function - custom write function (optional)
   custom-read     ; Function - custom read function (optional)
   skip-on-write?  ; Function - predicate to skip writing (optional)
   transform-write ; Function - transform value before writing (optional)
   transform-read  ; Function - transform value after reading (optional)
   ])

;; ============================================================================
;; Core DSL Functions
;; ============================================================================

(defn write-nbt-field
  "Write a single field to NBT according to its specification"
  [tile nbt field-spec]
  (let [{:keys [field-key nbt-key type atom? getter custom-write skip-on-write? transform-write]} field-spec]
    (when-not (and skip-on-write? (skip-on-write? tile))
      (let [value (if getter
                    (getter tile)
                    (get-field-value tile [field-key] atom?))
            value (if transform-write (transform-write value) value)]
        (if custom-write
          (custom-write tile nbt nbt-key value)
          (when-let [converter (get type-converters type)]
            ((:write converter) nbt nbt-key value)))))))

(defn read-nbt-field
  "Read a single field from NBT according to its specification"
  [tile nbt field-spec]
  (let [{:keys [field-key nbt-key type atom? setter custom-read default transform-read]} field-spec
        has-key? (if-let [converter (get type-converters type)]
                   ((:has-key? converter) nbt nbt-key)
                   (sd/has-key-safe? nbt nbt-key))]
    (when has-key?
      (let [value (if custom-read
                    (custom-read tile nbt nbt-key)
                    (when-let [converter (get type-converters type)]
                      ((:read converter) nbt nbt-key)))
            value (if transform-read (transform-read value) value)
            value (or value default)]
        (if setter
          (setter tile value)
          (set-field-value! tile [field-key] value atom?))))))

(defn write-nbt-fields
  "Write all fields to NBT according to specifications"
  [tile nbt field-specs]
  (doseq [field-spec field-specs]
    (write-nbt-field tile nbt field-spec))
  nbt)

(defn read-nbt-fields
  "Read all fields from NBT according to specifications"
  [tile nbt field-specs]
  (doseq [field-spec field-specs]
    (read-nbt-field tile nbt field-spec))
  tile)

;; =========================================================================
;; List Helpers for World Data
;; =========================================================================

(defn write-nbt-list-field
  "Write a list of items to NBT under a tag
  
  - nbt: INBTCompound
  - tag: String tag name
  - items: collection of items
  - to-nbt-fn: (fn [item] -> INBTCompound)
  - skip?: optional predicate (fn [item] -> bool)"
  [nbt tag items to-nbt-fn skip?]
  (let [list (sd/create-list)]
    (doseq [item items]
      (when-not (and skip? (skip? item))
        (sd/append! list (to-nbt-fn item))))
    (sd/set-entry! nbt tag list))
  nbt)

(defn read-nbt-list-field!
  "Read a list of items from NBT and append to a collection atom
  
  - nbt: INBTCompound
  - tag: String tag name
  - world-data: world data container passed to from-nbt-fn
  - collection-atom: atom to append items to
  - from-nbt-fn: (fn [world-data item-nbt] -> item)
  - rebuild-fn: optional (fn [world-data item] -> void)"
  [nbt tag world-data collection-atom from-nbt-fn rebuild-fn]
  (when (sd/has-key-safe? nbt tag)
    (let [list (sd/get-list nbt tag)
          size (sd/list-size list)]
      (dotimes [i size]
        (let [item-nbt (sd/list-structured list i)
              item (from-nbt-fn world-data item-nbt)]
          (swap! collection-atom conj item)
          (when rebuild-fn
            (rebuild-fn world-data item)))))))

;; ============================================================================
;; Macro: defnbt
;; ============================================================================

(defmacro defnbt
  "Define NBT serialization for a tile entity or record.
  
  Generates two functions:
  - write-{name}-to-nbt [tile nbt] -> nbt
  - read-{name}-from-nbt [tile nbt] -> tile
  
  Usage:
    (defnbt node
      [:energy \"energy\" :double]
      [:node-name \"nodeName\" :string]
      [:password \"password\" :string]
      [:placer-name \"placer\" :string]
      [:inventory \"inventory\" :inventory])
  
  Field specification format:
    [field-key nbt-key type & options]
  
  Options:
    :atom? true         - Field is an atom (default: false)
    :default value      - Default value if not in NBT
    :getter fn          - Getter function (tile) -> value
    :setter fn          - Setter function (tile value) -> tile
    :custom-write fn    - Custom write function (tile nbt key value) -> void
    :custom-read fn     - Custom read function (tile nbt key) -> value
    :skip-on-write? fn  - Predicate (tile) -> bool, skip if true
    :transform-write fn - Transform value before writing
    :transform-read fn  - Transform value after reading
  
  Advanced usage with options:
    (defnbt matrix
      [:placer-name \"placer\" :string]
      [:plate-count \"plateCount\" :int :atom? true]
      [:sub-id \"subId\" :int]
      [:direction \"direction\" :keyword]
      [:inventory \"inventory\" :inventory])"
  [name & field-specs]
  (let [name-str (clojure.core/name name)
        write-fn-name (symbol (str "write-" name-str "-to-nbt"))
        read-fn-name (symbol (str "read-" name-str "-from-nbt"))
        
        ;; Parse field specifications
        parsed-specs (mapv (fn [spec]
                             (let [[field-key nbt-key type & options] spec
                                   options-map (apply hash-map options)]
                               (map->NbtFieldSpec
                                 {:field-key field-key
                                  :nbt-key nbt-key
                                  :type type
                                  :atom? (get options-map :atom? false)
                                  :default (get options-map :default)
                                  :getter (get options-map :getter)
                                  :setter (get options-map :setter)
                                  :custom-write (get options-map :custom-write)
                                  :custom-read (get options-map :custom-read)
                                  :skip-on-write? (get options-map :skip-on-write?)
                                  :transform-write (get options-map :transform-write)
                                  :transform-read (get options-map :transform-read)})))
                           field-specs)]
    (doseq [spec parsed-specs]
      (validate-nbt-field-spec spec))
    
    `(do
       ;; Generate write function
       (defn ~write-fn-name
         ~(str "Save " name-str " to NBT\n\n"
               "Auto-generated by defnbt macro.\n\n"
               "Saves:\n"
               (str/join "\n" 
                 (map #(str "- " (clojure.core/name (:field-key %))) parsed-specs)))
         [~'tile ~'nbt]
         (write-nbt-fields ~'tile ~'nbt ~parsed-specs)
         ~'nbt)
       
       ;; Generate read function
       (defn ~read-fn-name
         ~(str "Load " name-str " from NBT\n\n"
               "Auto-generated by defnbt macro.\n\n"
               "Restores:\n"
               (str/join "\n"
                 (map #(str "- " (clojure.core/name (:field-key %))) parsed-specs)))
         [~'tile ~'nbt]
         (read-nbt-fields ~'tile ~'nbt ~parsed-specs)
         ~'tile)
       
       ;; Log registration
       (log/debug ~(str "Registered NBT serialization for: " name-str)))))

;; ============================================================================
;; Macro: defworldnbt
;; ============================================================================

(defmacro defworldnbt
  "Define NBT serialization for world data using list specs.
  
  Options:
    :create     - function to create world-data (fn [world] -> data)
    :lists      - vector of list specs
    :write-name - optional name for write function (default: {name}-to-nbt)
    :read-name  - optional name for read function (default: {name}-from-nbt)
    :after-read - optional seq of forms executed after lists are read
  
  List spec keys:
    :tag        - NBT tag name (string)
    :atom       - keyword for collection atom in world-data
    :to-nbt     - item serializer (fn [item] -> NBTTagCompound)
    :from-nbt   - item deserializer (fn [world-data item-nbt] -> item)
    :skip?      - optional predicate to skip writing an item
    :rebuild    - optional lookup rebuild config passed to caller-supplied rebuild function
  
  Example:
    (defworldnbt world-data
      :create create-world-data
      :lists [{:tag \"items\"
               :atom :items
               :to-nbt cn.li.mcmod.data/item-to-nbt
               :from-nbt cn.li.mcmod.data/item-from-nbt
               :skip? (fn [item] (:deleted item))
               :rebuild {:lookup-atom :item-lookup
                         :direct-keys [:id :name]
                         :collection-keys [:children]}}])"
  [name & options]
  (let [opts (apply hash-map options)
        create-fn (:create opts)
        lists-expr (:lists opts)
        after-read (:after-read opts)
        after-read-forms (cond
                           (nil? after-read) []
                           (and (sequential? after-read) (not (map? after-read))) after-read
                           :else [after-read])
        name-str (clojure.core/name name)
        write-fn-name (or (:write-name opts) (symbol (str name-str "-to-nbt")))
        read-fn-name (or (:read-name opts) (symbol (str name-str "-from-nbt")))]
    (when (vector? lists-expr)
      (doseq [list-spec lists-expr]
        (validate-world-list-spec list-spec)))
    
    `(do
       (defn ~write-fn-name
         ~(str "Serialize " name-str " to NBT\n\n"
               "Auto-generated by defworldnbt macro.")
         [~'world-data]
         (let [~'nbt (sd/create-structured)]
           (doseq [~'spec ~lists-expr]
             (let [{:keys [~'tag ~'atom ~'to-nbt ~'skip?]} ~'spec]
               (write-nbt-list-field ~'nbt ~'tag @(~'atom ~'world-data) ~'to-nbt ~'skip?)))
           ~'nbt))
       
       (defn ~read-fn-name
         ~(str "Deserialize " name-str " from NBT\n\n"
               "Auto-generated by defworldnbt macro.")
         [~'world ~'nbt]
         (let [~'world-data (~create-fn ~'world)]
           (doseq [~'spec ~lists-expr]
             (let [{:keys [~'tag ~'atom ~'from-nbt ~'rebuild]} ~'spec
                   ~'rebuild-fn (when ~'rebuild
                                  (fn [~'item]
                                    (rebuild-item-lookups! ~'world-data ~'item ~'rebuild)))]
               (read-nbt-list-field! ~'nbt ~'tag ~'world-data (~'atom ~'world-data) ~'from-nbt ~'rebuild-fn)))
           ~@after-read-forms
           ~'world-data)))))

;; ============================================================================
;; Convenience Macros for Common Patterns
;; ============================================================================

(defmacro defnbt-simple
  "Simplified defnbt for common case: all non-atom fields
  
  Usage:
    (defnbt-simple player
      :name \"playerName\" :string
      :health \"health\" :double
      :level \"level\" :int)"
  [name & field-specs]
  (let [grouped (partition 3 field-specs)
        expanded (mapv (fn [[field-key nbt-key type]]
                         [field-key nbt-key type])
                       grouped)]
    `(defnbt ~name ~@expanded)))

;; ============================================================================
;; Helper Functions for Complex Types
;; ============================================================================

(defn write-atom-int
  "Write an atom containing an integer"
  [_tile nbt nbt-key atom-value]
  (sd/set-int! nbt nbt-key @atom-value))

(defn read-atom-int
  "Read an integer into an atom"
  [tile nbt nbt-key]
  (when (sd/has-key-safe? nbt nbt-key)
    (reset! (get-in tile [nbt-key]) (sd/get-int nbt nbt-key))))

(defn write-atom-double
  "Write an atom containing a double"
  [_tile nbt nbt-key atom-value]
  (sd/set-double! nbt nbt-key @atom-value))

(defn read-atom-double
  "Read a double into an atom"
  [tile nbt nbt-key]
  (when (sd/has-key-safe? nbt nbt-key)
    (reset! (get-in tile [nbt-key]) (sd/get-double nbt nbt-key))))

(defn write-atom-boolean
  "Write an atom containing a boolean"
  [_tile nbt nbt-key atom-value]
  (sd/set-boolean! nbt nbt-key @atom-value))

(defn read-atom-boolean
  "Read a boolean into an atom"
  [tile nbt nbt-key]
  (when (sd/has-key-safe? nbt nbt-key)
    (reset! (get-in tile [nbt-key]) (sd/get-boolean nbt nbt-key))))

;; ============================================================================
;; Validation
;; ============================================================================

(defn- ^:private nbt-non-empty-str?
  "Named predicate for Malli schema — avoids anon-fn AOT symbol leak."
  [s]
  (pos? (count s)))

(def ^:private nbt-field-spec-schema
  [:map
   [:field-key keyword?]
   [:nbt-key [:and string? [:fn nbt-non-empty-str?]]]
   [:type keyword?]
   [:atom? {:optional true} boolean?]
   [:default {:optional true} any?]
   [:getter {:optional true} fn?]
   [:setter {:optional true} fn?]
   [:custom-write {:optional true} fn?]
   [:custom-read {:optional true} fn?]
   [:skip-on-write? {:optional true} fn?]
   [:transform-write {:optional true} fn?]
   [:transform-read {:optional true} fn?]])

;; Validators use schema-core/lazy-validator to avoid calling schema/validator
;; (and therefore Malli) during namespace loading / AOT compilation.
;; NOTE: `delay` is deliberately NOT used — see `schema/lazy-validator`.

(def ^:private valid-nbt-field-spec*
  (schema-core/lazy-validator nbt-field-spec-schema))

(def ^:private world-list-spec-schema
  [:map
   [:tag [:and string? [:fn nbt-non-empty-str?]]]
   [:atom keyword?]
   [:to-nbt fn?]
   [:from-nbt fn?]
   [:skip? {:optional true} fn?]
   [:rebuild {:optional true}
    [:map
     [:lookup-atom keyword?]
     [:direct-keys {:optional true} [:sequential keyword?]]
     [:collection-keys {:optional true} [:sequential keyword?]]]]])

(def ^:private valid-world-list-spec*
  (schema-core/lazy-validator world-list-spec-schema))

(defn validate-nbt-field-spec
  "Validate NBT field specification"
  [spec]
  (schema-core/require-valid nbt-field-spec-schema (valid-nbt-field-spec*) :nbt-field-spec spec)
  (when-not (get type-converters (:type spec))
    (when-not (or (:custom-write spec) (:custom-read spec))
      (throw (ex-info "Unknown NBT type and no custom converters"
                      {:type (:type spec)
                       :available-types (keys type-converters)}))))
  true)

(defn validate-world-list-spec
  "Validate a defworldnbt list spec at macro/load-time."
  [spec]
  (schema-core/require-valid world-list-spec-schema (valid-world-list-spec*) :world-nbt-list-spec spec)
  true)

