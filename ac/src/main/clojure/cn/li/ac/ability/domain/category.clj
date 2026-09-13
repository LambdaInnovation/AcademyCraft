(ns cn.li.ac.ability.domain.category
  "Pure ability category domain helpers.

  Category values remain plain maps for compatibility, but this namespace
  centralizes construction and validation in a domain-focused place."
  (:require [cn.li.ac.foundation.validation :as validation]))

(defn ability-category
  "Create a normalized ability category map."
  [{:keys [id name-key enabled prog-incr-rate level-matcher] :as spec}]
  (when-not (and (keyword? id) (string? name-key))
    (throw (IllegalArgumentException. "normalize-category-spec: id must be keyword, name-key must be string")))
  (merge {:enabled true
          :prog-incr-rate 1.0
          :level-matcher (fn [_] :normal)}
         spec
         {:id id
          :name-key name-key
          :enabled (if (contains? spec :enabled) (boolean enabled) true)
          :prog-incr-rate (double (or prog-incr-rate 1.0))
          :level-matcher (or level-matcher (fn [_] :normal))}))

(defn ability-category?
  [value]
  (and (map? value)
       (keyword? (:id value))
       (string? (:name-key value))))

(defn enabled?
  [category]
  (boolean (:enabled category true)))

(defn prog-incr-rate
  [category]
  (double (:prog-incr-rate category 1.0)))

(defn match-level
  [category level]
  ((or (:level-matcher category) (fn [_] :normal)) level))
