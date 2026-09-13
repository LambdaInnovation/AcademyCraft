(ns cn.li.mcmod.fluid.dsl
  "Platform-neutral fluid DSL metadata registry.

  Fluids are declared as pure data in mcmod; platform adapters (Forge/Fabric)
  translate this metadata into concrete runtime objects."
  (:require [clojure.string :as str]
            [cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]))





(defrecord FluidPhysicalProperties
  [luminosity density viscosity temperature can-convert-to-source supports-boat])

(defrecord FluidRenderingProperties
  [still-texture flowing-texture overlay-texture tint-color is-translucent])

(defrecord FluidBehaviorProperties
  [slope-find-distance level-decrease-per-block tick-rate explosion-resistance])

(defrecord FluidBlockProperties
  [block-id registry-name has-bucket? bucket-registry-name bucket-item-id])

(defrecord FluidSpec
  [id
   registry-name
   physical
   rendering
   behavior
   block
   metadata])

(defn- default-registry-name
  [id]
  (str/replace (str id) #"-" "_"))

(defn- normalize-hex-color
  [v]
  (cond
    (integer? v) (unchecked-int v)
    (string? v) (Integer/parseUnsignedInt (str/replace v #"^#" "") 16)
    :else -1))

(defn create-fluid-spec
  "Create a normalized FluidSpec map.

  Required keys:
  - id (string): DSL id, e.g. \"content-fluid\"

  Optional keys:
  - registry-name
  - physical, rendering, behavior, block, metadata"
  [id {:keys [registry-name physical rendering behavior block metadata]}]
  (let [rid (str id)
        reg-name (or registry-name (default-registry-name rid))
        block-id (or (:block-id block) rid)
        bucket-registry (or (:bucket-registry-name block) (str reg-name "_bucket"))
        bucket-item-id (or (:bucket-item-id block) (str rid "-bucket"))]
    (->FluidSpec
      rid
      reg-name
      (map->FluidPhysicalProperties
        (merge {:luminosity 0
                :density 1000
                :viscosity 1000
                :temperature 300
                :can-convert-to-source false
                :supports-boat true}
               physical))
      (map->FluidRenderingProperties
        (merge {:still-texture nil
                :flowing-texture nil
                :overlay-texture nil
                :tint-color -1
                :is-translucent false}
               (update rendering :tint-color (fn [c] (normalize-hex-color (or c -1))))))
      (map->FluidBehaviorProperties
        (merge {:slope-find-distance 4
                :level-decrease-per-block 1
                :tick-rate 5
                :explosion-resistance 100.0}
               behavior))
      (map->FluidBlockProperties
        (merge {:block-id block-id
                :registry-name (default-registry-name block-id)
                :has-bucket? true
                :bucket-registry-name bucket-registry
                :bucket-item-id bucket-item-id}
               block))
      (or metadata {}))))

(defn register-fluid!
  [fluid-spec]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:registry :fluids (:id fluid-spec)] fluid-spec))
  fluid-spec)

(defn get-fluid
  [fluid-id]
  (get-in @(fw/fw-atom) [:registry :fluids (str fluid-id)]))

(defn list-fluids
  []
  (keys (get-in @(fw/fw-atom) [:registry :fluids])))

