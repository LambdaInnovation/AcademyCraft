(ns cn.li.ac.ability.model.preset
  "Pure-data functions for PresetData.

  4 presets × 4 key slots. Each slot holds a controllable pair [cat-id ctrl-id]
  or nil. Sparse encoding: only non-nil entries stored.

  Map schema:
    {:active-preset  int       ; 0–3
     :slots         {[preset-idx key-idx] [cat-id ctrl-id]}}"
  (:require [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Constructors
;; ============================================================================

(defn new-preset-data []
  {:active-preset 0
   :slots         {}})

;; ============================================================================
;; Active Preset
;; ============================================================================

(defn get-active-preset [d]
  (:active-preset d))

(defn set-active-preset [d idx]
  (when-not (and (>= idx 0) (<= idx 3))
    (throw (IllegalArgumentException. "set-active-preset: idx must be 0-3")))
  (assoc d :active-preset idx))

;; ============================================================================
;; Slot Access
;; ============================================================================

(defn get-slot [d preset-idx key-idx]
  (get (:slots d) [preset-idx key-idx]))

(defn set-slot [d preset-idx key-idx controllable]
  (if (nil? controllable)
    (update d :slots dissoc [preset-idx key-idx])
    (assoc-in d [:slots [preset-idx key-idx]] controllable)))

(defn clear-slots
  "Remove all preset slot bindings while preserving the active preset index."
  [d]
  (assoc d :slots {}))

(defn get-active-slots
  "Return vec of 4 controllables (or nil) for the active preset."
  [d]
  (let [p (:active-preset d)]
    (mapv #(get (:slots d) [p %]) (range 4))))

;; ============================================================================
;; Serialization
;; ============================================================================

(defn preset-data->vec
  "Serialize to vector of [preset-idx key-idx cat-id ctrl-id]."
  [d]
  (mapv (fn [[[pi ki] [cat ctrl]]]
          [pi ki (name cat) (name ctrl)])
        (:slots d)))

(defn vec->preset-data
  "Deserialize. active-preset defaults to 0."
  ([coll] (vec->preset-data coll 0))
  ([coll active-preset]
   {:active-preset active-preset
    :slots (->> coll
                (reduce (fn [acc [pi ki cat ctrl]]
                          (assoc acc [(int pi) (int ki)]
                                 [(keyword cat) (keyword ctrl)]))
                        {}))}))
