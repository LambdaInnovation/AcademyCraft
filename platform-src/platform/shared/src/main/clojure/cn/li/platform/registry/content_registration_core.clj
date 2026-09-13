(ns cn.li.platform.registry.content-registration-core
  "Loader-neutral content-registration traversal and entity-spec normalization.

  Loaders supply registration callbacks (DeferredRegister, Fabric Registry, etc.).
  This namespace must not import Minecraft, Forge, Fabric, or NeoForge APIs."
  (:require [cn.li.mcmod.entity.dsl :as edsl]
            [cn.li.platform.registry.metadata :as metadata]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Shared pure helpers
;; ---------------------------------------------------------------------------

(defn normalize-hook-params
  "Normalize hook-param map keys to strings for Java interop registries."
  [params]
  (reduce-kv (fn [m k v]
               (assoc m
                      (cond
                        (keyword? k) (name k)
                        (string? k) k
                        :else (str k))
                      v))
             {}
             (or params {})))

(defn registry-source-snapshot
  "Resolve a registry source that may be a map, thunk, or IDeref."
  [source]
  (cond
    (map? source) source
    (fn? source) (or (source) {})
    (instance? clojure.lang.IDeref source) (or @source {})
    :else {}))

(defn- kw-name
  [v default]
  (name (or v default)))

;; ---------------------------------------------------------------------------
;; Entity-spec normalization (pure maps; loaders apply Java registration)
;; ---------------------------------------------------------------------------

(defn projectile-spec-fields
  "Normalize :properties/:projectile into a flat registration map."
  [entity-spec]
  (let [projectile (get-in entity-spec [:properties :projectile])
        hooks (:hooks projectile)]
    {:default-item-id (str (or (:default-item-id projectile) ""))
     :gravity (double (or (:gravity projectile) 0.05))
     :damage (double (or (:damage projectile) 0.0))
     :pickup-distance-sqr (double (or (:pickup-distance-sqr projectile) 2.25))
     :drop-item-on-discard? (not (false? (:drop-item-on-discard? projectile)))
     :on-hit-block (kw-name (:on-hit-block hooks) :none)
     :on-hit-entity (kw-name (:on-hit-entity hooks) :none)
     :on-anchored-tick (kw-name (:on-anchored-tick hooks) :none)
     :on-anchored-hurt (kw-name (:on-anchored-hurt hooks) :none)}))

(defn effect-spec-fields
  "Normalize :properties/:effect. `:renderer-id` is raw; loaders may override."
  [entity-spec]
  (let [effect (get-in entity-spec [:properties :effect])]
    {:life-ticks (int (or (:life-ticks effect) 15))
     :follow-owner? (not (false? (:follow-owner? effect)))
     :renderer-id (str (or (:renderer-id effect) "effect-billboard"))
     :hook (kw-name (:hook effect) :none)
     :hook-params (normalize-hook-params (:hook-params effect))}))

(defn ray-spec-fields
  "Normalize :properties/:ray. Numeric defaults match Forge/NeoForge;
  Fabric may override glow/color/renderer at the call site."
  [entity-spec]
  (let [ray (get-in entity-spec [:properties :ray])]
    {:life-ticks (int (or (:life-ticks ray) 30))
     :length (double (or (:length ray) 15.0))
     :blend-in-ms (double (or (:blend-in-ms ray) 100.0))
     :blend-out-ms (double (or (:blend-out-ms ray) 300.0))
     :inner-width (double (or (:inner-width ray) 0.03))
     :outer-width (double (or (:outer-width ray) 0.045))
     :glow-width (double (or (:glow-width ray) 0.3))
     :start-color (int (or (:start-color ray) 0x78DCFF))
     :end-color (int (or (:end-color ray) 0x32AAFF))
     :renderer-id (str (or (:renderer-id ray) "ray-composite"))
     :hook (kw-name (:hook ray) :none)
     :hook-params (normalize-hook-params (:hook-params ray))}))

(defn marker-spec-fields
  "Normalize :properties/:marker."
  [entity-spec]
  (let [marker (get-in entity-spec [:properties :marker])]
    {:life-ticks (int (or (:life-ticks marker) 40))
     :follow-target? (not (false? (:follow-target? marker)))
     :ignore-depth? (not (false? (:ignore-depth? marker)))
     :available? (not (false? (:available? marker)))
     :renderer-id (str (or (:renderer-id marker) "marker-billboard"))
     :hook (kw-name (:hook marker) :none)}))

(defn block-body-spec-fields
  "Normalize :properties/:block-body."
  [entity-spec]
  (let [block-body (get-in entity-spec [:properties :block-body])]
    {:default-block-id (str (or (:default-block-id block-body) "minecraft:stone"))
     :gravity (double (or (:gravity block-body) 0.05))
     :damage (double (or (:damage block-body) 0.0))
     :place-when-collide? (not (false? (:place-when-collide? block-body)))
     :renderer-id (str (or (:renderer-id block-body) "block-body"))
     :hook (kw-name (:hook block-body) :none)
     :behavior (kw-name (:behavior block-body) :none)
     :drag (double (or (:drag block-body) 1.0))}))

(defn entity-kind-fields
  "Return normalized fields for a scripted entity kind, or nil."
  [entity-kind entity-spec]
  (case entity-kind
    :scripted-projectile (projectile-spec-fields entity-spec)
    :scripted-effect (effect-spec-fields entity-spec)
    :scripted-ray (ray-spec-fields entity-spec)
    :scripted-marker (marker-spec-fields entity-spec)
    :scripted-block-body (block-body-spec-fields entity-spec)
    :scripted-mob nil
    nil))

;; ---------------------------------------------------------------------------
;; Registration descriptors (metadata → pure plans)
;; ---------------------------------------------------------------------------

(defn block-plan
  "Build a registration plan map for one block-id."
  [block-id]
  (let [has-be? (boolean (metadata/has-block-entity? block-id))
        fluid-id (metadata/get-fluid-id-for-block block-id)
        block-spec (metadata/get-block-spec block-id)]
    {:block-id block-id
     :registry-name (metadata/get-block-registry-name block-id)
     :physical (:physical block-spec)
     :fluid-id fluid-id
     :fluid-block? (boolean (metadata/fluid-block? block-id))
     ;; A FluidType's luminosity only drives fog/entity lighting; the block has
     ;; to emit separately, so loaders need it when building LiquidBlock props.
     :fluid-luminosity (or (when fluid-id
                             (get-in (metadata/get-fluid-spec fluid-id)
                                     [:physical :luminosity]))
                           0)
     :needs-dynamic-properties? (boolean (metadata/has-block-state-properties? block-id))
     :has-be? has-be?
     :tile-id (when has-be? (metadata/get-block-tile-id block-id))
     :should-create-block-item? (boolean (metadata/should-create-block-item? block-id))}))

(defn fluid-plan
  "Build a registration plan map for one fluid-id."
  [fluid-id]
  (let [fluid-spec (metadata/get-fluid-spec fluid-id)
        registry-name (metadata/get-fluid-registry-name fluid-id)]
    {:fluid-id fluid-id
     :registry-name registry-name
     :flowing-name (str registry-name "_flowing")
     :physical (:physical fluid-spec)
     :rendering (:rendering fluid-spec)
     :behavior (:behavior fluid-spec)
     :block-spec (:block fluid-spec)}))

(defn tile-plan
  "Build a registration plan map for one tile-id."
  [tile-id]
  {:tile-id tile-id
   :registry-name (metadata/get-tile-registry-name tile-id)
   :block-ids (metadata/get-tile-block-ids tile-id)})

(defn item-plan
  "Build a registration plan map for one item-id."
  [item-id]
  {:item-id item-id
   :registry-name (metadata/get-item-registry-name item-id)
   :item-spec (metadata/get-item-spec item-id)})

(defn sound-plan
  "Build a registration plan map for one sound-id."
  [sound-id]
  {:sound-id sound-id
   :registry-name (metadata/get-sound-registry-name sound-id)})

(defn effect-plan
  "Build a registration plan map for one effect-id."
  [effect-id]
  (let [effect-spec (metadata/get-effect-spec effect-id)]
    {:effect-id effect-id
     :registry-name (metadata/get-effect-registry-name effect-id)
     :category (:category effect-spec)
     :color (int (or (:color effect-spec) 0xAA0000))
     :tick-interval (int (or (:tick-interval effect-spec) 20))
     :damage-per-tick (float (or (:damage-per-tick effect-spec) 0.0))}))

(defn particle-plan
  "Build a registration plan map for one particle-id."
  [particle-id]
  (let [particle-spec (metadata/get-particle-spec particle-id)]
    {:particle-id particle-id
     :registry-name (metadata/get-particle-registry-name particle-id)
     :always-show? (boolean (:always-show? particle-spec))}))

(defn entity-plan
  "Build a registration plan map for one entity-id."
  [entity-id]
  (let [entity-spec (edsl/get-entity entity-id)
        entity-kind (:entity-kind entity-spec)
        registry-name (edsl/get-entity-registry-name entity-id)]
    {:entity-id entity-id
     :registry-name registry-name
     :entity-kind entity-kind
     :entity-spec entity-spec
     :category (name (or (:category entity-spec) :misc))
     :width (:width entity-spec)
     :height (:height entity-spec)
     :client-tracking-range (:client-tracking-range entity-spec)
     :update-interval (:update-interval entity-spec)
     :fire-immune? (:fire-immune? entity-spec)
     :kind-fields (when entity-kind (entity-kind-fields entity-kind entity-spec))}))

(defn should-register-block-item?
  "Shared BlockItem gate used by Forge/Fabric/NeoForge."
  [{:keys [should-create-block-item? fluid-block? has-be?]}]
  (and should-create-block-item?
       (or (not fluid-block?)
           has-be?)))

;; ---------------------------------------------------------------------------
;; Traversal (callback-driven)
;; ---------------------------------------------------------------------------

(defn for-each-fluid-plan!
  "Call `f` with each fluid-plan."
  [f]
  (doseq [fluid-id (or (metadata/get-all-fluid-ids) [])]
    (f (fluid-plan fluid-id))))

(defn for-each-block-plan!
  "Call `f` with each block-plan."
  [f]
  (doseq [block-id (or (metadata/get-all-block-ids) [])]
    (f (block-plan block-id))))

(defn for-each-tile-plan!
  "Call `f` with each tile-plan."
  [f]
  (doseq [tile-id (or (metadata/get-all-tile-ids) [])]
    (f (tile-plan tile-id))))

(defn for-each-item-plan!
  "Call `f` with each standalone item-plan."
  [f]
  (doseq [item-id (or (metadata/get-all-item-ids) [])]
    (f (item-plan item-id))))

(defn for-each-sound-plan!
  "Call `f` with each sound-plan."
  [f]
  (doseq [sound-id (or (metadata/get-all-sound-ids) [])]
    (f (sound-plan sound-id))))

(defn for-each-effect-plan!
  "Call `f` with each effect-plan."
  [f]
  (doseq [effect-id (or (metadata/get-all-effect-ids) [])]
    (f (effect-plan effect-id))))

(defn for-each-particle-plan!
  "Call `f` with each particle-plan."
  [f]
  (doseq [particle-id (or (metadata/get-all-particle-ids) [])]
    (f (particle-plan particle-id))))

(defn for-each-entity-plan!
  "Call `f` with each entity-plan. Logs and skips entities missing :entity-kind."
  [f]
  (doseq [entity-id (edsl/list-entities)]
    (let [plan (entity-plan entity-id)]
      (if (nil? (:entity-kind plan))
        (log/error "Skipping entity registration: missing :entity-kind"
                   {:entity-id entity-id})
        (f plan)))))
