(ns cn.li.ac.block.blockstate-datagen
  "Generate multipart blockstate datagen metadata from machine schemas.

  Runtime block specs read :block-state-properties from schema-runtime; this
  namespace mirrors those fields into BlockStateDefinition parts for JSON datagen."
  (:require [cn.li.ac.block.ability-interferer.schema :as interferer-schema]
            [cn.li.ac.block.imag-fusor.schema :as fusor-schema]
            [cn.li.ac.block.metal-former.schema :as former-schema]
            [cn.li.mcmod.block.state-schema :as state-schema]
            [cn.li.mcmod.config :as mcmod-config]))

(def ^:private horizontal-facings
  ["north" "east" "south" "west"])

(defn- mod-id []
  mcmod-config/mod-id)

(defn- blockstate-fields
  [schema]
  (filterv #(get % :block-state) schema))

(defn- properties-from-schema
  [schema]
  (state-schema/extract-block-state-properties (blockstate-fields schema)))

(defn- property-domain
  "Values to enumerate for datagen multipart conditions."
  [{:keys [type min max default] :as _prop}]
  (case type
    :boolean [false true]
    :horizontal-facing horizontal-facings
    :integer (vec (range (int (or min 0)) (inc (int (or max default 0)))))
    :else [default]))

(defn- property-value-maps
  "Eager single-property condition maps for one key (avoids nested lazy iters)."
  [properties prop-key]
  (mapv (fn [v] {prop-key v})
        (property-domain (get properties prop-key))))

(defn- merge-domain
  "Cartesian-extend partials with one property domain. Eager — no nested `for`/`lazy-seq`."
  [partials domain]
  (persistent!
   (reduce (fn [out partial]
             (reduce (fn [out' value]
                       (conj! out' (merge partial value)))
                     out
                     domain))
           (transient [])
           partials)))

(defn- cartesian-conditions
  "Build condition maps for every combination of `prop-keys` domains.

  Uses named eager helpers instead of nested `for` inside `reduce`, so the
  Clojure compiler does not emit deeply concatenated `$iter$fn$iter` class names
  (Windows MAX_PATH / Forge class-scan / stacktrace pain)."
  [properties prop-keys]
  (reduce merge-domain
          [{}]
          (mapv #(property-value-maps properties %) prop-keys)))

(defn- model-id
  [model-name]
  (str (mod-id) ":block/" model-name))

;; ============================================================================
;; Imag Fusor
;; ============================================================================

(defn imag-fusor-model-name
  [facing frame]
  (str "imag_fusor_" facing "_frame_" (int frame)))

(defn- imag-fusor-front-texture
  [frame]
  (if (zero? (int frame))
    (str (mod-id) ":block/ief_off")
    (str (mod-id) ":block/ief_working_" (int frame))))

(defn- imag-fusor-parts
  [properties]
  (mapv (fn [{:keys [facing frame]}]
          {:condition {:facing facing :frame frame}
           :models [(model-id (imag-fusor-model-name facing frame))]})
        (cartesian-conditions properties [:facing :frame])))

(defn- imag-fusor-definition
  []
  (let [schema fusor-schema/imag-fusor-schema
        properties (properties-from-schema schema)]
    {:registry-name "imag_fusor"
     :properties properties
     :parts (imag-fusor-parts properties)}))

;; ============================================================================
;; Metal Former
;; ============================================================================

(defn metal-former-model-name
  [facing]
  (str "metal_former_" facing))

(defn- metal-former-parts
  [properties]
  (mapv (fn [{:keys [facing]}]
          {:condition {:facing facing}
           :models [(model-id (metal-former-model-name facing))]})
        (cartesian-conditions properties [:facing])))

(defn- metal-former-definition
  []
  (let [schema former-schema/metal-former-schema
        properties (properties-from-schema schema)]
    {:registry-name "metal_former"
     :properties properties
     :parts (metal-former-parts properties)}))

;; ============================================================================
;; Ability Interferer
;; ============================================================================

(defn- ability-interferer-parts
  []
  [{:condition {:on true}
    :models [(model-id "ability_interferer")]}
   {:condition {:on false}
    :models [(model-id "ability_interf_off")]}])

(defn- ability-interferer-definition
  []
  (let [schema interferer-schema/ability-interferer-schema
        properties (properties-from-schema schema)]
    {:registry-name "ability_interferer"
     :properties properties
     :parts (ability-interferer-parts)}))

;; ============================================================================
;; Public API
;; ============================================================================

(def ^:private complex-block-specs
  [{:block-key :ability-interferer
    :definition-fn ability-interferer-definition}
   {:block-key :imag-fusor
    :definition-fn imag-fusor-definition}
   {:block-key :metal-former
    :definition-fn metal-former-definition}])

(defn complex-block-keys
  []
  (set (map #(get % :block-key) complex-block-specs)))

(defn complex-definitions-map
  "Map of block-key -> plain definition map (registry-name, properties, parts)."
  []
  (into {}
        (map (fn [{:keys [block-key definition-fn]}]
               [block-key (definition-fn)])
             complex-block-specs)))

(defn parse-imag-fusor-model-name
  [model-name]
  (when-let [[_ facing frame] (re-matches #"imag_fusor_(north|east|south|west)_frame_([0-4])" model-name)]
    {:facing facing
     :frame (Integer/parseInt frame)}))

(defn parse-metal-former-model-name
  [model-name]
  (when-let [[_ facing] (re-matches #"metal_former_(north|east|south|west)" model-name)]
    {:facing facing}))

(defn get-model-cube-texture-config
  [model-name]
  (or
    (when-let [{:keys [facing frame]} (parse-imag-fusor-model-name model-name)]
      (let [front (imag-fusor-front-texture frame)
            side (str (mod-id) ":block/machine_side")
            top (str (mod-id) ":block/machine_top")
            down (str (mod-id) ":block/machine_bottom")
            sides (case facing
                    "north" {:north front :south side :east side :west side}
                    "south" {:north side :south front :east side :west side}
                    "east" {:north side :south side :east front :west side}
                    "west" {:north side :south side :east side :west front}
                    {:north front :south side :east side :west side})]
        (merge {:down down :up top} sides)))
    (when-let [{:keys [facing]} (parse-metal-former-model-name model-name)]
      (let [front (str (mod-id) ":block/metal_former_front")
            back (str (mod-id) ":block/metal_former_back")
            right (str (mod-id) ":block/metal_former_right")
            left (str (mod-id) ":block/metal_former_left")
            top (str (mod-id) ":block/metal_former_top")
            down (str (mod-id) ":block/metal_former_bottom")
            sides (case facing
                    "north" {:north front :south back :east right :west left}
                    "south" {:north back :south front :east left :west right}
                    "east" {:north left :south right :east front :west back}
                    "west" {:north right :south left :east back :west front}
                    {:north front :south back :east right :west left})]
        (merge {:down down :up top} sides)))
    ;; Ability Interferer: ON state uses ability_interf_on texture on all faces
    (when (= model-name "ability_interferer")
      (let [tex (str (mod-id) ":block/ability_interf_on")]
        {:down tex :up tex :north tex :south tex :east tex :west tex}))))

(defn default-item-model-id
  "Default item model for complex blocks that use variant geometry."
  [registry-name]
  (case registry-name
    "imag_fusor" (model-id (imag-fusor-model-name "north" 0))
    nil))
