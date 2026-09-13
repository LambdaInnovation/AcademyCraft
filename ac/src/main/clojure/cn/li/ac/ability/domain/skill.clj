(ns cn.li.ac.ability.domain.skill
  "Pure ability skill domain helpers."
  (:require [cn.li.ac.ability.domain.category :as category]))

(defn ability-skill
  "Create a normalized skill map."
  [{:keys [id category-id level] :as spec}]
  (when-not (and (keyword? id) (keyword? category-id) (integer? level))
    (throw (IllegalArgumentException. "normalize-skill-spec: id & category-id must be keywords, level must be integer")))
  (merge {:controllable? true
          :enabled true
          :prerequisites []
          :conditions []
          :actions {}
          :pattern nil}
         spec
         {:id id
          :category-id category-id
          :level level
          :controllable? (boolean (:controllable? spec true))
          :enabled (boolean (:enabled spec true))}))

(defn ability-skill?
  [value]
  (and (map? value)
       (keyword? (:id value))
       (keyword? (:category-id value))
       (integer? (:level value))))

(defn controllable?
  [skill]
  (boolean (and (:enabled skill) (:controllable? skill))))

(defn full-id
  [skill]
  (str (name (:category-id skill)) "/" (name (:id skill))))

(defn controllable-key
  [skill]
  [(:category-id skill) (or (:ctrl-id skill) (:id skill))])

(defn matches-category?
  [skill cat-id]
  (= (:category-id skill) cat-id))

(defn prerequisite-ids
  [skill]
  (mapv #(get % :skill-id) (:prerequisites skill [])))
