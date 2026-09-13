(ns cn.li.mcmod.ui.kinds-lint
  "Static validation for node/kinds table — CI only, zero render-path cost."
  (:require [clojure.set :as set]))

(def ^:private backend-min-base
  "Minimum :oslots-backend-base per kind (matches mc1201 bake slot constants)."
  {:image 2
   :text 8
   :progress 8})

(defn- slot-array-size
  [kdef]
  (+ (long (:oslots-backend-base kdef 0)) 4))

(defn- unique-indices?
  [slot-map label kind errors]
  (let [idxs (vals slot-map)]
    (if (= (count idxs) (count (set idxs)))
      errors
      (conj errors (str kind " " label " has duplicate slot indices")))))

(defn- non-negative-indices?
  [slot-map label kind errors]
  (if (every? #(and (int? %) (>= % 0)) (vals slot-map))
    errors
    (conj errors (str kind " " label " has invalid slot index"))))

(defn- lint-prop-writer-spec
  [kind kdef prop-key spec errors]
  (let [required #{:slot :idx :sig}
        missing (set/difference required (set (keys spec)))]
    (cond
      (seq missing)
      (conj errors (str kind " :prop-writers/" prop-key " missing keys: " (vec missing)))

      (not (#{:dslot :oslot} (:slot spec)))
      (conj errors (str kind " :prop-writers/" prop-key " :slot must be :dslot or :oslot"))

      (not (#{:d :o} (:sig spec)))
      (conj errors (str kind " :prop-writers/" prop-key " :sig must be :d or :o"))

      :else
      (let [idx (:idx spec)
            cap (slot-array-size kdef)]
        (cond
          (not (int? idx))
          (conj errors (str kind " :prop-writers/" prop-key " :idx must be int"))

          (or (< idx 0) (>= idx cap))
          (conj errors (str kind " :prop-writers/" prop-key " :idx " idx " out of range [0," cap ")"))

          :else
          (let [dslot-idx (get-in kdef [:dslots prop-key])
                oslot-idx (get-in kdef [:oslots prop-key])]
            (cond
              (and (= (:slot spec) :dslot) (some? dslot-idx) (not= idx dslot-idx))
              (conj errors (str kind " :prop-writers/" prop-key " dslot idx mismatch: spec=" idx " table=" dslot-idx))

              (and (= (:slot spec) :oslot) (some? oslot-idx) (not= idx oslot-idx))
              (conj errors (str kind " :prop-writers/" prop-key " oslot idx mismatch: spec=" idx " table=" oslot-idx))

              :else errors)))))))

(defn- lint-kind
  [kind kdef]
  (let [base (:oslots-backend-base kdef 0)
        min-base (get backend-min-base kind 0)]
    (as-> [] errors
      (cond-> errors (< base min-base)
        (conj (str kind " :oslots-backend-base " base " < required minimum " min-base)))
      (cond-> errors (< base (count (:oslots kdef)))
        (conj (str kind " :oslots-backend-base " base " < oslot count " (count (:oslots kdef)))))
      (unique-indices? (:dslots kdef) ":dslots" kind errors)
      (unique-indices? (:oslots kdef) ":oslots" kind errors)
      (non-negative-indices? (:dslots kdef) ":dslots" kind errors)
      (non-negative-indices? (:oslots kdef) ":oslots" kind errors)
      (reduce (fn [errs [prop-key spec]]
                (lint-prop-writer-spec kind kdef prop-key spec errs))
              errors
              (:prop-writers kdef)))))

(defn lint-kinds
  "Return a vector of error strings; empty when kinds table is valid."
  [kinds]
  (vec (mapcat (fn [[kind kdef]] (lint-kind kind kdef)) kinds)))

(defn lint-kinds!
  "Throw ex-info when kinds table is invalid."
  [kinds]
  (let [errors (lint-kinds kinds)]
    (when (seq errors)
      (throw (ex-info "Invalid UI kinds table" {:errors errors})))
    kinds))
